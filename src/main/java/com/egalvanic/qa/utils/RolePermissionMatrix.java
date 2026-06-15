package com.egalvanic.qa.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loads the production RBAC permission matrix
 * ({@code testcase/prod_permissions-by-role_*.csv}) into an in-memory model
 * keyed by role, so tests can assert that each role's <em>live</em> permission
 * set (from {@code GET /api/auth/me}) matches the documented matrix.
 *
 * <p>CSV columns: {@code role_id, role_name, permission_name,
 * permission_resource, permission_action}. The {@code permission_name} column
 * (e.g. {@code accounts.view}, {@code features.assets.view}, {@code platform.web})
 * is the exact string the backend returns in the {@code permissions[]} array of
 * {@code /auth/me} and that the React frontend uses for UI gating, so it is the
 * value we compare against.</p>
 *
 * <p>Robust to the two role_id encodings present in the export (some quoted,
 * some bare) and to rows with a blank {@code permission_action}.</p>
 */
public final class RolePermissionMatrix {

    /** Default location of the prod export, relative to the project root. */
    public static final String DEFAULT_CSV_PATH =
            "testcase/prod_permissions-by-role_202606151113.csv";

    /** One role's identity + the full set of permission names granted to it. */
    public static final class RoleSpec {
        public final String roleId;
        public final String roleName;
        public final Set<String> permissions; // sorted, unmodifiable

        RoleSpec(String roleId, String roleName, Set<String> permissions) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.permissions = Collections.unmodifiableSet(permissions);
        }

        /** True if this role is granted {@code platform.web} (drives web access). */
        public boolean hasWebAccess() {
            return permissions.contains("platform.web");
        }
    }

    private final Map<String, RoleSpec> byRoleName;   // insertion-ordered
    private final int totalGrants;
    private final Set<String> allPermissions;          // sorted union across all roles

    private RolePermissionMatrix(Map<String, RoleSpec> byRoleName, int totalGrants) {
        this.byRoleName = byRoleName;
        this.totalGrants = totalGrants;
        Set<String> union = new TreeSet<>();
        for (RoleSpec spec : byRoleName.values()) {
            union.addAll(spec.permissions);
        }
        this.allPermissions = Collections.unmodifiableSet(union);
    }

    /** Load from the default prod CSV path (relative to project root). */
    public static RolePermissionMatrix loadDefault() {
        return load(DEFAULT_CSV_PATH);
    }

    /** Load and parse the permission matrix from the given CSV file. */
    public static RolePermissionMatrix load(String csvPath) {
        Path path = resolveCsv(csvPath);

        // role_name -> roleId, and role_name -> sorted permission set
        Map<String, String> roleIds = new LinkedHashMap<>();
        Map<String, Set<String>> perms = new LinkedHashMap<>();
        int grants = 0;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = br.readLine(); // header
            if (line == null) {
                throw new IllegalStateException("RBAC CSV is empty: " + path);
            }
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> cols = splitCsv(line);
                if (cols.size() < 3) continue; // malformed row, skip defensively

                String roleId = unquote(cols.get(0));
                String roleName = unquote(cols.get(1));
                String permissionName = unquote(cols.get(2));
                if (roleName.isEmpty() || permissionName.isEmpty()) continue;

                roleIds.putIfAbsent(roleName, roleId);
                perms.computeIfAbsent(roleName, k -> new TreeSet<>()).add(permissionName);
                grants++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading RBAC CSV: " + path, e);
        }

        Map<String, RoleSpec> specs = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : perms.entrySet()) {
            String roleName = e.getKey();
            specs.put(roleName, new RoleSpec(roleIds.get(roleName), roleName, e.getValue()));
        }
        return new RolePermissionMatrix(specs, grants);
    }

    /**
     * Resolve which CSV to read. An explicit/existing path is honoured verbatim
     * (reproducible, and respects the {@code RBAC_CSV_PATH} override). If the literal
     * file is missing, falls back to the newest {@code prod_permissions-by-role_*.csv}
     * in the same directory, so a re-export under a new timestamped name is picked up
     * rather than silently ignored (matching the {@code _*.csv} contract in the docs).
     */
    static Path resolveCsv(String csvPath) {
        Path path = Paths.get(csvPath);
        if (Files.exists(path)) return path;

        Path dir = path.getParent() != null ? path.getParent() : Paths.get(".");
        Path newest = null;
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> ds =
                         Files.newDirectoryStream(dir, "prod_permissions-by-role_*.csv")) {
                for (Path p : ds) {
                    if (newest == null || p.getFileName().toString()
                            .compareTo(newest.getFileName().toString()) > 0) {
                        newest = p; // timestamped names sort chronologically
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed scanning for RBAC CSV in " + dir, e);
            }
        }
        if (newest != null) {
            System.out.println("[RBAC] CSV '" + csvPath + "' not found; using newest export: " + newest);
            return newest;
        }
        throw new IllegalStateException(
                "RBAC permission matrix CSV not found at: " + path.toAbsolutePath()
                + " and no prod_permissions-by-role_*.csv in " + dir.toAbsolutePath()
                + " (set RBAC_CSV_PATH or place the prod export there)");
    }

    /** All role names, in the order first seen in the CSV. */
    public Set<String> roleNames() {
        return Collections.unmodifiableSet(byRoleName.keySet());
    }

    /**
     * The full permission vocabulary: every distinct {@code permission_name}
     * granted to at least one role (sorted). This is the universe against which
     * each role is checked cell-by-cell (granted ⇒ must be present, otherwise
     * ⇒ must be absent).
     */
    public Set<String> allPermissions() {
        return allPermissions;
    }

    /** Spec for a role, or null if the CSV has no such role. */
    public RoleSpec forRole(String roleName) {
        return byRoleName.get(roleName);
    }

    /** Total number of role&times;permission grants across the whole matrix. */
    public int totalGrants() {
        return totalGrants;
    }

    /** Number of distinct roles in the matrix. */
    public int roleCount() {
        return byRoleName.size();
    }

    // ---- CSV helpers -------------------------------------------------------

    /**
     * Minimal CSV splitter that honours double-quoted fields (which may contain
     * commas). Sufficient for this export, which only quotes some role_id values.
     */
    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // doubled quote inside a quoted field => literal quote
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String unquote(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t.trim();
    }
}
