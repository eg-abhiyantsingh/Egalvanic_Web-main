from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import time

def robust_login():
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
    except Exception as e:
        print(f"Error initializing Chrome driver: {e}")
        return
    
    try:
        # Navigate to the login page
        print("Navigating to https://acme.egalvanic.ai")
        driver.get("https://acme.egalvanic.ai")
        
        # Wait for the page to load and React to render
        print("Waiting for page to load...")
        time.sleep(10)
        
        # Wait for email field to be present
        print("Waiting for email field...")
        wait = WebDriverWait(driver, 30)
        email_field = wait.until(
            EC.presence_of_element_located((By.XPATH, "//input[@type='email'] | //input[@id='email'] | //input[contains(@placeholder, 'Email')]"))
        )
        print("Email field found. Filling in email...")
        email_field.send_keys("rahul+acme@egalvanic.com")
        
        # Wait for password field to be present
        print("Waiting for password field...")
        password_field = wait.until(
            EC.presence_of_element_located((By.XPATH, "//input[@type='password'] | //input[@id='password'] | //input[contains(@placeholder, 'Password')]"))
        )
        print("Password field found. Filling in password...")
        password_field.send_keys("RP@egalvanic123")
        
        # Wait for login button to be clickable
        print("Waiting for login button...")
        login_button = wait.until(
            EC.element_to_be_clickable((By.XPATH, "//button[@type='submit'] | //button[contains(text(), 'Login')] | //button[contains(text(), 'Sign')]"))
        )
        print("Login button found. Clicking...")
        login_button.click()
        
        # Wait for login to complete and page to load
        print("Login submitted. Waiting for page to load...")
        time.sleep(15)
        
        # Try to find and interact with the site dropdown
        print("Looking for site dropdown...")
        try:
            dropdown = wait.until(
                EC.element_to_be_clickable((By.XPATH, "//select[contains(@class, 'site') or contains(@id, 'site') or contains(@name, 'site')] | //div[contains(@class, 'dropdown') and contains(., 'site')]"))
            )
            print("Dropdown found.")
            
            # Check if it's a select element or a div-based dropdown
            if dropdown.tag_name == "select":
                from selenium.webdriver.support.ui import Select
                select = Select(dropdown)
                select.select_by_visible_text("test site")
                print("Selected 'test site' from dropdown.")
            else:
                # If it's a div-based dropdown
                dropdown.click()
                print("Clicked dropdown. Waiting for options...")
                # Wait for options to appear and click on "test site"
                test_site_option = wait.until(
                    EC.element_to_be_clickable((By.XPATH, "//option[contains(text(), 'test site')] | //li[contains(text(), 'test site')] | //div[contains(text(), 'test site')]"))
                )
                test_site_option.click()
                print("Selected 'test site' from dropdown options.")
        except Exception as e:
            print(f"Dropdown not found or error interacting with it: {e}")
        
        print("Process completed. Browser will remain open for 30 seconds so you can see the result.")
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
    robust_login()