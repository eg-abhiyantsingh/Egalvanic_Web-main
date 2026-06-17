package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

/**
 * Shared fixtures for the RBAC test classes: the canonical role roster (name +
 * credentials + expected role_id), the documented prod-vs-QA permission drift,
 * and a one-shot live-auth fetch ({@code POST /auth/login} → {@code GET /auth/me}).
 *
 * <p>Centralising these here keeps {@link RoleBasedPermissionContractTest}
 * (role-level set comparison) and {@link RolePermissionMatrixCellTest}
 * (per-cell coverage) in agreement — there is exactly one place to update a
 * credential, a role_id, or a known drift entry.</p>
 */
public final class RbacFixtures {

    private RbacFixtures() {}

    /** A role under test: display name, login, and the role_id expected from the matrix. */
    public static final class Role {
        public final String name;
        public final String email;
        public final String password;
        public final String roleId;

        Role(String name, String email, String password, String roleId) {
            this.name = name;
            this.email = email;
            this.password = password;
            this.roleId = roleId;
        }

        @Override public String toString() { return name; }
    }

    /** All roles in the prod matrix, most → least privileged. */
    public static final List<Role> ROLES = Collections.unmodifiableList(Arrays.asList(
            new Role("Admin", AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD,
                    "b60006dd-3cb6-455b-bf90-5d83198321b9"),
            new Role("Project Manager", AppConstants.PM_EMAIL, AppConstants.PM_PASSWORD,
                    "242dbe6a-063c-4057-ac4a-a0d32a773fc8"),
            new Role("Technician", AppConstants.TECH_EMAIL, AppConstants.TECH_PASSWORD,
                    "e84a0fbb-0b89-4433-807c-4b7886e1cd6d"),
            new Role("Electrical Engineer", AppConstants.EE_EMAIL, AppConstants.EE_PASSWORD,
                    "fd6b624e-3847-408f-848d-55779a4a3328"),
            new Role("Facility Manager", AppConstants.FM_EMAIL, AppConstants.FM_PASSWORD,
                    "54021b71-a055-4c58-91e1-05c705f643f4"),
            new Role("Account Manager", AppConstants.AM_EMAIL, AppConstants.AM_PASSWORD,
                    "92f38105-0c53-475b-ae57-52dcc4a96f11"),
            new Role("Client Portal", AppConstants.CP_EMAIL, AppConstants.CP_PASSWORD,
                    "2a85145f-31ca-4e2c-8dd2-82c958cd6380")
    ));

    /**
     * Known prod-vs-QA drift: permissions present in the prod CSV export but not
     * granted on the QA tenant (acme.qa). Verified live 2026-06-15. Environment
     * config lag, not a code regression — tolerated (logged/SKIPPED), while any
     * deviation outside this map hard-fails.
     */
    public static final Map<String, Set<String>> KNOWN_QA_DRIFT;
    static {
        Map<String, Set<String>> m = new HashMap<>();
        m.put("Admin", new HashSet<>(Arrays.asList("features.equipment_insights.view")));
        m.put("Project Manager", new HashSet<>(Arrays.asList("accounts.view")));
        m.put("Facility Manager", new HashSet<>(Arrays.asList("features.locations.view")));
        KNOWN_QA_DRIFT = Collections.unmodifiableMap(m);
    }

    public static boolean isKnownDrift(String roleName, String permission) {
        return KNOWN_QA_DRIFT.getOrDefault(roleName, Collections.emptySet()).contains(permission);
    }

    /**
     * If the given account authenticates as a DIFFERENT role than expected, return a message
     * explaining it (else null). A test account that logs in as another role is a broken fixture —
     * you cannot verify role X using role Y's session — so callers should SKIP (loudly), not fail,
     * exactly as for an unprovisioned account. Example seen 2026-06-17: the +admin account currently
     * logs in as "Project Manager".
     */
    public static String roleMismatchSkipMessage(Role role, LiveAuth live) {
        if (live != null && live.provisioned && live.roleName != null && !role.name.equals(live.roleName)) {
            return "Test account for '" + role.name + "' is mis-provisioned: it currently logs in as '"
                    + live.roleName + "'. Fix the QA account's role (or point " + role.name
                    + "'s *_EMAIL at a correct account) to restore coverage for this role.";
        }
        return null;
    }

    /**
     * Documented per-environment role_id differences: the QA tenant seeded some roles with a
     * different UUID than the prod export, even though the role name + permission set are
     * identical. Verified live 2026-06-15: QA "Account Manager" is 392a2233… vs prod 92f38105….
     * The live identity check expects the QA id where one is listed here, else the CSV/prod id —
     * so it still catches an account mapped to the WRONG role, without false-flagging this benign
     * UUID difference.
     */
    public static final Map<String, String> QA_ROLE_ID_OVERRIDE;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("Account Manager", "392a2233-e4a3-4322-a440-7fd62b4bed7e");
        QA_ROLE_ID_OVERRIDE = Collections.unmodifiableMap(m);
    }

    /** The role_id we expect {@code /auth/me} to return on QA (override if listed, else the prod/CSV id). */
    public static String expectedLiveRoleId(String roleName, String csvRoleId) {
        return QA_ROLE_ID_OVERRIDE.getOrDefault(roleName, csvRoleId);
    }

    /** role name → expected role_id, derived from {@link #ROLES}. */
    public static Map<String, String> expectedRoleIds() {
        Map<String, String> ids = new LinkedHashMap<>();
        for (Role r : ROLES) ids.put(r.name, r.roleId);
        return ids;
    }

    /** Snapshot of one role's live identity + permission set from {@code /auth/me}. */
    public static final class LiveAuth {
        public final boolean provisioned;   // login succeeded and /auth/me returned 200
        public final int loginStatus;
        public final Set<String> permissions;
        public final String roleId;
        public final String roleName;
        public final Boolean isAdmin;
        public final Boolean hasWebAccess;
        public final String token;          // bearer access_token (for authenticated API calls)

        private LiveAuth(boolean provisioned, int loginStatus, Set<String> permissions,
                         String roleId, String roleName, Boolean isAdmin, Boolean hasWebAccess, String token) {
            this.provisioned = provisioned;
            this.loginStatus = loginStatus;
            this.permissions = permissions == null
                    ? Collections.emptySet() : Collections.unmodifiableSet(permissions);
            this.roleId = roleId;
            this.roleName = roleName;
            this.isAdmin = isAdmin;
            this.hasWebAccess = hasWebAccess;
            this.token = token;
        }

        static LiveAuth unprovisioned(int loginStatus) {
            return new LiveAuth(false, loginStatus, null, null, null, null, null, null);
        }

        /**
         * Whether this snapshot is an <em>authoritative</em> outcome safe to cache for the
         * whole run: a successful login (provisioned) or a genuine auth rejection (401/403).
         * Transient/inconclusive outcomes (timeout, 5xx, parse error, empty token → status
         * 0/-2/5xx) are NOT cacheable, so the next caller re-attempts instead of inheriting
         * a poisoned SKIP.
         */
        public boolean cacheable() {
            return provisioned || loginStatus == 401 || loginStatus == 403;
        }
    }

    /** Process-wide cache so the whole suite logs in each role at most once. */
    private static final Map<String, LiveAuth> CACHE = new HashMap<>();

    /**
     * Cached variant of {@link #fetchLiveAuth(Role)} — the first caller in the
     * JVM performs the network round-trip; later callers (other test class, other
     * data-provider row) reuse it. Keeps total logins to one per role across the
     * suite, avoiding Cognito throttling.
     *
     * <p>Only <em>authoritative</em> results ({@link LiveAuth#cacheable()}: a 200
     * login or a genuine 401/403) are memoized. A transient blip (timeout, 5xx,
     * parse error) is NOT cached, so the next consumer re-attempts the role rather
     * than inheriting a poisoned SKIP that would silently drop ~113 cells under a
     * still-green run.</p>
     */
    public static synchronized LiveAuth cachedLiveAuth(Role role) {
        LiveAuth cached = CACHE.get(role.name);
        if (cached != null && cached.cacheable()) return cached;
        LiveAuth fresh = fetchLiveAuth(role);
        if (fresh.cacheable()) CACHE.put(role.name, fresh);
        return fresh;
    }

    private static boolean isTransient(int code) {
        return code == 0 || code == 429 || code >= 500;
    }

    /**
     * Execute a request with a single retry after a short pause, but only on a
     * <em>transient</em> status (timeout/throttle/5xx) or a transport exception —
     * never on a deterministic 401/403 (which would just waste time). Returns the
     * last response, or null if both attempts threw.
     */
    private static Response callWithRetry(Supplier<Response> call) {
        Response r = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                r = call.get();
            } catch (Exception e) {
                r = null; // transport failure — treat as transient and retry
            }
            if (r != null && !isTransient(r.getStatusCode())) return r;
            if (attempt < 2) {
                try { Thread.sleep(2000); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        return r;
    }

    /**
     * Log in as the role and fetch its live permission set. Requires
     * {@code RestAssured.baseURI} to already point at {BASE_URL}/api. Never throws:
     * returns an unprovisioned snapshot if the account does not exist or auth fails,
     * so callers can SKIP rather than error. Both the login and {@code /auth/me}
     * legs retry once on a transient failure.
     */
    public static LiveAuth fetchLiveAuth(Role role) {
        JSONObject payload = new JSONObject();
        payload.put("email", role.email);
        payload.put("password", role.password);
        payload.put("subdomain", AppConstants.VALID_COMPANY_CODE);

        Response login = callWithRetry(() -> given()
                .contentType(ContentType.JSON)
                .body(payload.toString())
                .when().post("/auth/login")
                .then().extract().response());
        if (login == null) return LiveAuth.unprovisioned(0);            // transport failure (not cached)
        int code = login.getStatusCode();
        if (code != 200) return LiveAuth.unprovisioned(code);
        String body = login.asString();
        if (body == null || body.trim().startsWith("<")) return LiveAuth.unprovisioned(code);
        String token;
        try { token = login.jsonPath().getString("access_token"); }
        catch (Exception e) { token = null; }
        if (token == null || token.isEmpty()) return LiveAuth.unprovisioned(code);

        final String bearer = token;
        Response me = callWithRetry(() -> given()
                .contentType(ContentType.JSON)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + bearer)
                .when().get("/auth/me")
                .then().extract().response());
        if (me == null) return LiveAuth.unprovisioned(0);
        if (me.getStatusCode() != 200) return LiveAuth.unprovisioned(me.getStatusCode());

        try {
            List<String> perms = me.jsonPath().getList("permissions");
            Set<String> permSet = new TreeSet<>(perms == null ? new ArrayList<>() : perms);
            // Read the boolean flags as BOXED values: getBoolean() returns a primitive and
            // NPEs (then gets swallowed → silent UNPROVISIONED) if the field is absent. get()
            // returns the boxed value or null, which the consumers already treat as a failure.
            Boolean isAdmin = me.jsonPath().get("is_admin");
            Boolean hasWeb = me.jsonPath().get("has_web_access");
            return new LiveAuth(true, code, permSet,
                    me.jsonPath().getString("roles[0].id"),
                    me.jsonPath().getString("roles[0].name"),
                    isAdmin, hasWeb, bearer);
        } catch (Exception e) {
            // Body parsing failed unexpectedly — surface as a distinct, non-cacheable
            // sentinel (-2), never as plain 'not provisioned' which would silently SKIP
            // a genuine 200 role.
            return LiveAuth.unprovisioned(-2);
        }
    }
}
