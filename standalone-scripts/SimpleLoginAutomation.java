import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SimpleLoginAutomation {

    public static void main(String[] args) {
        // Set the path to chromedriver (you'll need to download it separately)
        // System.setProperty("webdriver.chrome.driver", "/path/to/chromedriver");
        
        // Create WebDriver instance
        WebDriver driver = new ChromeDriver();
        
        try {
            // Maximize browser window
            driver.manage().window().maximize();
            
            // Navigate to the login page
            driver.get("https://acme.egalvanic.ai");
            
            // Wait for the page to load
            Thread.sleep(5000);
            
            // Try multiple strategies to find email field
            WebElement emailField = null;
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            // Try different XPath expressions for email field
            String[] emailXPaths = {
                "//input[@type='email']",
                "//input[@id='email']",
                "//input[@name='email']",
                "//input[contains(@placeholder, 'email')]",
                "//input[contains(@class, 'email')]",
                "//input[@type='text' and contains(@placeholder, 'email')]"
            };
            
            for (String xpath : emailXPaths) {
                try {
                    emailField = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.xpath(xpath))
                    );
                    System.out.println("Found email field with XPath: " + xpath);
                    break;
                } catch (Exception e) {
                    System.out.println("Email field not found with XPath: " + xpath);
                }
            }
            
            if (emailField == null) {
                throw new Exception("Could not locate email field with any of the attempted XPath expressions");
            }
            
            emailField.sendKeys("rahul+acme@egalvanic.com");
            
            // Try multiple strategies to find password field
            WebElement passwordField = null;
            
            // Try different XPath expressions for password field
            String[] passwordXPaths = {
                "//input[@type='password']",
                "//input[@id='password']",
                "//input[@name='password']",
                "//input[contains(@placeholder, 'password')]",
                "//input[contains(@class, 'password')]"
            };
            
            for (String xpath : passwordXPaths) {
                try {
                    passwordField = driver.findElement(By.xpath(xpath));
                    System.out.println("Found password field with XPath: " + xpath);
                    break;
                } catch (Exception e) {
                    System.out.println("Password field not found with XPath: " + xpath);
                }
            }
            
            if (passwordField == null) {
                throw new Exception("Could not locate password field with any of the attempted XPath expressions");
            }
            
            passwordField.sendKeys("RP@egalvanic123");
            
            // Try multiple strategies to find login button
            WebElement loginButton = null;
            
            // Try different XPath expressions for login button
            String[] loginButtonXPaths = {
                "//button[@type='submit']",
                "//button[contains(text(), 'Login')]",
                "//button[contains(text(), 'Sign')]",
                "//input[@type='submit']",
                "//button[@id='login']",
                "//button[@name='login']"
            };
            
            for (String xpath : loginButtonXPaths) {
                try {
                    loginButton = driver.findElement(By.xpath(xpath));
                    System.out.println("Found login button with XPath: " + xpath);
                    break;
                } catch (Exception e) {
                    System.out.println("Login button not found with XPath: " + xpath);
                }
            }
            
            if (loginButton == null) {
                throw new Exception("Could not locate login button with any of the attempted XPath expressions");
            }
            
            loginButton.click();
            
            // Wait for the page to load after login
            System.out.println("Login clicked, waiting for page to load...");
            Thread.sleep(10000); // Wait 10 seconds for page to load
            
            // Try multiple strategies to find the site dropdown
            WebElement dropdown = null;
            
            // Try different XPath expressions for dropdown
            String[] dropdownXPaths = {
                "//select[contains(@class, 'site-selector') or contains(@id, 'site') or contains(@name, 'site')]",
                "//div[contains(@class, 'dropdown') and contains(., 'site')]",
                "//select[contains(., 'site')]",
                "//div[contains(@class, 'site') and contains(@class, 'dropdown')]"
            };
            
            for (String xpath : dropdownXPaths) {
                try {
                    dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(By.xpath(xpath))
                    );
                    System.out.println("Found dropdown with XPath: " + xpath);
                    break;
                } catch (Exception e) {
                    System.out.println("Dropdown not found with XPath: " + xpath);
                }
            }
            
            if (dropdown != null) {
                // Check if it's a select element or a div-based dropdown
                if ("select".equals(dropdown.getTagName())) {
                    Select select = new Select(dropdown);
                    select.selectByVisibleText("test site");
                } else {
                    // If it's a div-based dropdown
                    dropdown.click();
                    // Wait for options to appear and click on "test site"
                    WebElement testSiteOption = wait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.xpath("//option[contains(text(), 'test site')] | //li[contains(text(), 'test site')] | //div[contains(text(), 'test site')]")
                        )
                    );
                    testSiteOption.click();
                }
                
                System.out.println("Successfully selected 'test site'");
            } else {
                System.out.println("Dropdown not found, but continuing...");
            }
            
            // Wait a bit to see the result
            Thread.sleep(5000);
            
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}