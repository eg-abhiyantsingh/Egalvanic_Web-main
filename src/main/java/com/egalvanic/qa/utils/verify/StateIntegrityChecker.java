package com.egalvanic.qa.utils.verify;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StateIntegrityChecker — captures key data BEFORE a flow and asserts AFTER
 * that nothing was lost, duplicated, or corrupted.
 *
 * WHY THIS EXISTS
 * ---------------
 * The suite has no cross-step data-integrity checks. Real users hit:
 *   - reload/back-button silently dropping unsaved or just-saved data
 *   - a "create" that duplicates a row
 *   - an edit that persists to the wrong record
 * Happy-path scripts that re-read the same element never notice. This compares
 * a structured snapshot taken at two points in a flow.
 *
 * Typical use:
 *   Snapshot before = StateIntegrityChecker.snapshotRows(driver,
 *                         By.cssSelector(".MuiDataGrid-row"));
 *   ... create one asset ...
 *   Snapshot after  = StateIntegrityChecker.snapshotRows(driver, ...);
 *   StateIntegrityChecker.assertGrew(before, after, 1, "create asset");
 *
 *   // round-trip: reload must not change the data
 *   Snapshot a = snapshotRows(...); driver.navigate().refresh();
 *   Snapshot b = snapshotRows(...);
 *   StateIntegrityChecker.assertUnchanged(a, b, "reload");
 */
public final class StateIntegrityChecker {

    private StateIntegrityChecker() {}

    public static final class Snapshot {
        public final List<String> values;   // ordered row/field signatures
        public final String label;
        Snapshot(String label, List<String> values) { this.label = label; this.values = values; }
        public int count() { return values.size(); }
        public int distinct() { return new HashSet<>(values).size(); }
        public List<String> duplicates() {
            Set<String> seen = new HashSet<>(); List<String> dup = new ArrayList<>();
            for (String v : values) if (!seen.add(v)) dup.add(v);
            return dup;
        }
    }

    /** Snapshot the visible text of every element matching {@code rows} (e.g. grid rows). */
    public static Snapshot snapshotRows(WebDriver driver, By rows) {
        List<String> vals = new ArrayList<>();
        for (WebElement e : driver.findElements(rows)) {
            try {
                String t = e.getText();
                if (t != null) vals.add(t.trim().replaceAll("\\s+", " "));
            } catch (Exception ignored) {}
        }
        return new Snapshot("rows", vals);
    }

    /** Snapshot arbitrary key->value form/detail fields you care about. */
    public static Snapshot snapshotFields(Map<String, String> fields) {
        List<String> vals = new ArrayList<>();
        Map<String, String> ordered = new LinkedHashMap<>(fields);
        ordered.forEach((k, v) -> vals.add(k + "=" + (v == null ? "" : v.trim())));
        return new Snapshot("fields", vals);
    }

    /** Snapshot the total row count a grid CLAIMS to have (footer "1–25 of N"). */
    public static long reportedTotal(WebDriver driver) {
        try {
            Object n = ((JavascriptExecutor) driver).executeScript(
                "var el=document.querySelector('.MuiTablePagination-displayedRows,[class*=\"displayedRows\"]');" +
                "if(!el) return -1;" +
                "var m=(el.innerText||'').match(/of\\s+([0-9,]+)/i);" +
                "return m ? parseInt(m[1].replace(/,/g,'')) : -1;");
            return n instanceof Number ? ((Number) n).longValue() : -1;
        } catch (Exception e) { return -1; }
    }

    // ----------------------------------------------------------- assertions

    /** Data must be byte-for-byte identical (e.g. across reload / back-button). */
    public static void assertUnchanged(Snapshot before, Snapshot after, String flow) {
        if (before.count() != after.count()) {
            throw new AssertionError("STATE CORRUPTION after [" + flow + "]: row count changed "
                    + before.count() + " -> " + after.count() + " (data lost or injected).");
        }
        List<String> diff = new ArrayList<>(before.values);
        diff.removeAll(after.values);
        if (!diff.isEmpty()) {
            throw new AssertionError("STATE CORRUPTION after [" + flow + "]: values changed/lost: " + diff);
        }
    }

    /** A create flow must add exactly {@code delta} rows — no more (dup), no fewer (lost). */
    public static void assertGrew(Snapshot before, Snapshot after, int delta, String flow) {
        int expected = before.count() + delta;
        if (after.count() != expected) {
            throw new AssertionError("STATE INTEGRITY after [" + flow + "]: expected " + expected
                    + " rows (" + before.count() + "+" + delta + ") but found " + after.count()
                    + (after.count() > expected ? " — possible DUPLICATION." : " — possible DATA LOSS."));
        }
        assertNoDuplicates(after, flow);
    }

    /** No row signature may appear twice. */
    public static void assertNoDuplicates(Snapshot snap, String flow) {
        List<String> dup = snap.duplicates();
        if (!dup.isEmpty()) {
            throw new AssertionError("DUPLICATE rows after [" + flow + "]: " + dup);
        }
    }

    /** The grid's own "of N" total must match the rows it actually rendered (within page size). */
    public static void assertTotalConsistent(WebDriver driver, Snapshot rendered, int pageSize, String flow) {
        long claimed = reportedTotal(driver);
        if (claimed >= 0 && claimed <= pageSize && claimed != rendered.count()) {
            throw new AssertionError("STATE INTEGRITY after [" + flow + "]: grid claims " + claimed
                    + " rows but rendered " + rendered.count() + " — count/display mismatch.");
        }
    }
}
