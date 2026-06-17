package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.RolePermissionMatrix;
import com.egalvanic.qa.utils.RolePermissionMatrix.RoleSpec;

import io.restassured.RestAssured;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * RBAC permission-contract tests — role-level summary view over the production
 * permission matrix in {@code testcase/prod_permissions-by-role_*.csv}.
 *
 * <p>For every role this logs in via the API, calls {@code GET /api/auth/me}
 * (the endpoint the React app uses to gate its UI), and asserts the live
 * permission set matches the documented matrix as a whole:</p>
 *
 * <ul>
 *   <li><b>Identity</b> — {@code roles[0].id}/{@code name} equal the CSV role.</li>
 *   <li><b>No privilege escalation (security)</b> — the live set contains NO
 *       permission beyond what the matrix grants. Hard fail on any extra.</li>
 *   <li><b>Completeness</b> — every granted permission is present, except the
 *       documented prod-vs-QA drift ({@link RbacFixtures#KNOWN_QA_DRIFT}).</li>
 *   <li><b>Derived-field consistency</b> — {@code has_web_access} agrees with the
 *       {@code platform.web} grant, and {@code is_admin} agrees with the Admin role.</li>
 * </ul>
 *
 * <p>For the granular, per-cell view (one reported test case per
 * role×permission), see {@link RolePermissionMatrixCellTest}.</p>
 *
 * <p>Roles whose QA test account is not yet provisioned (login ≠ 200) are
 * SKIPPED with a clear message rather than failed.</p>
 */
public class RoleBasedPermissionContractTest extends BaseAPITest {

    private static RolePermissionMatrix MATRIX;

    /**
     * Pinned snapshot of the baseline prod export (prod_permissions-by-role_202606151113.csv).
     * These detect a truncated/corrupt CSV. If you INTENTIONALLY re-export the prod matrix and
     * the counts legitimately change, update these two constants.
     */
    private static final int EXPECTED_SNAPSHOT_GRANTS = 555;
    private static final int EXPECTED_SNAPSHOT_PERMS = 113;

    @BeforeClass(alwaysRun = true)
    public void loadMatrix() {
        RestAssured.baseURI = API_BASE_URL;
        MATRIX = RolePermissionMatrix.load(AppConstants.RBAC_CSV_PATH);
        System.out.println("[RBAC] Loaded matrix: " + MATRIX.roleCount() + " roles, "
                + MATRIX.totalGrants() + " grants from " + AppConstants.RBAC_CSV_PATH);
    }

    @DataProvider(name = "roles")
    public Object[][] roles() {
        java.util.List<Role> rs = RbacFixtures.selectedRoles();   // honors -Drbac.roles
        Object[][] data = new Object[rs.size()][1];
        for (int i = 0; i < rs.size(); i++) data[i][0] = rs.get(i);
        return data;
    }

    @Test(dataProvider = "roles", description = "Live /auth/me permission set matches the prod matrix for each role")
    public void testRolePermissionContract(Role role) {
        String roleName = role.name;
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS,
                "RBAC Contract: " + roleName);

        RoleSpec expected = MATRIX.forRole(roleName);
        Assert.assertNotNull(expected,
                "Role '" + roleName + "' is missing from the permission matrix CSV");
        ExtentReportManager.logInfo("Role '" + roleName + "' — matrix grants "
                + expected.permissions.size() + " permissions (role_id " + expected.roleId + ")");

        // --- Login + fetch the live set (skip, don't fail, if not provisioned) ---
        LiveAuth la = RbacFixtures.cachedLiveAuth(role);
        if (!la.provisioned) {
            String msg = "Test account not provisioned for role '" + roleName + "' ("
                    + role.email + " → login status " + la.loginStatus + "). Create the account or "
                    + "set its *_EMAIL/*_PASSWORD env vars to enable coverage of this role.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }
        String mismatch = RbacFixtures.roleMismatchSkipMessage(role, la);
        if (mismatch != null) { ExtentReportManager.logSkip(mismatch); throw new SkipException(mismatch); }

        Set<String> live = new TreeSet<>(la.permissions);
        ExtentReportManager.logInfo("Live /auth/me returned " + live.size() + " permissions");

        StringBuilder failures = new StringBuilder();

        // 1) Identity — the account must actually be the role we think it is. Compare against
        // the QA-expected role_id (a documented per-env UUID override where one exists, else
        // the prod/CSV id), so a benign per-environment UUID difference is not a false failure.
        String expectedLiveId = RbacFixtures.expectedLiveRoleId(roleName, expected.roleId);
        if (!expectedLiveId.equals(la.roleId)) {
            failures.append("\n  • role_id mismatch: expected ").append(expectedLiveId)
                    .append(" but /auth/me says ").append(la.roleId)
                    .append(" (").append(la.roleName).append(")");
        }
        if (la.roleName != null && !roleName.equals(la.roleName)) {
            failures.append("\n  • role name mismatch: expected '").append(roleName)
                    .append("' but /auth/me says '").append(la.roleName).append("'");
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
        Set<String> drift = RbacFixtures.KNOWN_QA_DRIFT.getOrDefault(roleName, new TreeSet<>());
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
        if (la.hasWebAccess == null || la.hasWebAccess != expectWeb) {
            failures.append("\n  • has_web_access=").append(la.hasWebAccess)
                    .append(" but matrix ").append(expectWeb ? "grants" : "does NOT grant")
                    .append(" platform.web (expected ").append(expectWeb).append(")");
        }
        // 5) is_admin flag must match the Admin role only.
        boolean expectAdmin = "Admin".equals(roleName);
        if (la.isAdmin == null || la.isAdmin != expectAdmin) {
            failures.append("\n  • is_admin=").append(la.isAdmin)
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

        Map<String, String> expectedRoleIds = RbacFixtures.expectedRoleIds();
        Assert.assertEquals(MATRIX.roleCount(), expectedRoleIds.size(),
                "Matrix should contain exactly " + expectedRoleIds.size() + " roles, got "
                        + MATRIX.roleCount() + ": " + MATRIX.roleNames());

        StringBuilder problems = new StringBuilder();
        int sum = 0;
        for (Map.Entry<String, String> e : expectedRoleIds.entrySet()) {
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

        // Derived invariant (no magic number): per-role distinct counts must sum to the raw
        // grant-row count. A duplicate (role,permission) row would make these disagree.
        Assert.assertEquals(sum, MATRIX.totalGrants(),
                "Per-role distinct permission counts (" + sum + ") must equal total grant rows ("
                        + MATRIX.totalGrants() + ") — a duplicate (role,permission) row would break this.");

        // Pinned snapshot of the baseline export — guards against truncation/corruption.
        Assert.assertEquals(MATRIX.totalGrants(), EXPECTED_SNAPSHOT_GRANTS,
                "Baseline export had " + EXPECTED_SNAPSHOT_GRANTS + " grants; got " + MATRIX.totalGrants()
                        + ". If you re-exported the prod CSV on purpose, update EXPECTED_SNAPSHOT_GRANTS.");
        Assert.assertEquals(MATRIX.allPermissions().size(), EXPECTED_SNAPSHOT_PERMS,
                "Baseline export had " + EXPECTED_SNAPSHOT_PERMS + " distinct permissions; got "
                        + MATRIX.allPermissions().size()
                        + ". If you re-exported the prod CSV on purpose, update EXPECTED_SNAPSHOT_PERMS.");

        if (problems.length() > 0) {
            ExtentReportManager.logFail("CSV integrity problems:" + problems);
            Assert.fail("RBAC matrix CSV integrity failed:" + problems);
        }
        ExtentReportManager.logPass("RBAC matrix CSV intact: " + MATRIX.roleCount() + " roles, "
                + MATRIX.totalGrants() + " grants, " + MATRIX.allPermissions().size()
                + " permissions, all role_ids correct");
    }
}
