package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.constants.WorkTypeCatalog;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Work Orders — <b>per-Work-Type create → verify → cleanup E2E matrix</b> (ZP-3000 Services V2).
 *
 * <p>42 data-driven rows = the 14 Work Type dropdown options ({@link WorkTypeCatalog#ALL},
 * catalog display order) x 3 ordered phases:</p>
 * <ol>
 *   <li><b>TC_WTC_001 create</b> — fresh Create New Work Order dialog per type; name + Work Type
 *       only (Facility defaults to the current site). Scope policy: the six
 *       {@link WorkTypeCatalog#familyRepresentatives()} keep the default all-assets scope and
 *       record the settled "N matching assets" preview count; every OTHER service with a scope
 *       preview clicks "Start Empty Instead" (cheap create); Shutdown (Composite) (0 procedures)
 *       and General need no scope action. Hard-fails with the dialog text if Create doesn't close
 *       (surfaced validation error).</li>
 *   <li><b>TC_WTC_002 verify</b> — created WO appears in the /sessions grid, its detail page honours
 *       the family contract (header chips contain the type name for services; tab strip equals
 *       {@code Family.expectedTabs}), and — for full-scope family representatives — the Assets tab
 *       badge equals the recorded scope-preview count. That last check is the regression net for
 *       the 2026-07-20 "Clean, Tighten, Torque" preview-vs-created asset-count mismatch bug.</li>
 *   <li><b>TC_WTC_003 cleanup</b> — DELETE /api/ir_session/{id} (async receipt), then confirm the
 *       WO leaves the grid (polling + one list refresh for the async delete lag).</li>
 * </ol>
 *
 * <p>Phases communicate via static maps keyed by work-type name and rely on same-class ordering
 * (priorities 1/2/3 + preserve-order in suite-worktype-create-e2e.xml). All facts live-verified
 * 2026-07-21 on acme.qa site Z1 — see WorkTypeCatalog for the authoritative catalog.</p>
 */
public class WorkTypeCreateE2EMatrixTestNG extends WorkTypeUiBase {

    private static final String MODULE = AppConstants.MODULE_WORK_ORDERS;
    private static final String FEATURE = "Work Type Create E2E Matrix";

    /** Phase A -> B/C: work-type name -> created WO name. */
    private static final Map<String, String> createdByType = new LinkedHashMap<String, String>();
    /** Phase B -> C: work-type name -> /sessions/{id} session id of the created WO. */
    private static final Map<String, String> createdSessionIds = new LinkedHashMap<String, String>();
    /** Phase A -> B: work-type name -> settled "N matching assets" preview count (full-scope reps only). */
    private static final Map<String, Integer> previewCountByType = new LinkedHashMap<String, Integer>();

    /** The six family representatives get the EXPENSIVE full-scope create; everyone else goes cheap. */
    private static final Set<String> REPRESENTATIVE_NAMES = new LinkedHashSet<String>();
    static {
        for (WorkTypeCatalog.WorkTypeProfile p : WorkTypeCatalog.familyRepresentatives()) {
            REPRESENTATIVE_NAMES.add(p.name);
        }
    }

    /** Trailing tab count badge, e.g. "Assets46" -> 46, "Assets99+" -> 99 plus-capped. */
    private static final Pattern TAB_BADGE = Pattern.compile("(\\d+)\\s*(\\+?)$");

    // ================================================================
    // DATA PROVIDER — WorkTypeCatalog.ALL in exact catalog (dropdown) order
    // ================================================================

    @DataProvider(name = "allWorkTypes")
    public Object[][] allWorkTypes() {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (WorkTypeCatalog.WorkTypeProfile p : WorkTypeCatalog.ALL) {
            rows.add(new Object[]{p});
        }
        return rows.toArray(new Object[0][]);
    }

    // ================================================================
    // PHASE A — create one WO per work type (14 rows)
    // ================================================================

    @Test(priority = 1, dataProvider = "allWorkTypes",
          description = "TC_WTC_001: create a WO for every Work Type (full scope for family representatives, Start Empty Instead for other preview services)")
    public void testTC_WTC_001_CreatePerType(WorkTypeCatalog.WorkTypeProfile profile) {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_WTC_001 — " + profile.name);
        String shortKey = profile.isService() ? profile.apiKey : "general";
        String woName = "WTC_" + shortKey + "_" + System.currentTimeMillis();

        // Fresh dialog per row — cancel any stale dialog, then open anew.
        closeCreateDialogIfOpen();
        Assert.assertTrue(ensureCreateDialogOpen(),
                "Create New Work Order dialog should open on /sessions for '" + profile.name + "'.");
        logStep("Dialog open (facility='" + workOrderPage.getFacilityValue() + "') — filling WO name '" + woName + "'");
        workOrderPage.fillWoName(woName);
        Assert.assertTrue(selectWorkTypeCommitted(profile.name),
                "Work Type '" + profile.name + "' should COMMIT in the Autocomplete (MUI display-text-only quirk).");

        // ---- Scope policy ----
        if (REPRESENTATIVE_NAMES.contains(profile.name)) {
            if (profile.expectsScopePreview()) {
                // Keep the default all-assets scope; wait for the preview to settle (API observed
                // 3.7s, settle up to ~8s) and record N for phase B's preview-vs-created regression.
                int n = workOrderPage.getMatchingAssetsCount(15);
                Assert.assertTrue(n >= 0,
                        "Family representative '" + profile.name + "' should settle on 'N matching assets' within 15s."
                        + " Dialog text: " + snippet(workOrderPage.getCreateDialogText()));
                previewCountByType.put(profile.name, Integer.valueOf(n));
                logStep("Scope preview settled: " + n + " matching assets — keeping FULL all-assets scope");
            } else {
                logStep("Representative '" + profile.name + "' has no scope preview — no scope action");
            }
        } else if (profile.expectsScopePreview()) {
            // Cheap create for the non-representative preview services.
            workOrderPage.clickStartEmptyInstead();
            logStep("Non-representative service — clicked 'Start Empty Instead' (cheap empty-scope create)");
        } else if (profile.expectsNoProceduresNotice()) {
            logStep("0-procedure service — 'no procedures configured' notice present: "
                    + workOrderPage.hasNoProceduresNotice() + " (no scope action needed)");
        } else {
            logStep("General — fires no scope-preview, shows neither count nor notice (no scope action)");
        }

        // ---- Create gating: Name + Work Type should be enough (Facility defaults to site) ----
        boolean enabled = workOrderPage.isCreateButtonEnabled();
        for (int i = 0; i < 16 && !enabled; i++) {
            pause(500);
            enabled = workOrderPage.isCreateButtonEnabled();
        }
        Assert.assertTrue(enabled,
                "Create button should enable within 8s once WO Name + Work Type are set for '" + profile.name + "'."
                + " Dialog text: " + snippet(workOrderPage.getCreateDialogText()));

        workOrderPage.clickCreateWorkOrder();
        boolean closed = !workOrderPage.isCreateWorkOrderDialogOpen();
        for (int i = 0; i < 25 && !closed; i++) {
            pause(800);
            closed = !workOrderPage.isCreateWorkOrderDialogOpen();
        }
        if (!closed) {
            String dialogText = snippet(workOrderPage.getCreateDialogText());
            closeCreateDialogIfOpen(); // don't poison the next row
            Assert.fail("Create dialog did NOT close within 20s after clicking Create for '" + profile.name
                    + "' — a validation error likely surfaced. Dialog text: " + dialogText);
        }

        createdByType.put(profile.name, woName);
        ExtentReportManager.logPass("Created WO '" + woName + "' for work type '" + profile.name + "'"
                + (previewCountByType.containsKey(profile.name)
                        ? " (full scope, preview=" + previewCountByType.get(profile.name) + " assets)" : ""));
        Assert.assertTrue(createdByType.containsKey(profile.name),
                "Created-WO bookkeeping should record '" + profile.name + "' for phases B/C.");
    }

    // ================================================================
    // PHASE B — verify each created WO: grid row + detail-page family contract (14 rows)
    // ================================================================

    @Test(priority = 2, dataProvider = "allWorkTypes",
          description = "TC_WTC_002: created WO appears in the grid and its detail page honours the family contract (chips, tabs, preview-vs-created asset count)")
    public void testTC_WTC_002_VerifyCreated(WorkTypeCatalog.WorkTypeProfile profile) {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_WTC_002 — " + profile.name);
        String woName = createdByType.get(profile.name);
        if (woName == null) {
            Assert.fail("Phase A (TC_WTC_001) recorded no created WO for '" + profile.name
                    + "' — create failed upstream, nothing to verify.");
        }

        Assert.assertTrue(findWorkOrderInGrid(woName, 6),
                "Created WO '" + woName + "' should appear in the /sessions grid (backend index lags 5-15s; 6 search attempts).");
        Assert.assertTrue(openWorkOrderByName(woName),
                "Grid row for '" + woName + "' should open its detail page.");
        Assert.assertTrue(workOrderPage.isOnWoDetailPage(),
                "URL should match /sessions/{id} after opening '" + woName + "'. Actual: " + driver.getCurrentUrl());
        String sessionId = workOrderPage.getWoDetailId();
        Assert.assertFalse(sessionId.isEmpty(),
                "Detail URL should expose the session id for '" + woName + "'. URL: " + driver.getCurrentUrl());
        createdSessionIds.put(profile.name, sessionId);
        logStep("Detail page open: /sessions/" + sessionId);

        // ---- Header chips: chip[0]=work type name for services; GENERAL behavior gets pinned ----
        List<String> chips = workOrderPage.getWoHeaderChips();
        if (profile.isService()) {
            boolean hasTypeChip = false;
            for (String c : chips) {
                if (c.contains(profile.name)) { hasTypeChip = true; break; }
            }
            Assert.assertTrue(hasTypeChip,
                    "Detail header chips should include the work type '" + profile.name
                    + "' (live contract: chip[0]=type). Actual chips: " + chips);
        } else {
            String bodyText = "";
            try { bodyText = driver.findElement(By.tagName("body")).getText(); } catch (Exception ignored) {}
            logStep("GENERAL header chips (recorded to pin actual behavior): " + chips);
            Assert.assertTrue(!chips.isEmpty() || bodyText.contains(woName),
                    "GENERAL detail-header behavior pin: expected non-empty header chips OR the page to contain the WO name '"
                    + woName + "'. Actual chips: " + chips + " — if this fails, the GENERAL detail header renders neither.");
        }

        // ---- Tab strip per family contract ----
        List<String> tabs = workOrderPage.getDetailTabNames();
        if (profile.family.expectedTabs != null) {
            Assert.assertEquals(tabs, profile.family.expectedTabs,
                    "Detail tab strip drifted for family " + profile.family + " ('" + profile.name + "').");
            logStep("Tabs match " + profile.family + " contract: " + tabs);
        } else {
            logStep("GENERAL tabs (recorded, contract unpinned in catalog): " + tabs);
            Assert.assertTrue(tabs.contains("Assets"),
                    "GENERAL detail page should at least render an Assets tab. Actual tabs: " + tabs);
        }

        // ---- Preview-vs-created asset count (2026-07-20 CTT mismatch regression) ----
        Integer preview = previewCountByType.get(profile.name);
        if (REPRESENTATIVE_NAMES.contains(profile.name) && preview != null && preview.intValue() > 0) {
            int badge = -1;
            boolean plus = false;
            boolean consistent = false;
            String raw = "";
            long end = System.currentTimeMillis() + 15000L;
            while (System.currentTimeMillis() < end) {
                String r = rawTabTextFor("Assets");
                if (r != null) {
                    raw = r;
                    Matcher m = TAB_BADGE.matcher(r);
                    if (m.find()) {
                        badge = Integer.parseInt(m.group(1));
                        plus = "+".equals(m.group(2));
                        // "99+" caps the badge — the preview count must be at least the cap.
                        consistent = plus ? preview.intValue() >= 99 : badge == preview.intValue();
                        if (consistent) break;
                    }
                }
                pause(1000); // asset attachment settles asynchronously after create
            }
            Assert.assertTrue(badge >= 0,
                    "Assets tab never showed a numeric count badge within 15s for full-scope '" + profile.name
                    + "' (raw tab text: '" + raw + "') — cannot verify the preview-vs-created contract.");
            Assert.assertTrue(consistent,
                    "PREVIEW-vs-CREATED asset count MISMATCH (2026-07-20 'Clean, Tighten, Torque' bug class) for '"
                    + profile.name + "': scope preview promised " + preview
                    + " matching assets, created WO Assets tab shows " + (plus ? badge + "+" : String.valueOf(badge)) + ".");
            ExtentReportManager.logPass("Assets tab badge " + (plus ? badge + "+" : String.valueOf(badge))
                    + " consistent with scope-preview count " + preview);
        }

        // Leave the list clean for the next row.
        ensureOnWorkOrdersList();
        clearWorkOrderSearch();
        ExtentReportManager.logPass("'" + woName + "' verified: grid row, chips, tabs, session id " + sessionId);
        Assert.assertTrue(createdSessionIds.containsKey(profile.name),
                "Session-id bookkeeping should record '" + profile.name + "' for phase C cleanup.");
    }

    // ================================================================
    // PHASE C — API-delete each created WO and confirm it leaves the grid (14 rows)
    // ================================================================

    @Test(priority = 3, dataProvider = "allWorkTypes",
          description = "TC_WTC_003: DELETE /api/ir_session/{id} (async receipt) removes the created WO from the grid")
    public void testTC_WTC_003_CleanupPerType(WorkTypeCatalog.WorkTypeProfile profile) {
        ExtentReportManager.createTest(MODULE, FEATURE, "TC_WTC_003 — " + profile.name);
        String woName = createdByType.get(profile.name);
        if (woName == null) {
            Assert.fail("Phase A (TC_WTC_001) never created a WO for '" + profile.name
                    + "' — nothing to clean up (create failed upstream).");
        }

        String sessionId = createdSessionIds.get(profile.name);
        if (sessionId == null || sessionId.isEmpty()) {
            // Phase B failed before recording the id — resolve it from the grid so we can still clean up.
            logStep("Phase B recorded no session id — resolving '" + woName + "' via the grid");
            if (findWorkOrderInGrid(woName, 3) && openWorkOrderByName(woName)) {
                sessionId = workOrderPage.getWoDetailId();
                if (!sessionId.isEmpty()) createdSessionIds.put(profile.name, sessionId);
            }
        }
        Assert.assertTrue(sessionId != null && !sessionId.isEmpty(),
                "No session id available for '" + woName + "' (phase B failed AND grid resolution failed)"
                + " — cannot API-delete; MANUAL cleanup needed for WO '" + woName + "'.");

        Assert.assertTrue(apiDeleteWorkOrder(sessionId),
                "DELETE /api/ir_session/" + sessionId + " should return 2xx (async {\"_mutation\":{\"status\":\"received\"}} receipt).");
        logStep("DELETE accepted (async receipt) — polling the grid for disappearance of '" + woName + "'");

        // Async delete: findWorkOrderInGrid(name, 3) polls ~15s; if still visible, refresh once and re-check.
        boolean stillVisible = findWorkOrderInGrid(woName, 3);
        if (stillVisible) {
            logStep("Still visible after first poll window — refreshing the list once (async delete lag)");
            driver.navigate().refresh();
            reinstallHealthCapture();
            waitAndDismissAppAlert();
            pause(2000);
            stillVisible = findWorkOrderInGrid(woName, 3);
        }
        clearWorkOrderSearch();
        Assert.assertFalse(stillVisible,
                "WO '" + woName + "' (" + sessionId + ") still renders in the /sessions grid ~30s after an accepted DELETE"
                + " + one refresh — async delete never applied.");

        createdByType.remove(profile.name);
        createdSessionIds.remove(profile.name);
        ExtentReportManager.logPass("Deleted and confirmed gone: '" + woName + "' (" + sessionId + ")");
        Assert.assertFalse(createdByType.containsKey(profile.name),
                "Bookkeeping: '" + profile.name + "' should be cleared from the created-WO map after confirmed deletion.");
    }

    // ================================================================
    // SAFETY NET — best-effort delete of anything a failed row left behind
    // ================================================================

    @AfterClass(alwaysRun = true)
    public void cleanupLeftovers() {
        try { closeCreateDialogIfOpen(); } catch (Exception ignored) {}
        for (Map.Entry<String, String> e : new LinkedHashMap<String, String>(createdSessionIds).entrySet()) {
            try {
                boolean ok = apiDeleteWorkOrder(e.getValue());
                System.out.println("[WTC cleanup] leftover '" + e.getKey() + "' (" + e.getValue() + ") delete -> " + ok);
                if (ok) {
                    createdSessionIds.remove(e.getKey());
                    createdByType.remove(e.getKey());
                }
            } catch (Exception ex) {
                System.out.println("[WTC cleanup] leftover delete failed for " + e.getKey() + ": " + ex.getMessage());
            }
        }
        if (!createdByType.isEmpty()) {
            System.out.println("[WTC cleanup] WOs WITHOUT session ids remain (manual cleanup may be needed): " + createdByType);
        }
    }

    // ================================================================
    // LOCAL HELPERS
    // ================================================================

    /**
     * RAW text of the detail tab whose badge-stripped label equals {@code tabName}
     * (e.g. returns "Assets46" / "Assets99+" for tabName "Assets"; null when absent).
     * Reads //button[@role='tab'] directly so the count badge survives for parsing.
     */
    private String rawTabTextFor(String tabName) {
        for (WebElement t : driver.findElements(By.xpath("//button[@role='tab']"))) {
            String rawText = t.getText().trim();
            String stripped = rawText.replaceAll("\\d+\\+?$", "").trim();
            if (stripped.equalsIgnoreCase(tabName)) return rawText;
        }
        return null;
    }

    /** Flatten + truncate dialog/page text for assertion messages. */
    private static String snippet(String s) {
        if (s == null) return "";
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() > 400 ? flat.substring(0, 400) + "..." : flat;
    }
}
