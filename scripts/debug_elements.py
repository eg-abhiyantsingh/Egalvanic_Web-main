from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import time

def debug_elements():
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
        
        # Debug: List all elements on the page
        print("\n=== DEBUG: Listing all elements on the page ===")
        
        # List all div elements
        divs = driver.find_elements(By.TAG_NAME, "div")
        print(f"Found {len(divs)} div elements:")
        for i, div in enumerate(divs[:20]):  # Only show first 20 to avoid too much output
            class_attr = div.get_attribute("class") or ""
            id_attr = div.get_attribute("id") or ""
            if class_attr or id_attr:
                print(f"  {i+1}. div - class: '{class_attr}' id: '{id_attr}'")
        
        # List all elements with "site" in their text or attributes
        print("\n=== Looking for elements related to 'site' ===")
        site_elements = driver.find_elements(By.XPATH, "//*[contains(text(), 'site') or contains(@class, 'site') or contains(@id, 'site')]")
        print(f"Found {len(site_elements)} elements related to 'site':")
        for i, elem in enumerate(site_elements):
            tag_name = elem.tag_name
            text = elem.text or ""
            class_attr = elem.get_attribute("class") or ""
            id_attr = elem.get_attribute("id") or ""
            print(f"  {i+1}. {tag_name} - text: '{text}' class: '{class_attr}' id: '{id_attr}'")
            
        # Specifically look for MUI elements
        print("\n=== Looking for MUI elements ===")
        mui_elements = driver.find_elements(By.XPATH, "//*[contains(@class, 'Mui')]")
        print(f"Found {len(mui_elements)} MUI elements:")
        for i, elem in enumerate(mui_elements[:15]):  # Only show first 15 to avoid too much output
            tag_name = elem.tag_name
            class_attr = elem.get_attribute("class") or ""
            id_attr = elem.get_attribute("id") or ""
            text = elem.text or ""
            print(f"  {i+1}. {tag_name} - class: '{class_attr}' id: '{id_attr}' text: '{text}'")
            
        # Look for autocomplete specifically
        print("\n=== Looking for Autocomplete elements ===")
        autocomplete_elements = driver.find_elements(By.XPATH, "//*[contains(@class, 'Autocomplete') or contains(@class, 'autocomplete')]")
        print(f"Found {len(autocomplete_elements)} Autocomplete elements:")
        for i, elem in enumerate(autocomplete_elements):
            tag_name = elem.tag_name
            class_attr = elem.get_attribute("class") or ""
            id_attr = elem.get_attribute("id") or ""
            text = elem.text or ""
            print(f"  {i+1}. {tag_name} - class: '{class_attr}' id: '{id_attr}' text: '{text}'")
        
        print("\nDebug completed.")
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
    debug_elements()