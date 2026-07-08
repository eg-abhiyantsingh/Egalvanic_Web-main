package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <b>Input-validation contract audit</b> — how the WRITE APIs behave on bad authenticated input.
 *
 * <p>{@link ErrorContractApiTest} proves GETs never crash on malformed path params; this is its
 * mutation-surface sibling. Every probe here was reproduced with curl on 2026-07-08 before being
 * encoded (per the don't-over-report rule), and {@code POST /location/building/} is used as the
 * control: it answers a clean {@code 400 {"error":"Missing required field: name"}}, which proves
 * the backend <i>can</i> validate — so any endpoint that instead 500s or leaks is a real gap, not
 * a platform limitation.</p>
 *
 * <ol>
 *   <li><b>Malformed body never 5xx</b> (hard) — {@code {}}, wrong JSON type, a top-level array,
 *       and a non-UUID in a UUID column are all client errors. A 500 means an unhandled exception
 *       reached the DB driver / interpreter. Probes chosen to fail <i>before</i> any row is
 *       inserted, so this test creates nothing.</li>
 *   <li><b>No internal-detail leakage</b> (hard, security) — no error body may contain
 *       {@code psycopg2}, a Python traceback, a raw SQL fragment, or a source path. Leaking the
 *       schema ({@code column "sld_id" of relation "tasks"}) or driver internals to any caller is
 *       an information-disclosure defect (OWASP API8: Security Misconfiguration).</li>
 *   <li><b>Abusive list params never 5xx</b> (hard) — {@code POST /v2/issues/list} with
 *       {@code page=-1}, {@code page="abc"}, oversized {@code page_size}. Read-only; must degrade
 *       to a 4xx or a clamped 200, never a SQL {@code OFFSET must not be negative} 500.</li>
 *   <li><b>Mutation on unknown id</b> (hard on 5xx) — {@code PUT} with a random and a malformed id
 *       must not 5xx; a {@code 200 + HTML} SPA-shell reply (client cannot tell the write was a
 *       no-op) is reported.</li>
 *   <li><b>Required-field / enum enforcement</b> (soft, STRICT-gated, self-cleaning) — the one
 *       mutating test: create with a required field omitted or an invalid enum. Whatever the
 *       server returns (4xx = enforced, 201 = accepted) is reported; any record it creates is
 *       captured and deleted inline, and {@code @AfterClass} sweeps {@value #MARKER}* strays.</li>
 * </ol>
 *
 * <p><b>Failure policy</b> follows the Suite-3 convention set by {@link ListApiContractApiTest}:
 * <i>report by default, enforce under a gate</i>. Every finding — a 5xx on client input, an
 * internal-detail leak, over-permissive acceptance, SPA-shell masking — is written to
 * {@code reports/input-validation-report.md} and logged as a loud WARNING, but the daily monitor
 * stays green unless {@code -DSTRICT_INPUT_VALIDATION=true}, which escalates all findings to hard
 * failures (use it as the enforcement gate once the backend complies / to prevent regression).
 * The 5xx and leakage rows were reproduced with curl on 2026-07-08 and are real backend defects,
 * not test artifacts — the report labels them {@code DEFECT}.</p>
 */
public class InputValidationApiTest extends BaseAPITest {

    static final String SANDBOX_SITE_NAME = CrudLifecycleApiTest.SANDBOX_SITE_NAME;
    static final String MARKER = "ApiInval_";

    private static final String MODULE = "API — Input Validation";
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_INPUT_VALIDATION",
                    System.getenv().getOrDefault("STRICT_INPUT_VALIDATION", "false")));

    /** Substrings that must NEVER appear in a response body served to a client (package-shared). */
    static final Pattern LEAK = Pattern.compile(
            "psycopg2|sqlalchemy|Traceback \\(most recent call last\\)|File \"/|"
            + "relation \"|column \"|SELECT .* FROM |line \\d+, in |InvalidTextRepresentation|"
            + "NotNullViolation|ProgrammingError|OperationalError",
            Pattern.CASE_INSENSITIVE);

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    private String companyId;
    private String sandboxSldId;
    private String issueClassId;
    private final String runTag = MARKER + System.currentTimeMillis();
    private final List<String[]> created = Collections.synchronizedList(new ArrayList<>());

    // ── setup ────────────────────────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    public void resolveContext() {
        if (!hasAuthToken()) return;
        try {
            Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
            if (me.statusCode() == 200 && !me.asString().trim().startsWith("<")) {
                companyId = new JSONObject(me.asString()).optString("company_id", null);
            }
            if (companyId == null) return;
            Response slds = getAuthenticatedRequestSpec()
                    .when().get("/company/" + companyId + "/slds").then().extract().response();
            if (slds.statusCode() == 200) {
                JSONArray arr = new JSONObject(slds.asString()).optJSONArray("slds");
                for (int i = 0; arr != null && i < arr.length(); i++) {
                    if (SANDBOX_SITE_NAME.equalsIgnoreCase(arr.getJSONObject(i).optString("name", "").trim())) {
                        sandboxSldId = arr.getJSONObject(i).optString("id", null);
                        break;
                    }
                }
            }
            Response cls = getAuthenticatedRequestSpec().when().get("/issue_classes").then().extract().response();
            if (cls.statusCode() == 200 && cls.asString().trim().startsWith("[")) {
                JSONArray ca = new JSONArray(cls.asString());
                if (ca.length() > 0) issueClassId = ca.getJSONObject(0).optString("id", null);
            }
            System.out.println("[InputValidation] sandbox=" + sandboxSldId + " issueClass=" + issueClassId);
        } catch (Exception e) {
            System.out.println("[InputValidation] setup failed: " + e.getMessage());
        }
    }

    private RequestSpecification directWriteSpec() {
        return getAuthenticatedRequestSpec().header("x-direct-write", "true");
    }

    private void requireAuth() {
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
    }

    private void requireSandbox() {
        requireAuth();
        if (sandboxSldId == null) throw new SkipException(
                "Sandbox SLD '" + SANDBOX_SITE_NAME + "' not found — refusing to mutate a real site.");
    }

    private static String body(Response r) {
        String b = r.asString();
        return b == null ? "" : b;
    }

    private static String snippet(Response r) {
        String b = body(r).replaceAll("\\s+", " ").trim();
        return b.length() > 160 ? b.substring(0, 160) + "…" : b;
    }

    /**
     * Report-by-default gate (mirrors {@link ListApiContractApiTest}): under STRICT a finding is a
     * hard fail; otherwise it is a loud WARNING and the monitor stays green. {@code okOrNoFinding}
     * true = nothing to report.
     */
    private void enforce(boolean okOrNoFinding, String message) {
        if (STRICT) Assert.assertTrue(okOrNoFinding, message);
        else if (!okOrNoFinding) ExtentReportManager.logWarning("[would fail under STRICT] " + message);
    }

    // ── 1. malformed body → never 5xx (creates nothing) ────────────────────────

    @Test(description = "Validation: malformed request bodies yield 4xx, never a 5xx server crash")
    public void testMalformedBodyNeverCrashes() {
        ExtentReportManager.createTest(MODULE, "Malformed body never 5xx", "INVAL_NO_5XX");
        requireAuth();

        // Each probe is crafted to fail at parse / type-cast — BEFORE any DB insert.
        List<String[]> probes = new ArrayList<>();
        probes.add(new String[]{"POST", "/task/create", "{}"});
        probes.add(new String[]{"POST", "/task/create", "{\"sld_id\":\"not-a-uuid\",\"task_type\":\"PM\",\"title\":\"x\"}"});
        probes.add(new String[]{"POST", "/task/create", "[1,2,3]"});
        probes.add(new String[]{"POST", "/task/create", "{\"title\": "});          // truncated JSON
        probes.add(new String[]{"POST", "/location/building/", "{}"});             // control: clean 400
        probes.add(new String[]{"POST", "/location/building/", "{\"name\":\"x\",\"sld_id\":\"not-a-uuid\"}"});
        if (issueClassId != null && sandboxSldId != null) {
            probes.add(new String[]{"POST", "/issue/create",
                    "{\"sld_id\":\"" + sandboxSldId + "\",\"title\":\"x\",\"issue_class\":\"not-a-uuid\",\"status\":\"Open\"}"});
        }

        List<String> crashes = new ArrayList<>();
        StringBuilder tally = new StringBuilder();
        for (String[] p : probes) {
            Response r = fire(p[0], p[1], p[2]);
            int c = r.statusCode();
            tally.append(p[1]).append("=").append(c).append(' ');
            if (c >= 500) crashes.add(p[0] + " " + p[1] + " → " + c + " : " + snippet(r));
            // safety: if a probe unexpectedly created a row, capture its id for cleanup
            captureIfCreated(p[1], r);
        }
        REPORT.add(new String[]{"malformed-body-5xx", crashes.isEmpty() ? "OK" : "DEFECT", tally.toString().trim()});
        enforce(crashes.isEmpty(),
                "Malformed bodies must yield 4xx, never 5xx (unhandled exception on client input). "
                + "Backend proves it can (building/create → 400); these do not:\n  " + String.join("\n  ", crashes));
        ExtentReportManager.logInfo(probes.size() + " malformed-body probes; 5xx=" + crashes.size());
    }

    // ── 2. no internal-detail leakage (security) ───────────────────────────────

    @Test(description = "Validation: error bodies never leak DB driver / SQL / traceback internals")
    public void testNoServerErrorLeakage() {
        ExtentReportManager.createTest(MODULE, "No internal leakage", "INVAL_NO_LEAK");
        requireAuth();

        List<String[]> probes = new ArrayList<>();
        probes.add(new String[]{"POST", "/task/create", "{}"});
        probes.add(new String[]{"POST", "/task/create", "{\"sld_id\":\"not-a-uuid\",\"task_type\":\"PM\",\"title\":\"x\"}"});
        probes.add(new String[]{"POST", "/task/create", "[1,2,3]"});
        if (companyId != null && sandboxSldId != null) {
            probes.add(new String[]{"POST", "/v2/issues/list",
                    "{\"company_id\":\"" + companyId + "\",\"sld_id\":\"" + sandboxSldId + "\",\"page\":-1}"});
            probes.add(new String[]{"POST", "/v2/issues/list",
                    "{\"company_id\":\"" + companyId + "\",\"sld_id\":\"" + sandboxSldId + "\",\"page\":\"abc\"}"});
        }
        if (issueClassId != null && sandboxSldId != null) {
            probes.add(new String[]{"POST", "/issue/create",
                    "{\"sld_id\":\"" + sandboxSldId + "\",\"title\":\"x\",\"issue_class\":\"not-a-uuid\",\"status\":\"Open\"}"});
        }

        List<String> leaks = new ArrayList<>();
        for (String[] p : probes) {
            Response r = fire(p[0], p[1], p[2]);
            java.util.regex.Matcher m = LEAK.matcher(body(r));
            if (m.find()) leaks.add(p[1] + " (HTTP " + r.statusCode() + ") leaked: \"" + m.group() + "\" — " + snippet(r));
            captureIfCreated(p[1], r);
        }
        REPORT.add(new String[]{"internal-leakage", leaks.isEmpty() ? "OK" : "DEFECT",
                probes.size() + " probes, leaks=" + leaks.size()});
        enforce(leaks.isEmpty(),
                "Error responses leak DB/stack internals to an authenticated client (info disclosure, OWASP API8). "
                + "Return a generic 4xx instead:\n  " + String.join("\n  ", leaks));
        ExtentReportManager.logInfo(probes.size() + " error probes; internal-detail leaks=" + leaks.size());
    }

    // ── 3. abusive list params → never 5xx (read-only) ─────────────────────────

    @Test(description = "Validation: abusive pagination params on /v2/issues/list never 5xx")
    public void testAbusiveListParamsNeverCrash() {
        ExtentReportManager.createTest(MODULE, "Abusive list params never 5xx", "INVAL_LIST_ABUSE");
        requireAuth();
        if (companyId == null || sandboxSldId == null)
            throw new SkipException("company/sandbox context unavailable.");

        String base = "\"company_id\":\"" + companyId + "\",\"sld_id\":\"" + sandboxSldId + "\"";
        String[] bodies = {
                "{" + base + ",\"page\":-1,\"page_size\":25}",
                "{" + base + ",\"page\":\"abc\"}",
                "{" + base + ",\"page\":1,\"page_size\":999999}",
                "{" + base + ",\"page\":0,\"page_size\":-5}",
                "{" + base + ",\"page_size\":\"lots\"}",
        };
        List<String> crashes = new ArrayList<>();
        StringBuilder tally = new StringBuilder();
        for (String b : bodies) {
            Response r = fire("POST", "/v2/issues/list", b);
            tally.append(r.statusCode()).append(' ');
            if (r.statusCode() >= 500) crashes.add(b + " → " + r.statusCode() + " : " + snippet(r));
        }
        REPORT.add(new String[]{"abusive-list-params", crashes.isEmpty() ? "OK" : "DEFECT",
                bodies.length + " bodies, statuses: " + tally.toString().trim()});
        enforce(crashes.isEmpty(),
                "Abusive pagination params must degrade to 4xx / clamped 200, never a SQL 5xx:\n  "
                + String.join("\n  ", crashes));
        ExtentReportManager.logInfo(bodies.length + " abusive list-param probes; 5xx=" + crashes.size());
    }

    // ── 4. mutation on unknown id → never 5xx; 200-HTML reported ────────────────

    @Test(description = "Validation: PUT on unknown/malformed id never 5xx (200+HTML SPA-shell reported)")
    public void testMutationOnUnknownIdContract() {
        ExtentReportManager.createTest(MODULE, "Mutation on unknown id", "INVAL_UNKNOWN_ID");
        requireAuth();

        String[][] probes = {
                {"/task/update/00000000-0000-0000-0000-000000000000", "{\"title\":\"x\"}"},
                {"/task/update/not-a-uuid", "{\"title\":\"x\"}"},
                {"/issue/update/00000000-0000-0000-0000-000000000000", "{\"priority\":\"High\"}"},
        };
        List<String> crashes = new ArrayList<>(); List<String> htmlShell = new ArrayList<>();
        for (String[] p : probes) {
            Response r = directWriteSpec().body(p[1]).when().put(p[0]).then().extract().response();
            int c = r.statusCode();
            String b = body(r).trim();
            if (c >= 500) crashes.add(p[0] + " → " + c);
            else if (c == 200 && b.startsWith("<")) htmlShell.add(p[0]);
        }
        String detail = "5xx=" + crashes.size() + " html-200=" + htmlShell.size() + "/" + probes.length;
        REPORT.add(new String[]{"mutation-unknown-id",
                crashes.isEmpty() ? (htmlShell.isEmpty() ? "OK" : "SOFT-GAP") : "FAIL", detail});
        enforce(crashes.isEmpty(), "PUT on unknown/malformed id must not 5xx: " + crashes);
        enforce(htmlShell.isEmpty(),
                "PUT on an unknown id returns 200 + HTML SPA shell — client cannot detect the no-op: " + htmlShell);
        ExtentReportManager.logInfo("Mutation-on-unknown-id contract: " + detail);
    }

    // ── 5. required-field / enum enforcement (soft, self-cleaning) ─────────────

    @Test(description = "Validation: required-field & enum enforcement on create (reports accept-vs-reject)")
    public void testRequiredFieldAndEnumEnforcement() {
        ExtentReportManager.createTest(MODULE, "Required-field & enum enforcement", "INVAL_REQUIRED");
        requireSandbox();

        List<String[]> observations = new ArrayList<>();

        // (a) task with a valid sld_id but WRONG-TYPED task_type/title → is it rejected?
        observations.add(probeCreate("task: wrong field types (task_type=int, title=int)", "/task/create",
                new JSONObject().put("sld_id", sandboxSldId).put("task_type", 123).put("title", 456)));

        // (b) task missing the UI-required title (marker in description so the sweeper still catches it)
        observations.add(probeCreate("task: missing required 'title'", "/task/create",
                new JSONObject().put("sld_id", sandboxSldId).put("task_type", "PM")
                        .put("task_description", runTag + "_notitle")));

        if (issueClassId != null) {
            // (c) issue with an invalid status enum
            observations.add(probeCreate("issue: invalid status enum 'BOGUS_STATUS'", "/issue/create",
                    new JSONObject().put("sld_id", sandboxSldId).put("issue_class", issueClassId)
                            .put("title", runTag + "_badenum").put("status", "BOGUS_STATUS")
                            .put("priority", "Medium").put("proposed_resolution", "x")
                            .put("ir_photo_ids", new JSONArray()).put("details", new JSONArray())));

            // (d) issue missing the UI-required issue_class
            observations.add(probeCreate("issue: missing required 'issue_class'", "/issue/create",
                    new JSONObject().put("sld_id", sandboxSldId).put("title", runTag + "_noclass")
                            .put("status", "Open").put("priority", "Medium")
                            .put("proposed_resolution", "x").put("ir_photo_ids", new JSONArray())));
        }

        int accepted = 0, rejected = 0, crashed = 0;
        StringBuilder detail = new StringBuilder();
        for (String[] o : observations) {   // o = {label, verdict, statusCode}
            detail.append("- ").append(o[0]).append(": ").append(o[1]).append('\n');
            switch (o[1].split(" ")[0]) {
                case "ACCEPTED": accepted++; break;
                case "REJECTED": rejected++; break;
                default: crashed++;
            }
        }
        REPORT.add(new String[]{"required-field-enforcement",
                crashed > 0 ? "DEFECT" : (accepted > 0 ? "SOFT-GAP" : "OK"),
                "accepted=" + accepted + " rejected=" + rejected + " crashed=" + crashed});

        enforce(crashed == 0, "create with invalid input 5xx'd (should be 4xx):\n" + detail);
        enforce(accepted == 0,
                "Server accepted invalid/incomplete create payloads (no server-side validation):\n" + detail);
        ExtentReportManager.logInfo("Enforcement summary — accepted=" + accepted
                + " rejected=" + rejected + " crashed=" + crashed);
    }

    // ── probe + capture helpers ────────────────────────────────────────────────

    private Response fire(String method, String path, String rawBody) {
        RequestSpecification spec = directWriteSpec().body(rawBody);
        return ("POST".equals(method) ? spec.when().post(path) : spec.when().put(path))
                .then().extract().response();
    }

    /** Returns {label, "ACCEPTED nnn"|"REJECTED nnn"|"CRASH nnn", statusCode}; deletes any created row. */
    private String[] probeCreate(String label, String path, JSONObject payload) {
        Response r = directWriteSpec().body(payload.toString()).when().post(path).then().extract().response();
        int c = r.statusCode();
        captureIfCreated(path, r);
        String verdict;
        if (c >= 500) verdict = "CRASH " + c;
        else if (c >= 200 && c < 300) verdict = "ACCEPTED " + c;
        else verdict = "REJECTED " + c;
        return new String[]{label, verdict, String.valueOf(c)};
    }

    /** If a create response carries an id, register it for cleanup so no probe leaves residue. */
    private void captureIfCreated(String path, Response r) {
        int c = r.statusCode();
        if (c < 200 || c >= 300) return;
        String b = body(r).trim();
        if (!b.startsWith("{")) return;
        try {
            JSONObject o = new JSONObject(b);
            JSONObject data = o.optJSONObject("data");
            String id = data != null ? data.optString("id", null) : null;
            if (id == null && o.optJSONObject("building") != null) {
                created.add(new String[]{"building", o.getJSONObject("building").optString("id")});
                return;
            }
            if (id == null) return;
            if (path.contains("/task/")) created.add(new String[]{"task", id});
            else if (path.contains("/issue")) created.add(new String[]{"issue", id});
        } catch (Exception ignored) {}
    }

    private void deleteQuietly(String type, String id) {
        if (id == null || id.isEmpty()) return;
        try {
            switch (type) {
                case "task":
                    directWriteSpec().body("{}").when().delete("/task/delete/" + id); break;
                case "issue":
                    directWriteSpec().body(new JSONObject().put("is_deleted", true).toString())
                            .when().put("/issue/update/" + id); break;
                case "building":
                    directWriteSpec().body(new JSONObject().put("is_deleted", true).toString())
                            .when().put("/location/building/" + id); break;
                default:
            }
        } catch (Exception ignored) {}
    }

    @AfterClass(alwaysRun = true)
    public void cleanupAndReport() {
        // 1. delete everything captured this run
        for (String[] rec : created) deleteQuietly(rec[0], rec[1]);
        // 2. sweep marker + junk-title strays a crashed run may have left on the sandbox
        if (sandboxSldId != null) {
            try {
                Response r = getAuthenticatedRequestSpec().when().get("/tasks/" + sandboxSldId)
                        .then().extract().response();
                if (r.statusCode() == 200) {
                    JSONArray tasks = new JSONObject(r.asString()).optJSONArray("user_tasks");
                    for (int i = 0; tasks != null && i < tasks.length(); i++) {
                        JSONObject t = tasks.getJSONObject(i);
                        String title = t.optString("title", "");
                        if (!t.optBoolean("is_deleted", false)
                                && (title.startsWith(MARKER) || title.equals("456")))
                            deleteQuietly("task", t.optString("id"));
                    }
                }
            } catch (Exception ignored) {}
            if (companyId != null) try {
                JSONObject b = new JSONObject().put("company_id", companyId).put("sld_id", sandboxSldId)
                        .put("page", 1).put("page_size", 50).put("filters", new JSONObject()).put("search", MARKER);
                Response r = getAuthenticatedRequestSpec().body(b.toString())
                        .when().post("/v2/issues/list").then().extract().response();
                if (r.statusCode() == 200) {
                    JSONArray items = new JSONObject(r.asString()).getJSONObject("data").optJSONArray("items");
                    for (int i = 0; items != null && i < items.length(); i++)
                        deleteQuietly("issue", items.getJSONObject(i).optString("id"));
                }
            } catch (Exception ignored) {}
        }
        // 3. write report
        try {
            new File("reports").mkdirs();
            StringBuilder md = new StringBuilder("# Input-Validation Audit\n\n"
                    + "Sandbox: " + SANDBOX_SITE_NAME + " (" + sandboxSldId + ")  ·  STRICT_INPUT_VALIDATION=" + STRICT
                    + "\n\n_`DEFECT` = real backend gap (curl-verified); `SOFT-GAP` = over-permissive/masking; "
                    + "report-by-default, hard-fails only under `-DSTRICT_INPUT_VALIDATION=true`._\n"
                    + "\n| Probe | Result | Detail |\n|---|---|---|\n");
            for (String[] row : REPORT) md.append("| ").append(row[0]).append(" | ")
                    .append(row[1]).append(" | ").append(row[2].replace("\n", "<br>")).append(" |\n");
            try (FileWriter w = new FileWriter("reports/input-validation-report.md")) { w.write(md.toString()); }
            System.out.println("[InputValidation] wrote reports/input-validation-report.md");
        } catch (Exception e) {
            System.out.println("[InputValidation] report write failed: " + e.getMessage());
        }
    }
}
