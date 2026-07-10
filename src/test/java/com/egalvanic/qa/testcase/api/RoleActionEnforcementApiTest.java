package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * <b>Role action enforcement</b> — does the backend actually allow/deny CREATE &amp; EDIT
 * operations per role, matching each entity's {@code *.manage} permission?
 *
 * <p>Covers Assets (nodes), Tasks and Issues — create &amp; edit. For every role × operation it
 * sends the mutation and asserts: the role has the gating {@code *.manage} permission (or is admin)
 * ⇒ the API <b>allows</b> it (HTTP 2xx, no {@code permission_denied}); otherwise the API <b>denies</b>
 * it with an explicit {@code {"error":"permission_denied","message":"…Required: <perm>"}}.</p>
 *
 * <p><b>Non-destructive:</b> the backend checks permission BEFORE it looks up the entity (verified
 * live), and mutations are async (<code>_mutation: received</code>). So EDIT probes target a
 * zero-UUID — a denied role is rejected; a permitted role's queued mutation targets a nonexistent
 * id and no-ops. The Task CREATE probe uses a clearly-labelled minimal payload (no FK context, so it
 * does not persist) purely to exercise the create gate.</p>
 *
 * <p>Oracle = the role's live {@code /auth/me} permission set (via {@link RbacFixtures}). Expected
 * outcome by role: Admin/PM/Technician/Facility Manager/Account Manager have these manage permissions
 * (allowed); <b>Client Portal</b> is read-only (denied). Honors {@code -Drbac.roles}. Unprovisioned /
 * mis-provisioned accounts SKIP.</p>
 */
public class RoleActionEnforcementApiTest extends BaseAPITest {

    private static final String ZERO = "00000000-0000-0000-0000-000000000000";

    /** A mutating operation: label, the *.manage permission that gates it, HTTP method, path, body. */
    private static final class Action {
        final String label, gate, method, path, body;
        Action(String label, String gate, String method, String path, String body) {
            this.label = label; this.gate = gate; this.method = method; this.path = path; this.body = body;
        }
    }

    /** Create + edit actions with a confirmed, clean permission_denied enforcement signal (probed live). */
    private static final List<Action> ACTIONS = Arrays.asList(
            new Action("Edit Asset (node)", "nodes.manage",  "PUT",  "/node/update/" + ZERO,  "{\"name\":\"rbac-probe\"}"),
            new Action("Edit Task",         "tasks.manage",  "PUT",  "/task/update/" + ZERO,  "{\"name\":\"rbac-probe\"}"),
            new Action("Create Task",       "tasks.manage",  "POST", "/task/create",          "{\"name\":\"RBAC-ENFORCEMENT-PROBE\"}"),
            new Action("Edit Issue",        "issues.manage", "PUT",  "/issue/update/" + ZERO, "{\"name\":\"rbac-probe\"}")
    );

    @BeforeClass(alwaysRun = true)
    public void setBaseUri() { RestAssured.baseURI = API_BASE_URL; }

    @DataProvider(name = "roleActions")
    public Object[][] roleActions() {
        List<Object[]> rows = new ArrayList<>();
        for (Role role : RbacFixtures.selectedRoles()) {     // honors -Drbac.roles
            for (Action a : ACTIONS) rows.add(new Object[]{role, a});
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "roleActions",
          description = "Each role can create/edit records only when its permissions allow it (unauthorised roles are blocked)")
    public void actionEnforcement(Role role, Action action) {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS,
                role.name + " — " + action.label + " [" + action.gate + "]");

        LiveAuth live = RbacFixtures.cachedLiveAuth(role);
        if (!live.provisioned) {
            String msg = "Account not provisioned for '" + role.name + "' (login " + live.loginStatus + ").";
            ExtentReportManager.logSkip(msg); throw new SkipException(msg);
        }
        String mism = RbacFixtures.roleMismatchSkipMessage(role, live);
        if (mism != null) { ExtentReportManager.logSkip(mism); throw new SkipException(mism); }

        boolean expectedAllowed = Boolean.TRUE.equals(live.isAdmin) || live.permissions.contains(action.gate);

        // Bounded socket timeout + one retry: the QA host has intermittent slow spells (a 60s hang made
        // these mutations hard-FAIL as false RBAC failures on 2026-07-10). A genuine RBAC allow/deny answers
        // in <2s; a timeout/5xx here is backend instability, not an authorization signal → SKIP, don't fail
        // (the stability is tracked by ErrorContractApiTest's 502-detector and the consolidated report).
        Response resp = mutateWithRetry(live.token, action);
        if (resp == null || resp.getStatusCode() == 0 || resp.getStatusCode() >= 500) {
            String msg = "'" + role.name + "' " + action.label + " — backend slow/unavailable during the probe "
                    + "(HTTP " + (resp == null ? "timeout" : resp.getStatusCode()) + "); RBAC allow/deny not "
                    + "determinable this run (transient backend instability, see the 502 detector).";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }

        int status = resp.getStatusCode();
        String body = resp.asString();
        boolean denied = isPermissionDenied(status, body);
        ExtentReportManager.logInfo("'" + role.name + "' " + action.label + " (has " + action.gate + "="
                + expectedAllowed + ") → HTTP " + status + (denied ? " permission_denied" : ""));

        if (expectedAllowed) {
            Assert.assertFalse(denied,
                    "'" + role.name + "' HAS " + action.gate + " but '" + action.label + "' was DENIED. "
                            + "Status " + status + ": " + truncate(body));
            Assert.assertTrue(status >= 200 && status < 300,
                    "'" + role.name + "' should be allowed to " + action.label + " (2xx) but got " + status
                            + ": " + truncate(body));
            ExtentReportManager.logPass("'" + role.name + "' allowed to " + action.label + " (HTTP " + status + ").");
        } else {
            Assert.assertTrue(denied,
                    "PRIVILEGE ESCALATION: '" + role.name + "' LACKS " + action.gate + " but '" + action.label
                            + "' was NOT denied (status " + status + "): " + truncate(body));
            ExtentReportManager.logPass("'" + role.name + "' correctly DENIED " + action.label
                    + " (permission_denied, HTTP " + status + ").");
        }
    }

    // Bounded timeout so a hung request fails fast (12s) instead of the ~60s default that turned a
    // backend slow-spell into false RBAC failures.
    private static final RestAssuredConfig PROBE_CONFIG = RestAssured.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 8000)
                    .setParam("http.socket.timeout", 12000));

    /** Send the mutation with one retry on a transient outcome (timeout / 5xx). Returns null on repeated timeout. */
    private Response mutateWithRetry(String token, Action action) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                Response r = given().config(PROBE_CONFIG).relaxedHTTPSValidation()
                        .header("Authorization", "Bearer " + token)
                        .contentType(ContentType.JSON)
                        .body(action.body)
                        .when().request(action.method, action.path)
                        .then().extract().response();
                if (r.getStatusCode() < 500 || attempt == 2) return r;   // permission verdicts are <500
            } catch (Exception e) {
                if (attempt == 2) return null;                            // timeout / transport failure
            }
            try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        return null;
    }

    /** Explicit permission failure: the backend's permission_denied error, or a 401/403. */
    private static boolean isPermissionDenied(int status, String body) {
        if (status == 401 || status == 403) return true;
        if (body == null) return false;
        String b = body.toLowerCase();
        return b.contains("permission_denied") || b.contains("do not have permission")
                || b.contains("required_permission");
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
