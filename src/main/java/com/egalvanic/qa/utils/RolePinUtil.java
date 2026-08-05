package com.egalvanic.qa.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Pins the header Role dropdown to a target role (V1.36 dual-console fix).
 *
 * Since the V1.36 role rename, a fresh login lands on EITHER the operational console
 * ("Super Admin") or the setup console (renamed "Admin" = old EG Admin) nondeterministically.
 * Tests anchored on operational UI (facility selector, Assets/Issues/WO sidebar) silently break
 * on the setup console. Standalone test classes (those not extending BaseTest, which has its own
 * ensureActiveRole) call {@link #pin(WebDriver, String)} right after login.
 *
 * Tolerant by design: never throws; returns false if the switcher is absent or the option is
 * never offered. Safe to call when already on the target role (no-op).
 */
public final class RolePinUtil {

    private static final String NAMES_JS =
            "['Admin','Project Manager','Account Manager','Super Admin','Electrical Engineer']";

    private RolePinUtil() { }

    /** Switch the header Role Autocomplete to {@code target}. Returns true when the role shows it. */
    public static boolean pin(WebDriver driver, String target) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String readRole =
                    "var names=" + NAMES_JS + ";"
                  + "var ins=document.querySelectorAll('input');"
                  + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0) return ins[i].value; }"
                  + "return null;";
            Object current = null;
            for (int i = 0; i < 10 && current == null; i++) {
                current = js.executeScript(readRole);
                if (current == null) sleep(500);
            }
            if (current == null) {
                System.out.println("[RolePin] Role switcher not found — leaving role as-is");
                return false;
            }
            if (target.equals(current)) return true;
            System.out.println("[RolePin] Active role '" + current + "' — switching to '" + target + "'");
            String openScript =
                    "var names=" + NAMES_JS + ";"
                  + "var input=null, ins=document.querySelectorAll('input');"
                  + "for (var i=0;i<ins.length;i++){ if(names.indexOf(ins[i].value)>=0){ input=ins[i]; break; } }"
                  + "if(!input) return false;"
                  + "input.scrollIntoView({block:'center'}); input.focus();"
                  + "var w=input.closest('.MuiAutocomplete-root');"
                  + "var b=w?w.querySelector('.MuiAutocomplete-popupIndicator'):null;"
                  + "if(b) b.click(); else input.click(); return true;";
            boolean clicked = false;
            for (int attempt = 1; attempt <= 3 && !clicked; attempt++) {
                js.executeScript(openScript);
                for (int i = 0; i < 10 && !clicked; i++) {
                    sleep(700);
                    Object done = js.executeScript(
                            "var t=arguments[0];var opts=document.querySelectorAll(\"li[role='option']\");"
                          + "for (var i=0;i<opts.length;i++){ if(opts[i].textContent.trim()===t){"
                          + "['pointerdown','mousedown','pointerup','mouseup','click'].forEach(function(ev){"
                          + "opts[i].dispatchEvent(new MouseEvent(ev,{bubbles:true,cancelable:true}));}); return true; } }"
                          + "return false;", target);
                    clicked = Boolean.TRUE.equals(done);
                }
            }
            if (!clicked) {
                System.out.println("[RolePin] Role option '" + target + "' never offered");
                return false;
            }
            for (int i = 0; i < 30; i++) {
                sleep(1000);
                try { if (target.equals(js.executeScript(readRole))) break; } catch (Exception ignored) { }
            }
            sleep(2000);
            System.out.println("[RolePin] Active role now: " + js.executeScript(readRole));
            return true;
        } catch (Exception e) {
            System.out.println("[RolePin] pin failed (non-fatal): " + e.getMessage());
            return false;
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
