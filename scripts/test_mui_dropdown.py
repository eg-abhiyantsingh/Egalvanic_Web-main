from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.action_chains import ActionChains
import time

def test_mui_dropdown():
    # Setup Chrome driver
    options = Options()
    options.add_argument("--start-maximized")
    options.add_argument("--disable-blink-features=AutomationControlled")
    options.add_experimental_option("excludeSwitches", ["enable-automation"])
    options.add_experimental_option('useAutomationExtension', False)
    
    # Specify the path to chromedriver
    chromedriver_path = "/Users/vishwa/.wdm/drivers/chromedriver/mac64/142.0.7444.175/chromedriver-mac-x64/chromedriver"
    
    try:
        service = Service(chromedriver_path)
        driver = webdriver.Chrome(service=service, options=options)
        print("Chrome browser opened successfully")
    except Exception as e:
        print(f"Error initializing Chrome driver: {e}")
        return
    
    try:
        # Navigate to the login page
        print("Navigating to https://acme.egalvanic.ai")
        driver.get("https://acme.egalvanic.ai")
        
        # Wait for the page to load
        print("Waiting for page to load...")
        time.sleep(5)
        
        # Wait for email field to be present
        print("Waiting for email field...")
        wait = WebDriverWait(driver, 20)
        email_field = wait.until(
            EC.presence_of_element_located((By.ID, "email"))
        )
        print("Email field found. Filling in email...")
        email_field.send_keys("rahul+acme@egalvanic.com")
        
        # Find and fill password field
        print("Finding password field...")
        password_field = driver.find_element(By.ID, "password")
        print("Password field found. Filling in password...")
        password_field.send_keys("RP@egalvanic123")
        
        # Find and click login button
        print("Finding login button...")
        login_button = driver.find_element(By.XPATH, "//button[@type='submit']")
        print("Login button found. Clicking...")
        login_button.click()
        
        # Wait for login to complete and page to load
        print("Login submitted. Waiting for page to load...")
        time.sleep(10)
        
        # Try to find and interact with the site dropdown using the MUI approach
        print("Looking for MUI Autocomplete dropdown...")
        try:
            # Find the MUI Autocomplete dropdown
            mui_dropdown = wait.until(
                EC.element_to_be_clickable((By.XPATH, "//div[contains(@class,'MuiAutocomplete-root')]"))
            )
            print("MUI dropdown found.")
            
            # Click the dropdown using ActionChains
            actions = ActionChains(driver)
            actions.move_to_element(mui_dropdown).click().perform()
            print("Clicked MUI dropdown.")
            
            # Wait for the listbox to appear
            listbox = wait.until(
                EC.visibility_of_element_located((By.XPATH, "//ul[@role='listbox']"))
            )
            print("Listbox appeared.")
            
            # Wait a moment for options to load
            time.sleep(2)
            
            # Try to find and click "Test Site" option
            try:
                test_site_option = wait.until(
                    EC.element_to_be_clickable((By.XPATH, "//li[normalize-space()='Test Site']"))
                )
                print("Test Site option found.")
                actions.move_to_element(test_site_option).click().perform()
                print("✔ Test Site selected successfully!")
            except Exception as e:
                print(f"Could not find or click 'Test Site' option: {e}")
                # Try to find any option containing "Test Site"
                try:
                    options = driver.find_elements(By.XPATH, "//li[contains(text(), 'Test Site')]")
                    if options:
                        actions.move_to_element(options[0]).click().perform()
                        print("✔ Test Site selected successfully (alternative method)!")
                    else:
                        print("❌ No options containing 'Test Site' found.")
                except Exception as alt_e:
                    print(f"Alternative method also failed: {alt_e}")
                
        except Exception as e:
            print(f"MUI dropdown not found or error interacting with it: {e}")
        
        print("Process completed successfully!")
        print("Browser will remain open for 30 seconds so you can see the result.")
        time.sleep(30)
        
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        # Close the browser
        print("Closing browser...")
        driver.quit()

if __name__ == "__main__":
    test_mui_dropdown()