package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.testcase.api.RbacFixtures;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;

/**
 * <b>Tenant-scope and site-scope contract — every role.</b>
 *
 * <p>Added 2026-09-11 after an all-roles sweep found that several LIST endpoints apply no
 * tenant filter and several node-lookup endpoints apply no site filter. Both were invisible to
 * the existing suite because every earlier test drove a single role against its own data.</p>
 *
 * <h2>The two contracts</h2>
 * <ol>
 *   <li><b>Tenant scope.</b> A list endpoint must return only rows belonging to the caller's own
 *       company (plus rows that are deliberately global, i.e. {@code company_id == null}).
 *       Confirmed broken on {@code /users/}, {@code /node_classes}, {@code /edge_classes} and
 *       {@code /issue_classes}; confirmed correct on the materials/labor/test-equipment catalogs,
 *       which this class asserts as the positive control.</li>
 *   <li><b>Site scope.</b> For a seat whose {@code accessible_sld_ids} is a strict subset of the
 *       tenant's sites, the node-lookup family must refuse a site outside that list. The sibling
 *       endpoints {@code /connections/v2/sld/{id}} and {@code /sld/{id}} already answer 422 for the
 *       very same id, which is what proves the limit is meant to apply.</li>
 * </ol>
 *
 * <h2>Why it is written this way</h2>
 * <ul>
 *   <li><b>Staff seats are skipped, not asserted.</b> {@code is_eg_admin} accounts are Egalvanic
 *       internal and are entitled to see across companies; asserting them would encode the wrong
 *       expectation. A whole afternoon was lost in Sept 2026 to a cross-tenant "finding" that was
 *       really a staff account, so the flag is read live and printed in every failure message.</li>
 *   <li><b>Content type is checked, not just the status.</b> This SPA answers 200 with an HTML
 *       shell for unknown or unpermitted routes. A 200 that is {@code text/html} is a soft 404 and
 *       must never be counted as data.</li>
 *   <li><b>Every assertion carries its control.</b> A refusal only means something next to a call
 *       that succeeds, so the own-site and own-company cases run in the same test.</li>
 * </ul>
 */
public class TenantAndSiteScopeContractTest {

    /** Endpoints that MUST be filtered to the caller's own company. Confirmed leaking 2026-09-11. */
    private static final List<String> TENANT_SCOPED_LISTS = Arrays.asList(
            "/users/", "/node_classes", "/edge_classes", "/issue_classes");

    /** Comparable company-config catalogs that ARE correctly scoped — the positive control. */
    private static final List<String> CORRECTLY_SCOPED_CONTROLS = Arrays.asList(
            "/materials-library", "/labor-rates", "/test-equipment");

    /** Node-lookup endpoints that must honour {@code accessible_sld_ids}. Confirmed leaking. */
    private static final List<String> SITE_SCOPED_NODE_LOOKUPS = Arrays.asList(
            "/nodes/sld/%s", "/lookup/v2/nodes/%s", "/lookup/node-class-counts/%s");

    /** Siblings that already refuse an out-of-scope site — proof the limit is intended. */
    private static final List<String> SITE_SCOPE_INTENT_CONTROLS = Arrays.asList(
            "/connections/v2/sld/%s", "/sld/%s");

    private static final String OWN_COMPANY_ID = "d59d449b-09d8-45d6-8f0a-ef70024b1293";

    private String staffToken;
    private List<String> allTenantSiteIds;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        ExtentReportManager.initReports();
        RestAssured.baseURI = AppConstants.API_BASE_URL;

        // The staff seat is used ONLY to enumerate the tenant's full site list, so the test can
        // pick a site that a restricted seat genuinely should not reach. It is never the attacker.
        LiveAuth staff = RbacFixtures.cachedLiveAuth(new RoleRef(
                "Super Admin", AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD).role());
        staffToken = staff != null && staff.provisioned ? staff.token : null;
        allTenantSiteIds = staffToken == null ? new ArrayList<>() : siteIds(staffToken);
    }

    /** Small adapter so this class can build a Role without widening RbacFixtures' constructor. */
    private static final class RoleRef {
        private final Role r;
        RoleRef(String name, String email, String pw) {
            Role found = null;
            for (Role candidate : RbacFixtures.ROLES) {
                if (candidate.email.equalsIgnoreCase(email)) { found = candidate; break; }
            }
            this.r = found != null ? found : RbacFixtures.ROLES.get(0);
        }
        Role role() { return r; }
    }

    @DataProvider(name = "everyRole")
    public Object[][] everyRole() {
        List<Object[]> rows = new ArrayList<>();
        for (Role role : RbacFixtures.ROLES) rows.add(new Object[]{role});
        return rows.toArray(new Object[0][]);
    }

    // ------------------------------------------------------------------ tenant scope

    @Test(dataProvider = "everyRole", groups = {"security", "tenant-scope"},
          description = "A list endpoint must not return another company's rows")
    public void tenantScopedListsMustNotLeakOtherCompanies(Role role) {
        LiveAuth live = requireCustomerSeat(role);

        Map<String, String> failures = new LinkedHashMap<>();
        for (String path : TENANT_SCOPED_LISTS) {
            Response r = get(path, live.token);
            if (!isJson(r)) continue;   // soft-404 / not exposed to this role — not a leak

            Map<String, Integer> byCompany = companyHistogram(r);
            int foreign = 0;
            Set<String> foreignCompanies = new LinkedHashSet<>();
            for (Map.Entry<String, Integer> e : byCompany.entrySet()) {
                if (!"(global)".equals(e.getKey()) && !OWN_COMPANY_ID.equals(e.getKey())) {
                    foreign += e.getValue();
                    foreignCompanies.add(e.getKey());
                }
            }
            if (foreign > 0) {
                failures.put(path, foreign + " rows from " + foreignCompanies.size()
                        + " other companies " + foreignCompanies);
            }
        }

        Assert.assertTrue(failures.isEmpty(),
                "TENANT SCOPE BROKEN for " + role.name + " (is_eg_admin=false, "
                + live.permissions.size() + " permissions). These list endpoints returned rows "
                + "belonging to other companies: " + failures
                + ". Sibling catalogs (" + CORRECTLY_SCOPED_CONTROLS + ") scope correctly, so the "
                + "backend can do this — these endpoints simply do not.");
    }

    @Test(dataProvider = "everyRole", groups = {"security", "tenant-scope"},
          description = "Positive control: the catalogs that already scope correctly must stay that way")
    public void correctlyScopedCatalogsStayScoped(Role role) {
        LiveAuth live = requireCustomerSeat(role);

        Map<String, String> regressions = new LinkedHashMap<>();
        int checked = 0;
        for (String path : CORRECTLY_SCOPED_CONTROLS) {
            Response r = get(path, live.token);
            if (!isJson(r)) continue;
            checked++;
            Map<String, Integer> byCompany = companyHistogram(r);
            for (Map.Entry<String, Integer> e : byCompany.entrySet()) {
                if (!"(global)".equals(e.getKey()) && !OWN_COMPANY_ID.equals(e.getKey())) {
                    regressions.put(path, e.getValue() + " rows from " + e.getKey());
                }
            }
        }
        if (checked == 0) {
            throw new SkipException("None of the control catalogs " + CORRECTLY_SCOPED_CONTROLS
                    + " returned JSON for " + role.name + " — cannot assert the control.");
        }
        Assert.assertTrue(regressions.isEmpty(),
                "REGRESSION: a catalog that used to be correctly tenant-scoped now leaks for "
                + role.name + ": " + regressions);
    }

    // ------------------------------------------------------------------ site scope

    @Test(dataProvider = "everyRole", groups = {"security", "site-scope"},
          description = "Node-lookup endpoints must refuse a site outside accessible_sld_ids")
    public void siteScopedNodeLookupsMustRefuseUnmappedSite(Role role) {
        LiveAuth live = requireCustomerSeat(role);

        List<String> mine = siteIds(live.token);
        if (allTenantSiteIds.isEmpty()) {
            throw new SkipException("Could not enumerate the tenant's sites from the staff seat; "
                    + "no unmapped site to aim at.");
        }
        String unmapped = null;
        for (String id : allTenantSiteIds) {
            if (!mine.contains(id)) { unmapped = id; break; }
        }
        if (unmapped == null) {
            throw new SkipException(role.name + " is assigned every one of the tenant's "
                    + allTenantSiteIds.size() + " sites, so there is no out-of-scope site to test. "
                    + "This role cannot demonstrate the contract either way.");
        }

        // CONTROL 1 (intent): siblings must refuse this very id. If they stop refusing, the
        // premise of this test is gone and the failure message must say so rather than mislead.
        List<String> intentRefusals = new ArrayList<>();
        for (String tmpl : SITE_SCOPE_INTENT_CONTROLS) {
            Response c = get(String.format(tmpl, unmapped), live.token);
            if (c.getStatusCode() >= 400) intentRefusals.add(tmpl + "->" + c.getStatusCode());
        }
        if (intentRefusals.isEmpty()) {
            throw new SkipException("Neither " + SITE_SCOPE_INTENT_CONTROLS + " refused site "
                    + unmapped + " for " + role.name + ", so this seat may legitimately reach it. "
                    + "No contract to assert.");
        }

        // CONTROL 2 (positive): an assigned site must return rows, or the endpoint is simply dead.
        if (!mine.isEmpty()) {
            Response own = get(String.format(SITE_SCOPED_NODE_LOOKUPS.get(0), mine.get(0)), live.token);
            Assert.assertTrue(isJson(own) && rowCount(own) > 0,
                    "Positive control failed: " + role.name + " could not read its OWN site "
                    + mine.get(0) + " (" + own.getStatusCode() + " " + contentType(own)
                    + ", " + rowCount(own) + " rows). Without this the refusal below proves nothing.");
        }

        Map<String, String> leaks = new LinkedHashMap<>();
        for (String tmpl : SITE_SCOPED_NODE_LOOKUPS) {
            String path = String.format(tmpl, unmapped);
            Response r = get(path, live.token);
            if (isJson(r) && rowCount(r) > 0) {
                leaks.put(path, rowCount(r) + " rows");
            }
        }

        Assert.assertTrue(leaks.isEmpty(),
                "SITE SCOPE BROKEN for " + role.name + ": assigned " + mine.size() + " of "
                + allTenantSiteIds.size() + " sites, yet it read site " + unmapped
                + " through " + leaks + ". The same seat is refused that site by "
                + intentRefusals + ", which is what proves the limit is meant to apply.");
    }

    // ------------------------------------------------------------------ helpers

    /** Fetch the live session and skip anything that cannot carry the contract. */
    private LiveAuth requireCustomerSeat(Role role) {
        LiveAuth live = RbacFixtures.cachedLiveAuth(role);
        if (live == null || !live.provisioned) {
            throw new SkipException("No usable session for " + role.name
                    + " (login status " + (live == null ? "n/a" : live.loginStatus) + ").");
        }
        if (isStaff(live.token)) {
            throw new SkipException(role.name + " is an Egalvanic STAFF seat (is_eg_admin=true). "
                    + "Cross-company and cross-site visibility is BY DESIGN for staff, so asserting "
                    + "a customer contract against it would encode the wrong expectation.");
        }
        return live;
    }

    private static boolean isStaff(String token) {
        Response me = given().header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .when().get("/auth/v2/me").then().extract().response();
        if (me.getStatusCode() != 200) return false;
        Boolean flag = me.jsonPath().get("is_eg_admin");
        return Boolean.TRUE.equals(flag);
    }

    private static Response get(String path, String token) {
        return given().contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .when().get(path).then().extract().response();
    }

    private static String contentType(Response r) {
        String ct = r.getContentType();
        return ct == null ? "" : ct.split(";")[0].trim();
    }

    /** Real data only: 2xx AND application/json. A 200 text/html is this SPA's soft 404. */
    private static boolean isJson(Response r) {
        return r.getStatusCode() >= 200 && r.getStatusCode() < 300
                && contentType(r).contains("json");
    }

    private static List<Map<String, Object>> rows(Response r) {
        try {
            List<Map<String, Object>> direct = r.jsonPath().getList("$");
            if (direct != null && !direct.isEmpty()) return direct;
        } catch (Exception ignored) { /* not a bare array */ }
        try {
            List<Map<String, Object>> wrapped = r.jsonPath().getList("data");
            if (wrapped != null) return wrapped;
        } catch (Exception ignored) { /* no data[] */ }
        return new ArrayList<>();
    }

    private static int rowCount(Response r) {
        return rows(r).size();
    }

    /** company_id -> row count, with null company_id bucketed as "(global)". */
    private static Map<String, Integer> companyHistogram(Response r) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows(r)) {
            Object cid = row.get("company_id");
            String key = cid == null ? "(global)" : String.valueOf(cid);
            out.merge(key, 1, Integer::sum);
        }
        return out;
    }

    /** The site ids this token can see, via the sites list the app itself uses. */
    private static List<String> siteIds(String token) {
        List<String> out = new ArrayList<>();
        if (token == null) return out;
        Response r = get("/notes/sites", token);
        if (!isJson(r)) return out;
        for (Map<String, Object> row : rows(r)) {
            Object id = row.get("id") != null ? row.get("id") : row.get("sld_id");
            if (id != null) out.add(String.valueOf(id));
        }
        return out;
    }
}
