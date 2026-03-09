import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class IOSLaunchTest {

    public static void main(String[] args) {

        IOSDriver driver = null;

        try {
            DesiredCapabilities caps = new DesiredCapabilities();

            // Device details
            caps.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
            caps.setCapability(MobileCapabilityType.DEVICE_NAME, "iPad");
            caps.setCapability(MobileCapabilityType.UDID, "00008103-001E71EA147B001E");
            caps.setCapability("platformVersion", "17.0"); // change if needed

            // App to open
            caps.setCapability(IOSMobileCapabilityType.BUNDLE_ID, "com.egalvanic.zplatform-QA");

            // Automation engine
            caps.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");

            // Xcode / WDA signing
            caps.setCapability("xcodeOrgId", "YOUR_TEAM_ID_HERE");
            caps.setCapability("xcodeSigningId", "iPhone Developer");
            caps.setCapability("updatedWDABundleId", "com.abhiyant.wdarunner");

            // Additional capabilities for stability
            caps.setCapability("useNewWDA", true);
            caps.setCapability("wdaLaunchTimeout", 300000);
            caps.setCapability("wdaConnectionTimeout", 300000);
            caps.setCapability(MobileCapabilityType.NO_RESET, false);

            // Launch app
            driver = new IOSDriver(
                    new URL("http://127.0.0.1:4723/wd/hub"),
                    caps
            );

            // Set implicit wait
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            System.out.println("App launched successfully.");

            // Keep app open for few seconds
            Thread.sleep(5000);

        } catch (MalformedURLException e) {
            System.err.println("Invalid URL provided for Appium server");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error during iOS app launch");
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}