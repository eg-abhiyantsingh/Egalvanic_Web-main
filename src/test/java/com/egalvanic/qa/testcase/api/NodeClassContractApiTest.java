package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * <b>Node (asset) class taxonomy conformance (API).</b> Verifies the live {@code GET /api/node_classes}
 * taxonomy — the asset-class catalog the Assets page renders from — exposes every class defined in
 * {@code testcase/node_classes_gold.json}, which is regenerated from
 * {@code node_classes_template (13).xlsx} (the latest asset-class template).
 *
 * <p><b>Data-driven from the gold</b> (not a hard-coded list) so it auto-tracks template changes:
 * regenerate the gold from the template and this test follows. API-level (not UI) for determinism,
 * matching {@link IssueClassContractApiTest}. <b>Subset check</b> — the QA tenant also carries junk
 * (QANode/QA_ATS1/Test/Node Bus, duplicate global/override rows) that's intentionally ignored.</p>
 */
public class NodeClassContractApiTest extends BaseAPITest {

    private static final String GOLD_PATH = "testcase/node_classes_gold.json";

    @Test(description = "Live /api/node_classes exposes every class in node_classes_gold.json (from template 13)")
    public void testNodeClassTaxonomyMatchesGold() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, "Node Class Contract",
                "Node-class taxonomy conformance (gold/template 13 vs live)");

        Set<String> expected = loadGoldClassNames();
        ExtentReportManager.logInfo("Gold defines " + expected.size()
                + " node classes (from node_classes_template (13).xlsx).");

        Response resp = getAuthenticatedRequestSpec().when().get("/node_classes").then().extract().response();
        Assert.assertEquals(resp.getStatusCode(), 200,
                "GET /api/node_classes should return 200, got " + resp.getStatusCode() + ": " + truncate(resp.asString()));
        List<String> names = resp.jsonPath().getList("name");
        Assert.assertNotNull(names, "GET /api/node_classes returned no 'name' fields: " + truncate(resp.asString()));

        Set<String> live = new TreeSet<>();
        for (String n : names) if (n != null) live.add(n.trim());

        List<String> missing = new ArrayList<>();
        for (String c : expected) {
            boolean present = false;
            for (String l : live) if (l.equalsIgnoreCase(c)) { present = true; break; }
            if (!present) missing.add(c);
        }
        Assert.assertTrue(missing.isEmpty(),
                "Live /api/node_classes is MISSING gold/template-defined classes " + missing
                        + " (gold regenerated from node_classes_template (13).xlsx). Live has "
                        + live.size() + " distinct class names.");
        ExtentReportManager.logPass("All " + expected.size()
                + " gold node classes present in live /api/node_classes (" + live.size() + " distinct live).");
    }

    /** Read the class names from the version-controlled gold (derived from template 13). */
    private Set<String> loadGoldClassNames() {
        File f = new File(GOLD_PATH);
        if (!f.exists()) {
            throw new SkipException("Gold not found at " + f.getAbsolutePath()
                    + " — run from project root so testcase/node_classes_gold.json resolves.");
        }
        try (FileReader r = new FileReader(f)) {
            JSONObject classes = new JSONObject(new JSONTokener(r)).getJSONObject("classes");
            Set<String> names = new TreeSet<>();
            for (String k : classes.keySet()) names.add(k.trim());
            if (names.isEmpty()) throw new SkipException("Gold has no classes.");
            return names;
        } catch (SkipException se) {
            throw se;
        } catch (Exception e) {
            throw new SkipException("Failed to read gold " + GOLD_PATH + ": " + e.getMessage());
        }
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
