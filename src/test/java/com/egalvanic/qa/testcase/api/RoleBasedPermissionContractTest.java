package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.RolePermissionMatrix;
import com.egalvanic.qa.utils.RolePermissionMatrix.RoleSpec;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static io.restassured.RestAssured.given;

/**
 * RBAC permission-contract tests — covers the entire production permission
 * matrix in {@code testcase/prod_permissions-by-role_*.csv}.
 *
 * <p>For every role in the matrix this test logs in via the API, calls
 * {@code GET /api/auth/me} (the same endpoint the React app uses to gate its
 * UI), and asserts the live permission set matches the documented matrix:</p>
 *
 * <ul>
 *   <li><b>Identity</b> — {@code roles[0].id}/{@code name} equal the CSV role.</li>
 *   <li><b>No privilege escalation (security)</b> — the live set contains NO
 *       permission beyond what the matrix grants. Hard fail on any extra.</li>
 *   <li><b>Completeness</b> — every granted permission is present, except a small,
 *       explicitly documented set of prod-vs-QA drift ({@link #KNOWN_QA_DRIFT}).
 *       Any NEW missing permission hard-fails.</li>
 *   <li><b>Derived-field consistency</b> — {@code has_web_access} agrees with the
 *       {@code platform.web} grant, and {@code is_admin} agrees with the Admin role.</li>
 * </ul>
 *
 * <p>A single exact-set assertion per role therefore covers all of that role's
 * grants <em>and</em> the entire negative space (everything it must NOT have).</p>
 *
 * <p>Roles whose QA test account is not yet provisioned (login ≠ 200) are
 * SKIPPED with a clear message rather than failed — see {@link #testRolePermissionContract}.</p>
 */
public class RoleBasedPermissionContractTest extends BaseAPITest {

    private static RolePermissionMatrix MATRIX;

    /**
     * Known prod-vs-QA drift: permissions present in the prod CSV export but not
     * granted on the QA tenant (acme.qa). Verified live on 2026-06-15. These are
     * environment-configuration lag, not code regressions, so the completeness
     * check tolerates them — they are logged as ⚠️ warnings on every run, and any
     * missing permission OUTSIDE this map hard-fails the build.
     */
    private static final Map<String, Set<String>> KNOWN_QA_DRIFT = new HashMap<>();
    static {
        KNOWN_QA_DRIFT.put("Admin", setOf("features.equipment_insights.view"));
        KNOWN_QA_DRIFT.put("Project Manager", setOf("accounts.view"));
        KNOWN_QA_DRIFT.put("Facility Manager", setOf("features.locations.view"));
    }

    /** Stable role_id for each role name (from the prod export) — guards CSV integrity. */
    private static final Map<String, String> EXPECTED_ROLE_IDS = new HashMap<>();
    static {
        EXPECTED_ROLE_IDS.put("Admin", "b60006dd-3cb6-455b-bf90-5d83198321b9");
        EXPECTED_ROLE_IDS.put("Project Manager", "242dbe6a-063c-4057-ac4a-a0d32a773fc8");
        EXPECTED_ROLE_IDS.put("Technician", "e84a0fbb-0b89-4433-807c-4b7886e1cd6d");
        EXPECTED_ROLE_IDS.put("Facility Manager", "54021b71-a055-4c58-91e1-05c705f643f4");
        EXPECTED_ROLE_IDS.put("Client Portal", "2a85145f-31ca-4e2c-8dd2-82c958cd6380");
        EXPECTED_ROLE_IDS.put("Electrical Engineer", "fd6b624e-3847-408f-848d-55779a4a3328");
        EXPECTED_ROLE_IDS.put("Account Manager", "92f38105-0c53-475b-ae57-52dcc4a96f11");
    }

    @BeforeClass(alwaysRun = true)
    public void loadMatrix() {
        RestAssured.baseURI = API_BASE_URL;
        MATRIX = RolePermissionMatrix.load(AppConstants.RBAC_CSV_PATH);
        System.out.println("[RBAC] Loaded matrix: " + MATRIX.roleCount() + " roles, "
                + MATRIX.totalGrants() + " grants from " + AppConstants.RBAC_CSV_PATH);
    }

    @DataProvider(name = "roles")
    public Object[][] roles() {
        return new Object[][]{
                {"Admin", AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD},
                {"Project Manager", AppConstants.PM_EMAIL, AppConstants.PM_PASSWORD},
                {"Technician", AppConstants.TECH_EMAIL, AppConstants.TECH_PASSWORD},
                {"Facility Manager", AppConstants.FM_EMAIL, AppConstants.FM_PASSWORD},
                {"Client Portal", AppConstants.CP_EMAIL, AppConstants.CP_PASSWORD},
                {"Electrical Engineer", AppConstants.EE_EMAIL, AppConstants.EE_PASSWORD},
                {"Account Manager", AppConstants.AM_EMAIL, AppConstants.AM_PASSWORD},
        };
    }

    @Test(dataProvider = "roles", description = "Live /auth/me permission set matches the prod matrix for each role")
    public void testRolePermissionContract(String roleName, String email, String password) {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS,
                "RBAC Contract: " + roleName);

        RoleSpec expected = MATRIX.forRole(roleName);
        Assert.assertNotNull(expected,
                "Role '" + roleName + "' is missing from the permission matrix CSV");
        ExtentReportManager.logInfo("Role '" + roleName + "' — matrix grants "
                + expected.permissions.size() + " permissions (role_id " + expected.roleId + ")");

        // --- Login (skip, don't fail, if the QA account isn't provisioned) ---
        String token = loginToken(email, password);
        if (token == null) {
            String msg = "Test account not provisioned for role '" + roleName + "' ("
                    + email + " → login ≠ 200). Create the account or set its *_EMAIL/*_PASSWORD "
                    + "env vars to enable coverage of this role.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }

        // --- Fetch the live permission set the app actually uses for gating ---
        Response me = getRequestSpec()
                .header("Authorization", "Bearer " + token)
                .when().get("/auth/me")
                .then().extract().response();
        Assert.assertEquals(me.getStatusCode(), 200,
                "GET /auth/me should return 200 for an authenticated " + roleName
                        + ". Got " + me.getStatusCode() + ": " + truncate(me.asString()));

        List<String> livePermsList = me.jsonPath().getList("permissions");
        Assert.assertNotNull(livePermsList,
                "/auth/me response has no 'permissions' array for " + roleName);
        Set<String> live = new TreeSet<>(livePermsList);
        ExtentReportManager.logInfo("Live /auth/me returned " + live.size() + " permissions");

        String liveRoleId = me.jsonPath().getString("roles[0].id");
        String liveRoleName = me.jsonPath().getString("roles[0].name");
        Boolean isAdmin = me.jsonPath().getBoolean("is_admin");
        Boolean hasWebAccess = me.jsonPath().getBoolean("has_web_access");

        StringBuilder failures = new StringBuilder();

        // 1) Identity — the account must actually be the role we think it is.
        if (!expected.roleId.equals(liveRoleId)) {
            failures.append("\n  • role_id mismatch: expected ").append(expected.roleId)
                    .append(" but /auth/me says ").append(liveRoleId)
                    .append(" (").append(liveRoleName).append(")");
        }
        if (liveRoleName != null && !roleName.equals(liveRoleName)) {
            failures.append("\n  • role name mismatch: expected '").append(roleName)
                    .append("' but /auth/me says '").append(liveRoleName).append("'");
        }

        // 2) Privilege escalation guard (SECURITY) — no permission beyond the matrix.
        Set<String> extra = new TreeSet<>(live);
        extra.removeAll(expected.permissions);
        if (!extra.isEmpty()) {
            failures.append("\n  • PRIVILEGE ESCALATION: ").append(extra.size())
                    .append(" permission(s) granted that the matrix does NOT allow: ").append(extra);
        }

        // 3) Completeness — every granted permission present, minus documented drift.
        Set<String> missing = new TreeSet<>(expected.permissions);
        missing.removeAll(live);
        Set<String> drift = KNOWN_QA_DRIFT.getOrDefault(roleName, setOf());
        Set<String> driftSeen = new TreeSet<>(missing);
        driftSeen.retainAll(drift);
        missing.removeAll(drift);
        if (!driftSeen.isEmpty()) {
            ExtentReportManager.logWarning("Known prod-vs-QA drift for " + roleName
                    + " (granted in prod CSV, absent on QA tenant): " + driftSeen);
        }
        if (!missing.isEmpty()) {
            failures.append("\n  • MISSING ").append(missing.size())
                    .append(" granted permission(s) not returned by /auth/me (and not documented drift): ")
                    .append(missing);
        }

        // 4) Derived-field consistency — has_web_access ⇔ platform.web grant.
        boolean expectWeb = expected.hasWebAccess();
        if (hasWebAccess == null || hasWebAccess != expectWeb) {
            failures.append("\n  • has_web_access=").append(hasWebAccess)
                    .append(" but matrix ").append(expectWeb ? "grants" : "does NOT grant")
                    .append(" platform.web (expected ").append(expectWeb).append(")");
        }
        // 5) is_admin flag must match the Admin role only.
        boolean expectAdmin = "Admin".equals(roleName);
        if (isAdmin == null || isAdmin != expectAdmin) {
            failures.append("\n  • is_admin=").append(isAdmin)
                    .append(" but role '").append(roleName).append("' expects ").append(expectAdmin);
        }

        if (failures.length() > 0) {
            String report = "RBAC contract FAILED for '" + roleName + "':" + failures;
            ExtentReportManager.logFail(report);
            Assert.fail(report);
        }

        int verified = expected.permissions.size() - driftSeen.size();
        ExtentReportManager.logPass("RBAC contract verified for '" + roleName + "': "
                + verified + "/" + expected.permissions.size() + " granted permissions confirmed live, "
                + "0 unauthorized extras, identity + has_web_access(" + expectWeb + ") + is_admin("
                + expectAdmin + ") consistent"
                + (driftSeen.isEmpty() ? "" : " [" + driftSeen.size() + " documented QA drift]"));
    }

    @Test(description = "Permission matrix CSV parses completely and matches the known role roster")
    public void testMatrixCsvIntegrity() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_PERMISSION_MATRIX,
                "RBAC Matrix CSV Integrity");

        Assert.assertEquals(MATRIX.roleCount(), EXPECTED_ROLE_IDS.size(),
                "Matrix should contain exactly " + EXPECTED_ROLE_IDS.size() + " roles, got "
                        + MATRIX.roleCount() + ": " + MATRIX.roleNames());

        StringBuilder problems = new StringBuilder();
        int sum = 0;
        for (Map.Entry<String, String> e : EXPECTED_ROLE_IDS.entrySet()) {
            RoleSpec spec = MATRIX.forRole(e.getKey());
            if (spec == null) {
                problems.append("\n  • role '").append(e.getKey()).append("' absent from CSV");
                continue;
            }
            if (!e.getValue().equals(spec.roleId)) {
                problems.append("\n  • role '").append(e.getKey()).append("' role_id ")
                        .append(spec.roleId).append(" != expected ").append(e.getValue());
            }
            if (spec.permissions.isEmpty()) {
                problems.append("\n  • role '").append(e.getKey()).append("' has no permissions");
            }
            sum += spec.permissions.size();
            ExtentReportManager.logInfo(e.getKey() + " → " + spec.permissions.size()
                    + " distinct permissions (role_id " + spec.roleId + ")");
        }

        // Snapshot of the loaded export (prod_permissions-by-role_202606151113.csv):
        // 7 roles, 555 grants total. Guards against a truncated/corrupt CSV.
        Assert.assertEquals(MATRIX.totalGrants(), 555,
                "Expected 555 total grants in the pinned prod export, got " + MATRIX.totalGrants());
        Assert.assertEquals(sum, 555,
                "Sum of distinct per-role permission counts should be 555, got " + sum);

        if (problems.length() > 0) {
            ExtentReportManager.logFail("CSV integrity problems:" + problems);
            Assert.fail("RBAC matrix CSV integrity failed:" + problems);
        }
        ExtentReportManager.logPass("RBAC matrix CSV intact: 7 roles, 555 grants, all role_ids correct");
    }

    // --- helpers ------------------------------------------------------------

    /** Log in via {@code POST /auth/login}; return the bearer access_token, or null if login failed. */
    private String loginToken(String email, String password) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            payload.put("password", password);
            payload.put("subdomain", AppConstants.VALID_COMPANY_CODE);

            Response r = given()
                    .contentType(ContentType.JSON)
                    .body(payload.toString())
                    .when().post("/auth/login")
                    .then().extract().response();

            if (r.getStatusCode() != 200) return null;
            String body = r.asString();
            if (body == null || body.trim().startsWith("<")) return null;
            return r.jsonPath().getString("access_token");
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    private static Set<String> setOf(String... items) {
        Set<String> s = new LinkedHashSet<>();
        for (String i : items) s.add(i);
        return s;
    }
}
