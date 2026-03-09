package com.acme.tests.utilities;

import com.acme.utils.TestDataManager;
import com.acme.utils.LoggerUtil;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class TestDataManagerTest {
    
    @Test
    public void testReadJsonTestData() {
        LoggerUtil.logInfo("Testing JSON test data reading");
        
        // Read test data from JSON file
        JSONObject testData = TestDataManager.readJsonTestData(
            "src/main/resources/testdata/sample_test_data.json");
        
        Assert.assertNotNull(testData, "Test data should not be null");
        Assert.assertTrue(testData.has("users"), "Test data should contain users");
        Assert.assertTrue(testData.has("testScenarios"), "Test data should contain test scenarios");
        
        LoggerUtil.logInfo("JSON test data reading test passed");
    }
    
    @Test
    public void testGetJsonValue() {
        LoggerUtil.logInfo("Testing JSON value extraction");
        
        // Read test data from JSON file
        JSONObject testData = TestDataManager.readJsonTestData(
            "src/main/resources/testdata/sample_test_data.json");
        
        // Extract specific values
        Object firstUserName = TestDataManager.getJsonValue(testData, "users.0.name");
        Assert.assertEquals(firstUserName, "John Doe", "First user name should match");
        
        Object firstUserEmail = TestDataManager.getJsonValue(testData, "users.0.email");
        Assert.assertEquals(firstUserEmail, "john.doe@example.com", "First user email should match");
        
        Object firstUserPassword = TestDataManager.getJsonValue(testData, "users.0.credentials.password");
        Assert.assertEquals(firstUserPassword, "secret123", "First user password should match");
        
        LoggerUtil.logInfo("JSON value extraction test passed");
    }
    
    @Test
    public void testGenerateRandomData() {
        LoggerUtil.logInfo("Testing random data generation");
        
        // Generate different types of random data
        String email = TestDataManager.generateRandomData("email");
        Assert.assertTrue(email.contains("@") && email.contains(".com"), "Generated email should be valid");
        
        String phone = TestDataManager.generateRandomData("phone");
        Assert.assertTrue(phone.startsWith("+1") && phone.length() > 10, "Generated phone should be valid");
        
        String name = TestDataManager.generateRandomData("name");
        Assert.assertTrue(name.contains(" "), "Generated name should contain space");
        
        String username = TestDataManager.generateRandomData("username");
        Assert.assertTrue(username.startsWith("user"), "Generated username should start with 'user'");
        
        String password = TestDataManager.generateRandomData("password");
        Assert.assertTrue(password.length() > 5 && password.contains("!"), "Generated password should be valid");
        
        LoggerUtil.logInfo("Random data generation test passed");
    }
    
    @Test
    public void testMaskSensitiveData() {
        LoggerUtil.logInfo("Testing sensitive data masking");
        
        String password = "mySecretPassword123";
        String maskedPassword = TestDataManager.maskSensitiveData(password, '*');
        Assert.assertEquals(maskedPassword, "***************123", "Password should be properly masked");
        
        String shortPassword = "pwd";
        String maskedShortPassword = TestDataManager.maskSensitiveData(shortPassword, '*');
        Assert.assertEquals(maskedShortPassword, "***", "Short password should be masked as ***");
        
        LoggerUtil.logInfo("Sensitive data masking test passed");
    }
    
    @Test
    public void testObjectToMap() {
        LoggerUtil.logInfo("Testing object to map conversion");
        
        // Read test data from JSON file
        JSONObject testData = TestDataManager.readJsonTestData(
            "src/main/resources/testdata/sample_test_data.json");
        
        // Convert to map
        Map<String, Object> testDataMap = TestDataManager.objectToMap(testData);
        
        Assert.assertNotNull(testDataMap, "Converted map should not be null");
        Assert.assertTrue(testDataMap.containsKey("users"), "Map should contain users key");
        Assert.assertTrue(testDataMap.containsKey("testScenarios"), "Map should contain testScenarios key");
        
        LoggerUtil.logInfo("Object to map conversion test passed");
    }
}