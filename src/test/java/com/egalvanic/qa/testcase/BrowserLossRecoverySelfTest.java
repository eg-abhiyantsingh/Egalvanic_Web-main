package com.egalvanic.qa.testcase;

import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Self-test for {@code BaseTest.replaceBrowserIfLost()} — NOT part of any regression suite.
 *
 * <p>Test 1 kills the real Chrome behind the shared driver, the way a crash does. Test 2 must
 * then get a new, signed-in browser through the SAME driver object, instead of failing in 0 s
 * with "invalid session id" like every later test did before the recovery existed.
 *
 * <pre>
 * mvn test -DsuiteXmlFile=&lt;xml listing only this class&gt;
 * </pre>
 */
public class BrowserLossRecoverySelfTest extends BaseTest {

    @Test(priority = 1, description = "Simulate a browser crash by quitting the real Chrome")
    public void killTheBrowser() {
        Assert.assertTrue(driver instanceof SelfHealingDriver, "BaseTest must hand out the self-healing wrapper");
        ((SelfHealingDriver) driver).getWrappedDriver().quit();
        try {
            driver.getWindowHandle();
            Assert.fail("The browser should be gone after quit()");
        } catch (org.openqa.selenium.WebDriverException expected) {
            System.out.println("[SelfTest] Browser killed: " + expected.getClass().getSimpleName());
        }
    }

    @Test(priority = 2, description = "The next test gets a new signed-in browser through the same driver")
    public void nextTestHasAWorkingBrowser() {
        String url = driver.getCurrentUrl();
        System.out.println("[SelfTest] URL after recovery: " + url);
        Assert.assertTrue(url.startsWith(com.egalvanic.qa.constants.AppConstants.BASE_URL),
                "Recovered browser should be on the app, got " + url);
        Assert.assertFalse(driver.findElements(By.cssSelector("nav a[href]")).isEmpty(),
                "Recovered browser should be signed in and show the app navigation");
    }
}
