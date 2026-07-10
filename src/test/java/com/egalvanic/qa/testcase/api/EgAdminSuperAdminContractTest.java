package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.RolePermissionMatrix;

import io.restassured.RestAssured;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.TreeSet;

/**
 * <b>EG Admin is a full super-admin.</b> The other RBAC tests <em>tolerate</em> the EG Admin overlay
 * (they ignore its extra access when checking a base role). This test does the opposite — it
 * <em>positively verifies</em> that the EG Admin role genuinely delivers complete, super-admin access,
 * so "EG Admin = super admin" is a proven guarantee, not an assumption.
 *
 * <p>Any account currently holding EG Admin gives a super-admin session (all five base-role QA accounts
 * carry it today). This logs in as the first such account and asserts the super-admin contract:</p>
 * <ul>
 *   <li><b>is_admin = true</b> and <b>has_web_access = true</b> (a super-admin is an admin with web access).</li>
 *   <li><b>roles[] includes "EG Admin"</b> (the session really is the super-admin role).</li>
 *   <li><b>Full access</b> — the live permission set is a <b>superset of the entire prod permission
 *       matrix</b> ({@link RolePermissionMatrix#allPermissions()}); EG Admin holds every documented
 *       permission. (Superset, not an exact count, so a benign +/- of one grant does not false-fail.)</li>
 *   <li><b>Exclusive Reporting + Forms module</b> — the capability EG Admin uniquely adds: at least one
 *       {@code reports.*} and one {@code forms.*} permission are present.</li>
 * </ul>
 *
 * <p>If no EG-Admin-carrying account is provisioned this run, the test SKIPs (nothing to verify) rather
 * than failing.</p>
 */
public class EgAdminSuperAdminContractTest extends BaseAPITest {

    private static RolePermissionMatrix MATRIX;
    private static LiveAuth egAdminSession;
    private static String egAdminAccount;
    private static boolean anyProvisioned;      // did ANY role account log in this run?
    private static final StringBuilder loginTrace = new StringBuilder();

    @BeforeClass(alwaysRun = true)
    public void resolveSuperAdminSession() {
        RestAssured.baseURI = API_BASE_URL;
        // Infra prerequisite: the 113-perm vocabulary must load. A missing/corrupt CSV is a hard ERROR
        // (RolePermissionMatrix.load throws), never a swallowed skip — the superset check depends on it.
        MATRIX = RolePermissionMatrix.load(AppConstants.RBAC_CSV_PATH);
        for (Role role : RbacFixtures.selectedRoles()) {
            LiveAuth la = RbacFixtures.cachedLiveAuth(role);
            if (la.provisioned) anyProvisioned = true;
            loginTrace.append("\n    ").append(role.email).append(" → ")
                    .append(la.provisioned ? "provisioned, roles=" + la.roleNames : "login " + la.loginStatus);
            if (RbacFixtures.holdsEgAdmin(la)) {
                egAdminSession = la;
                egAdminAccount = role.email + " (base role '" + role.name + "' + EG Admin overlay)";
                break;
            }
        }
        System.out.println("[EG-Admin] super-admin session: "
                + (egAdminSession != null ? egAdminAccount + " — " + egAdminSession.permissions.size() + " perms"
                                          : "NONE available this run") + loginTrace);
    }

    @Test(description = "EG Admin is a full super-admin — admin flag, web access, and every permission (incl. Reporting/Forms)")
    public void egAdminIsFullSuperAdmin() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION,
                AppConstants.FEATURE_ROLE_ACCESS, "EG Admin is a full super-admin");

        if (egAdminSession == null) {
            // Two distinct, non-regression skip reasons (env/provisioning state, not an app failure):
            String msg = anyProvisioned
                ? "Account(s) logged in but NONE carry the EG Admin role this run — the overlay was removed "
                    + "from every base QA account (the cleanup superAdminOverlaySkipMessage recommends). Nothing "
                    + "to verify; add/keep an EG Admin account to enable this check." + loginTrace
                : "No RBAC role account is provisioned this run (every login non-200) — cannot obtain a "
                    + "super-admin session. Env/provisioning issue, not a regression." + loginTrace;
            ExtentReportManager.logSkip(msg);
            throw new SkipException(msg);
        }
        ExtentReportManager.logInfo("Super-admin session from " + egAdminAccount + " — "
                + egAdminSession.permissions.size() + " live permissions.");

        StringBuilder failures = new StringBuilder();

        // 1) the session really is the EG Admin role (subject-integrity anchor; guards a garbled roles[])
        if (!RbacFixtures.holdsEgAdmin(egAdminSession)) {
            failures.append("\n  • roles ").append(egAdminSession.roleNames).append(" do not include 'EG Admin'");
        }
        // 2) admin flag + web access — the definitional super-admin flags
        if (!Boolean.TRUE.equals(egAdminSession.isAdmin)) {
            failures.append("\n  • is_admin is ").append(egAdminSession.isAdmin).append(" — a super-admin must be is_admin=true");
        }
        if (!Boolean.TRUE.equals(egAdminSession.hasWebAccess)) {
            failures.append("\n  • has_web_access is ").append(egAdminSession.hasWebAccess).append(" — a super-admin must have web access");
        }

        // 3) FULL access — live permissions must be a SUPERSET of the entire prod matrix vocabulary.
        //    Tolerance: a matrix perm absent from the QA tenant altogether (documented KNOWN_QA_DRIFT) can't
        //    be held by anyone here, so it warns rather than false-failing — same tolerance the contract test uses.
        Set<String> driftUnion = new TreeSet<>();
        RbacFixtures.KNOWN_QA_DRIFT.values().forEach(driftUnion::addAll);
        Set<String> missing = new TreeSet<>(MATRIX.allPermissions());
        missing.removeAll(egAdminSession.permissions);
        Set<String> missingDrift = new TreeSet<>(missing); missingDrift.retainAll(driftUnion);
        missing.removeAll(driftUnion);
        if (!missingDrift.isEmpty()) {
            ExtentReportManager.logWarning("EG Admin missing " + missingDrift.size()
                    + " permission(s) that are documented QA-tenant drift (absent for everyone on QA): " + missingDrift);
        }
        if (!missing.isEmpty()) {
            failures.append("\n  • NOT full access: EG Admin is missing ").append(missing.size())
                    .append(" of the ").append(MATRIX.allPermissions().size())
                    .append(" matrix permissions (excluding documented drift): ").append(missing);
        }

        // 4) the exclusive Reporting + Forms module EG Admin uniquely adds (drift-robust: any perm in each family)
        boolean hasReporting = egAdminSession.permissions.stream().anyMatch(p -> p.startsWith("reports."));
        boolean hasForms = egAdminSession.permissions.stream().anyMatch(p -> p.startsWith("forms."));
        if (!hasReporting) failures.append("\n  • missing the Reporting module (no reports.* permission)");
        if (!hasForms) failures.append("\n  • missing the Forms module (no forms.* permission)");

        // Soft diagnostics (do not fail the build) — surfaced for follow-up, not gated.
        if (Boolean.TRUE.equals(egAdminSession.hasWebAccess) && !egAdminSession.permissions.contains("platform.web")) {
            ExtentReportManager.logWarning("has_web_access=true but the platform.web permission is absent — flag/grant divergence.");
        }
        Set<String> extras = new TreeSet<>(egAdminSession.permissions);
        extras.removeAll(MATRIX.allPermissions());
        ExtentReportManager.logInfo("EG Admin holds " + egAdminSession.permissions.size() + " permissions — the full "
                + MATRIX.allPermissions().size() + "-perm matrix" + (extras.isEmpty() ? "" : " plus " + extras.size()
                + " super-admin extra(s): " + extras) + ". (Extras are expected as new features ship; not bounded.)");

        if (failures.length() > 0) {
            String report = "EG Admin is NOT a full super-admin:" + failures;
            ExtentReportManager.logFail(report);
            Assert.fail(report);
        }

        ExtentReportManager.logPass("EG Admin confirmed as a full super-admin: is_admin=true, has_web_access=true, "
                + "holds all " + MATRIX.allPermissions().size() + " matrix permissions (" + egAdminSession.permissions.size()
                + " total live), plus the exclusive Reporting + Forms module.");
    }
}
