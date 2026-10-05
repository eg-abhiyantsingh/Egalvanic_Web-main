package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.ai.SelfHealingDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Remote-controlled, signed-in browser for manual ticket testing. BaseTest signs the seat in (headed Chrome), then this
 * test polls {@code -Dqa.daemon.dir}/in for command files and writes each result to out/&lt;name&gt;.txt:
 * <ul>
 *   <li>{@code <seq>.js}    — body of an async JS function run in the page; its return value is JSON-stringified</li>
 *   <li>{@code <seq>.get}   — a path or URL to open (then waits for the app and dismisses the 2FA prompt)</li>
 *   <li>{@code <seq>.shot}  — screenshot of the viewport to shots/&lt;file content&gt;.png</li>
 *   <li>{@code <seq>.click} — CSS selector (first line), optional 1-based index (second line); real WebDriver click</li>
 *   <li>{@code <seq>.keys}  — CSS selector (first line), text (rest); real key presses into that element</li>
 *   <li>{@code <seq>.alert} — {@code accept} or {@code dismiss} the open native confirm()/alert(); returns its text</li>
 *   <li>{@code <seq>.stop}  — ends the session</li>
 * </ul>
 * Nothing is done that a command doesn't ask for. Ends after {@code -Dqa.daemon.minutes} (default 240).
 */
public class QaDriverDaemon extends BaseTest {

    private WebDriver raw() { return driver instanceof SelfHealingDriver ? ((SelfHealingDriver) driver).getWrappedDriver() : driver; }

    private String runJs(String body) {
        String wrapper = "var done=arguments[arguments.length-1];"
                + "(async function(){" + body + "\n})().then(function(r){var s;try{s=JSON.stringify(r);}catch(e){s=String(r);}done('OK '+(s===undefined?'undefined':s));})"
                + ".catch(function(e){done('JSERR '+(e&&e.stack?String(e.stack).slice(0,800):String(e)));});";
        Object r = ((JavascriptExecutor) raw()).executeAsyncScript(wrapper);
        return String.valueOf(r);
    }

    private void waitApp() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            try { loginPage.dismissMfaPromptIfShowing(); } catch (Exception ignored) { }
            Object ok = ((JavascriptExecutor) raw()).executeScript("var t=(document.body&&document.body.innerText)||'';return !t.startsWith('Loading')&&document.querySelectorAll('a[href],button').length>5;");
            if (Boolean.TRUE.equals(ok)) break;
            Thread.sleep(700);
        }
        Thread.sleep(1500);
    }

    @Test(description = "Remote-controlled signed-in browser (manual ticket testing)")
    public void serve() throws Exception {
        ExtentReportManager.createTest("QA", "Driver daemon", "Remote session " + AppConstants.VALID_EMAIL.replaceAll("^[^+]*\\+?([^@]*)@.*$", "$1"));
        Path dir = Paths.get(System.getProperty("qa.daemon.dir", "/tmp/qa-daemon"));
        Path in = dir.resolve("in"), out = dir.resolve("out"), done = dir.resolve("done"), shots = dir.resolve("shots");
        for (Path p : new Path[]{in, out, done, shots}) Files.createDirectories(p);
        raw().manage().timeouts().scriptTimeout(Duration.ofSeconds(Integer.getInteger("qa.daemon.scriptSeconds", 240)));
        long until = System.currentTimeMillis() + Integer.getInteger("qa.daemon.minutes", 240) * 60_000L;
        Files.write(dir.resolve("READY"), ("ready " + raw().getCurrentUrl()).getBytes(StandardCharsets.UTF_8));
        System.out.println("[daemon] ready at " + raw().getCurrentUrl());
        while (System.currentTimeMillis() < until) {
            File[] cmds = in.toFile().listFiles(f -> f.isFile() && !f.getName().startsWith(".") && f.getName().contains("."));
            if (cmds == null || cmds.length == 0) { Thread.sleep(250); continue; }
            Arrays.sort(cmds, Comparator.comparing(File::getName));
            for (File f : cmds) {
                String name = f.getName();
                String base = name.substring(0, name.lastIndexOf('.'));
                String type = name.substring(name.lastIndexOf('.') + 1);
                String body = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                String result;
                long t0 = System.currentTimeMillis();
                try {
                    switch (type) {
                        case "js":
                            result = runJs(body);
                            break;
                        case "get": {
                            String u = body.trim();
                            raw().get(u.startsWith("http") ? u : AppConstants.BASE_URL + u);
                            waitApp();
                            result = "OK " + raw().getCurrentUrl();
                            break;
                        }
                        case "shot": {
                            Path p = shots.resolve(body.trim().replaceAll("[^A-Za-z0-9._-]", "_") + ".png");
                            Files.write(p, ((TakesScreenshot) raw()).getScreenshotAs(OutputType.BYTES));
                            result = "OK " + p;
                            break;
                        }
                        case "click": {
                            String[] lines = body.split("\n");
                            int idx = lines.length > 1 && !lines[1].trim().isEmpty() ? Integer.parseInt(lines[1].trim()) : 1;
                            By by = lines[0].startsWith("xpath:") ? By.xpath(lines[0].substring(6)) : By.cssSelector(lines[0].trim());
                            java.util.List<WebElement> els = raw().findElements(by);
                            if (els.size() < idx) { result = "ERR no element #" + idx + " for " + lines[0] + " (found " + els.size() + ")"; break; }
                            WebElement el = els.get(idx - 1);
                            ((JavascriptExecutor) raw()).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
                            Thread.sleep(200);
                            try { el.click(); } catch (Exception e) { new Actions(raw()).moveToElement(el).click().perform(); }
                            result = "OK clicked " + lines[0] + " #" + idx;
                            break;
                        }
                        case "keys": {
                            int nl = body.indexOf('\n');
                            String css = nl < 0 ? body.trim() : body.substring(0, nl).trim();
                            String text = nl < 0 ? "" : body.substring(nl + 1);
                            By by = css.startsWith("xpath:") ? By.xpath(css.substring(6)) : By.cssSelector(css);
                            WebElement el = raw().findElement(by);
                            el.sendKeys(text.replace("\\n", "\n"));
                            result = "OK typed " + text.length() + " chars into " + css;
                            break;
                        }
                        case "alert": {
                            // a native confirm()/alert(): read its text, then accept or dismiss it
                            org.openqa.selenium.Alert al = null;
                            for (int i = 0; i < 20 && al == null; i++) {
                                try { al = raw().switchTo().alert(); } catch (org.openqa.selenium.NoAlertPresentException e) { Thread.sleep(250); }
                            }
                            if (al == null) { result = "ERR no native dialog open"; break; }
                            String text = al.getText();
                            if ("dismiss".equals(body.trim())) al.dismiss(); else al.accept();
                            result = "OK " + body.trim() + "ed: " + text;
                            break;
                        }
                        case "stop":
                            Files.write(out.resolve(base + ".txt"), "OK stopping".getBytes(StandardCharsets.UTF_8));
                            Files.move(f.toPath(), done.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("[daemon] stop");
                            return;
                        default:
                            result = "ERR unknown type " + type;
                    }
                } catch (Exception e) {
                    result = "ERR " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()).split("\n")[0];
                }
                // after a click a native confirm() may be open: any further WebDriver call would dismiss it, so skip the URL read
                result = result + "\n[" + (System.currentTimeMillis() - t0) + " ms · " + ("click".equals(type) ? "url not read" : safeUrl()) + "]";
                Files.write(out.resolve(base + ".txt"), result.getBytes(StandardCharsets.UTF_8));
                Files.move(f.toPath(), done.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        System.out.println("[daemon] timed out");
    }

    private String safeUrl() { try { return raw().getCurrentUrl(); } catch (Exception e) { return "?"; } }
}
