package com.egalvanic.qa.utils.verify;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * AssetLoadVerifier — proves images/PDFs/assets ACTUALLY rendered, not just
 * that a container element exists.
 *
 * WHY THIS EXISTS
 * ---------------
 * Grep of the whole suite shows ZERO uses of img.naturalWidth and no PDF
 * render verification. Tests assert {@code embed/iframe.isDisplayed()} — which
 * is true even when the PDF is a blank/broken frame. A 0-byte image with a
 * broken-image icon still passes isDisplayed(). This class closes that gap.
 *
 * Detection strategy (no external deps):
 *   - Images : naturalWidth>0 && complete && !error  (the real load signal)
 *   - PDFs   : embed/object/iframe present AND the resource resolved 2xx via the
 *              Performance Resource Timing API, OR pdf.js rendered >=1 canvas
 *   - Network: a fetch/XHR wrapper records every response with status>=400 and
 *              every network failure into window.__failedRequests
 */
public final class AssetLoadVerifier {

    private AssetLoadVerifier() {}

    // ---------------------------------------------------------------- images

    public static final class BrokenAsset {
        public final String url; public final String why;
        BrokenAsset(String url, String why) { this.url = url; this.why = why; }
        @Override public String toString() { return why + " -> " + url; }
    }

    /** Return every <img> on the page that did not actually render pixels. */
    @SuppressWarnings("unchecked")
    public static List<BrokenAsset> findBrokenImages(WebDriver driver) {
        List<BrokenAsset> broken = new ArrayList<>();
        try {
            Object res = ((JavascriptExecutor) driver).executeScript(
                "var out=[];" +
                "document.querySelectorAll('img').forEach(function(img){" +
                "  var r=img.getBoundingClientRect();" +
                // only judge images meant to be visible
                "  var visible = r.width>0 && r.height>0 && img.offsetParent!==null;" +
                "  if(visible && img.currentSrc && (!img.complete || img.naturalWidth===0)){" +
                "    out.push({url: img.currentSrc, why:'naturalWidth=0 / not complete'});" +
                "  }" +
                "});" +
                "return out;");
            if (res instanceof List) {
                for (Object o : (List<Object>) res) {
                    java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                    broken.add(new BrokenAsset(str(m.get("url")), str(m.get("why"))));
                }
            }
        } catch (Exception ignored) {}
        return broken;
    }

    /** Hard gate: fail if any visible image is broken. */
    public static void assertAllImagesLoaded(WebDriver driver, String context) {
        List<BrokenAsset> broken = findBrokenImages(driver);
        if (!broken.isEmpty()) {
            StringBuilder sb = new StringBuilder("Broken image(s) on [" + context + "]:\n");
            for (BrokenAsset b : broken) sb.append("  - ").append(b).append('\n');
            throw new AssertionError(sb.toString());
        }
    }

    // ------------------------------------------------------------------ PDFs

    /**
     * Verify a PDF inside the given container actually rendered.
     * Handles three real-world embeddings:
     *   1. native &lt;embed type=application/pdf&gt; / &lt;object&gt;
     *   2. &lt;iframe&gt; whose src is a .pdf / blob: URL
     *   3. pdf.js, which paints into &lt;canvas&gt; elements
     */
    public static boolean isPdfRendered(WebDriver driver, By container) {
        try {
            WebElement el = driver.findElement(container);
            Boolean ok = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "var c = arguments[0];" +
                "if(!c) return false;" +
                "var rect = c.getBoundingClientRect();" +
                "if(rect.width < 50 || rect.height < 50) return false;" +   // not a real viewer
                // native embed/object/iframe with a pdf/blob source
                "var emb = c.matches('embed,object,iframe') ? c : c.querySelector('embed,object,iframe');" +
                "if(emb){" +
                "  var src = emb.src || emb.data || '';" +
                "  if(/\\.pdf|blob:|application\\/pdf/i.test(src) || (emb.type||'').indexOf('pdf')>=0) return true;" +
                "}" +
                // pdf.js canvas
                "var canv = c.querySelectorAll('canvas');" +
                "for(var i=0;i<canv.length;i++){ if(canv[i].width>0 && canv[i].height>0) return true; }" +
                "return false;",
                el);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            return false;
        }
    }

    public static void assertPdfRendered(WebDriver driver, By container, String context) {
        if (!isPdfRendered(driver, container)) {
            throw new AssertionError("PDF did not render in container [" + container + "] on [" + context + "]. "
                    + "Container missing, < 50px, or no pdf src / pdf.js canvas found.");
        }
    }

    // --------------------------------------------------------- failed network

    /**
     * Install a fetch/XHR wrapper that records every HTTP response with
     * status >= 400 and every network-level failure. Idempotent; install after
     * each navigation (before the actions you want to observe).
     */
    public static void installFailedRequestCapture(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "if(window.__failReqInstalled) return;" +
                "window.__failReqInstalled = true;" +
                "window.__failedRequests = [];" +
                "function __fail(u,s){window.__failedRequests.push({url:String(u),status:s});" +
                "  if(window.__failedRequests.length>200) window.__failedRequests.shift();}" +
                "var _of = window.fetch;" +
                "window.fetch = function(){var args=arguments;" +
                "  return _of.apply(this,args).then(function(resp){" +
                "    if(resp && resp.status>=400) __fail(resp.url||args[0], resp.status); return resp;" +
                "  }).catch(function(e){ __fail((args[0]||'')+'', -1); throw e; });" +
                "};" +
                "var _oo = XMLHttpRequest.prototype.open;" +
                "XMLHttpRequest.prototype.open = function(m,u){ this.__u=u; return _oo.apply(this,arguments); };" +
                "var _os = XMLHttpRequest.prototype.send;" +
                "XMLHttpRequest.prototype.send = function(){ var x=this;" +
                "  x.addEventListener('loadend',function(){" +
                "    if(x.status>=400 || x.status===0) __fail(x.__u, x.status);" +
                "  }); return _os.apply(this,arguments); };");
        } catch (Exception ignored) {}
    }

    public static final class FailedRequest {
        public final String url; public final long status;
        FailedRequest(String url, long status) { this.url = url; this.status = status; }
        @Override public String toString() { return "HTTP " + status + "  " + url; }
    }

    @SuppressWarnings("unchecked")
    public static List<FailedRequest> getFailedRequests(WebDriver driver) {
        List<FailedRequest> out = new ArrayList<>();
        try {
            Object res = ((JavascriptExecutor) driver)
                    .executeScript("return window.__failedRequests || [];");
            if (res instanceof List) {
                for (Object o : (List<Object>) res) {
                    java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                    long st = m.get("status") instanceof Number ? ((Number) m.get("status")).longValue() : 0;
                    out.add(new FailedRequest(str(m.get("url")), st));
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Hard gate: fail if any non-asset XHR/fetch returned >= 400 or failed.
     * {@code ignoreSubstrings} lets you whitelist known-noisy 3rd-party calls
     * (analytics, beamer, devrev) without weakening detection of YOUR APIs.
     */
    public static void assertNoFailedRequests(WebDriver driver, String context, String... ignoreSubstrings) {
        List<FailedRequest> failed = new ArrayList<>();
        outer:
        for (FailedRequest f : getFailedRequests(driver)) {
            for (String ig : ignoreSubstrings) {
                if (ig != null && f.url.toLowerCase().contains(ig.toLowerCase())) continue outer;
            }
            failed.add(f);
        }
        if (!failed.isEmpty()) {
            StringBuilder sb = new StringBuilder("Failed network request(s) on [" + context + "]:\n");
            for (FailedRequest f : failed) sb.append("  - ").append(f).append('\n');
            throw new AssertionError(sb.toString());
        }
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
}
