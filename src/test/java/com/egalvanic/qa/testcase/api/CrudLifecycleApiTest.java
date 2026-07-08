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

/**
 * <b>Authenticated CRUD lifecycle audit</b> — the gap every other Suite-3 class leaves open.
 *
 * <p>The full-catalog health check probes mutations <i>unauthenticated</i> (auth-gate only) and
 * the contract/pagination/error classes are GET-only. Nothing proved that an authenticated
 * create → read → update → delete round-trip actually works and stays consistent. This class
 * does, for three entities whose real payload contracts were captured from the frontend with
 * Playwright on 2026-07-08 (swagger marks these bodies "contract not yet pinned", so the UI
 * traffic IS the contract):</p>
 * <ol>
 *   <li><b>Task</b> — full CRUD: {@code POST /task/create}, {@code GET /task/{id}},
 *       {@code PUT /task/update/{id}} (partial ok), {@code DELETE /task/delete/{id}}
 *       (soft; requires a JSON Content-Type or the server 415s).</li>
 *   <li><b>Issue</b> — no DELETE endpoint exists: create/read/update plus soft-delete via
 *       {@code PUT {"is_deleted":true}}; list-visibility checked through the (properly
 *       paginated) {@code POST /v2/issues/list}.</li>
 *   <li><b>Building</b> — {@code POST /location/building/} (envelope {@code {building:{…}}}),
 *       partial PUT, hard DELETE is 405 by design, soft-delete via {@code is_deleted}.</li>
 * </ol>
 *
 * <p><b>Write-path note:</b> the backend has a dual write path — with the frontend's
 * {@code x-direct-write: true} header a mutation applies synchronously and returns the full
 * object; without it the API queues the mutation and answers
 * {@code {"_mutation":{"status":"received"}}}. This class uses direct-write (same as the UI)
 * so assertions are deterministic; the async path is audited by
 * {@link MutationSemanticsApiTest}.</p>
 *
 * <p><b>Safety:</b> mutations run ONLY on the dedicated sandbox SLD named
 * "{@value #SANDBOX_SITE_NAME}" (skip if absent — never write to a real site). Every record
 * carries the {@value #MARKER} prefix, cleanup runs in {@code @AfterClass} even when an
 * assertion failed, and each run first sweeps {@value #MARKER}* leftovers that a previously
 * crashed run may have stranded.</p>
 *
 * <p><b>Failure policy:</b> lifecycle steps are objective — any 5xx, a create that returns no
 * id, an update that does not stick, or a delete that leaves the record visible is a hard
 * fail. Contract observations that are choices rather than defects (e.g. GET returning 200
 * for a soft-deleted record) go to {@code reports/crud-lifecycle-report.md} as SOFT rows.</p>
 */
public class CrudLifecycleApiTest extends BaseAPITest {

    static final String SANDBOX_SITE_NAME = "test site for api check";
    static final String MARKER = "ApiCrud_";

    private static final String MODULE = "API — CRUD Lifecycle";
    /** list-visibility can lag a write by a moment even on the direct path — bounded poll. */
    private static final int  POLL_TRIES = 5;
    private static final long POLL_SLEEP_MS = 1500;

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    private String companyId;
    private String sandboxSldId;
    private final String runTag = MARKER + System.currentTimeMillis();

    // cleanup registry — entity type + id, drained in @AfterClass no matter what
    private final List<String[]> created = Collections.synchronizedList(new ArrayList<>());

    // ── setup: resolve the sandbox site BY NAME (never mutate a real site) ──

    @BeforeClass(alwaysRun = true)
    public void resolveSandbox() {
        if (!hasAuthToken()) return;
        try {
            Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
            if (me.statusCode() == 200 && !me.asString().trim().startsWith("<")) {
                companyId = new JSONObject(me.asString()).optString("company_id", null);
            }
            if (companyId == null) return;
            Response slds = getAuthenticatedRequestSpec()
                    .when().get("/company/" + companyId + "/slds").then().extract().response();
            if (slds.statusCode() != 200) return;
            JSONArray arr = new JSONObject(slds.asString()).optJSONArray("slds");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                if (SANDBOX_SITE_NAME.equalsIgnoreCase(s.optString("name", "").trim())) {
                    sandboxSldId = s.optString("id", null);
                    break;
                }
            }
            System.out.println("[CrudLifecycle] sandbox sld: " + sandboxSldId);
            if (sandboxSldId != null) sweepLeftovers("pre-run");
        } catch (Exception e) {
            System.out.println("[CrudLifecycle] sandbox resolve failed: " + e.getMessage());
        }
    }

    private void requireSandbox() {
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        if (sandboxSldId == null) throw new SkipException(
                "Sandbox SLD '" + SANDBOX_SITE_NAME + "' not found — refusing to mutate a real site.");
    }

    /** Same write path as the frontend: synchronous, returns the full object. */
    private RequestSpecification directWriteSpec() {
        return getAuthenticatedRequestSpec().header("x-direct-write", "true");
    }

    // ── task lifecycle ───────────────────────────────────────────────────────

    @Test(description = "Task: authenticated create → read → list-visible → partial update → delete → gone")
    public void testTaskCrudLifecycle() {
        ExtentReportManager.createTest(MODULE, "Task lifecycle", "CRUD_TASK");
        requireSandbox();
        String title = runTag + "_task";

        // CREATE — exact payload the frontend sends
        JSONObject payload = new JSONObject()
                .put("sld_id", sandboxSldId).put("task_type", "PM").put("completed", false)
                .put("title", title).put("task_description", "CRUD lifecycle probe — auto-deleted");
        Response create = directWriteSpec().body(payload.toString())
                .when().post("/task/create").then().extract().response();
        Assert.assertTrue(create.statusCode() == 201 || create.statusCode() == 200,
                "task create expected 201, got " + create.statusCode() + ": " + trim(create));
        String id = new JSONObject(create.asString()).getJSONObject("data").optString("id", null);
        Assert.assertNotNull(id, "task create returned no id: " + trim(create));
        created.add(new String[]{"task", id});

        // READ — echo must match what we sent
        JSONObject got = readData("/task/" + id);
        Assert.assertEquals(got.optString("title"), title, "task GET title mismatch after create");
        Assert.assertEquals(got.optString("sld_id"), sandboxSldId, "task GET sld_id mismatch");
        Assert.assertFalse(got.optBoolean("is_deleted", false), "fresh task is already is_deleted=true");

        // LIST visibility
        Assert.assertTrue(pollTaskInList(id, true), "created task never appeared in GET /tasks/{sld}");

        // UPDATE — partial PUT (title only) must stick and not clobber other fields
        String edited = title + "_EDITED";
        Response upd = directWriteSpec().body(new JSONObject().put("title", edited).toString())
                .when().put("/task/update/" + id).then().extract().response();
        Assert.assertEquals(upd.statusCode(), 200, "task update failed: " + trim(upd));
        JSONObject afterUpd = readData("/task/" + id);
        Assert.assertEquals(afterUpd.optString("title"), edited, "task update did not stick");
        Assert.assertEquals(afterUpd.optString("task_description"), "CRUD lifecycle probe — auto-deleted",
                "partial PUT clobbered an untouched field (task_description)");

        // DELETE — soft; server requires a JSON Content-Type on DELETE (415 otherwise)
        Response del = directWriteSpec().body("{}")
                .when().delete("/task/delete/" + id).then().extract().response();
        Assert.assertEquals(del.statusCode(), 200, "task delete failed: " + trim(del));
        JSONObject afterDel = readData("/task/" + id);
        Assert.assertTrue(afterDel.optBoolean("is_deleted", false),
                "task not marked is_deleted after DELETE");
        Assert.assertTrue(pollTaskInList(id, false), "deleted task still listed in GET /tasks/{sld}");
        REPORT.add(new String[]{"task", "OK",
                "create 201 → read → list-visible → partial-update stuck → delete → is_deleted + delisted"});
        ExtentReportManager.logPass("Task CRUD lifecycle clean (id " + id + ").");
    }

    // ── issue lifecycle (no DELETE endpoint — soft-delete contract) ─────────

    @Test(description = "Issue: authenticated create → read → v2-list-visible → update → soft-delete via is_deleted")
    public void testIssueCrudLifecycle() {
        ExtentReportManager.createTest(MODULE, "Issue lifecycle", "CRUD_ISSUE");
        requireSandbox();
        String title = runTag + "_issue";

        // resolve an issue class + its detail properties (the UI sends one detail row per property)
        Response classes = getAuthenticatedRequestSpec().when().get("/issue_classes").then().extract().response();
        Assert.assertEquals(classes.statusCode(), 200, "GET /issue_classes failed");
        JSONArray classArr = new JSONArray(classes.asString());
        Assert.assertTrue(classArr.length() > 0, "no issue classes available");
        JSONObject issueClass = classArr.getJSONObject(0);
        for (int i = 0; i < classArr.length(); i++) {
            if ("Repair Needed".equalsIgnoreCase(classArr.getJSONObject(i).optString("name"))) {
                issueClass = classArr.getJSONObject(i);
                break;
            }
        }
        JSONArray details = new JSONArray();
        JSONArray defs = issueClass.optJSONArray("definition");
        if (defs != null) {
            for (int i = 0; i < defs.length(); i++) {
                JSONObject p = defs.getJSONObject(i);
                details.put(new JSONObject()
                        .put("id", p.optString("id")).put("issue_class_property", p.optString("id"))
                        .put("value", "").put("name", p.optString("name")));
            }
        }

        // CREATE — exact payload shape the frontend sends
        JSONObject payload = new JSONObject()
                .put("sld_id", sandboxSldId).put("status", "Open").put("priority", "Medium")
                .put("immediate_hazard", false).put("customer_notified", false)
                .put("ir_photo_ids", new JSONArray())
                .put("issue_class", issueClass.optString("id"))
                .put("title", title).put("description", "CRUD lifecycle probe — auto-deleted")
                .put("proposed_resolution", "soft-deleted by automation")
                .put("details", details);
        Response create = directWriteSpec().body(payload.toString())
                .when().post("/issue/create").then().extract().response();
        Assert.assertTrue(create.statusCode() == 201 || create.statusCode() == 200,
                "issue create expected 201, got " + create.statusCode() + ": " + trim(create));
        String id = new JSONObject(create.asString()).getJSONObject("data").optString("id", null);
        Assert.assertNotNull(id, "issue create returned no id: " + trim(create));
        created.add(new String[]{"issue", id});

        // READ
        JSONObject got = readData("/issue/" + id);
        Assert.assertEquals(got.optString("title"), title, "issue GET title mismatch");
        Assert.assertEquals(got.optString("status"), "Open", "issue GET status mismatch");

        // v2 LIST visibility via search (the paginated list the app actually uses)
        Assert.assertTrue(pollIssueInV2List(title, true), "created issue never appeared in /v2/issues/list search");

        // UPDATE — priority change must stick
        Response upd = directWriteSpec().body(new JSONObject().put("priority", "High").toString())
                .when().put("/issue/update/" + id).then().extract().response();
        Assert.assertEquals(upd.statusCode(), 200, "issue update failed: " + trim(upd));
        Assert.assertEquals(readData("/issue/" + id).optString("priority"), "High",
                "issue update did not stick");

        // SOFT-DELETE — the only delete path an issue has
        Response del = directWriteSpec().body(new JSONObject().put("is_deleted", true).toString())
                .when().put("/issue/update/" + id).then().extract().response();
        Assert.assertEquals(del.statusCode(), 200, "issue soft-delete failed: " + trim(del));
        Assert.assertTrue(pollIssueInV2List(title, false), "soft-deleted issue still visible in /v2/issues/list");

        // contract observation, not a defect: GET keeps serving the soft-deleted record
        int getAfter = getAuthenticatedRequestSpec().when().get("/issue/" + id)
                .then().extract().response().statusCode();
        REPORT.add(new String[]{"issue", "OK",
                "create 201 → read → v2-list-visible → update stuck → is_deleted delists; GET-after-soft-delete=" + getAfter});
        ExtentReportManager.logPass("Issue CRUD lifecycle clean (id " + id + ").");
    }

    // ── building lifecycle ───────────────────────────────────────────────────

    @Test(description = "Building: authenticated create → hierarchy-visible → partial update → soft-delete (hard DELETE is 405)")
    public void testBuildingCrudLifecycle() {
        ExtentReportManager.createTest(MODULE, "Building lifecycle", "CRUD_BUILDING");
        requireSandbox();
        String name = runTag + "_building";

        // CREATE — envelope is {building:{…}}, not {data:{…}}
        JSONObject payload = new JSONObject()
                .put("name", name).put("access_notes", "CRUD lifecycle probe — auto-deleted")
                .put("sld_id", sandboxSldId);
        Response create = directWriteSpec().body(payload.toString())
                .when().post("/location/building/").then().extract().response();
        Assert.assertTrue(create.statusCode() == 201 || create.statusCode() == 200,
                "building create failed: " + create.statusCode() + ": " + trim(create));
        JSONObject env = new JSONObject(create.asString());
        JSONObject building = env.optJSONObject("building");
        Assert.assertNotNull(building, "building create envelope missing {building:{…}}: " + trim(create));
        String id = building.optString("id", null);
        Assert.assertNotNull(id, "building create returned no id");
        created.add(new String[]{"building", id});

        // hierarchy visibility
        Assert.assertTrue(pollBuildingInHierarchy(id, true),
                "created building never appeared in GET /location/sld/{sld}");

        // UPDATE — partial PUT
        String edited = name + "_EDITED";
        Response upd = directWriteSpec().body(new JSONObject().put("name", edited).toString())
                .when().put("/location/building/" + id).then().extract().response();
        Assert.assertEquals(upd.statusCode(), 200, "building update failed: " + trim(upd));
        JSONObject updated = new JSONObject(upd.asString()).optJSONObject("building");
        Assert.assertNotNull(updated, "building update envelope missing {building}");
        Assert.assertEquals(updated.optString("name"), edited, "building rename did not stick");
        Assert.assertEquals(updated.optString("access_notes"), "CRUD lifecycle probe — auto-deleted",
                "partial PUT clobbered access_notes");

        // hard DELETE is intentionally unsupported — document, don't fight it
        int hardDel = directWriteSpec().body("{}")
                .when().delete("/location/building/" + id).then().extract().response().statusCode();

        // SOFT-DELETE
        Response del = directWriteSpec().body(new JSONObject().put("is_deleted", true).toString())
                .when().put("/location/building/" + id).then().extract().response();
        Assert.assertEquals(del.statusCode(), 200, "building soft-delete failed: " + trim(del));
        Assert.assertTrue(pollBuildingInHierarchy(id, false),
                "soft-deleted building still in GET /location/sld/{sld}");
        REPORT.add(new String[]{"building", "OK",
                "create → hierarchy-visible → partial-update stuck → soft-delete delists; hard-DELETE=" + hardDel + " (405 by design)"});
        ExtentReportManager.logPass("Building CRUD lifecycle clean (id " + id + ").");
    }

    // ── read/poll helpers ────────────────────────────────────────────────────

    /** GET a {data:{…}} endpoint, hard-failing on non-200/non-JSON. */
    private JSONObject readData(String path) {
        Response r = getAuthenticatedRequestSpec().when().get(path).then().extract().response();
        Assert.assertEquals(r.statusCode(), 200, "GET " + path + " failed: " + trim(r));
        String body = r.asString();
        Assert.assertFalse(body == null || body.trim().startsWith("<"),
                "GET " + path + " returned non-JSON (SPA shell?)");
        JSONObject o = new JSONObject(body);
        return o.optJSONObject("data") != null ? o.getJSONObject("data") : o;
    }

    private boolean pollTaskInList(String id, boolean expectPresent) {
        for (int t = 0; t < POLL_TRIES; t++) {
            try {
                Response r = getAuthenticatedRequestSpec()
                        .when().get("/tasks/" + sandboxSldId).then().extract().response();
                if (r.statusCode() == 200) {
                    JSONArray tasks = new JSONObject(r.asString()).optJSONArray("user_tasks");
                    boolean present = false;
                    for (int i = 0; tasks != null && i < tasks.length(); i++) {
                        JSONObject task = tasks.getJSONObject(i);
                        if (id.equals(task.optString("id")) && !task.optBoolean("is_deleted", false)) {
                            present = true;
                            break;
                        }
                    }
                    if (present == expectPresent) return true;
                }
            } catch (Exception ignored) {}
            sleep(POLL_SLEEP_MS);
        }
        return false;
    }

    private boolean pollIssueInV2List(String searchTitle, boolean expectPresent) {
        for (int t = 0; t < POLL_TRIES; t++) {
            try {
                JSONObject body = new JSONObject()
                        .put("company_id", companyId).put("sld_id", sandboxSldId)
                        .put("page", 1).put("page_size", 25)
                        .put("filters", new JSONObject()).put("search", searchTitle);
                Response r = getAuthenticatedRequestSpec().body(body.toString())
                        .when().post("/v2/issues/list").then().extract().response();
                if (r.statusCode() == 200) {
                    JSONArray items = new JSONObject(r.asString())
                            .getJSONObject("data").optJSONArray("items");
                    boolean present = items != null && items.length() > 0;
                    if (present == expectPresent) return true;
                }
            } catch (Exception ignored) {}
            sleep(POLL_SLEEP_MS);
        }
        return false;
    }

    private boolean pollBuildingInHierarchy(String id, boolean expectPresent) {
        for (int t = 0; t < POLL_TRIES; t++) {
            try {
                Response r = getAuthenticatedRequestSpec()
                        .when().get("/location/sld/" + sandboxSldId).then().extract().response();
                if (r.statusCode() == 200) {
                    JSONArray buildings = new JSONObject(r.asString()).optJSONArray("buildings");
                    boolean present = false;
                    for (int i = 0; buildings != null && i < buildings.length(); i++) {
                        JSONObject b = buildings.getJSONObject(i);
                        if (id.equals(b.optString("id")) && !b.optBoolean("is_deleted", false)) {
                            present = true;
                            break;
                        }
                    }
                    if (present == expectPresent) return true;
                }
            } catch (Exception ignored) {}
            sleep(POLL_SLEEP_MS);
        }
        return false;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String trim(Response r) {
        String b = r.asString();
        return b == null ? "<null>" : b.length() > 300 ? b.substring(0, 300) + "…" : b;
    }

    // ── cleanup: this run's records + strays from crashed runs ──────────────

    private void deleteQuietly(String type, String id) {
        try {
            switch (type) {
                case "task":
                    directWriteSpec().body("{}").when().delete("/task/delete/" + id);
                    break;
                case "issue":
                    directWriteSpec().body(new JSONObject().put("is_deleted", true).toString())
                            .when().put("/issue/update/" + id);
                    break;
                case "building":
                    directWriteSpec().body(new JSONObject().put("is_deleted", true).toString())
                            .when().put("/location/building/" + id);
                    break;
                default:
            }
        } catch (Exception e) {
            System.out.println("[CrudLifecycle] cleanup " + type + " " + id + " failed: " + e.getMessage());
        }
    }

    /** Remove ANY ApiCrud_* record on the sandbox — safety net for previously crashed runs. */
    private void sweepLeftovers(String phase) {
        int swept = 0;
        try {
            Response r = getAuthenticatedRequestSpec().when().get("/tasks/" + sandboxSldId)
                    .then().extract().response();
            if (r.statusCode() == 200) {
                JSONArray tasks = new JSONObject(r.asString()).optJSONArray("user_tasks");
                for (int i = 0; tasks != null && i < tasks.length(); i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    if (task.optString("title").startsWith(MARKER) && !task.optBoolean("is_deleted", false)) {
                        deleteQuietly("task", task.optString("id"));
                        swept++;
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            JSONObject body = new JSONObject()
                    .put("company_id", companyId).put("sld_id", sandboxSldId)
                    .put("page", 1).put("page_size", 50)
                    .put("filters", new JSONObject()).put("search", MARKER);
            Response r = getAuthenticatedRequestSpec().body(body.toString())
                    .when().post("/v2/issues/list").then().extract().response();
            if (r.statusCode() == 200) {
                JSONArray items = new JSONObject(r.asString()).getJSONObject("data").optJSONArray("items");
                for (int i = 0; items != null && i < items.length(); i++) {
                    deleteQuietly("issue", items.getJSONObject(i).optString("id"));
                    swept++;
                }
            }
        } catch (Exception ignored) {}
        try {
            Response r = getAuthenticatedRequestSpec().when().get("/location/sld/" + sandboxSldId)
                    .then().extract().response();
            if (r.statusCode() == 200) {
                JSONArray buildings = new JSONObject(r.asString()).optJSONArray("buildings");
                for (int i = 0; buildings != null && i < buildings.length(); i++) {
                    JSONObject b = buildings.getJSONObject(i);
                    if (b.optString("name").startsWith(MARKER) && !b.optBoolean("is_deleted", false)) {
                        deleteQuietly("building", b.optString("id"));
                        swept++;
                    }
                }
            }
        } catch (Exception ignored) {}
        if (swept > 0) System.out.println("[CrudLifecycle] " + phase + " sweep removed " + swept + " stray record(s)");
    }

    @AfterClass(alwaysRun = true)
    public void cleanupAndReport() {
        for (String[] rec : created) deleteQuietly(rec[0], rec[1]);
        if (sandboxSldId != null) sweepLeftovers("post-run");
        try {
            new File("reports").mkdirs();
            StringBuilder md = new StringBuilder("# CRUD Lifecycle Audit\n\n"
                    + "Sandbox: " + SANDBOX_SITE_NAME + " (" + sandboxSldId + ")\n\n"
                    + "| Entity | Result | Detail |\n|---|---|---|\n");
            for (String[] row : REPORT) md.append("| ").append(row[0]).append(" | ")
                    .append(row[1]).append(" | ").append(row[2]).append(" |\n");
            try (FileWriter w = new FileWriter("reports/crud-lifecycle-report.md")) { w.write(md.toString()); }
            System.out.println("[CrudLifecycle] wrote reports/crud-lifecycle-report.md");
        } catch (Exception e) {
            System.out.println("[CrudLifecycle] report write failed: " + e.getMessage());
        }
    }
}
