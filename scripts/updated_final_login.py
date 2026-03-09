from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import NoSuchElementException, ElementClickInterceptedException, TimeoutException
from selenium.webdriver.common.action_chains import ActionChains
import time

def login_to_acme():
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
        
        # Wait for email field to be present (now we know the exact ID)
        print("Waiting for email field...")
        wait = WebDriverWait(driver, 20)
        email_field = wait.until(
            EC.presence_of_element_located((By.ID, "email"))
        )
        print("Email field found. Filling in email...")
        email_field.send_keys("rahul+acme@egalvanic.com")
        
        # Find and fill password field (now we know the exact ID)
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
        
        # Try to find and interact with the site dropdown using the new method
        print("Looking for site dropdown...")
        try:
            select_site(driver, wait)
        except Exception as e:
            print(f"Dropdown not found or error interacting with it: {e}")
        
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

def select_site(driver, wait):
    try:
        # open
        site_dropdown = wait.until(EC.element_to_be_clickable((By.XPATH, "//div[contains(@class,'MuiAutocomplete-root')]")))
        # Use ActionChains for a more robust click
        actions = ActionChains(driver)
        actions.move_to_element(site_dropdown).click().perform()
        
        wait.until(EC.visibility_of_element_located((By.XPATH, "//ul[@role='listbox']")))
        
        # Give time for all options to load
        time.sleep(2)
        
        # Try to find and click the "Test Site" option
        try:
            # Wait for the option to be clickable and click it
            option = wait.until(EC.element_to_be_clickable((By.XPATH, "//li[normalize-space()='Test Site']")))
            # Use ActionChains for a more robust click on the option
            actions = ActionChains(driver)
            actions.move_to_element(option).click().perform()
            print("✔ Test Site Selected Successfully")
            return True
        except TimeoutException:
            # If the specific option is not found, try a more general approach
            print("Trying alternative approach to find 'Test Site' option...")
            try:
                # Look for any li element containing "Test Site"
                options = driver.find_elements(By.XPATH, "//li[contains(text(), 'Test Site')]")
                if options:
                    # Click the first one found using ActionChains
                    actions = ActionChains(driver)
                    actions.move_to_element(options[0]).click().perform()
                    print("✔ Test Site Selected Successfully (alternative method)")
                    return True
                else:
                    print("❌ Could not find 'Test Site' option")
                    return False
            except Exception as alt_error:
                print(f"❌ Alternative method failed: {alt_error}")
                return False
                
    except Exception as e:
        print(f"Error in select_site: {e}")
        return False

if __name__ == "__main__":
    login_to_acme()