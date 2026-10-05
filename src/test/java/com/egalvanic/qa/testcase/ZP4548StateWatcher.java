package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZP-4548 (Simplify Subscription Page): watches acme stage while the developer applies her test setups (legacy L1–L4,
 * Foundation A1–A8, module B1, site C1). Polls GET /api/subscription every {@code zp4548.watch.interval} seconds for
 * {@code zp4548.watch.minutes} minutes. Each time the subscription state changes it opens Admin › Subscription, saves the
 * API answer, three screenshots and the page text, and checks the ticket's rules on that real state. Read-only: nothing
 * is clicked except navigation. Evidence: test-output/zp4548-watch/&lt;HHmmss&gt;-&lt;state&gt;/.
 */
public class ZP4548StateWatcher extends BaseTest {

    private static final Path OUT = Paths.get("test-output", "zp4548-watch");

    private Object js(String s, Object... a) { return ((JavascriptExecutor) driver).executeScript(s, a); }
    private Object jsAsync(String s, Object... a) { return ((JavascriptExecutor) driver).executeAsyncScript(s, a); }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private void open(String path) {
        driver.get(AppConstants.BASE_URL + path);
        for (int i = 0; i < 40; i++) {
            loginPage.dismissMfaPromptIfShowing();
            if (Boolean.TRUE.equals(js("var m=document.querySelector('main');return !!m&&/Activity|aren't available|Couldn't load|Only administrators/.test(m.innerText||'');"))) break;
            sleep(1000);
        }
        sleep(2500);
    }

    /** The subscription payload as text, fetched with the app's own token. */
    private String fetchSubscription() {
        Object r = jsAsync("var done=arguments[arguments.length-1];var tok=null;[localStorage,sessionStorage].forEach(function(s){for(var i=0;i<s.length;i++){"
                + "var m=String(s.getItem(s.key(i))).match(/eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+/);if(m&&!tok)tok=m[0];}});"
                + "fetch('/api/subscription',{headers:tok?{Authorization:'Bearer '+tok}:{},credentials:'include'}).then(function(r){return r.text();})"
                + ".then(function(b){done(b);}).catch(function(e){done('ERR '+e);});");
        return String.valueOf(r);
    }

    /** A short key for the state: plan, phase, order, dates, extension, eg-ai tier, usage and T2 totals. */
    private String stateKey(String body) {
        Object k = js("try{var d=JSON.parse(arguments[0]);var s=d.subscription||{};var u=d.usage||{};var t=d.t2||{};"
                + "return [s.plan,s.phase,s.status,s.order_number,(s.start_at||'').slice(0,10),(s.full_access_end_at||'').slice(0,10),(s.grace_end_at||'').slice(0,10),"
                + "(s.end_at||'').slice(0,10),'ext'+(s.extension_days||0),'ai-'+s.ai_tier,'renew-'+s.auto_renew,(s.ended_at||'').slice(0,10),"
                + "'a'+(u.assets&&u.assets.used)+'/'+(u.assets&&u.assets.limit),'s'+(u.sites&&u.sites.used)+'/'+(u.sites&&u.sites.limit),"
                + "'t2-'+t.total+'-'+JSON.stringify(t.counts||{}).replace(/[^0-9,]/g,''),'mods'+(d.modules||[]).filter(function(m){return m.subscribed;}).length].join('|');"
                + "}catch(e){return 'no-json';}", body);
        return String.valueOf(k);
    }

    /**
     * The ticket's checks on the rendered page, in the browser: returns a list of "PASS …" / "FAIL …" lines. The oracle
     * follows Avani's fix comment: no rate card / renewal options / renewal row; Included access = Sites, Assets, Support;
     * T2 after Included access and Terms; no old tier names; Modules card without $; Foundation labels by phase.
     */
    @SuppressWarnings("unchecked")
    private List<String> checks(String body) {
        Object r = js("var out=[];function ok(c,m){out.push((c?'PASS ':'FAIL ')+m);}"
                + "var d=JSON.parse(arguments[0]);var s=d.subscription;var plan=s.plan;var main=document.querySelector('main');var t=main.innerText;"
                + "ok(!/Rate card/.test(t),'no \"Rate card\" text');"
                + "ok(!/Renewal options/.test(t),'no Renewal options card');"
                + "if(plan==='legacy'||plan==='foundation') ok(!/\\nRenewal\\n/.test(t),'no Renewal row ('+plan+')');"
                + "ok(!/Interactive|Read-only|Read-Only|No license|No License/.test(t),'no old tier names');"
                + "ok(!/Platform users|AI users/.test(t),'no Platform users / AI users rows');"
                // Included access rows: the label cells of the Included access card
                + "var cards=[].slice.call(main.querySelectorAll('.MuiPaper-root'));function card(title){return cards.find(function(c){var h=c.firstElementChild;return h&&(h.innerText||'').trim().split('\\n')[0]===title;});}"
                + "var inc=card('Included access');if(inc){var keys=(inc.innerText||'').split('\\n').map(function(x){return x.trim();}).filter(function(x){return /^(Sites|Assets|Support|Platform users|AI users)$/.test(x);});"
                + "ok(keys.join(',')==='Sites,Assets,Support','Included access rows = '+keys.join(', '));}else ok(false,'Included access card found');"
                // card order
                + "var pos=function(x){return t.indexOf(x);};var pi=pos('Included access'),pt=pos('\\nTerms\\n'),p2=pos('T2 licenses'),pa=pos('\\nActivity\\n');"
                + "if(d.t2&&d.t2.total>0) ok(p2>pi&&p2>pt&&p2<pa,'T2 card after Included access and Terms, before Activity');else ok(p2<0,'no T2 card when there are no T2 accounts');"
                // modules card
                + "var mods=card('Modules');if(plan==='legacy'){ok(!mods,'legacy has no Modules card');var sc=card('Subscription');var st=sc?sc.innerText:'';"
                + "ok(/Plan[\\s\\S]*Term[\\s\\S]*Value/.test(st)&&!/Renewal/.test(st),'legacy Subscription card = Plan / Term / Value');"
                + "var fee=(t.match(/ANNUAL LICENSE FEES\\n[^\\n]*\\n([^\\n]*)/)||[])[1]||'';ok(s.phase==='expired'?/Last term|Since/.test(fee):(s.auto_renew?/^Renews /.test(fee):fee==='Legacy pricing'),'legacy fee caption \"'+fee+'\"');}"
                + "else if(mods){var mt=mods.innerText;ok(!/\\$\\s?\\d/.test(mt),'Modules card has no $');ok(!/Annual rate|Total|Rate card/.test(mt),'Modules card has no rate column / total / chip');}"
                + "else ok(false,'Modules card found');"
                // foundation oracle
                + "if(plan==='foundation'&&mods){var rows=[].slice.call(mods.querySelectorAll('tbody tr')).map(function(r){var c=r.querySelectorAll('td');return {n:(c[0]||{}).innerText,a:((c[2]||{}).innerText||'').split('\\n')[0].trim()};});"
                + "ok(!rows.some(function(r){return /Coming soon|Analytics Advanced|Sensor|Services/.test(r.n);}),'Foundation hides coming-soon and Services rows');"
                + "var ph=s.phase,ended=ph==='expired';var adv=ph==='full'?/^(Expires in \\d+ days?|Expires today)$/:ph==='grace'?/^(Shuts down in \\d+ days?|Shuts down today)$/:ph==='limited'?/^Locked$/:ph==='upcoming'?/^Starts /:/^Ended$/;"
                + "d.modules.filter(function(m){return m.tier==='core'||m.tier==='advanced';}).forEach(function(m){var r=rows.find(function(x){return x.n===m.name;});if(!r){ok(false,m.name+' row');return;}"
                + "var inOrder=m.subscribed||['included','until','shuts_down','locked'].indexOf(m.access&&m.access.code)>=0;"
                + "var exp=m.tier==='core'?(ended?/^Ended$/:/^Included$/):!inOrder?/^Not included$/:adv;ok(exp.test(r.a),m.name+' → '+r.a);});"
                + "d.ai_tiers.forEach(function(a){var r=rows.find(function(x){return x.n===a.name;});if(!r){ok(false,a.name+' row');return;}"
                + "var exp=a.tier==='free'?(ended?/^Ended$/:/^Included$/):a.current?adv:/^Not included$/;ok(exp.test(r.a),a.name+' → '+r.a);});"
                + "var fd=(d.policy.full_access_days||0)+(s.extension_days||0);ok(t.indexOf(d.policy.term_months+'-month term · '+fd+'-day full access')>=0,'Plan tile \"'+d.policy.term_months+'-month term · '+fd+'-day full access\"');"
                + "ok(/of 1,500 · \\d+ of 3 sites/.test(t)||d.usage.assets.limit!==1500,'Assets tile \"of 1,500 · N of 3 sites\"');"
                + "ok(t.indexOf('Advanced modules are open for the first '+fd+' days')>=0,'footer note names the '+fd+'-day window');}"
                // T2 header vs payload
                + "if(d.t2&&d.t2.total>0){var t2=card('T2 licenses');var tt=t2?t2.innerText:'';var names={interactive:'Full Access',read_only:'Lite',no_license:'Free'};"
                + "var chip=['interactive','read_only','no_license'].filter(function(k){return d.t2.counts[k];}).map(function(k){return d.t2.counts[k]+' '+names[k];}).join(' · ');"
                + "ok(tt.indexOf(chip)>=0,'T2 header \"'+chip+'\"');var money='$'+(d.t2.annual_cents/100).toLocaleString('en-US');ok(tt.indexOf(money+' / yr')>=0,'T2 total '+money+' / yr');"
                + "var rws=[].slice.call(t2.querySelectorAll('tbody tr')).map(function(r){return r.innerText.split('\\n')[0].trim();});ok(rws.length===Math.min(d.t2.total,d.t2.page_size||10),'T2 shows '+rws.length+' rows on page 1');}"
                + "return out;", body);
        return (List<String>) r;
    }

    private void shots(Path dir) throws Exception {
        Files.write(dir.resolve("1_top.png"), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        String[] marks = {"Modules|Subscription", "Included access"};
        String[] names = {"2_modules_or_subscription.png", "3_included_terms_t2.png"};
        for (int i = 0; i < marks.length; i++) {
            // card titles only: the leaf must sit inside a card (Paper), so the page header and overline never match
            js("var want=arguments[0].split('|');var els=[].slice.call(document.querySelectorAll('main .MuiPaper-root *')).filter(function(e){return e.childElementCount===0&&want.indexOf((e.textContent||'').trim())>=0;});"
                    + "var el=null;for(var w=0;w<want.length&&!el;w++){el=els.filter(function(e){return e.textContent.trim()===want[w];})[0]||null;}"
                    + "if(el){el.scrollIntoView({block:'start'});var sc=el.parentElement;while(sc&&!(sc.scrollHeight>sc.clientHeight+5&&/(auto|scroll)/.test(getComputedStyle(sc).overflowY)))sc=sc.parentElement;if(sc)sc.scrollTop-=12;}", marks[i]);
            sleep(600);
            Files.write(dir.resolve(names[i]), ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        }
    }

    @Test(description = "ZP-4548: watch the developer's stage setups and check each real subscription state (read-only)")
    public void watchStates() throws Exception {
        ExtentReportManager.createTest("ZP-4548", "Simplify Subscription Page", "State watcher");
        int minutes = Integer.getInteger("zp4548.watch.minutes", 60);
        int interval = Integer.getInteger("zp4548.watch.interval", 30);
        long until = System.currentTimeMillis() + minutes * 60_000L;
        Files.createDirectories(OUT);
        Path log = OUT.resolve("watch.log");
        open("/admin/subscription");
        String last = "";
        int states = 0, fails = 0;
        DateTimeFormatter hms = DateTimeFormatter.ofPattern("HHmmss");
        while (System.currentTimeMillis() < until) {
            String body = fetchSubscription();
            String key = stateKey(body);
            String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if ("no-json".equals(key)) {
                // session lapsed or a transient error: reopen the page (the app refreshes its token) and poll again
                Files.write(log, (now + " no JSON (" + body.substring(0, Math.min(60, body.length())).replaceAll("\\s+", " ") + ") — reopening\n").getBytes(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                open("/admin/subscription");
            } else if (!key.equals(last)) {
                states++;
                open("/admin/subscription");
                // the page fetched on mount; make sure it shows the same state we keyed (the dev may switch again mid-way)
                String body2 = fetchSubscription();
                Path dir = OUT.resolve(LocalTime.now().format(hms) + "-" + key.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("_+", "_"));
                Files.createDirectories(dir);
                Files.write(dir.resolve("api_subscription.json"), body2.getBytes());
                List<String> res = new ArrayList<>();
                res.add("state " + stateKey(body2) + (stateKey(body2).equals(key) ? "" : " (changed while loading; first was " + key + ")"));
                try { res.addAll(checks(body2)); } catch (Exception e) { res.add("FAIL checks threw: " + e.getMessage().split("\n")[0]); }
                String text = String.valueOf(js("return (document.querySelector('main')||document.body).innerText;"));
                Files.write(dir.resolve("page_text.txt"), text.getBytes());
                Object banner = js("var e=[].slice.call(document.querySelectorAll('a[href^=\"mailto:customer-success\"]')).find(function(a){return a.offsetParent!==null;});"
                        + "if(!e)return 'none';var b=e;for(var i=0;i<6&&b.parentElement;i++){b=b.parentElement;if(b.querySelector('button'))break;}return (b.innerText||'').replace(/\\s+/g,' ').trim();");
                res.add("banner: " + banner);
                shots(dir);
                Files.write(dir.resolve("checks.txt"), String.join("\n", res).getBytes());
                long f = res.stream().filter(x -> x.startsWith("FAIL")).count();
                fails += (int) f;
                String line = now + " NEW STATE " + key + " → " + dir.getFileName() + " · " + (res.size() - 2) + " checks, " + f + " FAIL";
                System.out.println("[ZP-4548 watch] " + line);
                Files.write(log, (line + "\n").getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                last = stateKey(body2);
            } else {
                Files.write(log, (now + " same " + key + "\n").getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            }
            sleep(interval * 1000L);
        }
        System.out.println("[ZP-4548 watch] done: " + states + " states, " + fails + " failed checks");
        Assert.assertEquals(fails, 0, "Failed checks across the watched states (see test-output/zp4548-watch/*/checks.txt)");
    }
}
