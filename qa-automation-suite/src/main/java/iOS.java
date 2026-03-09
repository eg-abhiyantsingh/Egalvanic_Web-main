import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class iOS {  // Changed class name to match file name

    private IOSDriver driver;

    @BeforeClass
    public void setUp() throws MalformedURLException {

        DesiredCapabilities caps = new DesiredCapabilities();

        // ===============================
        // Device Information
        // ===============================
        caps.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
        caps.setCapability(MobileCapabilityType.DEVICE_NAME, "iPad");
        caps.setCapability(MobileCapabilityType.UDID, "00008103-001E71EA147B001E");
        caps.setCapability("platformVersion", "17.0"); // update if different

        // ===============================
        // App Information
        // ===============================
        caps.setCapability(IOSMobileCapabilityType.BUNDLE_ID, "com.egalvanic.zplatform-QA");

        // ===============================
        // Automation
        // ===============================
        caps.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");

        // ===============================
        // Xcode / WDA Signing (MANDATORY)
        // ===============================
        caps.setCapability("xcodeOrgId", "YOUR_TEAM_ID_HERE"); // <-- add from Xcode
        caps.setCapability("xcodeSigningId", "iPhone Developer");
        caps.setCapability("updatedWDABundleId", "com.abhiyant.wdarunner");

        // ===============================
        // WebDriverAgent Settings
        // ===============================
        caps.setCapability("useNewWDA", true);
        caps.setCapability("wdaLaunchTimeout", 300000);
        caps.setCapability("wdaConnectionTimeout", 300000);

        // ===============================
        // App State
        // ===============================
        caps.setCapability(MobileCapabilityType.NO_RESET, false);

        // ===============================
        // Start Driver
        // ===============================
        driver = new IOSDriver(
                new URL("http://127.0.0.1:4723/wd/hub"),
                caps
        );

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @Test
    public void launchAppTest() {
        System.out.println("Z Platform QA app launched successfully on iPad.");
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}