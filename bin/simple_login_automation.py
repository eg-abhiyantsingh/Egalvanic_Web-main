from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import time

def login_and_navigate():
    # Setup Chrome driver with explicit path
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
        
        # Wait for the page to load
        time.sleep(5)
        
        # Try multiple strategies to find email field
        email_field = None
        wait = WebDriverWait(driver, 15)
        
        # Try different XPath expressions for email field
        email_xpaths = [
            "//input[@type='email']",
            "//input[@id='email']",
            "//input[@name='email']",
            "//input[contains(@placeholder, 'email')]",
            "//input[contains(@class, 'email')]",
            "//input[@type='text' and contains(@placeholder, 'email')]"
        ]
        
        for xpath in email_xpaths:
            try:
                email_field = wait.until(
                    EC.presence_of_element_located((By.XPATH, xpath))
                )
                print(f"Found email field with XPath: {xpath}")
                break
            except Exception as e:
                print(f"Email field not found with XPath: {xpath}")
        
        if email_field is None:
            raise Exception("Could not locate email field with any of the attempted XPath expressions")
        
        email_field.send_keys("rahul+acme@egalvanic.com")
        
        # Try multiple strategies to find password field
        password_field = None
        
        # Try different XPath expressions for password field
        password_xpaths = [
            "//input[@type='password']",
            "//input[@id='password']",
            "//input[@name='password']",
            "//input[contains(@placeholder, 'password')]",
            "//input[contains(@class, 'password')]"
        ]
        
        for xpath in password_xpaths:
            try:
                password_field = driver.find_element(By.XPATH, xpath)
                print(f"Found password field with XPath: {xpath}")
                break
            except Exception as e:
                print(f"Password field not found with XPath: {xpath}")
        
        if password_field is None:
            raise Exception("Could not locate password field with any of the attempted XPath expressions")
        
        password_field.send_keys("RP@egalvanic123")
        
        # Try multiple strategies to find login button
        login_button = None
        
        # Try different XPath expressions for login button
        login_button_xpaths = [
            "//button[@type='submit']",
            "//button[contains(text(), 'Login')]",
            "//button[contains(text(), 'Sign')]",
            "//input[@type='submit']",
            "//button[@id='login']",
            "//button[@name='login']"
        ]
        
        for xpath in login_button_xpaths:
            try:
                login_button = driver.find_element(By.XPATH, xpath)
                print(f"Found login button with XPath: {xpath}")
                break
            except Exception as e:
                print(f"Login button not found with XPath: {xpath}")
        
        if login_button is None:
            raise Exception("Could not locate login button with any of the attempted XPath expressions")
        
        login_button.click()
        
        # Wait for the page to load after login
        print("Login clicked, waiting for page to load...")
        time.sleep(10)  # Wait 10 seconds for page to load
        
        # Try multiple strategies to find the site dropdown
        dropdown = None
        
        # Try different XPath expressions for dropdown
        dropdown_xpaths = [
            "//select[contains(@class, 'site-selector') or contains(@id, 'site') or contains(@name, 'site')]",
            "//div[contains(@class, 'dropdown') and contains(., 'site')]",
            "//select[contains(., 'site')]",
            "//div[contains(@class, 'site') and contains(@class, 'dropdown')]"
        ]
        
        for xpath in dropdown_xpaths:
            try:
                dropdown = wait.until(
                    EC.element_to_be_clickable((By.XPATH, xpath))
                )
                print(f"Found dropdown with XPath: {xpath}")
                break
            except Exception as e:
                print(f"Dropdown not found with XPath: {xpath}")
        
        if dropdown is not None:
            # Check if it's a select element or a div-based dropdown
            if dropdown.tag_name == "select":
                from selenium.webdriver.support.ui import Select
                select = Select(dropdown)
                select.select_by_visible_text("test site")
            else:
                # If it's a div-based dropdown
                dropdown.click()
                # Wait for options to appear and click on "test site"
                test_site_option = wait.until(
                    EC.element_to_be_clickable((By.XPATH, "//option[contains(text(), 'test site')] | //li[contains(text(), 'test site')] | //div[contains(text(), 'test site')]"))
                )
                test_site_option.click()
            
            print("Successfully selected 'test site'")
        else:
            print("Dropdown not found, but continuing...")
        
        # Wait a bit to see the result
        time.sleep(5)
        
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        # Close the browser
        driver.quit()

if __name__ == "__main__":
    login_and_navigate()