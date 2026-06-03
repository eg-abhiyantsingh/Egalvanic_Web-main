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

    /** Run an axe audit on the whole page and return all violations. */
    public static List<Rule> scan(WebDriver driver) {
        Results results = new AxeBuilder().withTags(WCAG_AA_TAGS).analyze(driver);
        List<Rule> v = results.getViolations();
        return v == null ? new ArrayList<>() : v;
    }

    /** Violations whose impact is critical or serious (the build-breakers). */
    public static List<Rule> blockingViolations(WebDriver driver) {
        return scan(driver).stream()
                .filter(r -> r.getImpact() != null
                        && BLOCKING_IMPACTS.contains(r.getImpact().toLowerCase()))
                .collect(Collectors.toList());
    }

    /** Human-readable one-liner per violation: impact, rule id, node count, help. */
    public static String describe(List<Rule> violations) {
        if (violations.isEmpty()) return "no violations";
        StringBuilder sb = new StringBuilder();
        for (Rule r : violations) {
            int nodes = r.getNodes() == null ? 0 : r.getNodes().size();
            sb.append("\n  - [").append(r.getImpact()).append("] ")
              .append(r.getId()).append(" (").append(nodes).append(" node(s)): ")
              .append(r.getHelp());
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
}
