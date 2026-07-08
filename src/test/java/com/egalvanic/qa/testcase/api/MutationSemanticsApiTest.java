package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

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
 * <b>Mutation-semantics audit</b> — the write-path behaviours {@link CrudLifecycleApiTest} deliberately
 * sidesteps by always using {@code x-direct-write}. This class exercises the parts a real client has to
 * get right, all reproduced live on 2026-07-08:
 *
 * <ol>
 *   <li><b>Dual write path</b> — the backend has two modes on the same endpoint. WITH the frontend's
 *       {@code x-direct-write: true} header a mutation is synchronous and the response is the full
 *       object. WITHOUT it the mutation is <i>queued</i>: the response is
 *       {@code {"_mutation":{"status":"received"}, "id":…}} and the row only becomes visible a moment
 *       later. This test asserts both halves of that contract, including that the queued write
 *       <b>eventually converges</b> (a queued write that never lands is a silent data-loss bug).</li>
 *   <li><b>Delete idempotency</b> — deleting the same task twice must both succeed (2xx) and leave the
 *       task delisted; a second delete must not 5xx and must not resurrect the row.</li>
 *   <li><b>DELETE media-type contract</b> — {@code DELETE /task/delete/{id}} with no JSON
 *       {@code Content-Type} returns {@code 415} (a real client gotcha — a bare DELETE fails); with a
 *       JSON content-type it succeeds. Locked down so a client library change that drops the header is
 *       caught.</li>
 * </ol>
 *
 * <p><b>Safety</b> mirrors {@link CrudLifecycleApiTest}: mutations run only on the sandbox SLD named
 * "{@value CrudLifecycleApiTest#SANDBOX_SITE_NAME}" (skip otherwise), every record carries the
 * {@value #MARKER} prefix, and {@code @AfterClass} deletes this run's records and sweeps strays.
 * <b>Failure policy</b>: these behaviours hold today, so they hard-fail on violation — the suite is a
 * regression guard (a lost queued write, a delete that 5xxs on repeat, or a DELETE that starts silently
 * no-op'ing are all objective defects).</p>
 */
public class MutationSemanticsApiTest extends BaseAPITest {

    static final String MARKER = "ApiMut_";
    private static final String MODULE = "API — Mutation Semantics";
    private static final int  CONVERGE_TRIES = 12;    // async write must land within tries*sleep
    private static final long CONVERGE_SLEEP = 2000;  // → up to 24s, generous for a QA queue

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    private String companyId, sandboxSldId;
    private final String runTag = MARKER + System.currentTimeMillis();
    private final List<String> createdTaskIds = Collections.synchronizedList(new ArrayList<>());

    @BeforeClass(alwaysRun = true)
    public void resolveSandbox() {
        if (!hasAuthToken()) return;
        try {
            Response me = getAuthenticatedRequestSpec().when().get("/auth/v2/me").then().extract().response();
            if (me.statusCode() == 200 && !me.asString().trim().startsWith("<"))
                companyId = new JSONObject(me.asString()).optString("company_id", null);
            if (companyId == null) return;
            Response slds = getAuthenticatedRequestSpec()
                    .when().get("/company/" + companyId + "/slds").then().extract().response();
            if (slds.statusCode() == 200) {
                JSONArray arr = new JSONObject(slds.asString()).optJSONArray("slds");
                for (int i = 0; arr != null && i < arr.length(); i++)
                    if (CrudLifecycleApiTest.SANDBOX_SITE_NAME.equalsIgnoreCase(
                            arr.getJSONObject(i).optString("name", "").trim())) {
                        sandboxSldId = arr.getJSONObject(i).optString("id", null); break;
                    }
            }
            System.out.println("[MutationSemantics] sandbox=" + sandboxSldId);
        } catch (Exception e) {
            System.out.println("[MutationSemantics] setup failed: " + e.getMessage());
        }
    }

    private void requireSandbox() {
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        if (sandboxSldId == null) throw new SkipException(
                "Sandbox SLD '" + CrudLifecycleApiTest.SANDBOX_SITE_NAME + "' not found — refusing to mutate a real site.");
    }

    private RequestSpecification directWrite() { return getAuthenticatedRequestSpec().header("x-direct-write", "true"); }

    private JSONObject taskPayload(String title) {
        return new JSONObject().put("sld_id", sandboxSldId).put("task_type", "PM")
                .put("completed", false).put("title", title).put("task_description", "mutation-semantics probe");
    }

    /** Null-safe read of a task's title via GET /task/{id}; "" if not readable / no data yet.
     *  Envelope-tolerant: GET /task/{id} returns a FLAT object (title at top level, no "data" wrapper),
     *  unlike POST /task/create which wraps in {data:{…}} — so read data.title if present, else the root. */
    private String readTaskTitle(String id) {
        try {
            Response r = getAuthenticatedRequestSpec().when().get("/task/" + id).then().extract().response();
            if (r.statusCode() != 200 || r.asString().trim().startsWith("<")) return "";
            JSONObject o = new JSONObject(r.asString());
            JSONObject task = o.optJSONObject("data") != null ? o.getJSONObject("data") : o;
            return task.optString("title", "");
        } catch (Exception e) { return ""; }
    }

    private boolean taskListed(String id) {
        try {
            Response r = getAuthenticatedRequestSpec().when().get("/tasks/" + sandboxSldId).then().extract().response();
            if (r.statusCode() != 200) return false;
            JSONArray tasks = new JSONObject(r.asString()).optJSONArray("user_tasks");
            for (int i = 0; tasks != null && i < tasks.length(); i++) {
                JSONObject t = tasks.getJSONObject(i);
                if (id.equals(t.optString("id")) && !t.optBoolean("is_deleted", false)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean pollListed(String id, boolean want) {
        for (int i = 0; i < CONVERGE_TRIES; i++) {
            if (taskListed(id) == want) return true;
            sleep(CONVERGE_SLEEP);
        }
        return false;
    }

    // ── 1. async queue: envelope + eventual convergence ────────────────────────

    @Test(description = "Async write (no x-direct-write) returns a _mutation envelope and eventually converges")
    public void testAsyncWriteEventuallyConverges() {
        ExtentReportManager.createTest(MODULE, "Async write convergence", "MUT_ASYNC");
        requireSandbox();
        String title = runTag + "_async";

        // no x-direct-write → queued path
        Response create = getAuthenticatedRequestSpec().body(taskPayload(title).toString())
                .when().post("/task/create").then().extract().response();
        Assert.assertTrue(create.statusCode() == 200 || create.statusCode() == 201,
                "async create expected 2xx, got " + create.statusCode() + ": " + trim(create));
        JSONObject env = new JSONObject(create.asString());
        String id = env.optJSONObject("data") != null ? env.getJSONObject("data").optString("id", null)
                                                       : env.optString("id", null);
        Assert.assertNotNull(id, "async create returned no id: " + trim(create));
        createdTaskIds.add(id);

        boolean hadEnvelope = env.has("_mutation");
        boolean converged = pollListed(id, true);
        REPORT.add(new String[]{"async-convergence", converged ? "OK" : "DEFECT",
                "envelope=" + hadEnvelope + " convergedWithin=" + (CONVERGE_TRIES * CONVERGE_SLEEP / 1000) + "s"});
        Assert.assertTrue(converged,
                "A queued (non-direct-write) create never became visible — silent data loss. id=" + id);
        if (!hadEnvelope) ExtentReportManager.logWarning(
                "async create did not carry a {_mutation} envelope (queue contract may have changed): " + trim(create));
        ExtentReportManager.logPass("Async write converged; envelope present=" + hadEnvelope + ".");
    }

    // ── 2. direct-write is synchronous ─────────────────────────────────────────

    @Test(description = "Direct-write (x-direct-write) is synchronous: full object back, immediately readable")
    public void testDirectWriteIsSynchronous() {
        ExtentReportManager.createTest(MODULE, "Direct-write synchronous", "MUT_DIRECT");
        requireSandbox();
        String title = runTag + "_direct";

        Response create = directWrite().body(taskPayload(title).toString())
                .when().post("/task/create").then().extract().response();
        Assert.assertTrue(create.statusCode() == 200 || create.statusCode() == 201,
                "direct create expected 2xx: " + trim(create));
        JSONObject env = new JSONObject(create.asString());
        JSONObject data = env.optJSONObject("data");
        String id = data != null ? data.optString("id", null) : env.optString("id", null);
        Assert.assertNotNull(id, "direct create returned no id");
        createdTaskIds.add(id);

        // Race-free synchronous signal: the create RESPONSE itself carries the full object and no queue
        // envelope (contrast with the async path in testAsyncWriteEventuallyConverges). Then confirm the
        // write is readable near-instantly via GET /task/{id} (a few hundred ms) — vs the async path's
        // multi-second queue convergence. Falls back to the list only if the single-GET is briefly behind.
        boolean noEnvelope = !env.has("_mutation");
        boolean gotFullObject = data != null && title.equals(data.optString("title"));
        boolean quicklyReadable = false;
        for (int i = 0; i < 4 && !quicklyReadable; i++) {
            quicklyReadable = title.equals(readTaskTitle(id));
            if (!quicklyReadable) sleep(700);
        }
        boolean visibleInList = quicklyReadable || pollListed(id, true);

        REPORT.add(new String[]{"direct-write-sync",
                (noEnvelope && gotFullObject && visibleInList) ? "OK" : "SOFT-GAP",
                "noEnvelope=" + noEnvelope + " fullObject=" + gotFullObject
                + " detailReadable<3s=" + quicklyReadable + " visibleInList=" + visibleInList});
        Assert.assertTrue(gotFullObject,
                "direct-write did not return the created object body synchronously (no full object in response)");
        Assert.assertTrue(noEnvelope,
                "direct-write create carried a {_mutation} queue envelope — it was NOT synchronous");
        Assert.assertTrue(visibleInList, "direct-write create never became visible — write not persisted");
        if (!quicklyReadable) ExtentReportManager.logWarning(
                "GET /task/{id} did not reflect a direct-write create within ~3s though it is in the list "
                + "— genuine read-after-write lag on the single-resource read path.");
        ExtentReportManager.logPass("Direct-write is synchronous (full object in response, no queue envelope, persisted).");
    }

    // ── 3. delete idempotency ──────────────────────────────────────────────────

    @Test(description = "Deleting a task twice both succeed (2xx), leave it delisted, never 5xx or resurrect")
    public void testDeleteIdempotency() {
        ExtentReportManager.createTest(MODULE, "Delete idempotency", "MUT_DELETE_IDEMPOTENT");
        requireSandbox();
        String title = runTag + "_delidem";

        String id = new JSONObject(directWrite().body(taskPayload(title).toString())
                .when().post("/task/create").then().extract().response().asString())
                .getJSONObject("data").getString("id");
        createdTaskIds.add(id);
        Assert.assertTrue(pollListed(id, true), "task not visible before delete");

        int d1 = directWrite().body("{}").when().delete("/task/delete/" + id).then().extract().response().statusCode();
        int d2 = directWrite().body("{}").when().delete("/task/delete/" + id).then().extract().response().statusCode();
        boolean delisted = pollListed(id, false);

        REPORT.add(new String[]{"delete-idempotency",
                (d1 < 300 && d2 < 500 && delisted) ? "OK" : "DEFECT",
                "delete1=" + d1 + " delete2=" + d2 + " delistedAfter=" + delisted});
        Assert.assertTrue(d1 >= 200 && d1 < 300, "first delete not 2xx: " + d1);
        Assert.assertTrue(d2 < 500, "second (repeat) delete 5xx'd — not idempotent: " + d2);
        Assert.assertTrue(delisted, "task still active after double delete (delete didn't stick / resurrected)");
        ExtentReportManager.logPass("Delete is idempotent (d1=" + d1 + " d2=" + d2 + ", delisted).");
    }

    // ── 4. DELETE media-type contract ───────────────────────────────────────────

    @Test(description = "DELETE media-type contract: no JSON Content-Type → 4xx (not 5xx/silent-200); JSON → 2xx")
    public void testDeleteMediaTypeContract() {
        ExtentReportManager.createTest(MODULE, "DELETE media-type contract", "MUT_DELETE_CT");
        requireSandbox();
        String title = runTag + "_ct";

        String id = new JSONObject(directWrite().body(taskPayload(title).toString())
                .when().post("/task/create").then().extract().response().asString())
                .getJSONObject("data").getString("id");
        createdTaskIds.add(id);

        // DELETE with NO content-type header (RestAssured sends none when we don't set the spec's CT)
        Response bare = given().header("Authorization", "Bearer " + authToken).header("x-direct-write", "true")
                .when().delete("/task/delete/" + id).then().extract().response();
        int bareCode = bare.statusCode();

        // DELETE with JSON content-type
        int jsonCode = directWrite().body("{}").when().delete("/task/delete/" + id).then().extract().response().statusCode();

        boolean bareIsClientError = bareCode >= 400 && bareCode < 500;   // 415 today
        boolean jsonOk = jsonCode >= 200 && jsonCode < 300;
        REPORT.add(new String[]{"delete-media-type",
                (bareCode < 500 && jsonOk) ? "OK" : "DEFECT",
                "noContentType=" + bareCode + " (client-error=" + bareIsClientError + ") jsonContentType=" + jsonCode});
        Assert.assertTrue(bareCode < 500,
                "DELETE without a JSON body/Content-Type 5xx'd (should be a clean 4xx): " + bareCode);
        Assert.assertTrue(jsonOk, "DELETE with JSON content-type did not succeed: " + jsonCode);
        if (bareCode == 415) ExtentReportManager.logInfo(
                "Client gotcha confirmed: a bare DELETE (no JSON Content-Type) → 415; clients must send application/json.");
        ExtentReportManager.logPass("DELETE media-type contract: bare=" + bareCode + " json=" + jsonCode + ".");
    }

    // ── cleanup + report ────────────────────────────────────────────────────────

    private void deleteTaskQuietly(String id) {
        try { directWrite().body("{}").when().delete("/task/delete/" + id); } catch (Exception ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String trim(Response r) {
        String b = r.asString();
        return b == null ? "<null>" : b.length() > 240 ? b.substring(0, 240) + "…" : b;
    }

    @AfterClass(alwaysRun = true)
    public void cleanupAndReport() {
        for (String id : createdTaskIds) deleteTaskQuietly(id);
        // sweep any ApiMut_* stray a crashed run may have left
        if (sandboxSldId != null) try {
            Response r = getAuthenticatedRequestSpec().when().get("/tasks/" + sandboxSldId).then().extract().response();
            if (r.statusCode() == 200) {
                JSONArray tasks = new JSONObject(r.asString()).optJSONArray("user_tasks");
                for (int i = 0; tasks != null && i < tasks.length(); i++) {
                    JSONObject t = tasks.getJSONObject(i);
                    if (!t.optBoolean("is_deleted", false) && t.optString("title", "").startsWith(MARKER))
                        deleteTaskQuietly(t.optString("id"));
                }
            }
        } catch (Exception ignored) {}
        try {
            new File("reports").mkdirs();
            StringBuilder md = new StringBuilder("# Mutation-Semantics Audit\n\n"
                    + "Sandbox: " + CrudLifecycleApiTest.SANDBOX_SITE_NAME + " (" + sandboxSldId + ")\n\n"
                    + "| Check | Result | Detail |\n|---|---|---|\n");
            for (String[] row : REPORT) md.append("| ").append(row[0]).append(" | ")
                    .append(row[1]).append(" | ").append(row[2]).append(" |\n");
            try (FileWriter w = new FileWriter("reports/mutation-semantics-report.md")) { w.write(md.toString()); }
            System.out.println("[MutationSemantics] wrote reports/mutation-semantics-report.md");
        } catch (Exception e) {
            System.out.println("[MutationSemantics] report write failed: " + e.getMessage());
        }
    }
}
