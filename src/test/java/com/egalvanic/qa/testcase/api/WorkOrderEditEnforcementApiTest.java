package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

/**
 * <b>Work-order edit — backend authorization enforcement.</b>
 *
 * <p>Proves that the API actually <em>enforces</em> edit permission on a work order
 * (a "job"), not merely that {@code /auth/me} reports it. For every role it sends a
 * <b>value-preserving</b> {@code PUT /api/job/{id}} — the job's own current field values
 * (unwrapped from the {@code {job:{…}}} GET envelope, server-managed fields dropped) — and asserts:</p>
 * <ul>
 *   <li>role has {@code jobs.manage} (or is admin) ⇒ the edit is <b>allowed</b> (HTTP 200,
 *       no {@code permission_denied});</li>
 *   <li>role lacks it ⇒ the edit is <b>denied</b> — the backend returns
 *       {@code {"error":"permission_denied","message":"…Required: jobs.manage"}}.</li>
 * </ul>
 *
 * <p>Oracle = the role's <em>live</em> {@code /auth/me} permission set (via {@link RbacFixtures}).
 * This was verified reliable by live probe (Client Portal → permission_denied, PM → 200), unlike
 * list endpoints whose enforcement signal is ambiguous. Unprovisioned roles SKIP. The PUT preserves the
 * job's field values; a successful write does bump the server-managed {@code modified_at} timestamp (an
 * unavoidable side effect of exercising a real edit endpoint), but no business field changes.</p>
 */
public class WorkOrderEditEnforcementApiTest extends BaseAPITest {

    /** The permission the backend requires to edit a job (per its own permission_denied message). */
    private static final String EDIT_PERMISSION = "jobs.manage";

    private static String jobId;
    private static String jobBody;   // the job's own current fields (unwrapped from {job:{…}}), server-managed
                                     // fields removed — sent back as a no-op edit to exercise the RBAC gate

    @BeforeClass(alwaysRun = true)
    public void pickAJob() {
        RestAssured.baseURI = API_BASE_URL;
        // Use any provisioned role with jobs.view to read a real job (Admin first, then others).
        String token = null;
        for (Role r : RbacFixtures.ROLES) {
            LiveAuth la = RbacFixtures.cachedLiveAuth(r);
            if (la.provisioned && la.token != null
                    && (Boolean.TRUE.equals(la.isAdmin) || la.permissions.contains("jobs.view"))) {
                token = la.token;
                break;
            }
        }
        if (token == null) {
            System.out.println("[WO-EDIT] No provisioned role available to read a job.");
            return;
        }
        Response list = given().header("Authorization", "Bearer " + token)
                .when().get("/job/").then().extract().response();
        String raw = list.asString();
        System.out.println("[WO-EDIT] /job/ status=" + list.getStatusCode()
                + " len=" + (raw == null ? 0 : raw.length()) + " head=" + truncate(raw));
        if (list.getStatusCode() == 200 && raw != null) {
            try {
                org.json.JSONArray arr;
                String t = raw.trim();
                if (t.startsWith("[")) {
                    arr = new org.json.JSONArray(t);
                } else {
                    org.json.JSONObject obj = new org.json.JSONObject(t);
                    String key = obj.has("jobs") ? "jobs" : obj.has("data") ? "data"
                            : obj.has("items") ? "items" : obj.has("results") ? "results" : null;
                    arr = key != null ? obj.getJSONArray(key) : new org.json.JSONArray();
                }
                for (int i = 0; i < arr.length(); i++) {
                    String id = arr.getJSONObject(i).optString("id", "");
                    if (!id.isEmpty() && !"null".equals(id)) { jobId = id; break; }
                }
            } catch (Exception e) {
                System.out.println("[WO-EDIT] could not parse /job/ list: " + e.getMessage());
            }
        }
        if (jobId != null) {
            Response detail = given().header("Authorization", "Bearer " + token)
                    .when().get("/job/" + jobId).then().extract().response();
            if (detail.getStatusCode() == 200) {
                // GET /job/{id} returns a WRAPPED envelope {job:{…}, success} — unwrap to the real job
                // object, else the PUT sends unrecognized top-level keys the backend silently ignores
                // (so the job's own fields wouldn't round-trip). Drop server-managed fields so the payload
                // carries only the job's current values (a genuine no-op edit).
                try {
                    org.json.JSONObject dj = new org.json.JSONObject(detail.asString());
                    org.json.JSONObject job = dj.has("job") ? dj.getJSONObject("job") : dj;
                    job.remove("modified_at");
                    job.remove("created_at");
                    jobBody = job.toString();
                } catch (Exception e) {
                    jobBody = detail.asString();   // fall back to the raw body
                }
            }
        }
        System.out.println("[WO-EDIT] target job id: " + jobId
                + " (list status " + list.getStatusCode() + ")");
    }

    @DataProvider(name = "roles")
    public Object[][] roles() {
        java.util.List<Role> rs = RbacFixtures.selectedRoles();   // honors -Drbac.roles
        Object[][] data = new Object[rs.size()][1];
        for (int i = 0; i < rs.size(); i++) data[i][0] = rs.get(i);
        return data;
    }

    @Test(dataProvider = "roles",
          description = "Only roles allowed to edit Work Orders can edit them (others are blocked)")
    public void workOrderEditEnforcement(Role role) {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS,
                "WO Edit Enforcement: " + role.name);

        if (jobId == null || jobBody == null) {
            throw new SkipException("No work order (job) available to test edit enforcement against.");
        }
        LiveAuth live = RbacFixtures.cachedLiveAuth(role);
        if (!live.provisioned) {
            String msg = "Account not provisioned for '" + role.name + "' (login " + live.loginStatus + ").";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }

        boolean expectedAllowed = Boolean.TRUE.equals(live.isAdmin) || live.permissions.contains(EDIT_PERMISSION);

        // No-op edit: PUT the job's own current body back.
        Response resp = given()
                .header("Authorization", "Bearer " + live.token)
                .contentType(ContentType.JSON)
                .body(jobBody)
                .when().put("/job/" + jobId)
                .then().extract().response();

        int status = resp.getStatusCode();
        String body = resp.asString();
        boolean denied = isPermissionDenied(status, body);
        ExtentReportManager.logInfo("'" + role.name + "' has " + EDIT_PERMISSION + "=" + expectedAllowed
                + " → PUT /job/{id} returned " + status + (denied ? " (permission_denied)" : ""));

        if (expectedAllowed) {
            Assert.assertFalse(denied,
                    "'" + role.name + "' HAS " + EDIT_PERMISSION + " but the edit was DENIED. "
                            + "Status " + status + ": " + truncate(body));
            Assert.assertTrue(status >= 200 && status < 300,
                    "'" + role.name + "' should be allowed to edit (2xx) but got " + status + ": " + truncate(body));
            ExtentReportManager.logPass("'" + role.name + "' can edit a work order (allowed, HTTP " + status + ").");
        } else {
            Assert.assertTrue(denied,
                    "PRIVILEGE ESCALATION: '" + role.name + "' LACKS " + EDIT_PERMISSION
                            + " but the edit was NOT denied (status " + status + "): " + truncate(body));
            ExtentReportManager.logPass("'" + role.name + "' correctly blocked from editing a work order "
                    + "(permission_denied, HTTP " + status + ").");
        }
    }

    /** The backend signals an authorization failure via an explicit error code/message (HTTP 422), or a 401/403. */
    private static boolean isPermissionDenied(int status, String body) {
        if (status == 401 || status == 403) return true;
        if (body == null) return false;
        String b = body.toLowerCase();
        return b.contains("permission_denied") || b.contains("do not have permission")
                || b.contains("required_permission");
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 240 ? s.substring(0, 240) + "…" : s;
    }
}
