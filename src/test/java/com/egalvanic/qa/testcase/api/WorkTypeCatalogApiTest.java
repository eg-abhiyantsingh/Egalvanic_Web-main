package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.constants.WorkTypeCatalog;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Work Type API Contract</b> — the API surface behind the v1.35 (ZP-3000 Services /
 * Procedures-V2) Create-Work-Order matrix, pinned against {@link WorkTypeCatalog} (84 rows).
 *
 * <p>Every fact asserted here was live-verified 2026-07-21 on acme.qa (site Z1):</p>
 * <ul>
 *   <li>{@code GET /procedures-v2/services} → 200 {@code {data:[13 × {id,key,name,type,
 *       de_energized,procedure_count}], success:true}}; type strings are exactly
 *       AF / IR / COM / Checklist / Schedule / "PM Forms".</li>
 *   <li>{@code GET /procedures-v2/procedures?service_id={id}} → 200 {@code {data:[…], success:true}}.</li>
 *   <li>{@code POST /ir_session/scope-preview {sld_id, work_type_id, asset_scope:null}} → 200
 *       {@code {data:{applicable, assets:[{id,label,node_class_name,room_label}],
 *       matching_count, reason}, success:true}} — and {@code matching_count == assets.length}
 *       (regression tripwire for the 2026-07-20 CTT preview-vs-created mismatch bug).</li>
 *   <li>Fixture sessions: {@code GET /ir_session/{id}/{full|assets|team}} → 200 JSON;
 *       {@code /full} carries the WO name + work_type_id.</li>
 *   <li>Negatives: all three endpoints 401 without auth; {@code service_id=not-a-uuid} 500s
 *       (generic HTML, no SQL text — strict &lt;500 row is {@code known-product-bug});
 *       garbage {@code sld_id} 500s AND leaks raw psycopg2 + the full SELECT (the VERIFIED
 *       backend psycopg2-leak defect class — BOTH its rows are {@code known-product-bug});
 *       missing {@code work_type_id} degrades gracefully (200, applicable:false,
 *       reason "no work type / sld" — pinned as by-design, not a bug);
 *       {@code DELETE /ir_session/{random-uuid}} returns 200 with an async
 *       {@code {_mutation:{status:"received"}}} receipt even for a nonexistent id
 *       (contract OBSERVATION of the async-delete semantics, pinned so a move to
 *       synchronous 404 semantics gets flagged).</li>
 * </ul>
 *
 * <p><b>Efficiency:</b> the services catalog is fetched once per run and each service's
 * scope-preview is POSTed once (a per-class cache keyed by serviceId feeds both the
 * contract rows and the asset-shape rows).</p>
 *
 * <p><b>Failure policy:</b> if a row fails, FIRST re-pull the endpoint — the product catalog
 * may have drifted; update {@link WorkTypeCatalog} in the same commit as the fix.</p>
 */
public class WorkTypeCatalogApiTest extends BaseAPITest {

    private static final String FEATURE = "Work Type API Contract";

    private static final String SERVICES_PATH      = "/procedures-v2/services";
    private static final String PROCEDURES_PATH    = "/procedures-v2/procedures";
    private static final String SCOPE_PREVIEW_PATH = "/ir_session/scope-preview";

    /** services-API "type" string → catalog family (exact strings live-verified 2026-07-21). */
    private static final Map<String, WorkTypeCatalog.Family> TYPE_TO_FAMILY = new LinkedHashMap<>();
    static {
        TYPE_TO_FAMILY.put("AF",        WorkTypeCatalog.Family.AF);
        TYPE_TO_FAMILY.put("IR",        WorkTypeCatalog.Family.IR);
        TYPE_TO_FAMILY.put("COM",       WorkTypeCatalog.Family.COM);
        TYPE_TO_FAMILY.put("Checklist", WorkTypeCatalog.Family.CHECKLIST);
        TYPE_TO_FAMILY.put("Schedule",  WorkTypeCatalog.Family.SCHEDULE);
        TYPE_TO_FAMILY.put("PM Forms",  WorkTypeCatalog.Family.PM_FORMS);
    }

    /**
     * Tokens that must NEVER reach an API client, whatever the status code.
     * Live 2026-07-21 the garbage-sld scope-preview body contained
     * {@code psycopg2.errors.InvalidTextRepresentation} plus {@code [SQL: SELECT nodes.id …]}.
     */
    private static final String[] SQL_LEAK_TOKENS = {
            "psycopg2", "sqlalchemy", "traceback (most recent", "[sql:"
    };

    // ── per-run response caches (hit each endpoint once) ────────────────────

    private Response servicesResponse;
    private final Map<String, Response> scopePreviewCache = new ConcurrentHashMap<>();

    /** GET /procedures-v2/services exactly once per run. */
    private synchronized Response servicesCatalog() {
        if (servicesResponse == null) {
            servicesResponse = getAuthenticatedRequestSpec()
                    .when().get(SERVICES_PATH)
                    .then().extract().response();
            System.out.println("[WorkTypeApi] GET " + SERVICES_PATH + " -> "
                    + servicesResponse.statusCode() + " in " + servicesResponse.getTime() + "ms");
        }
        return servicesResponse;
    }

    /** Envelope-validated services data[] (hard-asserts 200 / JSON / success / array). */
    private JSONArray servicesData() {
        Response r = servicesCatalog();
        Assert.assertEquals(r.statusCode(), 200,
                "GET " + SERVICES_PATH + " must return 200 (body: " + head(r.asString()) + ")");
        String body = r.asString();
        Assert.assertFalse(body == null || body.trim().startsWith("<"),
                SERVICES_PATH + " must answer JSON, not HTML: " + head(body));
        JSONObject root = new JSONObject(body);
        Assert.assertTrue(root.optBoolean("success", false),
                SERVICES_PATH + " envelope must carry success:true — got: " + head(body));
        JSONArray data = root.optJSONArray("data");
        Assert.assertNotNull(data,
                SERVICES_PATH + " envelope must carry a data[] array — got: " + head(body));
        return data;
    }

    /** POST /ir_session/scope-preview exactly once per service (both priority-4 and -5 rows reuse it). */
    private Response scopePreview(final WorkTypeCatalog.WorkTypeProfile profile) {
        return scopePreviewCache.computeIfAbsent(profile.serviceId, new java.util.function.Function<String, Response>() {
            @Override
            public Response apply(String serviceId) {
                JSONObject body = new JSONObject();
                body.put("sld_id", WorkTypeCatalog.Z1_SLD_ID);
                body.put("work_type_id", serviceId);
                body.put("asset_scope", JSONObject.NULL);
                Response r = getAuthenticatedRequestSpec()
                        .body(body.toString())
                        .when().post(SCOPE_PREVIEW_PATH)
                        .then().extract().response();
                System.out.println("[WorkTypeApi] scope-preview " + profile.name + " -> "
                        + r.statusCode() + " in " + r.getTime() + "ms");
                return r;
            }
        });
    }

    /** Envelope-validated scope-preview data{} for a service (hard-asserts 200 / success / data). */
    private JSONObject scopePreviewData(WorkTypeCatalog.WorkTypeProfile profile) {
        Response r = scopePreview(profile);
        Assert.assertEquals(r.statusCode(), 200,
                "POST " + SCOPE_PREVIEW_PATH + " for '" + profile.name + "' must return 200 (body: "
                        + head(r.asString()) + ")");
        JSONObject root = new JSONObject(r.asString());
        Assert.assertTrue(root.optBoolean("success", false),
                "scope-preview envelope for '" + profile.name + "' must carry success:true — got: "
                        + head(r.asString()));
        JSONObject data = root.optJSONObject("data");
        Assert.assertNotNull(data,
                "scope-preview envelope for '" + profile.name + "' must carry a data{} object — got: "
                        + head(r.asString()));
        return data;
    }

    // ── small helpers ────────────────────────────────────────────────────────

    /** Hard-fail (never skip) when the class-level API login broke — every row is untrustable then. */
    private void requireAuth() {
        if (!hasAuthToken()) {
            Assert.fail("No API auth token — POST /auth/login failed in BaseAPITest.setUp(). "
                    + "Check " + AppConstants.BASE_URL + " reachability / branding-API slowness / "
                    + "credentials BEFORE trusting any Work Type API row (nothing was actually tested).");
        }
    }

    private static String head(String body) {
        if (body == null) return "null";
        String t = body.trim().replaceAll("\\s+", " ");
        return t.length() > 300 ? t.substring(0, 300) + "…" : t;
    }

    /** First SQL/driver-internal token found in the body (lowercased scan), or null when clean. */
    private static String findSqlLeakToken(String body) {
        String lower = body == null ? "" : body.toLowerCase();
        for (String t : SQL_LEAK_TOKENS) {
            if (lower.contains(t)) return t;
        }
        return null;
    }

    /** The family's fixture representative (the work type its Z1 fixture WO was created with). */
    private static WorkTypeCatalog.WorkTypeProfile representativeOf(WorkTypeCatalog.Family family) {
        for (WorkTypeCatalog.WorkTypeProfile p : WorkTypeCatalog.familyRepresentatives()) {
            if (p.family == family) return p;
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DATA PROVIDERS
    // ════════════════════════════════════════════════════════════════════════

    @DataProvider(name = "catalogShapeChecks")
    public Object[][] catalogShapeChecks() {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (String check : Arrays.asList("count-is-13", "unique-keys", "unique-ids", "unique-names")) {
            rows.add(new Object[]{check});
        }
        return rows.toArray(new Object[0][]);
    }

    /** The 13 service work types from the authoritative catalog, display order. */
    @DataProvider(name = "allServices")
    public Object[][] allServices() {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (WorkTypeCatalog.WorkTypeProfile p : WorkTypeCatalog.SERVICES) {
            rows.add(new Object[]{p});
        }
        return rows.toArray(new Object[0][]);
    }

    /** 6 Z1 fixture sessions × {full, assets, team} = 18 endpoint rows. */
    @DataProvider(name = "fixtureEndpoints")
    public Object[][] fixtureEndpoints() {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (WorkTypeCatalog.Family family : WorkTypeCatalog.Z1_FIXTURE_SESSION_IDS.keySet()) {
            for (String sub : Arrays.asList("full", "assets", "team")) {
                rows.add(new Object[]{family, sub});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    /** {label, method, path, jsonBodyOrNull} — auth-gate probes without an Authorization header. */
    @DataProvider(name = "noAuthEndpoints")
    public Object[][] noAuthEndpoints() {
        String afServiceId = WorkTypeCatalog.byName("Arc Flash Data Collection").serviceId;
        String irFixtureId = WorkTypeCatalog.Z1_FIXTURE_SESSION_IDS.get(WorkTypeCatalog.Family.IR);
        String previewBody = new JSONObject()
                .put("sld_id", WorkTypeCatalog.Z1_SLD_ID)
                .put("work_type_id", WorkTypeCatalog.byName("Infrared Thermography").serviceId)
                .put("asset_scope", JSONObject.NULL).toString();
        List<Object[]> rows = new ArrayList<Object[]>();
        rows.add(new Object[]{"GET services catalog", "GET", SERVICES_PATH, null});
        rows.add(new Object[]{"GET procedures list", "GET", PROCEDURES_PATH + "?service_id=" + afServiceId, null});
        rows.add(new Object[]{"POST scope-preview", "POST", SCOPE_PREVIEW_PATH, previewBody});
        rows.add(new Object[]{"GET fixture session /full", "GET", "/ir_session/" + irFixtureId + "/full", null});
        return rows.toArray(new Object[0][]);
    }

    /** Malformed requests that MUST NOT be answered with a 5xx (currently both 500 — known bug). */
    @DataProvider(name = "malformedRequests")
    public Object[][] malformedRequests() {
        List<Object[]> rows = new ArrayList<Object[]>();
        rows.add(new Object[]{"procedures?service_id=not-a-uuid"});
        rows.add(new Object[]{"scope-preview sld_id=garbage-not-a-uuid"});
        return rows.toArray(new Object[0][]);
    }

    /** Execute one named malformed request (shared by the strict-status and leak-tripwire rows). */
    private Response fireMalformed(String key) {
        if (key.startsWith("procedures")) {
            return getAuthenticatedRequestSpec()
                    .when().get(PROCEDURES_PATH + "?service_id=not-a-uuid")
                    .then().extract().response();
        }
        JSONObject body = new JSONObject();
        body.put("sld_id", "garbage-not-a-uuid");
        body.put("work_type_id", WorkTypeCatalog.byName("Infrared Thermography").serviceId);
        body.put("asset_scope", JSONObject.NULL);
        return getAuthenticatedRequestSpec()
                .body(body.toString())
                .when().post(SCOPE_PREVIEW_PATH)
                .then().extract().response();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. Services catalog shape (4 rows)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 1, dataProvider = "catalogShapeChecks",
          description = "TC_WTAPI_001: GET /procedures-v2/services — 200 success envelope with exactly 13 uniquely keyed/id'd/named services")
    public void testServicesCatalogShape(String check) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_001 — " + check);
        requireAuth();
        JSONArray data = servicesData();   // hard-asserts 200 / JSON / success:true / data[]
        ExtentReportManager.logInfo("services catalog: " + data.length() + " entries; checking '" + check + "'");

        if ("count-is-13".equals(check)) {
            Assert.assertEquals(data.length(), 13,
                    "services catalog must hold exactly 13 services (the WO dialog shows these 13 + 'General'). "
                            + "Drifted? Re-pull " + SERVICES_PATH + " and update WorkTypeCatalog in the same commit.");
            ExtentReportManager.logPass("services catalog has exactly 13 entries.");
            return;
        }

        String field = "unique-keys".equals(check) ? "key" : "unique-ids".equals(check) ? "id"
                : "unique-names".equals(check) ? "name" : null;
        Assert.assertNotNull(field, "unknown catalog shape check '" + check + "'");

        List<String> values = new ArrayList<String>();
        for (int i = 0; i < data.length(); i++) {
            String v = data.getJSONObject(i).optString(field, "");
            Assert.assertFalse(v.isEmpty(),
                    "service entry #" + i + " has an empty '" + field + "': " + head(data.getJSONObject(i).toString()));
            values.add(v);
        }
        Set<String> distinct = new LinkedHashSet<String>(values);
        Assert.assertEquals(distinct.size(), values.size(),
                "duplicate service '" + field + "' values in the catalog: " + values);
        ExtentReportManager.logPass("all " + values.size() + " service '" + field + "' values are non-empty and unique.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. Per-service pinned catalog entry (13 rows)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 2, dataProvider = "allServices",
          description = "TC_WTAPI_002: services catalog entry matches the WorkTypeCatalog pin (name/id/de_energized/family)")
    public void testServicePinned(WorkTypeCatalog.WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_002 — " + profile);
        requireAuth();
        JSONArray data = servicesData();

        JSONObject entry = null;
        for (int i = 0; i < data.length(); i++) {
            JSONObject e = data.getJSONObject(i);
            if (profile.apiKey.equals(e.optString("key"))) { entry = e; break; }
        }
        Assert.assertNotNull(entry, "service key '" + profile.apiKey + "' (" + profile.name
                + ") missing from the live catalog — drifted? Re-pull " + SERVICES_PATH + ".");
        ExtentReportManager.logInfo("live entry: " + entry.toString());

        Assert.assertEquals(entry.optString("name"), profile.name,
                "name drift for key '" + profile.apiKey + "' (NB: NETA Testing's key is 'de-energized-testing' by design)");
        Assert.assertEquals(entry.optBoolean("de_energized"), profile.deEnergized,
                "de_energized drift for '" + profile.name + "' — this flag drives Auto-Schedule semantics");
        Assert.assertEquals(entry.optString("id"), profile.serviceId,
                "service id drift for '" + profile.name + "' (uuid5-style, stable per tenant)");

        String type = entry.optString("type");
        WorkTypeCatalog.Family mapped = TYPE_TO_FAMILY.get(type);
        Assert.assertNotNull(mapped, "unknown services-API type string '" + type + "' for '" + profile.name
                + "' — expected one of " + TYPE_TO_FAMILY.keySet());
        Assert.assertEquals(mapped, profile.family,
                "family drift for '" + profile.name + "': API type '" + type + "' maps to " + mapped
                        + " but the catalog pins " + profile.family);

        int pc = entry.optInt("procedure_count", -1);
        if (pc != profile.procedureCountAtCapture) {
            ExtentReportManager.logWarning("procedure_count drift for '" + profile.name + "': captured "
                    + profile.procedureCountAtCapture + " (2026-07-21), live " + pc
                    + " — admins edit procedures; expectsNoProceduresNotice()/expectsScopePreview() semantics may shift.");
        }
        Assert.assertTrue(pc >= 0,
                "procedure_count must be a non-negative int for '" + profile.name + "', got " + pc);
        ExtentReportManager.logPass("'" + profile.name + "' pinned entry verified (procedure_count live=" + pc + ").");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. Procedures list per service (13 rows)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 3, dataProvider = "allServices",
          description = "TC_WTAPI_003: GET /procedures-v2/procedures?service_id={id} — 200 JSON envelope with a data[] array")
    public void testProceduresListPerService(WorkTypeCatalog.WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_003 — " + profile);
        requireAuth();

        Response r = getAuthenticatedRequestSpec()
                .queryParam("service_id", profile.serviceId)
                .when().get(PROCEDURES_PATH)
                .then().extract().response();
        String body = r.asString();
        ExtentReportManager.logInfo("GET " + PROCEDURES_PATH + "?service_id=" + profile.serviceId
                + " -> " + r.statusCode() + " in " + r.getTime() + "ms");

        Assert.assertEquals(r.statusCode(), 200,
                "procedures list for '" + profile.name + "' must return 200 (body: " + head(body) + ")");
        String contentType = String.valueOf(r.getContentType());
        Assert.assertTrue(contentType.contains("json"),
                "procedures list for '" + profile.name + "' must be application/json, got '" + contentType + "'");
        Assert.assertFalse(body == null || body.trim().startsWith("<"),
                "procedures list for '" + profile.name + "' answered HTML, not JSON: " + head(body));

        // What the 200 body actually guarantees (live 2026-07-21): {data:[…], success:true}.
        // Accept a bare array too so a benign de-enveloping doesn't false-alarm — but an
        // array node MUST exist somewhere sane.
        JSONArray arrayNode;
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            arrayNode = new JSONArray(trimmed);
        } else {
            JSONObject root = new JSONObject(trimmed);
            if (root.has("success")) {
                Assert.assertTrue(root.optBoolean("success", false),
                        "procedures envelope for '" + profile.name + "' carries success:false — " + head(body));
            }
            arrayNode = root.optJSONArray("data");
            if (arrayNode == null) arrayNode = root.optJSONArray("procedures");
            if (arrayNode == null) arrayNode = root.optJSONArray("items");
        }
        Assert.assertNotNull(arrayNode,
                "procedures response for '" + profile.name + "' has no array node (data/procedures/items): " + head(body));
        ExtentReportManager.logPass("'" + profile.name + "': procedures array present with " + arrayNode.length()
                + " entries (catalog captured procedure_count=" + profile.procedureCountAtCapture + ").");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. Scope-preview contract per service (13 rows)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 4, dataProvider = "allServices",
          description = "TC_WTAPI_004: POST /ir_session/scope-preview — success envelope, int matching_count >= 0, and matching_count == assets.length")
    public void testScopePreviewContract(WorkTypeCatalog.WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_004 — " + profile);
        requireAuth();

        JSONObject data = scopePreviewData(profile);   // hard-asserts 200 / success:true / data{}

        Object mc = data.opt("matching_count");
        Assert.assertTrue(mc instanceof Integer || mc instanceof Long,
                "data.matching_count for '" + profile.name + "' must be a JSON int, got "
                        + (mc == null ? "null/absent" : mc.getClass().getSimpleName() + "=" + mc));
        int count = ((Number) mc).intValue();
        Assert.assertTrue(count >= 0,
                "data.matching_count for '" + profile.name + "' must be >= 0, got " + count);

        JSONArray assets = data.optJSONArray("assets");
        Assert.assertNotNull(assets,
                "data.assets for '" + profile.name + "' must be an array — got: " + head(data.toString()));

        ExtentReportManager.logInfo("'" + profile.name + "': matching_count=" + count
                + ", assets.length=" + assets.length() + ", applicable=" + data.opt("applicable")
                + ", reason=" + data.opt("reason"));

        // CRITICAL regression tripwire — the 2026-07-20 CTT bug class was a preview count that
        // disagreed with what actually materialised. The preview must at least agree with itself.
        Assert.assertEquals(assets.length(), count,
                "scope-preview self-consistency broken for '" + profile.name
                        + "': matching_count=" + count + " but assets[] has " + assets.length()
                        + " entries (CTT preview-vs-created mismatch class, 2026-07-20).");
        ExtentReportManager.logPass("'" + profile.name + "' scope-preview consistent: matching_count "
                + count + " == assets.length " + assets.length() + ".");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. Scope-preview asset shape per service (13 rows, reuses the cached response)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 5, dataProvider = "allServices",
          description = "TC_WTAPI_005: scope-preview assets[0] carries non-empty id/label/node_class_name (cached response — one POST per service)")
    public void testScopePreviewAssetShape(WorkTypeCatalog.WorkTypeProfile profile) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_005 — " + profile);
        requireAuth();

        JSONObject data = scopePreviewData(profile);   // cache hit — no second POST for this service
        JSONArray assets = data.optJSONArray("assets");
        Assert.assertNotNull(assets, "data.assets missing for '" + profile.name + "': " + head(data.toString()));
        int count = data.optInt("matching_count", -1);

        if (count > 0) {
            JSONObject first = assets.getJSONObject(0);
            ExtentReportManager.logInfo("'" + profile.name + "' first preview asset: " + head(first.toString()));
            Assert.assertFalse(first.optString("id", "").isEmpty(),
                    "assets[0].id must be non-empty for '" + profile.name + "': " + head(first.toString()));
            Assert.assertFalse(first.optString("label", "").isEmpty(),
                    "assets[0].label must be non-empty for '" + profile.name + "': " + head(first.toString()));
            Assert.assertFalse(first.optString("node_class_name", "").isEmpty(),
                    "assets[0].node_class_name must be non-empty for '" + profile.name + "': " + head(first.toString()));
            ExtentReportManager.logPass("'" + profile.name + "': " + count
                    + " preview assets, assets[0] shape {id,label,node_class_name} verified.");
        } else {
            // 0-match services (e.g. Shutdown (Composite), 0 procedures) must still be internally
            // consistent: an empty assets[] — never phantom rows under a zero count.
            Assert.assertEquals(assets.length(), 0,
                    "'" + profile.name + "' reports matching_count=" + count
                            + " but assets[] is not empty (" + assets.length() + " rows).");
            ExtentReportManager.logPass("'" + profile.name + "': 0 matching assets and assets[] is empty — consistent.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. Z1 fixture session endpoints (6 fixtures × full/assets/team = 18 rows)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 6, dataProvider = "fixtureEndpoints",
          description = "TC_WTAPI_006: GET /ir_session/{fixture}/{full|assets|team} — 200 JSON; /full identifies the fixture WO")
    public void testFixtureSessionEndpoint(WorkTypeCatalog.Family family, String sub) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_006 — " + family + " /" + sub);
        requireAuth();

        String sessionId = WorkTypeCatalog.Z1_FIXTURE_SESSION_IDS.get(family);
        Assert.assertNotNull(sessionId, "no Z1 fixture session id pinned for family " + family);

        Response r = getAuthenticatedRequestSpec()
                .when().get("/ir_session/" + sessionId + "/" + sub)
                .then().extract().response();
        String body = r.asString();
        ExtentReportManager.logInfo("GET /ir_session/" + sessionId + "/" + sub + " -> " + r.statusCode()
                + " in " + r.getTime() + "ms (" + (body == null ? 0 : body.length()) + " chars)");

        Assert.assertEquals(r.statusCode(), 200,
                family + " fixture /" + sub + " must return 200 — fixture deleted/renamed? "
                        + "(see docs/changelogs/2026-07-21-service-wo-fixture-set-z1.md; body: " + head(body) + ")");
        String contentType = String.valueOf(r.getContentType());
        Assert.assertTrue(contentType.contains("json"),
                family + " fixture /" + sub + " must be application/json, got '" + contentType + "'");
        Assert.assertFalse(body == null || body.trim().startsWith("<"),
                family + " fixture /" + sub + " answered HTML, not JSON: " + head(body));

        if ("full".equals(sub)) {
            String woName = WorkTypeCatalog.Z1_FIXTURE_WO_NAMES.get(family);
            WorkTypeCatalog.WorkTypeProfile rep = representativeOf(family);
            // Live 2026-07-21: /full carries data.session.name (the WO name) and
            // data.session.work_type_id (the service id) — the type NAME is not embedded.
            boolean identified = (woName != null && body.contains(woName))
                    || (rep != null && (body.contains(rep.name) || body.contains(rep.serviceId)));
            Assert.assertTrue(identified,
                    family + " fixture /full must identify the fixture — expected WO name '" + woName
                            + "' or work type '" + (rep == null ? "?" : rep.name + "'/'" + rep.serviceId) + "' in body: "
                            + head(body));
            ExtentReportManager.logPass(family + " /full identifies fixture WO '" + woName + "'.");
        } else {
            String trimmed = body.trim();
            Assert.assertTrue(trimmed.startsWith("{") || trimmed.startsWith("["),
                    family + " fixture /" + sub + " must be a JSON document, got: " + head(body));
            ExtentReportManager.logPass(family + " /" + sub + " returns 200 JSON (" + trimmed.length() + " chars).");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. Negatives — auth gate (4 rows)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 7, dataProvider = "noAuthEndpoints",
          description = "TC_WTAPI_007: every Work Type endpoint rejects requests without an Authorization header (401/403)")
    public void testNegativeNoAuth(String label, String method, String path, String jsonBody) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_007 — no-auth " + label);
        requireAuth();   // env sanity: prove login works before trusting a 401 as an auth gate

        Response r;
        if ("POST".equals(method)) {
            r = getRequestSpec().body(jsonBody).when().post(path).then().extract().response();
        } else {
            r = getRequestSpec().when().get(path).then().extract().response();
        }
        int status = r.statusCode();
        ExtentReportManager.logInfo("no-auth " + method + " " + path + " -> " + status);
        Assert.assertTrue(status == 401 || status == 403,
                "unauthenticated " + method + " " + path + " must be rejected with 401/403, got "
                        + status + " (body: " + head(r.asString()) + ") — auth gate missing = data leak.");
        ExtentReportManager.logPass("no-auth " + label + " correctly rejected with " + status + ".");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. Negatives — malformed input must not 5xx (2 rows, KNOWN PRODUCT BUG)
    //    Live 2026-07-21: BOTH rows return 500 — the verified backend defect class
    //    "500 + psycopg2/SQL leak on bad GET/query params" (project_api_testing).
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 8, dataProvider = "malformedRequests", groups = {"known-product-bug"},
          description = "TC_WTAPI_008: malformed service_id / sld_id must be a 4xx, never a 500 (currently 500s — verified backend bug)")
    public void testMalformedRequestNotServerError(String key) {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_008 — " + key);
        requireAuth();

        Response r = fireMalformed(key);
        int status = r.statusCode();
        ExtentReportManager.logInfo("malformed '" + key + "' -> " + status + " (body: " + head(r.asString()) + ")");
        Assert.assertTrue(status < 500,
                "malformed input '" + key + "' must be rejected with a 4xx, got " + status
                        + " — unvalidated uuid hits the DB raw (verified psycopg2 500 class, live 2026-07-21).");
        ExtentReportManager.logPass("malformed '" + key + "' handled without a server error: " + status + " — BUG FIXED, "
                + "remove this row from the known-product-bug group.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. Negatives — SQL-leak tripwires (1 always-on + 1 known-product-bug row)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 9,
          description = "TC_WTAPI_009: procedures?service_id=not-a-uuid — whatever the status, the body must not leak psycopg2/SQL internals")
    public void testNoSqlLeakOnBadServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_009 — leak tripwire: procedures?service_id=not-a-uuid");
        requireAuth();

        Response r = fireMalformed("procedures?service_id=not-a-uuid");
        String body = r.asString();
        ExtentReportManager.logInfo("bad service_id -> " + r.statusCode() + " (body: " + head(body) + ")");
        // Live 2026-07-21: 500 with a GENERIC HTML error page — ugly but not leaking. This row
        // stays always-on so the 500 can never regress from opaque into a psycopg2/SQL dump.
        String leaked = findSqlLeakToken(body);
        Assert.assertNull(leaked,
                "procedures?service_id=not-a-uuid leaked DB internals ('" + leaked + "') to the client — "
                        + "SQL/schema disclosure (status " + r.statusCode() + ", body: " + head(body) + ").");
        ExtentReportManager.logPass("bad service_id error body is opaque (status " + r.statusCode()
                + ", no psycopg2/SQL text).");
    }

    @Test(priority = 9, groups = {"known-product-bug"},
          description = "TC_WTAPI_010: scope-preview with garbage sld_id must not leak psycopg2/SQL (currently leaks the full SELECT — verified backend bug)")
    public void testNoSqlLeakOnGarbageSldId() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_010 — leak tripwire: scope-preview sld_id=garbage");
        requireAuth();

        Response r = fireMalformed("scope-preview sld_id=garbage-not-a-uuid");
        String body = r.asString();
        ExtentReportManager.logInfo("garbage sld_id -> " + r.statusCode() + " (body: " + head(body) + ")");
        // Live 2026-07-21 this 500 body contained psycopg2.errors.InvalidTextRepresentation plus
        // the full "[SQL: SELECT nodes.id, nodes.type, …]" statement — schema disclosure to any
        // authenticated client. Grouped known-product-bug until the backend wraps the error.
        String leaked = findSqlLeakToken(body);
        Assert.assertNull(leaked,
                "scope-preview with garbage sld_id leaked DB internals ('" + leaked + "') — psycopg2 + full SELECT "
                        + "visible to the client (status " + r.statusCode() + ", body: " + head(body) + ").");
        ExtentReportManager.logPass("garbage sld_id error body is opaque (status " + r.statusCode()
                + ") — BUG FIXED, remove this row from the known-product-bug group.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 10. Negatives — missing work_type_id degrades gracefully (1 row, always-on)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 10,
          description = "TC_WTAPI_011: scope-preview without work_type_id — graceful no-op (200 applicable:false/0 assets) or clean 4xx, never 5xx")
    public void testScopePreviewMissingWorkTypeGraceful() {
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_011 — scope-preview missing work_type_id");
        requireAuth();

        JSONObject payload = new JSONObject();
        payload.put("sld_id", WorkTypeCatalog.Z1_SLD_ID);
        payload.put("asset_scope", JSONObject.NULL);   // work_type_id deliberately absent
        Response r = getAuthenticatedRequestSpec()
                .body(payload.toString())
                .when().post(SCOPE_PREVIEW_PATH)
                .then().extract().response();
        int status = r.statusCode();
        String body = r.asString();
        ExtentReportManager.logInfo("missing work_type_id -> " + status + " (body: " + head(body) + ")");

        Assert.assertTrue(status < 500,
                "scope-preview without work_type_id must not 5xx, got " + status + " (body: " + head(body) + ")");
        if (status >= 200 && status < 300) {
            // Live-pinned graceful contract (2026-07-21): 200 {success:true, data:{applicable:false,
            // assets:[], matching_count:0, reason:"no work type / sld"}} — by design, NOT a bug.
            JSONObject root = new JSONObject(body);
            Assert.assertTrue(root.optBoolean("success", false),
                    "graceful no-op must still carry success:true — got: " + head(body));
            JSONObject data = root.optJSONObject("data");
            Assert.assertNotNull(data, "graceful no-op must carry data{} — got: " + head(body));
            Assert.assertFalse(data.optBoolean("applicable", true),
                    "without a work_type_id the preview must be applicable:false — got: " + head(body));
            Assert.assertEquals(data.optInt("matching_count", -1), 0,
                    "without a work_type_id matching_count must be 0 — got: " + head(body));
            JSONArray assets = data.optJSONArray("assets");
            Assert.assertTrue(assets != null && assets.length() == 0,
                    "without a work_type_id assets[] must be empty — got: " + head(body));
            ExtentReportManager.logPass("missing work_type_id -> 200 graceful no-op (applicable:false, 0 assets).");
        } else {
            Assert.assertTrue(status >= 400 && status < 500,
                    "missing work_type_id answered an unexpected status class: " + status + " (body: " + head(body) + ")");
            ExtentReportManager.logPass("missing work_type_id cleanly rejected with " + status + ".");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. Contract OBSERVATION — async delete receipt for a nonexistent id (1 row)
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 11,
          description = "TC_WTAPI_012: DELETE /ir_session/{random-uuid} — 200 async receipt {_mutation.status:received} even for a nonexistent id (pinned observation)")
    public void testDeleteNonexistentUuidAsyncReceipt() {
        String randomId = UUID.randomUUID().toString();
        ExtentReportManager.createTest(AppConstants.MODULE_WORK_ORDERS, FEATURE, "API testing - TC_WTAPI_012 — DELETE nonexistent /ir_session/" + randomId);
        requireAuth();

        // CONTRACT OBSERVATION (live 2026-07-21), not an endorsement: the delete path is an async
        // mutation queue — it answers 200 {"_mutation":{"status":"received"},"id":…} WITHOUT
        // checking the id exists (a fresh random uuid gets the same receipt). Pinned so a move to
        // synchronous 404 semantics — which would change WorkTypeUiBase.apiDeleteWorkOrder() and
        // every cleanup path — is flagged immediately. Requires Content-Type: application/json.
        Response r = getAuthenticatedRequestSpec()
                .body("{}")
                .when().delete("/ir_session/" + randomId)
                .then().extract().response();
        String body = r.asString();
        ExtentReportManager.logInfo("DELETE /ir_session/" + randomId + " -> " + r.statusCode()
                + " (body: " + head(body) + ")");

        Assert.assertEquals(r.statusCode(), 200,
                "async-delete semantics changed: nonexistent-id DELETE now returns " + r.statusCode()
                        + " (was a 200 receipt on 2026-07-21) — re-verify apiDeleteWorkOrder() cleanup paths. Body: "
                        + head(body));
        JSONObject root = new JSONObject(body);
        JSONObject mutation = root.optJSONObject("_mutation");
        Assert.assertNotNull(mutation,
                "delete receipt must carry _mutation{} — got: " + head(body));
        Assert.assertEquals(mutation.optString("status"), "received",
                "delete receipt _mutation.status must be 'received' — got: " + head(body));
        Assert.assertEquals(root.optString("id"), randomId,
                "delete receipt must echo the requested id — got: " + head(body));
        ExtentReportManager.logPass("DELETE nonexistent id -> 200 {_mutation.status:'received', id echoed} — "
                + "async receipt contract unchanged.");
    }
}
