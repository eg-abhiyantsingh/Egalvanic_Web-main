package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.RolePermissionMatrix;
import com.egalvanic.qa.utils.RolePermissionMatrix.RoleSpec;

import io.restassured.RestAssured;

import org.testng.Assert;
import org.testng.ITest;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Per-cell RBAC coverage</b> — turns every cell of the production
 * role × permission matrix ({@code testcase/prod_permissions-by-role_*.csv})
 * into its own, individually-reported test case.
 *
 * <p>For the full permission vocabulary (every distinct {@code permission_name}
 * granted to any role) and every role, this emits one test case asserting:</p>
 * <ul>
 *   <li><b>granted cell</b> — the matrix grants it ⇒ {@code /auth/me} must contain it
 *       (positive). Missing ⇒ FAIL, unless it is documented prod-vs-QA drift ⇒ SKIP.</li>
 *   <li><b>denied cell</b> — the matrix does NOT grant it ⇒ {@code /auth/me} must NOT
 *       contain it (negative). Present ⇒ FAIL (privilege escalation).</li>
 * </ul>
 *
 * <p>With 7 roles × 113 permissions this is <b>791 test cases</b> (555 granted +
 * 236 denied). Each role is logged in <b>once</b> (in {@link #loginAllRoles()}),
 * its live permission set cached, and every cell asserted against that cache, so
 * the whole matrix costs 7 logins, not 791. Roles whose QA account is not
 * provisioned have all their cells SKIPPED with a clear message.</p>
 */
public class RolePermissionMatrixCellTest extends BaseAPITest implements ITest {

    private static RolePermissionMatrix MATRIX;
    private static final Map<String, LiveAuth> LIVE = new LinkedHashMap<>();

    /**
     * Per-invocation test name so the native TestNG/surefire/JUnit-XML reports
     * (and CI) show e.g. "Technician / accounts.view [denied]" instead of 791
     * identical "permissionCell" rows. Set at the top of each cell.
     */
    private final ThreadLocal<String> currentCellName = new ThreadLocal<>();

    @Override
    public String getTestName() {
        String n = currentCellName.get();
        return n != null ? n : "permissionCell";
    }

    @BeforeClass(alwaysRun = true)
    public void loginAllRoles() {
        RestAssured.baseURI = API_BASE_URL;
        MATRIX = RolePermissionMatrix.load(AppConstants.RBAC_CSV_PATH);
        System.out.println("[RBAC-CELL] Matrix: " + MATRIX.roleCount() + " roles, "
                + MATRIX.allPermissions().size() + " distinct permissions, "
                + MATRIX.totalGrants() + " grants.");
        for (Role role : RbacFixtures.selectedRoles()) {   // honors -Drbac.roles
            LiveAuth la = RbacFixtures.cachedLiveAuth(role);
            LIVE.put(role.name, la);
            System.out.println("[RBAC-CELL] " + role.name + ": "
                    + (la.provisioned ? la.permissions.size() + " live permissions"
                                      : "UNPROVISIONED (login status " + la.loginStatus + ")"));
        }
    }

    /**
     * One row per (role, permission) cell across the entire vocabulary.
     * 7 roles × 113 permissions = 791 cells.
     */
    @DataProvider(name = "cells")
    public Object[][] cells() {
        RolePermissionMatrix matrix = RolePermissionMatrix.load(AppConstants.RBAC_CSV_PATH);
        List<Object[]> rows = new ArrayList<>();
        for (Role role : RbacFixtures.selectedRoles()) {   // honors -Drbac.roles
            RoleSpec spec = matrix.forRole(role.name);
            for (String permission : matrix.allPermissions()) {
                boolean granted = spec != null && spec.permissions.contains(permission);
                rows.add(new Object[]{role.name, permission, granted});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "cells",
          description = "Each role×permission cell of the matrix is enforced live via /auth/me")
    public void permissionCell(String roleName, String permission, boolean expectedGranted) {
        String label = permission + (expectedGranted ? " [granted]" : " [denied]");
        currentCellName.set(roleName + " / " + label);
        ExtentReportManager.createTest(
                AppConstants.MODULE_AUTHENTICATION, "RBAC: " + roleName, label);

        LiveAuth la = LIVE.get(roleName);
        if (la == null || !la.provisioned) {
            String msg = "Role '" + roleName + "' account not provisioned (login status "
                    + (la == null ? "n/a" : la.loginStatus) + ") — cell '" + permission + "' not covered.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }
        // Mis-provisioned fixture (account logs in as a different role) → can't verify this role's cell.
        if (la.roleName != null && !roleName.equals(la.roleName)) {
            String msg = "Role '" + roleName + "' account is mis-provisioned (logs in as '" + la.roleName
                    + "') — cell '" + permission + "' not covered; fix the QA account.";
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }
        // Contaminated fixture: account also carries the "EG Admin" super-admin overlay → its
        // full-access permission set makes every "denied" cell falsely trip. Skip, don't fail
        // (a real escalation lacks the assigned overlay and still fails — see fixture Javadoc).
        String overlay = RbacFixtures.superAdminOverlaySkipMessage(roleName, null, la);
        if (overlay != null) {
            ExtentReportManager.logSkip(overlay);
            throw new SkipException(overlay);
        }

        boolean actuallyPresent = la.permissions.contains(permission);

        if (expectedGranted) {
            if (actuallyPresent) {
                ExtentReportManager.logPass("GRANTED & present: '" + roleName + "' has '" + permission + "'");
                return;
            }
            if (RbacFixtures.isKnownDrift(roleName, permission)) {
                String msg = "KNOWN prod-vs-QA drift: '" + roleName + "' is granted '" + permission
                        + "' in the prod matrix but it is absent on the QA tenant.";
                ExtentReportManager.logWarning(msg);
                throw new SkipException(msg);
            }
            String fail = "MISSING permission: matrix grants '" + permission + "' to '" + roleName
                    + "' but /auth/me does NOT return it.";
            ExtentReportManager.logFail(fail);
            Assert.fail(fail);
        } else {
            if (!actuallyPresent) {
                ExtentReportManager.logPass("DENIED & absent: '" + roleName + "' correctly lacks '" + permission + "'");
                return;
            }
            String fail = "PRIVILEGE ESCALATION: matrix does NOT grant '" + permission + "' to '"
                    + roleName + "' but /auth/me returns it.";
            ExtentReportManager.logFail(fail);
            Assert.fail(fail);
        }
    }
}
