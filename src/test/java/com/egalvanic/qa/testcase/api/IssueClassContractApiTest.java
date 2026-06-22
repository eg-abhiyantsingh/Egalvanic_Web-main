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
 * <b>Issue-class taxonomy conformance (API).</b> Verifies the live {@code GET /api/issue_classes}
 * taxonomy — the data that populates the Create-Issue "Issue Class" dropdown — exposes every class
 * defined in {@code testcase/issue_classes_template.xlsx} (the source-of-truth template, 7 classes).
 *
 * <p>Done at the API layer, not the UI: the MUI Autocomplete "Issue Class" dropdown is unreliable to
 * open in Selenium (the input locator times out headed), whereas {@code /api/issue_classes} is
 * deterministic and is exactly what the dropdown renders from. This is the issue-side analogue of the
 * RBAC permission contract test and the asset node-classes gold conformance.</p>
 *
 * <p><b>Subset check.</b> The QA tenant also carries junk classes (e.g. {@code DEVTOOL_TEST…},
 * {@code Test}/{@code Test 1}, {@code Other}) and duplicate global/override rows — those are
 * intentionally ignored; we only assert the template's classes are all <i>present</i>.</p>
 */
public class IssueClassContractApiTest extends BaseAPITest {

    /** The 7 issue classes defined in testcase/issue_classes_template.xlsx (latest). */
    private static final String[] TEMPLATE_CLASSES = {
            "NEC Violation", "NFPA 70B Violation", "OSHA Violation",
            "Repair Needed", "Replacement Needed", "Thermal Anomaly", "Ultrasonic Anomaly"
    };

    /** Violation classes whose Subcategory is a real, stable enum the app must offer. */
    private static final String[] VIOLATION_CLASSES = {
            "NEC Violation", "NFPA 70B Violation", "OSHA Violation"
    };

    @Test(description = "Live /api/issue_classes exposes every class in issue_classes_template.xlsx")
    public void testIssueClassTaxonomyMatchesTemplate() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, "Issue Class Contract",
                "Issue-class taxonomy conformance (template vs live)");

        Response resp = fetchIssueClasses();
        List<String> names = resp.jsonPath().getList("name");
        Assert.assertNotNull(names, "GET /api/issue_classes returned no 'name' fields: " + truncate(resp.asString()));

        Set<String> live = new LinkedHashSet<>();
        for (String n : names) if (n != null) live.add(n.trim());
        ExtentReportManager.logInfo("Live issue classes (" + live.size() + " incl. tenant extras): " + live);

        List<String> missing = new ArrayList<>();
        for (String expected : TEMPLATE_CLASSES) {
            boolean present = false;
            for (String l : live) if (l.equalsIgnoreCase(expected)) { present = true; break; }
            if (!present) missing.add(expected);
        }
        Assert.assertTrue(missing.isEmpty(),
                "Live /api/issue_classes is MISSING template-defined classes " + missing
                        + " (per testcase/issue_classes_template.xlsx). Live taxonomy: " + live);
        ExtentReportManager.logPass("All " + TEMPLATE_CLASSES.length
                + " template issue classes are present in live /api/issue_classes.");
    }

    @Test(description = "Each violation class exposes a Subcategory with selectable options (live)")
    public void testViolationClassesHaveSubcategoryOptions() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, "Issue Class Contract",
                "Violation classes expose Subcategory options");

        Response resp = fetchIssueClasses();
        for (String vc : VIOLATION_CLASSES) {
            // GPath: first class with this name → its 'Subcategory' attribute → its options.
            List<Object> options = resp.jsonPath().getList(
                    "find { it.name == '" + vc + "' }.definition.find { it.name == 'Subcategory' }.options");
            Assert.assertNotNull(options,
                    "'" + vc + "' has no 'Subcategory' attribute in /api/issue_classes (template defines one).");
            Assert.assertFalse(options.isEmpty(),
                    "'" + vc + "' Subcategory exposes no options live (template defines a select list).");
            ExtentReportManager.logPass("'" + vc + "' Subcategory has " + options.size() + " options live.");
        }
    }

    private Response fetchIssueClasses() {
        Response resp = getAuthenticatedRequestSpec().when().get("/issue_classes").then().extract().response();
        Assert.assertEquals(resp.getStatusCode(), 200,
                "GET /api/issue_classes should return 200, got " + resp.getStatusCode() + ": " + truncate(resp.asString()));
        return resp;
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
