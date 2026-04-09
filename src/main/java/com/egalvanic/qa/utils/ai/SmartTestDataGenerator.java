package com.egalvanic.qa.utils.ai;

import com.github.javafaker.Faker;
import org.testng.annotations.DataProvider;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Smart Test Data Generator — AI-Enhanced Random Data for Automation
 *
 * Provides realistic, edge-case, and boundary-value test data to reduce
 * the gap between manual and automation testing. Manual testers naturally
 * try creative inputs; this class does the same programmatically.
 *
 * Features:
 *   - Realistic data via JavaFaker (names, emails, phones, addresses)
 *   - Domain-specific generators (asset names, serial numbers, voltages)
 *   - Edge case generators (empty, max length, special chars, unicode, XSS, SQL injection)
 *   - Boundary value generators per field type
 *   - TestNG @DataProvider integration
 *   - Context-aware smartValue() with optional Claude AI fallback
 *
 * Thread-safe for parallel execution via ThreadLocal<Faker>.
 */
public class SmartTestDataGenerator {

    private SmartTestDataGenerator() {}

    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(Faker::new);
    private static final Random RANDOM = new Random();

    private static Faker faker() { return FAKER.get(); }

    // ================================================================
    // REALISTIC DATA GENERATORS
    // ================================================================

    public static String name()        { return faker().name().fullName(); }
    public static String firstName()   { return faker().name().firstName(); }
    public static String lastName()    { return faker().name().lastName(); }
    public static String email()       { return faker().internet().emailAddress(); }
    public static String phone()       { return faker().phoneNumber().cellPhone(); }
    public static String address()     { return faker().address().streetAddress(); }
    public static String city()        { return faker().address().city(); }
    public static String state()       { return faker().address().state(); }
    public static String zipCode()     { return faker().address().zipCode(); }
    public static String companyName() { return faker().company().name(); }
    public static String jobTitle()    { return faker().job().title(); }
    public static String sentence()    { return faker().lorem().sentence(); }
    public static String paragraph()   { return faker().lorem().paragraph(); }

    // ================================================================
    // DOMAIN-SPECIFIC GENERATORS (eGalvanic context)
    // ================================================================

    private static final String[] MANUFACTURERS = {
        "Caterpillar", "Siemens", "ABB", "Eaton", "Schneider Electric",
        "GE", "Mitsubishi", "Toshiba", "Cummins", "Kohler"
    };

    private static final String[] ASSET_CLASSES = {
        "Generator", "Transformer", "Switchgear", "Panel Board",
        "UPS", "ATS", "Motor", "VFD", "MCC", "Busway"
    };

    private static final int[] VOLTAGES = {120, 208, 240, 277, 480, 600, 4160, 13800};
    private static final int[] KVA_RATINGS = {50, 75, 100, 150, 225, 300, 500, 750, 1000, 1500, 2000};
    private static final String[] ISSUE_CLASSES = {"Electrical", "Mechanical", "Safety", "Environmental"};
    private static final String[] PRIORITIES = {"Low", "Medium", "High", "Critical"};

    public static String assetName() {
        return ASSET_CLASSES[RANDOM.nextInt(ASSET_CLASSES.length)] + "-" +
               faker().bothify("??###").toUpperCase();
    }

    public static String serialNumber() {
        return "SN-" + System.currentTimeMillis() % 100000 + "-" +
               faker().bothify("####").toUpperCase();
    }

    public static String qrCode() {
        return "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String locationName() {
        return "Building " + faker().address().buildingNumber();
    }

    public static String floorName() {
        return "Floor " + ThreadLocalRandom.current().nextInt(1, 20);
    }

    public static String roomName() {
        String[] types = {"Server Room", "Electrical Room", "MDF", "IDF", "Mechanical Room", "Office"};
        return types[RANDOM.nextInt(types.length)] + " " + faker().bothify("##");
    }

    public static String issueSummary() {
        String[] templates = {
            "Equipment malfunction in %s",
            "Unexpected noise from %s",
            "Temperature reading abnormal on %s",
            "Maintenance required for %s",
            "Safety concern: %s needs inspection",
            "Voltage fluctuation detected at %s"
        };
        String template = templates[RANDOM.nextInt(templates.length)];
        return String.format(template, assetName());
    }

    public static String workOrderTitle() {
        String[] actions = {"Inspect", "Repair", "Replace", "Upgrade", "Test", "Calibrate"};
        return actions[RANDOM.nextInt(actions.length)] + " " +
               ASSET_CLASSES[RANDOM.nextInt(ASSET_CLASSES.length)] + " - " +
               faker().bothify("WO-####");
    }

    public static String manufacturer() {
        return MANUFACTURERS[RANDOM.nextInt(MANUFACTURERS.length)];
    }

    public static String assetClass() {
        return ASSET_CLASSES[RANDOM.nextInt(ASSET_CLASSES.length)];
    }

    public static String voltage() {
        return String.valueOf(VOLTAGES[RANDOM.nextInt(VOLTAGES.length)]);
    }

    public static int ampereRating() {
        return ThreadLocalRandom.current().nextInt(100, 5001);
    }

    public static String kvaRating() {
        return String.valueOf(KVA_RATINGS[RANDOM.nextInt(KVA_RATINGS.length)]);
    }

    public static double powerFactor() {
        return Math.round((0.80 + RANDOM.nextDouble() * 0.19) * 100.0) / 100.0;
    }

    public static double replacementCost() {
        return Math.round(ThreadLocalRandom.current().nextDouble(1000, 500000) * 100.0) / 100.0;
    }

    public static String issueClass() {
        return ISSUE_CLASSES[RANDOM.nextInt(ISSUE_CLASSES.length)];
    }

    public static String priority() {
        return PRIORITIES[RANDOM.nextInt(PRIORITIES.length)];
    }

    // ================================================================
    // EDGE CASE GENERATORS
    // ================================================================

    public static String empty() { return ""; }

    public static String maxLength(int n) {
        return faker().lorem().characters(n);
    }

    public static String specialChars() {
        return "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
    }

    public static String unicode() {
        return "\u6d4b\u8bd5\u30c6\u30b9\u30c8 \u0627\u062e\u062a\u0628\u0627\u0631 \uD83D\uDD27\uD83D\uDCA1\u2705";
    }

    public static String emoji() {
        return "\uD83D\uDD27 \uD83C\uDFED \u26A1 \uD83D\uDEE0\uFE0F \uD83D\uDCCA \uD83D\uDCA1";
    }

    private static final String[] SQL_INJECTIONS = {
        "'; DROP TABLE users; --",
        "1' OR '1'='1",
        "admin'--",
        "1; SELECT * FROM passwords",
        "' UNION SELECT NULL,NULL,NULL--",
        "1' AND SLEEP(5)--",
        "Robert'); DROP TABLE students;--"
    };

    public static String sqlInjection() {
        return SQL_INJECTIONS[RANDOM.nextInt(SQL_INJECTIONS.length)];
    }

    private static final String[] XSS_VECTORS = {
        "<script>alert('XSS')</script>",
        "<img src=x onerror=alert(1)>",
        "javascript:alert(document.cookie)",
        "<svg/onload=alert('XSS')>",
        "\"><script>alert('XSS')</script>",
        "'-alert(1)-'",
        "<body onload=alert('XSS')>"
    };

    public static String xssVector() {
        return XSS_VECTORS[RANDOM.nextInt(XSS_VECTORS.length)];
    }

    public static String htmlInjection() {
        return "<b>bold</b><img src=x onerror=alert(1)><iframe src='evil.com'>";
    }

    public static String longString() {
        return faker().lorem().characters(10000);
    }

    public static String whitespaceOnly() {
        return "   \t\n   \t  ";
    }

    public static String numericString() {
        return "999999999999";
    }

    public static String negativeNumber() {
        return "-1";
    }

    public static String zeroValue() {
        return "0";
    }

    /**
     * Returns a random edge case input. Useful for monkey testing.
     */
    public static String randomEdgeCase() {
        int choice = RANDOM.nextInt(10);
        switch (choice) {
            case 0: return empty();
            case 1: return specialChars();
            case 2: return unicode();
            case 3: return sqlInjection();
            case 4: return xssVector();
            case 5: return longString();
            case 6: return whitespaceOnly();
            case 7: return numericString();
            case 8: return negativeNumber();
            default: return emoji();
        }
    }

    // ================================================================
    // BOUNDARY VALUE GENERATORS
    // ================================================================

    /**
     * Returns boundary values appropriate for the given field type.
     */
    public static List<String> boundaryValues(String fieldType) {
        if (fieldType == null) fieldType = "text";

        switch (fieldType.toLowerCase()) {
            case "number":
            case "numeric":
                return Arrays.asList("0", "1", "-1", "999999999", "-999999999", "0.001",
                        "0.0", "2147483647", "-2147483648");
            case "email":
                return Arrays.asList("a@b.c", "test@test.com", "invalid", "@nodomain.com",
                        "noat.com", "user@.com", "user@domain", "a@b.c.d.e.f",
                        faker().internet().emailAddress());
            case "phone":
                return Arrays.asList("1", "1234567890", "+1-555-555-5555",
                        "letters-not-phone", "000-000-0000", "+91" + faker().phoneNumber().subscriberNumber(10));
            case "url":
                return Arrays.asList("http://", "https://valid.com", "ftp://file.txt",
                        "not-a-url", "javascript:alert(1)", "//protocol-relative.com");
            default: // text
                return Arrays.asList("", "a", faker().lorem().characters(255),
                        faker().lorem().characters(1000), specialChars(), unicode());
        }
    }

    // ================================================================
    // CONTEXT-AWARE GENERATOR
    // ================================================================

    /**
     * Generates an intelligent test value based on the field label and type.
     * Uses label heuristics first, then field type, then random.
     * Optionally enhances with Claude API if configured.
     */
    public static String smartValue(String fieldLabel, String fieldType) {
        if (fieldLabel == null) fieldLabel = "";
        if (fieldType == null) fieldType = "text";

        String label = fieldLabel.toLowerCase().trim();

        // Label-based heuristics (mimics manual tester intuition)
        if (label.contains("email"))       return email();
        if (label.contains("phone") || label.contains("tel"))  return phone();
        if (label.contains("name") && label.contains("first")) return firstName();
        if (label.contains("name") && label.contains("last"))  return lastName();
        if (label.contains("name") && label.contains("asset")) return assetName();
        if (label.contains("name"))        return name();
        if (label.contains("address"))     return address();
        if (label.contains("city"))        return city();
        if (label.contains("state"))       return state();
        if (label.contains("zip") || label.contains("postal")) return zipCode();
        if (label.contains("company"))     return companyName();
        if (label.contains("title") && label.contains("work")) return workOrderTitle();
        if (label.contains("title") && label.contains("issue")) return issueSummary();
        if (label.contains("title"))       return faker().book().title();
        if (label.contains("serial"))      return serialNumber();
        if (label.contains("qr"))          return qrCode();
        if (label.contains("manufacturer"))return manufacturer();
        if (label.contains("voltage"))     return voltage();
        if (label.contains("ampere") || label.contains("amp"))  return String.valueOf(ampereRating());
        if (label.contains("kva"))         return kvaRating();
        if (label.contains("power factor"))return String.valueOf(powerFactor());
        if (label.contains("cost") || label.contains("price")) return String.valueOf(replacementCost());
        if (label.contains("priority"))    return priority();
        if (label.contains("class") && label.contains("issue")) return issueClass();
        if (label.contains("class"))       return assetClass();
        if (label.contains("description") || label.contains("notes") || label.contains("comment"))
            return faker().lorem().sentence(10);
        if (label.contains("building"))    return locationName();
        if (label.contains("floor"))       return floorName();
        if (label.contains("room"))        return roomName();
        if (label.contains("date"))        return faker().date().birthday().toString();
        if (label.contains("url") || label.contains("website")) return faker().internet().url();
        if (label.contains("password"))    return faker().internet().password(8, 20, true, true, true);

        // Field type fallback
        switch (fieldType.toLowerCase()) {
            case "email":    return email();
            case "number":   return String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000));
            case "tel":      return phone();
            case "url":      return faker().internet().url();
            case "textarea": return faker().lorem().paragraph();
            case "password": return faker().internet().password();
            default:         return faker().lorem().sentence(5);
        }
    }

    // ================================================================
    // TESTNG @DataProvider METHODS
    // ================================================================

    /**
     * 5 rows of realistic random data: [name, email, phone, address, company]
     */
    @DataProvider(name = "validInputs")
    public static Object[][] validInputs() {
        Object[][] data = new Object[5][5];
        for (int i = 0; i < 5; i++) {
            data[i] = new Object[]{name(), email(), phone(), address(), companyName()};
        }
        return data;
    }

    /**
     * Edge case inputs for text fields.
     */
    @DataProvider(name = "edgeCaseInputs")
    public static Object[][] edgeCaseInputs() {
        return new Object[][]{
            {"Empty string",    empty()},
            {"Special chars",   specialChars()},
            {"Unicode/CJK",     unicode()},
            {"SQL injection",   sqlInjection()},
            {"XSS vector",      xssVector()},
            {"HTML injection",  htmlInjection()},
            {"Long string",     maxLength(5000)},
            {"Whitespace only", whitespaceOnly()},
            {"Numeric string",  numericString()},
            {"Negative number", negativeNumber()},
            {"Emoji",           emoji()},
        };
    }

    /**
     * Boundary values for text fields: [description, value]
     */
    @DataProvider(name = "boundaryInputs")
    public static Object[][] boundaryInputs() {
        return new Object[][]{
            {"Empty",         ""},
            {"Single char",   "a"},
            {"255 chars",     maxLength(255)},
            {"256 chars",     maxLength(256)},
            {"1000 chars",    maxLength(1000)},
            {"10000 chars",   maxLength(10000)},
        };
    }

    /**
     * Domain-specific asset creation data: [name, serialNumber, assetClass, manufacturer, voltage]
     */
    @DataProvider(name = "assetCreateData")
    public static Object[][] assetCreateData() {
        Object[][] data = new Object[5][5];
        for (int i = 0; i < 5; i++) {
            data[i] = new Object[]{assetName(), serialNumber(), assetClass(), manufacturer(), voltage()};
        }
        return data;
    }

    /**
     * Email variations: [description, email, isValid]
     */
    @DataProvider(name = "emailVariations")
    public static Object[][] emailVariations() {
        return new Object[][]{
            {"Valid email",           email(),                    true},
            {"Minimal valid",         "a@b.co",                  true},
            {"No @ sign",             "invalidemail.com",         false},
            {"No domain",             "user@",                    false},
            {"No TLD",                "user@domain",              false},
            {"Double @",              "user@@domain.com",         false},
            {"Spaces",                "user @domain.com",         false},
            {"Special in local",      "user+tag@domain.com",      true},
            {"XSS in email",          "<script>@test.com",        false},
            {"SQL in email",          "admin'--@test.com",        false},
        };
    }

    /**
     * Generate N rows of context-aware data for a specific field label.
     */
    public static Object[][] dataForField(String fieldLabel, int rowCount) {
        Object[][] data = new Object[rowCount][1];
        for (int i = 0; i < rowCount; i++) {
            data[i] = new Object[]{smartValue(fieldLabel, "text")};
        }
        return data;
    }
}
