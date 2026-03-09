from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import time

def simple_navigation():
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
        
        # Wait for 10 seconds to see the page
        print("Waiting for 10 seconds...")
        time.sleep(10)
        
        # Print page title
        print(f"Page title: {driver.title}")
        
        # Print current URL
        print(f"Current URL: {driver.current_url}")
        
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        # Close the browser
        driver.quit()
        print("Browser closed.")

if __name__ == "__main__":
    simple_navigation()