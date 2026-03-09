from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import time

def get_page_source():
    # Setup Chrome driver with explicit path
    options = Options()
    options.add_argument("--headless")  # Run in headless mode
    
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
        
        # Get page source
        print("Getting page source...")
        page_source = driver.page_source
        
        # Save to file
        with open("page_source.html", "w") as f:
            f.write(page_source)
        
        print("Page source saved to page_source.html")
        print(f"Page title: {driver.title}")
        
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        # Close the browser
        driver.quit()

if __name__ == "__main__":
    get_page_source()