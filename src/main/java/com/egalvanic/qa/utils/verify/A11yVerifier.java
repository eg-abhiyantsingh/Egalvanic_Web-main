package com.egalvanic.qa.utils.verify;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;

import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Accessibility (WCAG 2.x) verifier — integrates the official Deque axe-core
 * Selenium binding (com.deque.html.axe-core:selenium). We do NOT reinvent a
 * scanner; axe-core injects its bundled axe.min.js via executeScript (CSP-safe)
 * and runs the audit in-page.
 *
 * Catches: missing form labels, insufficient color contrast, missing/incorrect
 * ARIA, missing alt text, bad heading order, non-keyboard-operable controls,
 * missing document language — the WCAG class of bugs DOM assertions never see.
 *
 * Severity policy (tunable): axe impact levels are critical > serious > moderate
 * > minor. By default we HARD-FAIL on critical + serious and WARN on the rest,
 * so the build goes red only on genuinely blocking a11y defects.
 *
 * Boundary: automated a11y catches ~30-50% of WCAG issues; it does NOT replace
 * manual screen-reader / keyboard audits. State that in reports.
 */
public final class A11yVerifier {

    private A11yVerifier() {}

    /** WCAG 2.0/2.1 level A + AA tag set — the standard compliance target. */
    private static final List<String> WCAG_AA_TAGS =
            Arrays.asList("wcag2a", "wcag2aa", "wcag21a", "wcag21aa");

    private static final List<String> BLOCKING_IMPACTS = Arrays.asList("critical", "serious");

    /**
     * SHARED CHROME — DOM that is identical on every route (sidebar nav, drawer, the global
     * floating action button) plus the third-party widgets the app embeds.
     *
     * Verified live on 2026-08-08 against V1.36: the sidebar rendered
     * {@code li.MuiListItem-root} ("Legacy Procedures") directly inside a {@code div.MuiBox-root}
     * instead of a {@code <ul>} (axe rule {@code listitem}, serious), and the global
     * {@code MuiFab} carries no accessible name (axe rule {@code button-name}, critical).
     *
     * <p><b>Re-checked 2026-08-24 after the nav redesign:</b> the selectors below all still
     * match ({@code nav}×3, {@code .MuiDrawer-root}×2, {@code .MuiFab-root}×1), so the
     * filtering keeps working. But the stray-{@code <li>} defect appears to be FIXED — every
     * {@code li.MuiListItem-root} now sits inside a proper {@code ul.MuiList-root}. Re-run the
     * BUG-B tripwires before citing the {@code listitem} violation as live; the
     * {@code button-name} Fab defect was not re-verified.
     *
     * Consequence: those are ONE real defect each, but a whole-page scan re-reports them in
     * EVERY module that runs an a11y check — turning 2 defects into N failures and burying each
     * module's own, page-specific violations in duplicate noise. The dedicated per-module a11y
     * tripwires (BUG-B) own the whole-page reporting; functional tests should use
     * {@link #assertNoPageSpecificViolations} so they only go red on THEIR page's markup.
     */
    private static final List<String> SHARED_CHROME_SELECTORS = Arrays.asList(
            "nav",                    // sidebar <nav> wrapper
            ".MuiDrawer-root",        // sidebar drawer paper (holds the stray <li>)
            ".MuiFab-root",           // global floating action button (no accessible name)
            "[id*='beamer']",         // Beamer notification widget (third-party)
            "[class*='beamer']",
            "[id*='devrev']",         // DevRev support widget (third-party)
            "iframe");                // any embedded third-party frame

    /** Run an axe audit on the whole page and return all violations. */
    public static List<Rule> scan(WebDriver driver) {
        Results results = new AxeBuilder().withTags(WCAG_AA_TAGS).analyze(driver);
        List<Rule> v = results.getViolations();
        return v == null ? new ArrayList<>() : v;
    }

    /**
     * Run an axe audit with the app's shared chrome excluded, so only violations owned by the
     * CURRENT page's own markup are returned. See {@link #SHARED_CHROME_SELECTORS} for why.
     */
    public static List<Rule> scanPageOnly(WebDriver driver) {
        AxeBuilder b = new AxeBuilder().withTags(WCAG_AA_TAGS);
        for (String sel : SHARED_CHROME_SELECTORS) {
            b = b.exclude(sel);
        }
        List<Rule> v = b.analyze(driver).getViolations();
        return v == null ? new ArrayList<>() : v;
    }

    /** Violations whose impact is critical or serious (the build-breakers). */
    public static List<Rule> blockingViolations(WebDriver driver) {
        return scan(driver).stream()
                .filter(r -> r.getImpact() != null
                        && BLOCKING_IMPACTS.contains(r.getImpact().toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Human-readable one-liner per violation: impact, rule id, node count, help — PLUS the CSS
     * target of each offending node. Without the targets an a11y failure reads "2 node(s): Buttons
     * must have discernible text", which tells whoever debugs it nothing about WHERE the button is;
     * with them the fix is a direct lookup in the DOM.
     */
    public static String describe(List<Rule> violations) {
        if (violations.isEmpty()) return "no violations";
        StringBuilder sb = new StringBuilder();
        for (Rule r : violations) {
            int nodes = r.getNodes() == null ? 0 : r.getNodes().size();
            sb.append("\n  - [").append(r.getImpact()).append("] ")
              .append(r.getId()).append(" (").append(nodes).append(" node(s)): ")
              .append(r.getHelp());
            if (r.getNodes() != null) {
                int shown = 0;
                for (com.deque.html.axecore.results.CheckedNode n : r.getNodes()) {
                    if (shown++ >= 3) {                       // keep the message readable
                        sb.append("\n        … and ").append(nodes - 3).append(" more node(s)");
                        break;
                    }
                    sb.append("\n        at ").append(targetOf(n));
                }
            }
        }
        return sb.toString();
    }

    /** axe's node target is an untyped nested list of CSS selectors — flatten it defensively. */
    private static String targetOf(com.deque.html.axecore.results.CheckedNode n) {
        Object t;
        try {
            t = n.getTarget();
        } catch (RuntimeException e) {
            return "<target unavailable>";
        }
        if (t == null) return "<unknown>";
        String s = (t instanceof List) ? flatten((List<?>) t) : String.valueOf(t);
        return s.length() > 160 ? s.substring(0, 157) + "..." : s;
    }

    private static String flatten(List<?> list) {
        StringBuilder sb = new StringBuilder();
        for (Object o : list) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(o instanceof List ? flatten((List<?>) o) : String.valueOf(o));
        }
        return sb.toString();
    }

    /**
     * Hard-assert there are no critical/serious WCAG violations on the current page.
     * Moderate/minor are logged but do not fail. {@code context} names the page.
     */
    public static void assertNoBlockingViolations(WebDriver driver, String context) {
        List<Rule> all = scan(driver);
        List<Rule> blocking = all.stream()
                .filter(r -> r.getImpact() != null
                        && BLOCKING_IMPACTS.contains(r.getImpact().toLowerCase()))
                .collect(Collectors.toList());
        List<Rule> minor = all.stream()
                .filter(r -> !blocking.contains(r))
                .collect(Collectors.toList());

        if (!minor.isEmpty()) {
            System.out.println("[A11yVerifier] " + context
                    + " — " + minor.size() + " moderate/minor WCAG issue(s) (warn): "
                    + describe(minor));
        }
        if (!blocking.isEmpty()) {
            throw new AssertionError("[A11yVerifier] " + context + " has "
                    + blocking.size() + " critical/serious WCAG violation(s):"
                    + describe(blocking));
        }
    }

    /**
     * Hard-assert there are no critical/serious WCAG violations owned by THIS PAGE's own markup,
     * ignoring the app's shared chrome (sidebar / global FAB / third-party widgets).
     *
     * Use this inside FUNCTIONAL tests. The shared-chrome violations are real, but they are the
     * same two defects on every route and the dedicated per-module a11y tripwires (BUG-B) already
     * report them; re-failing every functional test on them costs the functional coverage that test
     * exists to provide while adding no new information.
     *
     * Use {@link #assertNoBlockingViolations} in dedicated a11y tests, where whole-page scope is
     * the point.
     */
    public static void assertNoPageSpecificViolations(WebDriver driver, String context) {
        List<Rule> all = scanPageOnly(driver);
        List<Rule> blocking = all.stream()
                .filter(r -> r.getImpact() != null
                        && BLOCKING_IMPACTS.contains(r.getImpact().toLowerCase()))
                .collect(Collectors.toList());

        if (blocking.isEmpty()) {
            System.out.println("[A11yVerifier] " + context
                    + " — no page-specific critical/serious WCAG violations"
                    + " (shared chrome excluded; tracked separately as BUG-B)");
            return;
        }
        throw new AssertionError("[A11yVerifier] " + context + " has "
                + blocking.size() + " critical/serious WCAG violation(s) in its OWN markup"
                + " (shared sidebar/FAB chrome excluded — those are BUG-B):"
                + describe(blocking));
    }
}
