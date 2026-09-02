package com.egalvanic.qa.utils;

import org.openqa.selenium.By;

/**
 * The locator vocabulary of the V1.36 web design, in one place.
 *
 * <p><b>Why this class exists.</b> The redesign broke locators in a specific way, and the fix
 * is not "write better XPaths" one field at a time — it is to stop guessing at structure and
 * key on the attributes the app actually emits. A DOM audit of /assets, /sessions and /tasks
 * on 2026-09-02 established what is available:
 *
 * <table>
 *   <caption>Attribute availability, live QA V1.36</caption>
 *   <tr><th>Attribute</th><th>Count/page</th><th>Verdict</th></tr>
 *   <tr><td>{@code data-testid}</td><td>2</td>
 *       <td>Unusable — both are MUI's own {@code sentinelStart}/{@code sentinelEnd}
 *           focus traps. The app ships no test ids.</td></tr>
 *   <tr><td>{@code id}</td><td>24–32</td>
 *       <td><b>Actively dangerous.</b> Most are React 18 {@code useId} output —
 *           {@code input[id="«r4»"]}, {@code id="«rc»"} — which is generated per render
 *           and differs between mounts. Two are stable and hand-written:
 *           {@code #page-header-actions} and {@code #sld-root}.</td></tr>
 *   <tr><td>{@code aria-label}</td><td>53–61</td>
 *       <td>Good, and the answer to the two-level rail: every category button carries
 *           one. See {@link NavCatalog}.</td></tr>
 *   <tr><td>{@code data-field}</td><td>90–117</td>
 *       <td><b>Best available.</b> MUI DataGrid stamps the column's field name on every
 *           cell: {@code status}, {@code priority}, {@code due_date}, {@code label},
 *           {@code qr_code}, {@code title}, {@code actions}…  Semantic, stable across
 *           re-skins, and identical in shape on every grid page.</td></tr>
 *   <tr><td>{@code data-id}</td><td>10–13</td>
 *       <td>The DataGrid row's server id — the right way to target one known row.</td></tr>
 * </table>
 *
 * <p>Two consequences drive everything below. First, <b>every list page is a
 * {@code MuiDataGrid}, not a {@code <table>}</b> — {@code //tbody//tr} matches nothing, and a
 * test asserting "0 rows" against it passes vacuously. Second, <b>form inputs carry no
 * {@code name}, no {@code aria-label} and no usable {@code id}</b>, so a field can only be
 * found through its visible label; the reliable way is to climb to the enclosing
 * {@code MuiFormControl-root} rather than walk {@code following::input[1]}, which crosses
 * section boundaries the moment the layout reflows.
 *
 * @see NavCatalog for the sidebar and route catalog
 */
public final class V136Locators {

    private V136Locators() { }

    // ================================================================
    // CONTAINERS
    // ================================================================

    /** The main content region. Scope assertions here: sidebar chrome otherwise satisfies them. */
    public static final By MAIN = By.cssSelector("main");

    /**
     * The stable page-header action container. Present on every list page, but it is
     * sometimes EMPTY while the primary button renders elsewhere in {@code <main>} — verified
     * on /sessions, where "Create Work Order" is not inside it. Treat it as a narrowing hint,
     * never as a guarantee.
     */
    public static final By PAGE_HEADER_ACTIONS = By.cssSelector("#page-header-actions");

    /** A modal dialog — the Create/Edit surface for most modules, and the WO wizard. */
    public static final By DIALOG = By.cssSelector(".MuiDialog-paper, [role='dialog']");

    /**
     * The right-hand slide-over drawer. Distinct from {@link #DIALOG} and from the sidebar,
     * which is <i>also</i> a MuiDrawer — hence the {@code anchorRight} + {@code modal}
     * qualifiers. Asset create/edit uses this surface.
     */
    public static final By RIGHT_DRAWER =
            By.cssSelector(".MuiDrawer-anchorRight.MuiDrawer-modal .MuiDrawer-paper");

    /** Either editing surface, for code that must handle both. */
    public static final By EDIT_SURFACE = By.cssSelector(
            ".MuiDialog-paper, .MuiDrawer-anchorRight.MuiDrawer-modal .MuiDrawer-paper, [role='dialog']");

    // ================================================================
    // DATA GRID
    // ================================================================

    /** The grid itself. Every V1.36 list page renders one; none renders a {@code <table>}. */
    public static final By GRID = By.cssSelector(".MuiDataGrid-root");

    /** Data rows only — excludes the header row, which also carries {@code role='row'}. */
    public static final By GRID_ROWS = By.cssSelector(".MuiDataGrid-row[data-id]");

    /** Column headers. */
    public static final By GRID_HEADERS = By.cssSelector("[role='columnheader']");

    /** The row-count readout, for pagination assertions. */
    public static final By PAGINATION_TEXT = By.cssSelector(".MuiTablePagination-displayedRows");

    /**
     * Every cell of one column, by the column's server field name — {@code status},
     * {@code priority}, {@code due_date}, {@code label}, {@code title}, {@code created_at},
     * {@code actions}, and so on.
     *
     * <p>This is the locator that replaces column-index XPaths such as
     * {@code //tr/td[4]}. An index silently reads the wrong column the moment a column is
     * added, reordered or hidden by a user preference — and the test still passes, because
     * a wrong value is rarely an empty one.
     */
    public static By gridCells(String dataField) {
        return By.cssSelector(".MuiDataGrid-row[data-id] [data-field='" + dataField + "']");
    }

    /** One known row, by its server id. */
    public static By gridRow(String rowId) {
        return By.cssSelector(".MuiDataGrid-row[data-id='" + rowId + "']");
    }

    /** One cell of one known row. */
    public static By gridCell(String rowId, String dataField) {
        return By.cssSelector(
                ".MuiDataGrid-row[data-id='" + rowId + "'] [data-field='" + dataField + "']");
    }

    /** The first row whose visible text contains {@code text} — for "find the row I just made". */
    public static By gridRowContaining(String text) {
        return By.xpath("//div[contains(@class,'MuiDataGrid-row')][@data-id]"
                + "[.//*[contains(normalize-space(.)," + quote(text) + ")]]");
    }

    // ================================================================
    // FORM FIELDS — label-relative, FormControl-scoped
    // ================================================================

    /**
     * The input belonging to a visible label, found by climbing to the enclosing
     * {@code MuiFormControl-root} rather than walking the document in order.
     *
     * <p><b>What this replaces.</b> Sixteen locators in the page-object layer used
     * {@code //label[contains(text(),'Name')]/following::input[1]}. That has three failure
     * modes on V1.36, all silent: {@code following::} leaves the field's own container and
     * will happily return an input from the next section; {@code contains(text(),…)} misses
     * any label whose text sits in a nested element; and an unscoped match picks up the same
     * label from a second form mounted elsewhere in the SPA. Climbing to the FormControl
     * cannot leave the field, because the label and the input are siblings inside it.
     *
     * <p>Matching is {@code starts-with} on purpose: MUI renders the required marker as part
     * of the label's text, so the live label for a required field reads "WO Name / # *".
     */
    public static By fieldByLabel(String label) {
        return By.xpath("//label[starts-with(normalize-space(.)," + quote(label) + ")]"
                + "/ancestor::div[contains(@class,'MuiFormControl-root')][1]"
                + "//input[not(@type='hidden')]");
    }

    /** {@link #fieldByLabel} scoped to one surface — pass {@code ".MuiDialog-paper"} etc. */
    public static By fieldByLabelIn(String containerCss, String label) {
        return By.xpath("//*[contains(@class,'" + containerCss + "')]"
                + "//label[starts-with(normalize-space(.)," + quote(label) + ")]"
                + "/ancestor::div[contains(@class,'MuiFormControl-root')][1]"
                + "//input[not(@type='hidden')]");
    }

    /** The textarea belonging to a visible label. */
    public static By textareaByLabel(String label) {
        return By.xpath("//label[starts-with(normalize-space(.)," + quote(label) + ")]"
                + "/ancestor::div[contains(@class,'MuiFormControl-root')][1]"
                + "//textarea[not(@aria-hidden='true')]");
    }

    /**
     * The Autocomplete input for a label. Same climb as {@link #fieldByLabel}, but anchored on
     * the {@code MuiAutocomplete-root} so the caller knows a popper is involved: these fields
     * only open their option list in response to typing.
     */
    public static By autocompleteByLabel(String label) {
        return By.xpath("//div[contains(@class,'MuiAutocomplete-root')]"
                + "[.//label[starts-with(normalize-space(.)," + quote(label) + ")]]//input");
    }

    /** The MUI Select trigger for a label (a div, not an input — it has no text to type into). */
    public static By selectByLabel(String label) {
        return By.xpath("//div[contains(@class,'MuiFormControl-root')]"
                + "[./label[starts-with(normalize-space(.)," + quote(label) + ")]]"
                + "//div[contains(@class,'MuiSelect-select')]");
    }

    /** Open dropdown options, for either an Autocomplete popper or a Select menu. */
    public static final By LISTBOX_OPTIONS = By.cssSelector("[role='listbox'] [role='option']");

    /** One dropdown option by exact visible text. */
    public static By option(String text) {
        return By.xpath("//*[@role='option'][normalize-space(.)=" + quote(text) + "]");
    }

    // ================================================================
    // BUTTONS
    // ================================================================

    /**
     * A button by its visible label.
     *
     * <p>Uses {@code normalize-space(.)}, never {@code text()}. MUI wraps a button's label in
     * a nested {@code <span class="MuiButton-label">}, so {@code contains(text(),'Save')}
     * evaluates against the button's own (empty) text node and matches nothing — a bug that
     * was live in two page objects. The dot form reads the whole subtree.
     */
    public static By button(String label) {
        return By.xpath("//button[normalize-space(.)=" + quote(label) + "]");
    }

    /** A button by visible label, scoped to {@code <main>} so sidebar chrome can't match. */
    public static By mainButton(String label) {
        return By.xpath("//main//button[normalize-space(.)=" + quote(label) + "]");
    }

    /** A button by visible label, scoped to the open dialog or right drawer. */
    public static By surfaceButton(String label) {
        return By.xpath("//*[contains(@class,'MuiDialog-paper')"
                + " or contains(@class,'MuiDrawer-paper')]"
                + "//button[normalize-space(.)=" + quote(label) + "]");
    }

    /** The destructive confirm button in a delete dialog. */
    public static final By CONFIRM_DELETE = By.cssSelector(
            "[role='dialog'] .MuiButton-containedError, .MuiDialog-paper .MuiButton-containedError");

    /** A row's kebab / overflow menu trigger. */
    public static final By ROW_KEBAB = By.cssSelector("[data-field='actions'] button");

    /** An open menu's item, by visible text. */
    public static By menuItem(String text) {
        return By.xpath("//*[@role='menuitem'][starts-with(normalize-space(.)," + quote(text) + ")]");
    }

    // ================================================================
    // TABS AND WIZARD STEPS
    // ================================================================

    /**
     * A tab by label, scoped to {@code <main>}.
     *
     * <p>The scoping is load-bearing, not tidiness. Four rail category names double as
     * in-page tab labels, and "Engineering" is both: an unscoped
     * {@code //*[text()='Engineering']} on the asset detail page matches the sidebar rail
     * button first and navigates to /arc-flash, so the test then asserts against a different
     * page entirely. Observed live 2026-09-02.
     *
     * <p>{@code starts-with} because tabs carry count badges — the live label is
     * "Engineering 4", not "Engineering".
     */
    public static By tab(String label) {
        return By.xpath("//main//*[@role='tab'][starts-with(normalize-space(.)," + quote(label) + ")]");
    }

    /** The currently selected tab. */
    public static final By ACTIVE_TAB = By.cssSelector("main [role='tab'][aria-selected='true']");

    /** The stepper in a multi-step dialog (the Create Work Order wizard). */
    public static final By STEPPER = By.cssSelector(".MuiStepper-root");

    /** Every step label in the open wizard, in order. */
    public static final By STEP_LABELS = By.cssSelector(".MuiStepLabel-label");

    /** One wizard step's label, by its text. */
    public static By step(String label) {
        return By.xpath("//*[contains(@class,'MuiStepLabel-label')]"
                + "[starts-with(normalize-space(.)," + quote(label) + ")]");
    }

    /** The active step label. */
    public static final By ACTIVE_STEP =
            By.cssSelector(".MuiStepLabel-label.Mui-active, .MuiStep-root .Mui-active");

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * XPath string literal for {@code raw}, safe for text containing quotes.
     *
     * <p>XPath 1.0 has no escape mechanism inside a string literal, so a value containing
     * both {@code '} and {@code "} has to be assembled with {@code concat()}. Asset and work
     * order names entered by tests routinely contain apostrophes.
     */
    public static String quote(String raw) {
        String s = (raw == null) ? "" : raw;
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = s.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",\"'\",");
            sb.append("'").append(parts[i]).append("'");
        }
        return sb.append(")").toString();
    }
}
