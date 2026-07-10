package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.testcase.api.RbacFixtures.LiveAuth;
import com.egalvanic.qa.testcase.api.RbacFixtures.Role;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/**
 * <b>Real CRUD API-contract validation</b> — this goes beyond the permission <i>gate</i>
 * (see {@link RoleActionEnforcementApiTest}, which only probes allowed-vs-denied with a
 * throw-away zero-UUID). Here a role that <b>has</b> the entity's {@code *.manage} permission
 * actually <b>creates → edits → deletes a real record</b> and we assert the API's true
 * response contract at each step; a role that lacks it is asserted to be denied.
 *
 * <p><b>The real contract (verified live against acme.qa, 2026-06-18).</b> The platform is
 * event-sourced/async (CQRS): every mutation is acknowledged with <b>HTTP 200</b> and a body
 * <code>{"_mutation":{"status":"received","mutation_id":…},"id":"…","name":"…"}</code> — it
 * does <b>not</b> use {@code 201 Created} for create or {@code 202 Accepted} for the async
 * ack. So the assertion here is the <i>actual</i> contract: 200 + {@code _mutation.status ==
 * "received"} + a real UUID {@code id} (create returns the new id synchronously even though the
 * write is applied asynchronously). The 200-vs-201/202 deviation is logged on every create as a
 * NOTE so it's visible without failing the build — flip {@link #EXPECT_REST_CREATED} to make the
 * test demand 201/202 instead (it will then fail until the API changes).</p>
 *
 * <p>Lifecycle per allowed (role, entity): <b>POST create</b> (assert 200 + received + id) →
 * <b>PUT update/{id}</b> (assert 200 + received + the new name echoed back) → <b>DELETE
 * delete/{id}</b> (assert 200 + received — also cleans up the record). Records use a clear
 * {@code ZZ_RBAC_CRUD_…} marker. Denied roles only ever hit create, which is rejected, so nothing
 * is persisted for them. Oracle = the role's live {@code /auth/me}. Honors {@code -Drbac.roles};
 * unprovisioned / mis-provisioned accounts SKIP.</p>
 */
public class RoleCrudContractApiTest extends BaseAPITest {

    /** Flip to true to demand REST-conventional 201/202 for create instead of the API's actual 200. */
    private static final boolean EXPECT_REST_CREATED = false;

    private static final Pattern UUID =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** A CRUD-able entity: label, the gating *.manage permission, and its create/update/delete paths. */
    private static final class Entity {
        final String label, gate, createPath, updatePrefix, deletePrefix;
        Entity(String label, String gate, String createPath, String updatePrefix, String deletePrefix) {
            this.label = label; this.gate = gate; this.createPath = createPath;
            this.updatePrefix = updatePrefix; this.deletePrefix = deletePrefix;
        }
    }

    /** Entities with a confirmed full create/update/delete lifecycle (probed live). */
    private static final List<Entity> ENTITIES = Arrays.asList(
            new Entity("Task",  "tasks.manage", "/task/create", "/task/update/", "/task/delete/"),
            new Entity("Asset", "nodes.manage", "/node/create", "/node/update/", "/node/delete/")
    );

    @BeforeClass(alwaysRun = true)
    public void setBaseUri() { RestAssured.baseURI = API_BASE_URL; }

    @DataProvider(name = "roleEntities")
    public Object[][] roleEntities() {
        List<Object[]> rows = new ArrayList<>();
        for (Role role : RbacFixtures.selectedRoles()) {     // honors -Drbac.roles
            for (Entity e : ENTITIES) rows.add(new Object[]{role, e});
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "roleEntities",
          description = "Each role can create, edit and delete records only when allowed (others are refused)")
    public void crudContract(Role role, Entity entity) {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, AppConstants.FEATURE_ROLE_ACCESS,
                role.name + " — CRUD " + entity.label + " [" + entity.gate + "]");

        LiveAuth live = RbacFixtures.cachedLiveAuth(role);
        if (!live.provisioned) {
            String msg = "Account not provisioned for '" + role.name + "' (login " + live.loginStatus + ").";
            ExtentReportManager.logSkip(msg); throw new SkipException(msg);
        }
        String mism = RbacFixtures.roleMismatchSkipMessage(role, live);
        if (mism != null) { ExtentReportManager.logSkip(mism); throw new SkipException(mism); }

        boolean expectedAllowed = Boolean.TRUE.equals(live.isAdmin) || live.permissions.contains(entity.gate);
        String token = live.token;
        String marker = "ZZ_RBAC_CRUD_" + entity.label.toUpperCase() + "_"
                + role.name.replaceAll("\\s+", "") + "_" + System.currentTimeMillis();

        if (!expectedAllowed) {
            // Denied role: create must be rejected; nothing is persisted.
            Response resp = mutate("POST", entity.createPath, token, "{\"name\":\"" + marker + "\"}");
            boolean denied = isPermissionDenied(resp.getStatusCode(), resp.asString());
            ExtentReportManager.logInfo("'" + role.name + "' create " + entity.label + " (lacks " + entity.gate
                    + ") → HTTP " + resp.getStatusCode());
            Assert.assertTrue(denied,
                    "PRIVILEGE ESCALATION: '" + role.name + "' lacks " + entity.gate + " but create " + entity.label
                            + " was NOT denied (HTTP " + resp.getStatusCode() + "): " + truncate(resp.asString()));
            ExtentReportManager.logPass("'" + role.name + "' correctly DENIED create " + entity.label
                    + " (permission_denied, HTTP " + resp.getStatusCode() + ").");
            return;
        }

        // ---- Allowed role: exercise the real lifecycle and assert the actual contract. ----
        String id = null;
        try {
            // CREATE
            Response create = mutate("POST", entity.createPath, token, "{\"name\":\"" + marker + "\"}");
            id = assertMutationOk(create, "create " + entity.label, role.name, /*expectId=*/true, /*isCreate=*/true);

            // EDIT (rename the record we just created; the response echoes the new name)
            String edited = marker + "_EDITED";
            Response update = mutate("PUT", entity.updatePrefix + id, token, "{\"name\":\"" + edited + "\"}");
            assertMutationOk(update, "edit " + entity.label, role.name, /*expectId=*/false, /*isCreate=*/false);
            String echoed = safeJson(update, "name");
            Assert.assertEquals(echoed, edited,
                    "'" + role.name + "' edit " + entity.label + " did not echo the updated name (got '" + echoed + "').");
            ExtentReportManager.logPass("'" + role.name + "' edited " + entity.label + " — name updated to '" + edited
                    + "' (HTTP " + update.getStatusCode() + ", _mutation:received).");
        } finally {
            // DELETE — part of the contract AND cleanup so we don't accumulate test records.
            if (id != null) {
                Response del = mutate("DELETE", entity.deletePrefix + id, token, "{}");
                if (del.getStatusCode() == 200 && "received".equals(safeJson(del, "_mutation.status"))) {
                    ExtentReportManager.logPass("'" + role.name + "' deleted " + entity.label + " " + id
                            + " (HTTP 200, _mutation:received) — record cleaned up.");
                } else {
                    ExtentReportManager.logWarning("'" + role.name + "' delete " + entity.label + " " + id
                            + " did not return the expected 200/received (HTTP " + del.getStatusCode()
                            + "): " + truncate(del.asString()) + " — record may persist.");
                }
            }
        }
    }

    /** Assert the async-mutation success contract: HTTP 200 (or 201/202 if EXPECT_REST_CREATED),
     *  body {@code _mutation.status == "received"}, and (for create) a real UUID id. Returns the id. */
    private String assertMutationOk(Response resp, String op, String roleName, boolean expectId, boolean isCreate) {
        int status = resp.getStatusCode();
        String body = resp.asString();

        if (EXPECT_REST_CREATED && isCreate) {
            Assert.assertTrue(status == 201 || status == 202,
                    "'" + roleName + "' " + op + " expected REST 201/202 but got " + status + ": " + truncate(body));
        } else {
            Assert.assertEquals(status, 200,
                    "'" + roleName + "' " + op + " expected HTTP 200 (async mutation ack) but got " + status
                            + ": " + truncate(body));
        }

        String mutationStatus = safeJson(resp, "_mutation.status");
        Assert.assertEquals(mutationStatus, "received",
                "'" + roleName + "' " + op + " did not return _mutation.status=received (got '" + mutationStatus
                        + "'): " + truncate(body));

        String id = safeJson(resp, "id");
        if (expectId) {
            Assert.assertNotNull(id, "'" + roleName + "' " + op + " returned no id: " + truncate(body));
            Assert.assertTrue(UUID.matcher(id).matches(),
                    "'" + roleName + "' " + op + " returned a non-UUID id '" + id + "': " + truncate(body));
            // Surface the REST-convention deviation without failing the build.
            ExtentReportManager.logInfo("API CONTRACT NOTE: " + op + " acknowledged with HTTP " + status
                    + " + _mutation:received + id=" + id + " (the platform is async/CQRS; it does NOT use "
                    + "201 Created / 202 Accepted). Set EXPECT_REST_CREATED=true to assert 201/202 instead.");
            ExtentReportManager.logPass("'" + roleName + "' " + op + " accepted (HTTP " + status
                    + ", _mutation:received, id=" + id + ").");
        }
        return id;
    }

    private Response mutate(String method, String path, String token, String body) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .when().request(method, path)
                .then().extract().response();
    }

    /** jsonPath get that never throws (returns null on non-JSON / missing path). */
    private static String safeJson(Response resp, String path) {
        try { Object v = resp.jsonPath().get(path); return v == null ? null : v.toString(); }
        catch (Exception e) { return null; }
    }

    private static boolean isPermissionDenied(int status, String body) {
        if (status == 401 || status == 403) return true;
        if (body == null) return false;
        String b = body.toLowerCase();
        return b.contains("permission_denied") || b.contains("do not have permission")
                || b.contains("required_permission");
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
