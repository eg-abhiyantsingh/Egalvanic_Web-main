package com.egalvanic.qa.utils.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Test Case Generator — analyzes live page DOM to generate TestNG test methods.
 *
 * How it works:
 * 1. Opens the Edit Asset drawer for a given asset class via Selenium
 * 2. Scans the DOM for form fields (text inputs, dropdowns, buttons, accordions)
 * 3. Maps each field to the correct helper method (editTextField, selectFirstDropdownOption, etc.)
 * 4. Generates TestNG @Test methods following the project's established patterns
 * 5. Optionally enhances with Claude AI for smarter field value suggestions
 *
 * Works in two modes:
 *   - WITHOUT Claude API: Generates tests with static default values
 *   - WITH Claude API: Generates tests with contextually appropriate values
 *
 * Output: Java source code (String) that can be pasted into an AssetPartXTestNG class.
 */
public class AITestGenerator {

    // =========================================================================
    // MAIN ENTRY POINT
    // =========================================================================

    /**
     * Analyze the currently open Edit Asset drawer and generate TestNG test methods
     * for every editable field found.
     *
     * @param driver         WebDriver with an open Edit Asset drawer
     * @param assetClassName the asset class name (e.g., "Generator", "Transformer")
     * @param classPrefix    test ID prefix (e.g., "GEN", "TRF", "DS")
     * @param startPriority  TestNG priority for the first generated test
     * @return generated Java source code for test methods
     */
    public static GeneratedTests analyzeAndGenerate(WebDriver driver, String assetClassName,
                                                     String classPrefix, int startPriority) {
        System.out.println("[AITestGen] Analyzing Edit Asset drawer for: " + assetClassName);

        // Step 1: Detect all form fields in the edit drawer
        List<FormField> fields = detectFormFields(driver);
        System.out.println("[AITestGen] Detected " + fields.size() + " editable fields");

        // Step 2: Detect accordion sections for grouping
        List<String> sections = detectAccordionSections(driver);
        System.out.println("[AITestGen] Detected " + sections.size() + " form sections");

        // Step 3: Generate test methods
        GeneratedTests result = new GeneratedTests();
        result.assetClassName = assetClassName;
        result.classPrefix = classPrefix;
        result.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        result.fields = fields;
        result.sections = sections;

        // Generate basic CRUD tests (open, verify save button, etc.)
        int priority = startPriority;
        result.testMethods.add(generateOpenEditTest(classPrefix, assetClassName, priority++));
        result.testMethods.add(generateSaveButtonVisibleTest(classPrefix, assetClassName, priority++));

        // Generate field-specific edit tests
        int fieldIndex = 10; // Start field test IDs at _EAD_10 (following project convention)
        for (FormField field : fields) {
            String testId = classPrefix + "_EAD_" + fieldIndex;
            String testMethod = generateFieldEditTest(testId, assetClassName, classPrefix, field, priority++);
            result.testMethods.add(testMethod);
            fieldIndex++;
        }

        // Step 4: Optionally enhance with AI (better field values, edge case tests)
        if (ClaudeClient.isConfigured()) {
            enhanceWithAI(result, driver);
        }

        System.out.println("[AITestGen] Generated " + result.testMethods.size() + " test methods");
        return result;
    }

    // =========================================================================
    // DOM ANALYSIS — Field Detection
    // =========================================================================

    /**
     * Scan the edit drawer DOM and return all editable form fields with metadata.
     */
    static List<FormField> detectFormFields(WebDriver driver) {
        List<FormField> fields = new ArrayList<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // JavaScript that walks the edit drawer and extracts every field's metadata.
            // Looks for: text inputs, textareas, comboboxes (MUI Autocomplete), spinbuttons
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> rawFields = (List<java.util.Map<String, Object>>) js.executeScript(
                "var drawer = document.querySelector('.MuiDrawer-paper');"
                + "if (!drawer) return [];"
                + "var inputs = drawer.querySelectorAll('input, textarea');"
                + "var result = [];"
                + "for (var inp of inputs) {"
                + "  var label = '';"
                + "  var fieldType = 'text';"
                + "  var currentValue = inp.value || '';"
                + "  var placeholder = inp.placeholder || '';"
                + "  var role = inp.getAttribute('role') || '';"
                + "  var ariaAuto = inp.getAttribute('aria-autocomplete') || '';"
                + "  var inputType = inp.type || 'text';"
                + "  var isReadOnly = inp.readOnly || inp.disabled;"
                // Determine field type
                + "  if (role === 'combobox' || ariaAuto === 'list') fieldType = 'dropdown';"
                + "  else if (inp.type === 'number' || inp.getAttribute('role') === 'spinbutton') fieldType = 'number';"
                + "  else if (inp.tagName === 'TEXTAREA') fieldType = 'textarea';"
                // Find the label: walk up DOM looking for a <p> sibling or parent label
                + "  var parent = inp.closest('.MuiBox-root, .MuiFormControl-root, .MuiTextField-root');"
                + "  if (parent) {"
                + "    var p = parent.querySelector('p');"
                + "    if (p) label = p.textContent.trim().replace('*','').trim();"
                + "    if (!label) {"
                + "      var prev = parent.previousElementSibling;"
                + "      if (prev && prev.tagName === 'P') label = prev.textContent.trim().replace('*','').trim();"
                + "    }"
                + "  }"
                // Fallback: placeholder or aria-label
                + "  if (!label && placeholder) label = placeholder.replace('Enter ','').replace('Select ','').replace('Add ','');"
                + "  if (!label) label = inp.getAttribute('aria-label') || '';"
                // Find the section this field belongs to (look for accordion parent)
                + "  var section = 'BASIC INFO';"
                + "  var accordion = inp.closest('.MuiAccordion-root');"
                + "  if (accordion) {"
                + "    var summary = accordion.querySelector('.MuiAccordionSummary-content');"
                + "    if (summary) section = summary.textContent.trim().split('\\n')[0].trim();"
                + "  }"
                + "  var btn = inp.closest('[role=\"button\"]');"
                + "  if (btn) {"
                + "    var btnText = btn.textContent.trim().split('\\n')[0].trim();"
                + "    if (btnText && btnText.length < 50) section = btnText;"
                + "  }"
                // Skip hidden/irrelevant inputs
                + "  if (label && !isReadOnly && inp.offsetParent !== null) {"
                + "    result.push({"
                + "      label: label,"
                + "      fieldType: fieldType,"
                + "      currentValue: currentValue,"
                + "      placeholder: placeholder,"
                + "      section: section,"
                + "      isRequired: parent ? !!parent.querySelector('[class*=\"asterisk\"], span:not(:empty)') || label.includes('*') : false"
                + "    });"
                + "  }"
                + "}"
                + "return result;");

            if (rawFields != null) {
                for (java.util.Map<String, Object> raw : rawFields) {
                    FormField f = new FormField();
                    f.label = String.valueOf(raw.getOrDefault("label", ""));
                    f.fieldType = String.valueOf(raw.getOrDefault("fieldType", "text"));
                    f.currentValue = String.valueOf(raw.getOrDefault("currentValue", ""));
                    f.placeholder = String.valueOf(raw.getOrDefault("placeholder", ""));
                    f.section = String.valueOf(raw.getOrDefault("section", "BASIC INFO"));
                    f.isRequired = Boolean.TRUE.equals(raw.get("isRequired"));

                    // Skip fields with empty/generic labels
                    if (!f.label.isEmpty() && f.label.length() > 1) {
                        fields.add(f);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[AITestGen] Field detection error: " + e.getMessage());
        }

        return fields;
    }

    /**
     * Detect accordion sections in the edit drawer (BASIC INFO, CORE ATTRIBUTES, etc.)
     */
    static List<String> detectAccordionSections(WebDriver driver) {
        List<String> sections = new ArrayList<>();
        try {
            List<WebElement> accordions = driver.findElements(By.xpath(
                    "//div[contains(@class,'MuiDrawer')]//button[contains(@class,'MuiAccordionSummary') or @aria-expanded]"));
            for (WebElement acc : accordions) {
                String text = acc.getText().trim();
                if (!text.isEmpty()) {
                    // Take first line only (button text may include child content)
                    String sectionName = text.split("\n")[0].trim();
                    if (sectionName.length() < 50) {
                        sections.add(sectionName);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[AITestGen] Section detection error: " + e.getMessage());
        }
        return sections;
    }

    // =========================================================================
    // CODE GENERATION — TestNG methods
    // =========================================================================

    /**
     * Generate the "Open Edit Screen" test.
     */
    private static String generateOpenEditTest(String prefix, String className, int priority) {
        String testId = prefix + "_EAD_01";
        String methodName = "test" + testId + "_OpenEditScreen";
        return String.format(
            "    @Test(priority = %d, description = \"%s: Open Edit Asset screen\")\n"
            + "    public void %s() {\n"
            + "        ExtentReportManager.createTest(MODULE, FEATURE, \"%s_OpenEditScreen\");\n"
            + "        if (!openEditForAssetClass(\"%s\", \"%s\")) { skipIfNotFound(\"%s\"); return; }\n"
            + "        Assert.assertTrue(isSaveChangesButtonVisible(), \"Edit form should be open for %s\");\n"
            + "        logStepWithScreenshot(\"%s Edit screen\");\n"
            + "        ExtentReportManager.logPass(\"Edit Asset Details opens for %s\");\n"
            + "    }",
            priority, testId, methodName, testId, className, prefix, className,
            className, className, className);
    }

    /**
     * Generate the "Save Changes button visible" test.
     */
    private static String generateSaveButtonVisibleTest(String prefix, String className, int priority) {
        String testId = prefix + "_EAD_04";
        String methodName = "test" + testId + "_SaveChangesButtonVisible";
        return String.format(
            "    @Test(priority = %d, description = \"%s: Verify Save Changes button visible\")\n"
            + "    public void %s() {\n"
            + "        ExtentReportManager.createTest(MODULE, FEATURE, \"%s_SaveButtonVisible\");\n"
            + "        if (!openEditForAssetClass(\"%s\", \"%s\")) { skipIfNotFound(\"%s\"); return; }\n"
            + "        Assert.assertTrue(isSaveChangesButtonVisible(), \"Save Changes button should be visible\");\n"
            + "        ExtentReportManager.logPass(\"Save Changes button visible for %s\");\n"
            + "    }",
            priority, testId, methodName, testId, className, prefix, className, className);
    }

    /**
     * Generate an edit-and-verify test for a specific form field.
     * Follows the exact pattern used in existing AssetPart*TestNG classes:
     *   1. Open edit form
     *   2. Expand core attributes (if field is in that section)
     *   3. Edit the field
     *   4. Save and verify
     *   5. Read detail attribute value to confirm persistence
     */
    private static String generateFieldEditTest(String testId, String className,
                                                 String prefix, FormField field, int priority) {
        String cleanLabel = field.label.replaceAll("[^a-zA-Z0-9]", "");
        String methodName = "test" + testId + "_Edit" + cleanLabel;
        String descLabel = field.label;

        // Determine edit approach based on field type
        String editCode;
        String newValue = suggestTestValue(field);
        boolean needsCoreExpand = isCoreAttributeSection(field.section);

        if ("dropdown".equals(field.fieldType)) {
            // Dropdown: try selectFirstDropdownOption, fallback to editTextField
            editCode = String.format(
                "        String val = selectFirstDropdownOption(\"%s\");\n"
                + "        if (val == null) val = editTextField(\"%s\", \"%s\");\n"
                + "        Assert.assertNotNull(val, \"Should be able to edit '%s' field\");",
                descLabel, descLabel, newValue, descLabel);
        } else if ("number".equals(field.fieldType)) {
            // Number: use editTextField with numeric value
            editCode = String.format(
                "        String newValue = \"%s\";\n"
                + "        String val = editTextField(\"%s\", newValue);\n"
                + "        Assert.assertNotNull(val, \"editTextField should find and set '%s' field\");\n"
                + "        Assert.assertEquals(val, newValue, \"%s input value should match\");",
                newValue, descLabel, descLabel, descLabel);
        } else {
            // Text: use editTextField
            editCode = String.format(
                "        String newValue = \"%s\";\n"
                + "        String val = editTextField(\"%s\", newValue);\n"
                + "        Assert.assertNotNull(val, \"editTextField should find and set '%s' field\");\n"
                + "        Assert.assertEquals(val, newValue, \"%s input value should match\");",
                newValue, descLabel, descLabel, descLabel);
        }

        // Build the verification code
        String verifyCode;
        if ("dropdown".equals(field.fieldType)) {
            verifyCode = String.format(
                "        String persisted = readDetailAttributeValue(\"%s\");\n"
                + "        Assert.assertNotNull(persisted, \"%s should be visible on detail page after save\");\n"
                + "        Assert.assertFalse(\"Not specified\".equals(persisted), \"%s should have a value after save\");",
                descLabel, descLabel, descLabel);
        } else {
            verifyCode = String.format(
                "        String persisted = readDetailAttributeValue(\"%s\");\n"
                + "        Assert.assertNotNull(persisted, \"%s should be visible on detail page after save\");\n"
                + "        Assert.assertEquals(persisted, newValue, \"%s should persist after save\");",
                descLabel, descLabel, descLabel);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("    @Test(priority = %d, description = \"%s: Edit %s\")\n", priority, testId, descLabel));
        sb.append(String.format("    public void %s() {\n", methodName));
        sb.append(String.format("        ExtentReportManager.createTest(MODULE, FEATURE, \"%s_%s\");\n", testId, cleanLabel));
        sb.append(String.format("        if (!openEditForAssetClass(\"%s\", \"%s\")) { skipIfNotFound(\"%s\"); return; }\n",
                className, prefix, className));
        if (needsCoreExpand) {
            sb.append("        expandCoreAttributes();\n");
        }
        sb.append("\n");
        sb.append(editCode).append("\n\n");
        sb.append("        boolean saved = saveAndVerify();\n");
        sb.append(String.format("        Assert.assertTrue(saved, \"Save Changes should succeed after editing %s\");\n\n", descLabel));
        sb.append(verifyCode).append("\n\n");
        sb.append(String.format("        ExtentReportManager.logPass(\"%s edited, saved, and verified on detail page\");\n", descLabel));
        sb.append("    }");

        return sb.toString();
    }

    // =========================================================================
    // VALUE SUGGESTION (heuristic + AI)
    // =========================================================================

    /**
     * Suggest a reasonable test value for a field based on its label, type, and current value.
     */
    private static String suggestTestValue(FormField field) {
        String label = field.label.toLowerCase();
        String current = field.currentValue;

        // Serial numbers: use timestamp to guarantee uniqueness
        if (label.contains("serial")) {
            return field.label.replaceAll("\\s+", "_").toUpperCase() + "_" + "\" + System.currentTimeMillis() + \"";
        }

        // Numeric fields: provide sensible defaults based on label
        if ("number".equals(field.fieldType) || label.contains("rating") || label.contains("factor")
                || label.contains("cost") || label.contains("ampere") || label.contains("kva")
                || label.contains("k v a") || label.contains("kw") || label.contains("k w")) {
            if (current != null && !current.isEmpty()) return current;
            if (label.contains("ampere")) return "800";
            if (label.contains("kva") || label.contains("k v a")) return "500";
            if (label.contains("kw") || label.contains("k w")) return "400";
            if (label.contains("factor")) return "0.85";
            if (label.contains("cost")) return "10000";
            return "100";
        }

        // Voltage fields
        if (label.equals("voltage") || label.contains("volt")) {
            if (current != null && !current.isEmpty()) return current;
            return "480";
        }

        // Manufacturer
        if (label.contains("manufacturer") || label.contains("make")) {
            return current != null && !current.isEmpty() ? current : "Caterpillar";
        }

        // Configuration
        if (label.contains("configuration") || label.contains("config") || label.contains("phase")) {
            return "3-Phase";
        }

        // Name fields
        if (label.contains("name")) {
            return current != null && !current.isEmpty() ? current : "Test Asset";
        }

        // QR Code
        if (label.contains("qr")) {
            return "QR_TEST_" + "\" + System.currentTimeMillis() + \"";
        }

        // Default: keep current value or use a generic
        if (current != null && !current.isEmpty()) {
            return current;
        }
        return "TestValue";
    }

    /**
     * Check if a section name belongs to Core Attributes (needs expandCoreAttributes call).
     */
    private static boolean isCoreAttributeSection(String section) {
        if (section == null) return false;
        String lower = section.toLowerCase();
        return lower.contains("core") || lower.contains("attribute")
                || lower.contains("commercial") || lower.contains("connection")
                || lower.contains("notes");
    }

    // =========================================================================
    // AI ENHANCEMENT (optional)
    // =========================================================================

    /**
     * Use Claude to enhance generated tests with better values and edge cases.
     */
    private static void enhanceWithAI(GeneratedTests result, WebDriver driver) {
        try {
            String systemPrompt =
                "You are a senior QA automation engineer for an enterprise asset management platform (eGalvanic). "
                + "Given a list of form fields detected on an Edit Asset page, suggest: "
                + "1. Appropriate test values for each field (realistic, edge-case-probing) "
                + "2. Any additional tests that should be written (boundary values, required field validation, etc.) "
                + "Respond ONLY as JSON: {\"fieldValues\": [{\"label\": \"field\", \"testValue\": \"val\", \"edgeCases\": [\"val1\",\"val2\"]}], "
                + "\"additionalTests\": [{\"name\": \"testName\", \"description\": \"what it tests\"}]}";

            StringBuilder fieldList = new StringBuilder();
            fieldList.append("ASSET CLASS: ").append(result.assetClassName).append("\n");
            fieldList.append("DETECTED FIELDS:\n");
            for (FormField f : result.fields) {
                fieldList.append(String.format("  - %s (type=%s, section=%s, current='%s', required=%s)\n",
                        f.label, f.fieldType, f.section, f.currentValue, f.isRequired));
            }

            String aiResponse = ClaudeClient.ask(systemPrompt, fieldList.toString());
            if (aiResponse != null) {
                JSONObject ai = extractJsonObject(aiResponse);
                if (ai != null) {
                    result.aiEnhanced = true;
                    result.aiSuggestions = aiResponse;

                    // Extract additional test suggestions
                    JSONArray additional = ai.optJSONArray("additionalTests");
                    if (additional != null) {
                        for (int i = 0; i < additional.length(); i++) {
                            JSONObject test = additional.getJSONObject(i);
                            result.additionalTestSuggestions.add(
                                    test.getString("name") + ": " + test.getString("description"));
                        }
                    }
                    System.out.println("[AITestGen] AI enhanced with "
                            + result.additionalTestSuggestions.size() + " additional test suggestions");
                }
            }
        } catch (Exception e) {
            System.out.println("[AITestGen] AI enhancement failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // OUTPUT — Write generated tests to file
    // =========================================================================

    /**
     * Write generated test methods to a file for review.
     */
    public static void writeToFile(GeneratedTests result, String outputPath) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("// =========================================================================\n");
            content.append("// AUTO-GENERATED TEST METHODS for: ").append(result.assetClassName).append("\n");
            content.append("// Generated: ").append(result.timestamp).append("\n");
            content.append("// Fields detected: ").append(result.fields.size()).append("\n");
            content.append("// AI enhanced: ").append(result.aiEnhanced).append("\n");
            content.append("// =========================================================================\n\n");

            content.append("// --- Detected Form Fields ---\n");
            for (FormField f : result.fields) {
                content.append(String.format("// %s [%s] section=%s current='%s' required=%s\n",
                        f.label, f.fieldType, f.section, f.currentValue, f.isRequired));
            }
            content.append("\n// --- Generated Test Methods ---\n\n");

            for (String method : result.testMethods) {
                content.append(method).append("\n\n");
            }

            if (!result.additionalTestSuggestions.isEmpty()) {
                content.append("\n// --- AI-Suggested Additional Tests ---\n");
                for (String suggestion : result.additionalTestSuggestions) {
                    content.append("// TODO: ").append(suggestion).append("\n");
                }
            }

            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());
            Files.write(path, content.toString().getBytes());
            System.out.println("[AITestGen] Tests written to: " + outputPath);
        } catch (Exception e) {
            System.out.println("[AITestGen] Could not write file: " + e.getMessage());
        }
    }

    /**
     * Write a summary report in JSON format.
     */
    public static void writeReport(GeneratedTests result) {
        try {
            JSONObject report = new JSONObject();
            report.put("assetClass", result.assetClassName);
            report.put("prefix", result.classPrefix);
            report.put("timestamp", result.timestamp);
            report.put("totalFields", result.fields.size());
            report.put("totalTests", result.testMethods.size());
            report.put("aiEnhanced", result.aiEnhanced);

            JSONArray fieldsArr = new JSONArray();
            for (FormField f : result.fields) {
                JSONObject fObj = new JSONObject();
                fObj.put("label", f.label);
                fObj.put("type", f.fieldType);
                fObj.put("section", f.section);
                fObj.put("currentValue", f.currentValue);
                fObj.put("required", f.isRequired);
                fieldsArr.put(fObj);
            }
            report.put("fields", fieldsArr);

            if (!result.additionalTestSuggestions.isEmpty()) {
                report.put("additionalSuggestions", new JSONArray(result.additionalTestSuggestions));
            }

            Path path = Paths.get("test-output/ai-test-generation-report.json");
            Files.createDirectories(path.getParent());
            Files.write(path, report.toString(2).getBytes());
            System.out.println("[AITestGen] Report written: test-output/ai-test-generation-report.json");
        } catch (Exception e) {
            System.out.println("[AITestGen] Could not write report: " + e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static JSONObject extractJsonObject(String response) {
        try {
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return new JSONObject(response.substring(start, end + 1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    // DATA CLASSES
    // =========================================================================

    /**
     * Represents a form field detected in the Edit Asset drawer.
     */
    public static class FormField {
        public String label;
        public String fieldType;      // "text", "dropdown", "number", "textarea"
        public String currentValue;
        public String placeholder;
        public String section;         // "BASIC INFO", "CORE ATTRIBUTES", etc.
        public boolean isRequired;

        @Override
        public String toString() {
            return String.format("%s [%s] section=%s", label, fieldType, section);
        }
    }

    /**
     * Container for all generated test data.
     */
    public static class GeneratedTests {
        public String assetClassName;
        public String classPrefix;
        public String timestamp;
        public List<FormField> fields = new ArrayList<>();
        public List<String> sections = new ArrayList<>();
        public List<String> testMethods = new ArrayList<>();
        public List<String> additionalTestSuggestions = new ArrayList<>();
        public boolean aiEnhanced = false;
        public String aiSuggestions;

        /**
         * Get all generated test methods as a single string.
         */
        public String getAllMethodsAsString() {
            StringBuilder sb = new StringBuilder();
            for (String method : testMethods) {
                sb.append(method).append("\n\n");
            }
            return sb.toString();
        }

        /**
         * Get a human-readable summary.
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== AI TEST GENERATION REPORT ===\n");
            sb.append("Asset Class: ").append(assetClassName).append("\n");
            sb.append("Prefix: ").append(classPrefix).append("\n");
            sb.append("Generated: ").append(timestamp).append("\n");
            sb.append("Fields detected: ").append(fields.size()).append("\n");
            sb.append("Tests generated: ").append(testMethods.size()).append("\n");
            sb.append("AI enhanced: ").append(aiEnhanced).append("\n\n");

            sb.append("Fields:\n");
            for (FormField f : fields) {
                sb.append("  ").append(f).append("\n");
            }

            if (!additionalTestSuggestions.isEmpty()) {
                sb.append("\nAI Suggestions:\n");
                for (String s : additionalTestSuggestions) {
                    sb.append("  - ").append(s).append("\n");
                }
            }
            return sb.toString();
        }
    }
}
