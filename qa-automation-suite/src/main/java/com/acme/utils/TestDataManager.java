package com.acme.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TestDataManager {
    
    /**
     * Read test data from JSON file
     * @param filePath Path to the JSON file
     * @return JSONObject containing the test data
     */
    public static JSONObject readJsonTestData(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            return new JSONObject(content);
        } catch (IOException e) {
            LoggerUtil.logError("Error reading JSON test data file: " + filePath, e);
            return null;
        }
    }
    
    /**
     * Read test data from JSON file and convert to Object[][]
     * @param filePath Path to the JSON file
     * @param dataArrayKey Key for the array of test data objects
     * @return Object[][] containing test data
     */
    public static Object[][] readJsonTestDataAsDataProvider(String filePath, String dataArrayKey) {
        try {
            JSONObject jsonData = readJsonTestData(filePath);
            if (jsonData != null && jsonData.has(dataArrayKey)) {
                JSONArray dataArray = jsonData.getJSONArray(dataArrayKey);
                Object[][] data = new Object[dataArray.length()][1];
                for (int i = 0; i < dataArray.length(); i++) {
                    data[i][0] = dataArray.getJSONObject(i);
                }
                return data;
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error converting JSON test data to DataProvider format", e);
        }
        return new Object[0][0];
    }
    
    /**
     * Read test data from properties file
     * @param filePath Path to the properties file
     * @return Properties object containing the test data
     */
    public static Properties readPropertiesTestData(String filePath) {
        Properties props = new Properties();
        try {
            props.load(Files.newInputStream(Paths.get(filePath)));
        } catch (IOException e) {
            LoggerUtil.logError("Error reading properties test data file: " + filePath, e);
        }
        return props;
    }
    
    /**
     * Get a specific value from JSON test data
     * @param jsonData JSONObject containing test data
     * @param keyPath Dot-separated path to the desired value (e.g., "user.credentials.email")
     * @return The value at the specified path, or null if not found
     */
    public static Object getJsonValue(JSONObject jsonData, String keyPath) {
        try {
            String[] keys = keyPath.split("\\.");
            Object current = jsonData;
            
            for (String key : keys) {
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).get(key);
                } else {
                    return null;
                }
            }
            
            return current;
        } catch (Exception e) {
            LoggerUtil.logError("Error getting JSON value for key path: " + keyPath, e);
            return null;
        }
    }
    
    /**
     * Generate random test data
     * @param dataType Type of data to generate (email, phone, name, etc.)
     * @return Generated random data
     */
    public static String generateRandomData(String dataType) {
        Random random = new Random();
        switch (dataType.toLowerCase()) {
            case "email":
                return "testuser" + random.nextInt(10000) + "@example.com";
            case "phone":
                return "+1" + (random.nextInt(900000000) + 100000000);
            case "name":
                String[] firstNames = {"John", "Jane", "Alice", "Bob", "Charlie", "Diana"};
                String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"};
                return firstNames[random.nextInt(firstNames.length)] + " " + 
                       lastNames[random.nextInt(lastNames.length)];
            case "username":
                return "user" + random.nextInt(10000);
            case "password":
                return "Pass" + random.nextInt(10000) + "!";
            default:
                return "random" + random.nextInt(10000);
        }
    }
    
    /**
     * Mask sensitive data for logging
     * @param data The data to mask
     * @param maskChar Character to use for masking
     * @return Masked data
     */
    public static String maskSensitiveData(String data, char maskChar) {
        if (data == null || data.length() <= 4) {
            return "***";
        }
        
        int maskLength = Math.max(1, data.length() - 4);
        StringBuilder masked = new StringBuilder();
        
        for (int i = 0; i < maskLength; i++) {
            masked.append(maskChar);
        }
        
        masked.append(data.substring(maskLength));
        return masked.toString();
    }
    
    /**
     * Convert Object to Map for easier manipulation
     * @param obj Object to convert
     * @return Map representation of the object
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj instanceof JSONObject) {
            return jsonObjectToMap((JSONObject) obj);
        } else if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        } else {
            LoggerUtil.logWarning("Unsupported object type for conversion to Map: " + obj.getClass().getName());
            return new HashMap<>();
        }
    }
    
    /**
     * Convert JSONObject to Map
     * @param jsonObject JSONObject to convert
     * @return Map representation of the JSONObject
     */
    private static Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            
            if (value instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonArrayToList((JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        
        return map;
    }
    
    /**
     * Convert JSONArray to List
     * @param jsonArray JSONArray to convert
     * @return List representation of the JSONArray
     */
    private static List<Object> jsonArrayToList(JSONArray jsonArray) {
        List<Object> list = new ArrayList<>();
        
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            
            if (value instanceof JSONObject) {
                list.add(jsonObjectToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(jsonArrayToList((JSONArray) value));
            } else {
                list.add(value);
            }
        }
        
        return list;
    }
}