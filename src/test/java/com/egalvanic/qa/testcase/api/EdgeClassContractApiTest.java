package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Edge (connection) class taxonomy conformance (API).</b> Verifies the live
 * {@code GET /api/edge_classes} taxonomy — the conductor/connection types the Connections module and
 * the SLD edge picker render from — exposes every class defined in
 * {@code testcase/edge_classes_template (3).xlsx} (the latest edge-class template: Busway, Cable, DC Cable).
 *
 * <p>API-level (not UI) for determinism, matching {@link IssueClassContractApiTest} and
 * {@link NodeClassContractApiTest}. <b>Subset check</b> — the QA tenant also carries junk edge classes
 * (DEVTOOL_TEST EdgeClass Updated, TestClass, NewNewClass, Cable - Kurt/Simple/Egan) that's ignored.</p>
 */
public class EdgeClassContractApiTest extends BaseAPITest {

    /** The 3 edge classes defined in testcase/edge_classes_template (3).xlsx (latest). */
    private static final String[] TEMPLATE_CLASSES = {"Busway", "Cable", "DC Cable"};

    @Test(description = "Live /api/edge_classes exposes every class in edge_classes_template (3).xlsx")
    public void testEdgeClassTaxonomyMatchesTemplate() {
        ExtentReportManager.createTest(AppConstants.MODULE_CONNECTIONS, "Edge Class Contract",
                "Edge-class taxonomy conformance (template vs live)");

        Response resp = getAuthenticatedRequestSpec().when().get("/edge_classes").then().extract().response();
        Assert.assertEquals(resp.getStatusCode(), 200,
                "GET /api/edge_classes should return 200, got " + resp.getStatusCode() + ": " + truncate(resp.asString()));
        List<String> names = resp.jsonPath().getList("name");
        Assert.assertNotNull(names, "GET /api/edge_classes returned no 'name' fields: " + truncate(resp.asString()));

        Set<String> live = new LinkedHashSet<>();
        for (String n : names) if (n != null) live.add(n.trim());
        ExtentReportManager.logInfo("Live edge classes (" + live.size() + " incl. tenant extras): " + live);

        List<String> missing = new ArrayList<>();
        for (String expected : TEMPLATE_CLASSES) {
            boolean present = false;
            for (String l : live) if (l.equalsIgnoreCase(expected)) { present = true; break; }
            if (!present) missing.add(expected);
        }
        Assert.assertTrue(missing.isEmpty(),
                "Live /api/edge_classes is MISSING template-defined classes " + missing
                        + " (per edge_classes_template (3).xlsx). Live taxonomy: " + live);
        ExtentReportManager.logPass("All " + TEMPLATE_CLASSES.length
                + " template edge classes present in live /api/edge_classes.");
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
