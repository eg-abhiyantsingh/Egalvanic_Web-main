from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
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
        
        # Try to find and interact with the site dropdown
        print("Looking for site dropdown...")
        try:
            # Wait for dropdown to appear
            dropdown = wait.until(
                EC.element_to_be_clickable((By.XPATH, "//select[contains(@id, 'site') or contains(@name, 'site')] | //div[contains(@class, 'dropdown') and contains(., 'site')]"))
            )
            print("Dropdown found.")
            
            # Check if it's a select element or a div-based dropdown
            if dropdown.tag_name == "select":
                from selenium.webdriver.support.ui import Select
                select = Select(dropdown)
                # Try to select "test site"
                try:
                    select.select_by_visible_text("test site")
                    print("Selected 'test site' from dropdown.")
                except:
                    # If that doesn't work, try to print all available options
                    options = select.options
                    print("Available dropdown options:")
                    for option in options:
                        print(f"  - {option.text}")
            else:
                # If it's a div-based dropdown
                dropdown.click()
                print("Clicked dropdown. Waiting for options...")
                # Wait for options to appear and click on "test site"
                test_site_option = wait.until(
                    EC.element_to_be_clickable((By.XPATH, "//div[contains(text(), 'test site')] | //li[contains(text(), 'test site')]"))
                )
                test_site_option.click()
                print("Selected 'test site' from dropdown options.")
        except Exception as e:
            print(f"Dropdown not found or error interacting with it: {e}")
            # Let's try to see what elements are actually on the page
            print("Checking page elements...")
            elements = driver.find_elements(By.XPATH, "//*[contains(text(), 'site') or contains(@class, 'dropdown') or contains(@id, 'site')]")
            for element in elements:
                print(f"Found element: {element.tag_name} with text '{element.text}' and id '{element.get_attribute('id')}'")
        
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
    login_to_acme()