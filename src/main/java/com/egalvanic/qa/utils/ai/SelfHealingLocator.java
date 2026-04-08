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
import java.util.*;

/**
 * Self-Healing Locator Framework
 *
 * When a locator fails (element not found), this class:
 * 1. Captures the current DOM context around where the element should be
 * 2. Tries 6 alternative locator strategies (text, placeholder, role, CSS class, etc.)
 * 3. If local strategies fail, optionally asks Claude AI to suggest a new locator
 * 4. Logs the healed locator to a persistent JSON registry for future runs
 *
 * Usage in test code:
 *   WebElement el = SelfHealingLocator.findElement(driver, By.xpath("//old/broken/path"), "Save Button");
 *
 * The "description" parameter helps AI understand what element you're looking for.
 */
public class SelfHealingLocator {

    private static final String HEAL_LOG_PATH = "test-output/healed-locators.json";
    private static final Map<String, HealRecord> healRegistry = new LinkedHashMap<>();
    private static boolean initialized = false;

    /**
     * Find element with self-healing: tries the original locator first,
     * then alternative strategies if it fails.
     *
     * @param driver      WebDriver instance
     * @param original    the original locator (may be broken)
     * @param description human-readable description (e.g., "Save Changes button")
     * @return the found WebElement
     * @throws NoSuchElementException if all healing strategies fail
     */
    public static WebElement findElement(WebDriver driver, By original, String description) {
        loadRegistry();

        // Check if we have a previously healed locator for this original
        String key = original.toString();
        HealRecord cached = healRegistry.get(key);
        if (cached != null) {
            try {
                WebElement el = driver.findElement(cached.healedLocator);
                cached.hitCount++;
                return el;
            } catch (NoSuchElementException e) {
                // Healed locator also broken — remove from cache and re-heal
                healRegistry.remove(key);
            }
        }

        // Try the original locator first
        try {
            return driver.findElement(original);
        } catch (NoSuchElementException e) {
            System.out.println("[SelfHeal] Original locator FAILED: " + original);
            System.out.println("[SelfHeal] Attempting to heal for: " + description);
        }

        // === HEALING PHASE ===
        // Strategy 1-6: Local heuristic strategies (fast, no API call)
        WebElement healed = tryLocalStrategies(driver, original, description);

        // Strategy 7: AI-powered healing (calls Claude if local strategies fail)
        if (healed == null && ClaudeClient.isConfigured()) {
            healed = tryAIHealing(driver, original, description);
        }

        if (healed != null) {
            System.out.println("[SelfHeal] HEALED successfully for: " + description);
            saveRegistry();
            return healed;
        }

        // All strategies exhausted
        throw new NoSuchElementException(
                "[SelfHeal] Could not heal locator for '" + description + "'. Original: " + original);
    }

    /**
     * Find elements (plural) with self-healing fallback.
     */
    public static List<WebElement> findElements(WebDriver driver, By original, String description) {
        List<WebElement> elements = driver.findElements(original);
        if (!elements.isEmpty()) return elements;

        System.out.println("[SelfHeal] No elements found for: " + original + " (" + description + ")");

        // Try local strategies to find at least one
        WebElement single = tryLocalStrategies(driver, original, description);
        if (single != null) {
            return Collections.singletonList(single);
        }
        return Collections.emptyList();
    }

    // =========================================================================
    // LOCAL HEALING STRATEGIES (no API call, fast)
    // =========================================================================

    private static WebElement tryLocalStrategies(WebDriver driver, By original, String description) {
        String origStr = original.toString();

        // Extract useful hints from the original locator
        String textHint = extractTextFromLocator(origStr);
        String placeholderHint = extractPlaceholderFromLocator(origStr);
        String roleHint = extractRoleFromLocator(origStr);

        List<LocatorStrategy> strategies = new ArrayList<>();

        // Strategy 1: Visible text match (buttons, links, headings)
        if (textHint != null) {
            strategies.add(new LocatorStrategy("text-exact",
                    By.xpath("//*[normalize-space()='" + textHint + "']")));
            strategies.add(new LocatorStrategy("text-contains",
                    By.xpath("//*[contains(normalize-space(),'" + textHint + "')]")));
        }

        // Strategy 2: Placeholder match (input fields)
        if (placeholderHint != null) {
            strategies.add(new LocatorStrategy("placeholder",
                    By.xpath("//input[@placeholder='" + placeholderHint + "'] | //textarea[@placeholder='" + placeholderHint + "']")));
        }

        // Strategy 3: aria-label match
        if (textHint != null) {
            strategies.add(new LocatorStrategy("aria-label",
                    By.xpath("//*[@aria-label='" + textHint + "' or @title='" + textHint + "']")));
        }

        // Strategy 4: Role-based match
        if (roleHint != null && textHint != null) {
            strategies.add(new LocatorStrategy("role+text",
                    By.xpath("//*[@role='" + roleHint + "' and contains(normalize-space(),'" + textHint + "')]")));
        }

        // Strategy 5: Description-based search (use the human description)
        if (description != null && !description.isEmpty()) {
            String desc = description.trim();
            strategies.add(new LocatorStrategy("desc-text",
                    By.xpath("//*[normalize-space()='" + desc + "']")));
            strategies.add(new LocatorStrategy("desc-contains",
                    By.xpath("//*[contains(normalize-space(),'" + desc + "')]")));
            strategies.add(new LocatorStrategy("desc-aria",
                    By.xpath("//*[@aria-label='" + desc + "' or @title='" + desc + "']")));
            strategies.add(new LocatorStrategy("desc-placeholder",
                    By.xpath("//input[@placeholder='" + desc + "'] | //textarea[@placeholder='" + desc + "']")));
        }

        // Strategy 6: CSS class pattern extraction
        String classHint = extractClassFromLocator(origStr);
        if (classHint != null) {
            strategies.add(new LocatorStrategy("css-class",
                    By.cssSelector("." + classHint)));
        }

        // Try each strategy
        for (LocatorStrategy strategy : strategies) {
            try {
                List<WebElement> found = driver.findElements(strategy.locator);
                if (!found.isEmpty()) {
                    WebElement el = found.get(0);
                    if (el.isDisplayed()) {
                        System.out.println("[SelfHeal] Strategy '" + strategy.name + "' found element: " + strategy.locator);
                        registerHeal(original, strategy.locator, strategy.name, description);
                        return el;
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    // =========================================================================
    // AI-POWERED HEALING (calls Claude API)
    // =========================================================================

    private static WebElement tryAIHealing(WebDriver driver, By original, String description) {
        try {
            // Capture DOM context around the area where element should be
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String pageSnippet = (String) js.executeScript(
                    "var body = document.body.innerHTML;"
                    + "if (body.length > 8000) body = body.substring(0, 8000);"
                    + "return body;");

            String systemPrompt =
                    "You are a Selenium test automation expert specializing in self-healing locators. "
                    + "Given a broken XPath/CSS locator and the current page HTML, suggest 3 alternative "
                    + "locators that would find the same element. Return ONLY a JSON array of objects with "
                    + "'strategy' (xpath or css) and 'locator' (the actual locator string). No explanation.";

            String userPrompt = String.format(
                    "BROKEN LOCATOR: %s\n"
                    + "ELEMENT DESCRIPTION: %s\n"
                    + "CURRENT PAGE URL: %s\n"
                    + "PAGE HTML (truncated):\n%s",
                    original, description, driver.getCurrentUrl(), pageSnippet);

            String aiResponse = ClaudeClient.ask(systemPrompt, userPrompt);
            if (aiResponse == null) return null;

            // Parse AI suggestions and try each one
            JSONArray suggestions = extractJsonArray(aiResponse);
            if (suggestions == null) return null;

            for (int i = 0; i < suggestions.length(); i++) {
                JSONObject suggestion = suggestions.getJSONObject(i);
                String strategy = suggestion.optString("strategy", "xpath");
                String locatorStr = suggestion.getString("locator");

                By aiLocator;
                if ("css".equalsIgnoreCase(strategy)) {
                    aiLocator = By.cssSelector(locatorStr);
                } else {
                    aiLocator = By.xpath(locatorStr);
                }

                try {
                    List<WebElement> found = driver.findElements(aiLocator);
                    if (!found.isEmpty() && found.get(0).isDisplayed()) {
                        System.out.println("[SelfHeal] AI suggestion #" + (i + 1) + " WORKED: " + aiLocator);
                        registerHeal(original, aiLocator, "ai-claude", description);
                        return found.get(0);
                    }
                } catch (Exception ignored) {}
            }
            System.out.println("[SelfHeal] All AI suggestions failed for: " + description);
        } catch (Exception e) {
            System.out.println("[SelfHeal] AI healing error: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // HEAL REGISTRY (persistent JSON log)
    // =========================================================================

    private static void registerHeal(By original, By healed, String strategy, String description) {
        HealRecord record = new HealRecord();
        record.originalLocator = original;
        record.healedLocator = healed;
        record.strategy = strategy;
        record.description = description;
        record.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        record.hitCount = 1;
        healRegistry.put(original.toString(), record);
    }

    private static void loadRegistry() {
        if (initialized) return;
        initialized = true;
        try {
            Path path = Paths.get(HEAL_LOG_PATH);
            if (Files.exists(path)) {
                String content = new String(Files.readAllBytes(path));
                JSONArray arr = new JSONArray(content);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    HealRecord r = new HealRecord();
                    r.strategy = obj.getString("strategy");
                    r.description = obj.getString("description");
                    r.timestamp = obj.getString("timestamp");
                    r.hitCount = obj.optInt("hitCount", 0);

                    String healedStr = obj.getString("healedLocator");
                    if (healedStr.startsWith("By.xpath:")) {
                        r.healedLocator = By.xpath(healedStr.substring(10).trim());
                    } else if (healedStr.startsWith("By.cssSelector:")) {
                        r.healedLocator = By.cssSelector(healedStr.substring(16).trim());
                    }

                    String origStr = obj.getString("originalLocator");
                    r.originalLocator = By.xpath(origStr); // simplified
                    healRegistry.put(origStr, r);
                }
                System.out.println("[SelfHeal] Loaded " + healRegistry.size() + " healed locators from registry");
            }
        } catch (Exception e) {
            System.out.println("[SelfHeal] Could not load registry: " + e.getMessage());
        }
    }

    static void saveRegistry() {
        try {
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, HealRecord> entry : healRegistry.entrySet()) {
                HealRecord r = entry.getValue();
                JSONObject obj = new JSONObject();
                obj.put("originalLocator", entry.getKey());
                obj.put("healedLocator", r.healedLocator.toString());
                obj.put("strategy", r.strategy);
                obj.put("description", r.description);
                obj.put("timestamp", r.timestamp);
                obj.put("hitCount", r.hitCount);
                arr.put(obj);
            }
            Path path = Paths.get(HEAL_LOG_PATH);
            Files.createDirectories(path.getParent());
            Files.write(path, arr.toString(2).getBytes());
        } catch (Exception e) {
            System.out.println("[SelfHeal] Could not save registry: " + e.getMessage());
        }
    }

    /**
     * Bridge method for SelfHealingDriver — register a healed locator discovered
     * by the driver's own alternative locator generation.
     */
    public static void registerHealFromDriver(By original, By healed) {
        loadRegistry();
        registerHeal(original, healed, "driver-auto", original.toString());
        saveRegistry();
    }

    /**
     * Bridge method for SelfHealingDriver — check if we have a cached heal
     * for the given locator from a previous run.
     *
     * @return the healed By locator, or null if none cached
     */
    public static By getCachedHeal(By original) {
        loadRegistry();
        String key = original.toString();
        HealRecord cached = healRegistry.get(key);
        if (cached != null && cached.healedLocator != null) {
            cached.hitCount++;
            return cached.healedLocator;
        }
        return null;
    }

    /**
     * Generate a summary report of all healed locators.
     */
    public static String getHealingSummary() {
        if (healRegistry.isEmpty()) return "No locators were healed in this session.";
        StringBuilder sb = new StringBuilder();
        sb.append("=== SELF-HEALING LOCATOR REPORT ===\n");
        sb.append(String.format("Total healed: %d\n\n", healRegistry.size()));
        for (Map.Entry<String, HealRecord> entry : healRegistry.entrySet()) {
            HealRecord r = entry.getValue();
            sb.append(String.format("  [%s] %s\n", r.strategy, r.description));
            sb.append(String.format("    Original: %s\n", entry.getKey()));
            sb.append(String.format("    Healed:   %s\n", r.healedLocator));
            sb.append(String.format("    Hits: %d | Since: %s\n\n", r.hitCount, r.timestamp));
        }
        return sb.toString();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static String extractTextFromLocator(String locator) {
        // Extract text from patterns like normalize-space()='Save Changes'
        int idx = locator.indexOf("normalize-space()='");
        if (idx >= 0) {
            int start = idx + "normalize-space()='".length();
            int end = locator.indexOf("'", start);
            if (end > start) return locator.substring(start, end);
        }
        // Extract from contains(normalize-space(),'text')
        idx = locator.indexOf("contains(normalize-space(),'");
        if (idx >= 0) {
            int start = idx + "contains(normalize-space(),'".length();
            int end = locator.indexOf("'", start);
            if (end > start) return locator.substring(start, end);
        }
        // Extract from text()='...'
        idx = locator.indexOf("text()='");
        if (idx >= 0) {
            int start = idx + "text()='".length();
            int end = locator.indexOf("'", start);
            if (end > start) return locator.substring(start, end);
        }
        return null;
    }

    private static String extractPlaceholderFromLocator(String locator) {
        int idx = locator.indexOf("@placeholder='");
        if (idx >= 0) {
            int start = idx + "@placeholder='".length();
            int end = locator.indexOf("'", start);
            if (end > start) return locator.substring(start, end);
        }
        return null;
    }

    private static String extractRoleFromLocator(String locator) {
        int idx = locator.indexOf("@role='");
        if (idx >= 0) {
            int start = idx + "@role='".length();
            int end = locator.indexOf("'", start);
            if (end > start) return locator.substring(start, end);
        }
        return null;
    }

    private static String extractClassFromLocator(String locator) {
        int idx = locator.indexOf("contains(@class,'");
        if (idx >= 0) {
            int start = idx + "contains(@class,'".length();
            int end = locator.indexOf("'", start);
            if (end > start) return locator.substring(start, end);
        }
        return null;
    }

    private static JSONArray extractJsonArray(String response) {
        try {
            // Find JSON array in response (may be wrapped in markdown code block)
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start >= 0 && end > start) {
                return new JSONArray(response.substring(start, end + 1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    private static class HealRecord {
        By originalLocator;
        By healedLocator;
        String strategy;
        String description;
        String timestamp;
        int hitCount;
    }

    private static class LocatorStrategy {
        final String name;
        final By locator;

        LocatorStrategy(String name, By locator) {
            this.name = name;
            this.locator = locator;
        }
    }
}
