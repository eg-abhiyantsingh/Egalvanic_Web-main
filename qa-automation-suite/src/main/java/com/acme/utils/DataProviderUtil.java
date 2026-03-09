package com.acme.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataProviderUtil {
    
    /**
     * Read test data from CSV file
     * @param filePath Path to the CSV file
     * @return 2D array of test data
     */
    public static Object[][] readTestDataFromCSV(String filePath) {
        List<Object[]> testData = new ArrayList<>();
        
        try (FileReader fileReader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord record : csvParser) {
                List<String> rowData = new ArrayList<>();
                record.forEach(rowData::add);
                testData.add(rowData.toArray());
            }
            
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return testData.toArray(new Object[0][]);
    }
    
    /**
     * Read login test data from CSV
     * @param filePath Path to the CSV file
     * @return 2D array of login test data (email, password, expected result)
     */
    public static Object[][] readLoginTestData(String filePath) {
        List<Object[]> loginData = new ArrayList<>();
        
        try (FileReader fileReader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord record : csvParser) {
                String email = record.get("email");
                String password = record.get("password");
                String expectedResult = record.get("expectedResult");
                
                loginData.add(new Object[]{email, password, expectedResult});
            }
            
        } catch (IOException e) {
            System.err.println("Error reading login test data CSV file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return loginData.toArray(new Object[0][]);
    }
}