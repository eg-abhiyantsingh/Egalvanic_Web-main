from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import time

def simple_login():
    # Setup Chrome driver with explicit path
    options = Options()
    options.add_argument("--start-maximized")
    
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
        print("Waiting for page to load...")
        time.sleep(5)
        
        # Find and fill email field
        print("Finding email field...")
        email_field = driver.find_element(By.ID, "email")
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
        
        # Wait for login to complete
        print("Login submitted. Waiting for 10 seconds...")
        time.sleep(10)
        
        print("Process completed.")
        
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        # Don't close the browser so we can see the result
        print("Browser will remain open. Please close manually when done.")

if __name__ == "__main__":
    simple_login()