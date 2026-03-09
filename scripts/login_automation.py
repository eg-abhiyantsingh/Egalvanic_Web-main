from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
import time

def login_and_navigate():
    # Setup Chrome driver with WebDriver Manager
    options = webdriver.ChromeOptions()
    options.add_argument("--start-maximized")
    driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=options)
    
    try:
        # Navigate to the login page
        driver.get("https://acme.egalvanic.ai")
        
        # Wait for the email field to be present and enter email
        email_field = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.XPATH, "//input[@type='email']"))
        )
        email_field.send_keys("rahul+acme@egalvanic.com")
        
        # Find and enter password
        password_field = driver.find_element(By.XPATH, "//input[@type='password']")
        password_field.send_keys("RP@egalvanic123")
        
        # Click login button
        login_button = driver.find_element(By.XPATH, "//button[@type='submit']")
        login_button.click()
        
        # Wait for the page to load after login
        print("Login successful, waiting for page to load...")
        time.sleep(5)  # Wait for page to load
        
        # Look for the dropdown and select "test site"
        # We'll wait for the dropdown to appear
        dropdown = WebDriverWait(driver, 20).until(
            EC.element_to_be_clickable((By.XPATH, "//select[contains(@class, 'site-selector') or contains(@id, 'site') or contains(@name, 'site')] | //div[contains(@class, 'dropdown') and contains(., 'site')]"))
        )
        
        # If it's a select element
        if dropdown.tag_name == "select":
            from selenium.webdriver.support.ui import Select
            select = Select(dropdown)
            select.select_by_visible_text("test site")
        else:
            # If it's a div-based dropdown
            dropdown.click()
            # Wait for options to appear and click on "test site"
            test_site_option = WebDriverWait(driver, 10).until(
                EC.element_to_be_clickable((By.XPATH, "//option[contains(text(), 'test site')] | //li[contains(text(), 'test site')] | //div[contains(text(), 'test site')]"))
            )
            test_site_option.click()
        
        print("Successfully selected 'test site'")
        time.sleep(3)  # Wait to see the result
        
    except Exception as e:
        print(f"An error occurred: {str(e)}")
    finally:
        # Close the browser
        driver.quit()

if __name__ == "__main__":
    login_and_navigate()