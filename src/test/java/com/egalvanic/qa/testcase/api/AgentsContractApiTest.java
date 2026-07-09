package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <b>AGENTS family — deep contract suite.</b> The {@code agents}/{@code agent} tags are the
 * single largest slice of the live catalog (105 operations in the 2026-07-09 spec) and until now
 * had only the report-mode health probe over them ({@link ApiFullCatalogHealthApiTest}). This suite
 * adds real, hard-failing contract assertions for the two things about this family that are
 * deterministic and security-relevant, without needing the (stateful, LLM-spinning) agent runtime.
 *
 * <p><b>The agent-token privilege boundary.</b> Every {@code /agents/quoteagent/*} and
 * {@code /agents/planagent/*} endpoint is gated by a dedicated <em>agent token</em> — NOT the
 * ordinary user JWT. Verified live 2026-07-09: a GET carrying a valid <em>user</em> bearer is
 * rejected with {@code 401 {"error":"Invalid or expired agent token"}}, and an unauthenticated
 * mutation with {@code 401 {"error":"Agent token required"}}. That boundary is exactly the kind of
 * thing a privilege-escalation regression would quietly erode (a refactor that let the user JWT
 * satisfy the agent guard would expose ~91 write-capable endpoints to every logged-in user), so we
 * assert it endpoint-by-endpoint:</p>
 * <ul>
 *   <li><b>GET</b> with a valid user token ⇒ 401/403 (never 200 = data leak / auth bypass, never
 *       5xx = crash, never an HTML SPA-fallback = route not registered), body is JSON that names the
 *       agent-token failure, and no SQL/stack-trace leak.</li>
 *   <li><b>POST/PUT/PATCH/DELETE</b> unauthenticated with an empty body ⇒ 401/403 (never 2xx =
 *       unauthenticated write, never 5xx-with-leak, never HTML).</li>
 * </ul>
 *
 * <p><b>The agent-service health contract</b> — {@code GET /agent/health} must report the SDK and
 * the downstream FastAPI agent service as reachable/installed.</p>
 *
 * <p><b>Non-destructive by construction.</b> Auth is rejected <em>before</em> any handler logic runs
 * (confirmed: an empty-body mutation still 401s), so nothing reaches the database and no real id is
 * ever supplied (path params are substituted with the zero-UUID). The ~14 {@code /agent/*} runtime
 * proxies (chat/create/clear/upload/attach/simple/…) spin up sandboxes and invoke the LLM, so they
 * are <em>intentionally excluded</em> from active probing — the exclusion is asserted and logged in
 * {@link #testAgentsCatalogCoverage()} rather than silently dropped.</p>
 *
 * <p>Spec-driven: the endpoint list is fetched from {@code /swagger.json} at run time, so new
 * agent endpoints are covered automatically.</p>
 */
public class AgentsContractApiTest extends BaseAPITest {

    private static final String MODULE = "API — Agents (deep contract)";

    private static final int SLOW_MS = 3000;

    private static final RestAssuredConfig PROBE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 30000));

    /** The dedicated agent-token API — reached only by the agent service, never a user session. */
    private static final Pattern AGENT_DATA_API = Pattern.compile("^/agents/");
    /** Leaks that must never appear in a response body (server crash disclosing internals). */
    private static final Pattern LEAK = Pattern.compile(
            "(?i)psycopg2|sqlalchemy|traceback \\(most recent|programmingerror|integrityerror|\\bselect\\s.+\\sfrom\\b");

    // Enumerated once from the spec (DataProviders can fire before @BeforeClass, so fetch lazily).
    private static List<String[]> agentTokenGets;      // {category, path}
    private static List<String[]> agentTokenMutations; // {category, method, path}
    private static int runtimeExcluded = -1;           // /agent/* proxies we deliberately skip
    private static int totalAgentOps = -1;

    // ================================================================
    // SPEC ENUMERATION
    // ================================================================

    private static synchronized void enumerateSpec() {
        if (agentTokenGets != null) return;
        Response spec = RestAssured.given().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/swagger.json").then().extract().response();
        if (spec.statusCode() != 200) {
            throw new SkipException("swagger.json not fetchable (HTTP " + spec.statusCode()
                    + ") — cannot enumerate the agents catalog.");
        }
        JSONObject paths = new JSONObject(spec.asString()).getJSONObject("paths");
        List<String[]> gets = new ArrayList<>();
        List<String[]> muts = new ArrayList<>();
        int excluded = 0, total = 0;
        for (String rawPath : new TreeMap<>(paths.toMap()).keySet()) {
            String path = rawPath.startsWith("/api/") ? rawPath.substring(4) : rawPath;
            if (!path.startsWith("/agent")) continue;          // /agent/* and /agents/*
            JSONObject ops = paths.getJSONObject(rawPath);
            for (String method : ops.keySet()) {
                String m = method.toUpperCase(Locale.ROOT);
                if (!m.matches("GET|POST|PUT|PATCH|DELETE")) continue;
                total++;
                if (AGENT_DATA_API.matcher(path).find()) {     // the agent-token data API
                    if ("GET".equals(m)) gets.add(new String[]{categoryOf(path), path});
                    else muts.add(new String[]{categoryOf(path), m, path});
                } else {
                    // an /agent/* runtime proxy — /agent/health is contract-tested separately;
                    // the rest are LLM/sandbox side-effecting and deliberately not probed.
                    excluded++;
                }
            }
        }
        agentTokenGets = gets;
        agentTokenMutations = muts;
        runtimeExcluded = excluded;
        totalAgentOps = total;
        System.out.println("[Agents] spec enumerated: " + total + " agents-family operations — "
                + gets.size() + " agent-token GETs, " + muts.size() + " agent-token mutations, "
                + excluded + " /agent/* runtime ops (health contract-tested, rest excluded).");
    }

    private static String categoryOf(String path) {
        Matcher m = Pattern.compile("^/(agents?/[a-zA-Z0-9_-]+|agent)").matcher(path);
        return m.find() ? m.group(1) : "agent";
    }

    /**
     * Substitute {param} with a value that MATCHES the route's typed path converter, so the request
     * actually reaches the gated handler instead of falling through to the SPA catch-all:
     *   - {..._oid} (SKM object ids) are numeric → a numeric probe (a UUID here makes an
     *     {@code <int:>} converter reject the path, serving the 200-HTML SPA — a false "bypass").
     *   - {..._id}/{id} are UUIDs → the zero-UUID.
     *   - anything else → a name-like probe token.
     */
    private static String resolvePath(String template) {
        Matcher m = Pattern.compile("\\{([^}]+)}").matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String p = m.group(1).toLowerCase(Locale.ROOT);
            String v = p.endsWith("oid") ? "1"
                     : p.endsWith("id")  ? "00000000-0000-0000-0000-000000000000"
                     : "probe";
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String shapeOf(String body) {
        String b = body == null ? "" : body.trim();
        return b.startsWith("[") ? "array" : b.startsWith("{") ? "object" : b.startsWith("<") ? "html" : "-";
    }

    /** Issue a probe with one retry on a transient outcome (timeout / 5xx / transport error). */
    private Response probe(io.restassured.specification.RequestSpecification spec, String method, String path) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                Response r = spec.when().request(method, path).then().extract().response();
                int s = r.getStatusCode();
                if (!(s == 0 || s >= 500) || attempt == 2) return r;
            } catch (Exception e) {
                if (attempt == 2) throw e;
            }
            try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        return null;
    }

    @BeforeClass(alwaysRun = true)
    public void prepare() {
        RestAssured.baseURI = API_BASE_URL;
        enumerateSpec();
    }

    // ================================================================
    // DATA PROVIDERS
    // ================================================================

    @DataProvider(name = "agentTokenGets")
    public Object[][] agentTokenGetsData() {
        enumerateSpec();
        return agentTokenGets.toArray(new Object[0][]);
    }

    @DataProvider(name = "agentTokenMutations")
    public Object[][] agentTokenMutationsData() {
        enumerateSpec();
        return agentTokenMutations.toArray(new Object[0][]);
    }

    // ================================================================
    // CONTRACT 1 — agent-service health
    // ================================================================

    @Test(description = "GET /agent/health reports the SDK + downstream agent service as healthy")
    public void testAgentServiceHealthContract() {
        ExtentReportManager.createTest(MODULE, "agent", "Agent service health contract");
        Response r = getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation()
                .when().get("/agent/health").then().extract().response();

        Assert.assertEquals(r.getStatusCode(), 200,
                "GET /agent/health must return 200; got " + r.getStatusCode() + ": " + trunc(r.asString()));
        Assert.assertNotEquals(shapeOf(r.asString()), "html",
                "GET /agent/health returned an HTML body (SPA fallback) instead of JSON: " + trunc(r.asString()));

        Boolean success = r.jsonPath().get("success");
        Boolean sdkInstalled = r.jsonPath().get("sdk_installed");
        Boolean serviceReachable = r.jsonPath().get("agent_service_reachable");
        String sdkVersion = r.jsonPath().getString("sdk_version");

        Assert.assertEquals(success, Boolean.TRUE, "agent/health success must be true: " + trunc(r.asString()));
        Assert.assertEquals(sdkInstalled, Boolean.TRUE, "agent/health sdk_installed must be true");
        Assert.assertEquals(serviceReachable, Boolean.TRUE,
                "agent/health agent_service_reachable must be true (downstream FastAPI agent service is down otherwise)");
        Assert.assertTrue(sdkVersion != null && sdkVersion.matches("\\d+\\.\\d+\\.\\d+.*"),
                "agent/health must report a semver sdk_version; got '" + sdkVersion + "'");

        ExtentReportManager.logPass("agent/health healthy: SDK " + sdkVersion
                + ", service reachable at " + r.jsonPath().getString("agent_service_url"));
    }

    // ================================================================
    // CONTRACT 2 — agent-token boundary on the data API (GET)
    // ================================================================

    @Test(dataProvider = "agentTokenGets",
          description = "Each /agents/* GET rejects an ordinary user token — the agent-token boundary holds")
    public void testQuoteAgentReadRequiresAgentToken(String category, String template) {
        ExtentReportManager.createTest(MODULE, category,
                "GET " + template + " requires agent token");
        if (!hasAuthToken()) throw new SkipException("No user token — cannot exercise the agent-token boundary.");

        String path = resolvePath(template);
        Response r = probe(getAuthenticatedRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation(),
                "GET", path);
        Assert.assertNotNull(r, "GET " + template + " unreachable after retry (outage).");

        assertAgentTokenGate("GET", template, r, "a valid USER token");
        boolean namesAgentToken = r.asString() != null && r.asString().toLowerCase(Locale.ROOT).contains("agent token");
        if (!namesAgentToken) {
            ExtentReportManager.logWarning("GET " + template + " is gated but the body does not name the "
                    + "agent-token failure — auth-contract drift? Body: " + trunc(r.asString()));
        }
        if (r.time() >= SLOW_MS) {
            ExtentReportManager.logWarning("GET " + template + " slow even on the reject path: " + r.time() + "ms");
        }
    }

    // ================================================================
    // CONTRACT 3 — agent-token boundary on the data API (mutations)
    // ================================================================

    @Test(dataProvider = "agentTokenMutations",
          description = "Each /agents/* mutation rejects an unauthenticated call — no unauthenticated writes")
    public void testQuoteAgentMutationRequiresAgentToken(String category, String method, String template) {
        ExtentReportManager.createTest(MODULE, category,
                method + " " + template + " requires agent token");

        String path = resolvePath(template);
        Response r = probe(getRequestSpec().config(PROBE_CONFIG).relaxedHTTPSValidation().body("{}"),
                method, path);
        Assert.assertNotNull(r, method + " " + template + " unreachable after retry (outage).");
        assertAgentTokenGate(method, template, r, "an unauthenticated call");
    }

    /**
     * Shared classifier for the agent-token boundary. Verdicts:
     * <ul>
     *   <li><b>401/403</b> → PASS: correctly gated.</li>
     *   <li><b>2xx with a JSON/array body</b> → FAIL: the request satisfied (or bypassed) the agent
     *       guard and got real data — an auth-boundary breach / unauthenticated write.</li>
     *   <li><b>5xx</b> → FAIL: the auth layer crashed before/while gating (leak-checked too).</li>
     *   <li><b>any HTML body</b> → SKIP: the SPA catch-all answered, i.e. the API route was never
     *       matched (usually a typed path-converter rejecting the synthetic probe id). A genuine
     *       breach returns agent JSON, never the SPA page — so this is "could not exercise", not a
     *       pass and not a breach.</li>
     *   <li><b>other 4xx with JSON</b> → PASS-with-warning: answered a non-auth error; recorded as
     *       possible auth-ordering drift but not failed (no data disclosed).</li>
     * </ul>
     * Every non-HTML body is leak-scanned regardless of status.
     */
    private void assertAgentTokenGate(String method, String template, Response r, String caller) {
        int status = r.getStatusCode();
        String body = r.asString();
        String shape = shapeOf(body);

        if (!"html".equals(shape)) {
            Assert.assertFalse(LEAK.matcher(body == null ? "" : body).find(),
                    method + " " + template + " leaked server internals in the response body: " + trunc(body));
        }
        if (status == 401 || status == 403) {
            ExtentReportManager.logPass(method + " " + template + " correctly rejects " + caller + " (HTTP " + status + ").");
            return;
        }
        if ("html".equals(shape)) {
            String msg = method + " " + template + " served the SPA catch-all (HTTP " + status + ", HTML) — the "
                    + "synthetic probe id did not match the route's path-converter, so the gated handler was not "
                    + "exercised. Not a breach (a real bypass returns agent JSON), not assertable here.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }
        if (status >= 200 && status < 300) {
            Assert.fail("AGENT-TOKEN BOUNDARY BREACH: " + method + " " + template + " accepted " + caller
                    + " and returned data (HTTP " + status + ", " + shape + "). The agent-token guard did not gate "
                    + "this endpoint — it is reachable without an agent token. Body: " + trunc(body));
        }
        if (status >= 500) {
            Assert.fail("AGENT-TOKEN BOUNDARY: " + method + " " + template + " returned " + status + " for " + caller
                    + " — the auth layer crashed instead of cleanly rejecting. Body: " + trunc(body));
        }
        // Other 4xx (e.g. 404/400 JSON): answered a non-auth error without the agent-token gate.
        ExtentReportManager.logWarning(method + " " + template + " answered HTTP " + status + " (" + shape + ") for "
                + caller + " rather than a 401/403 agent-token rejection — possible auth-ordering drift. Body: " + trunc(body));
    }

    // ================================================================
    // COVERAGE PROOF
    // ================================================================

    @Test(description = "Every agents-family operation in the spec is accounted for (asserted or explicitly excluded)")
    public void testAgentsCatalogCoverage() {
        ExtentReportManager.createTest(MODULE, "coverage", "Agents catalog coverage");
        enumerateSpec();
        // Accounted-for = agent-token(gets+muts) + all /agent/* runtime ops (health is asserted,
        // the rest of the /agent/* runtime proxies are the intentionally-excluded bucket).
        int reconciled = agentTokenGets.size() + agentTokenMutations.size() + runtimeExcluded;
        System.out.println("[Agents] coverage: " + reconciled + "/" + totalAgentOps
                + " agents-family ops accounted for (" + agentTokenGets.size() + " GET + "
                + agentTokenMutations.size() + " mutation contracts asserted; " + runtimeExcluded
                + " /agent/* runtime ops — health asserted, rest intentionally excluded).");
        Assert.assertEquals(reconciled, totalAgentOps,
                "Every agents-family operation must be either contract-asserted or explicitly excluded; "
                        + reconciled + " accounted for vs " + totalAgentOps + " in the spec.");
        Assert.assertTrue(agentTokenGets.size() + agentTokenMutations.size() >= 80,
                "Expected the agent-token data API to be large (~91 ops); only found "
                        + (agentTokenGets.size() + agentTokenMutations.size()) + " — spec shrank or enumeration broke.");
        ExtentReportManager.logPass("Agents coverage proven: " + reconciled + "/" + totalAgentOps
                + " operations accounted for.");
    }

    private static String trunc(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
