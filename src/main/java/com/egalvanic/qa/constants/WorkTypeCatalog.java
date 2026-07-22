package com.egalvanic.qa.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative catalog of the v1.35 (ZP-3000 Services / Procedures-V2) Work Type
 * dropdown on the Create New Work Order dialog (/sessions).
 *
 * Live-verified 2026-07-21 on acme.qa.egalvanic.ai (site Z1) against BOTH surfaces:
 *  - GET /api/procedures-v2/services  (13 services: id/key/name/type/de_energized/procedure_count)
 *  - the Work Type MUI Autocomplete   (those 13 + "General", exact display order below)
 *
 * The dropdown therefore has 14 options = 13 service work types + "General" (no service,
 * legacy-neutral WO). Each service maps to one of six UX families that drive the entire
 * post-create detail-page contract (tabs, asset-grid columns, completion surface).
 *
 * Detail-page family contracts below were live-captured 2026-07-21 from the six Z1
 * fixture WOs (docs/changelogs/2026-07-21-service-wo-fixture-set-z1.md).
 *
 * If an assertion against this catalog fails, FIRST re-pull GET /api/procedures-v2/services —
 * the product catalog may have drifted; update this file in the same commit as the fix.
 */
public final class WorkTypeCatalog {

    /** The six service-type UX families + GENERAL (no service attached). */
    public enum Family {
        AF        (Arrays.asList("Assets", "SLD", "Equipment Designations", "Issues", "Attachments"), "Arc Flash"),
        IR        (Arrays.asList("Assets", "Issues", "IR Photos", "Attachments"),                     "IR Photos"),
        COM       (Arrays.asList("Assets", "Condition Assessment", "Issues", "Attachments"),          "C.O.M."),
        CHECKLIST (Arrays.asList("Assets", "Tasks", "Issues", "Attachments"),                         "Tasks"),
        SCHEDULE  (Arrays.asList("Assets", "Panel Schedules", "Issues", "Attachments"),               "Schedule"),
        PM_FORMS  (Arrays.asList("Assets", "Forms", "Issues", "Attachments"),                         "Forms"),
        /** No service: detail contract intentionally unpinned — WorkTypeAutoScheduleEdgeTestNG pins it live. */
        GENERAL   (null, null);

        /** Expected detail-page tab strip, in order, counts stripped ("Assets46" -> "Assets"). Null = not yet pinned. */
        public final List<String> expectedTabs;
        /** The asset-grid column unique to this family (beyond Asset/Asset Class/QR Code/Location/Issues). Null = none pinned. */
        public final String typeSpecificColumn;

        Family(List<String> expectedTabs, String typeSpecificColumn) {
            this.expectedTabs = expectedTabs == null ? null : Collections.unmodifiableList(expectedTabs);
            this.typeSpecificColumn = typeSpecificColumn;
        }
    }

    /** One Work Type dropdown option and everything the product promises about it. */
    public static final class WorkTypeProfile {
        /** Exact dropdown label (and services-API "name"). */
        public final String name;
        /** services-API "key" (null for General). NOTE: NETA Testing's key is "de-energized-testing" — name and key deliberately differ. */
        public final String apiKey;
        /** services-API "id" on tenant acme (uuid5-style, stable per tenant; null for General). */
        public final String serviceId;
        public final Family family;
        /** de_energized flag from the services API (drives Auto-Schedule semantics per dev videos). */
        public final boolean deEnergized;
        /**
         * procedure_count observed 2026-07-21. DRIFTS as admins edit procedures — never assert
         * exact equality against live; use {@link #expectsNoProceduresNotice()} semantics instead.
         */
        public final int procedureCountAtCapture;

        WorkTypeProfile(String name, String apiKey, String serviceId, Family family,
                        boolean deEnergized, int procedureCountAtCapture) {
            this.name = name;
            this.apiKey = apiKey;
            this.serviceId = serviceId;
            this.family = family;
            this.deEnergized = deEnergized;
            this.procedureCountAtCapture = procedureCountAtCapture;
        }

        public boolean isService() { return serviceId != null; }

        /**
         * Dialog contract (live 2026-07-21): a service with 0 procedures shows
         * "This work type has no procedures configured — no assets will be pulled in automatically."
         * instead of the "N matching assets" scope preview. General shows NEITHER (no service, no preview).
         */
        public boolean expectsNoProceduresNotice() { return isService() && procedureCountAtCapture == 0; }

        /** True when selecting this type fires POST /api/ir_session/scope-preview and renders "N matching assets". */
        public boolean expectsScopePreview() { return isService() && procedureCountAtCapture > 0; }

        @Override public String toString() { return name + " [" + family + (deEnergized ? ", de-energized" : "") + "]"; }
    }

    /** All 14 dropdown options in EXACT display order (live 2026-07-21). */
    public static final List<WorkTypeProfile> ALL;
    /** The 13 service work types (ALL minus General), display order. */
    public static final List<WorkTypeProfile> SERVICES;
    /** "General" — no service, legacy-neutral WO. */
    public static final WorkTypeProfile GENERAL;

    /** Site whose SLD backs all pinned scope/fixture facts. */
    public static final String FIXTURE_SITE = "Z1";
    /** Z1's sld_id as sent by the create dialog's scope-preview calls (live 2026-07-21). */
    public static final String Z1_SLD_ID = "f5be0573-dd42-44de-906f-534e72c08eb0";

    /** Z1 fixture WO name per family (docs/changelogs/2026-07-21-service-wo-fixture-set-z1.md). */
    public static final Map<Family, String> Z1_FIXTURE_WO_NAMES;
    /** Z1 fixture ir_session id per family (deep-link /sessions/{id}). */
    public static final Map<Family, String> Z1_FIXTURE_SESSION_IDS;

    static {
        List<WorkTypeProfile> all = new ArrayList<>();
        all.add(new WorkTypeProfile("Arc Flash Data Collection",       "arc-flash-study",                "d625cfa0-5447-52c5-858e-9ecd5c84d0fb", Family.AF,        false, 35));
        all.add(new WorkTypeProfile("Arc Flash Label Placement",       "arc-flash-label-placement",      "9de69871-ad71-56f4-8f04-515b5738770b", Family.CHECKLIST, false, 13));
        all.add(new WorkTypeProfile("Cleaning",                        "cleaning",                       "180c4243-25df-581c-895a-9e883f38948f", Family.PM_FORMS,  true,  18));
        all.add(new WorkTypeProfile("Clean, Tighten, Torque",          "clean-tighten-torque",           "8e578df1-2b96-5733-8f0e-c00fef0a92b8", Family.PM_FORMS,  true,  19));
        all.add(new WorkTypeProfile("Condition Assessment",            "condition-assessment",           "173c2ca2-8e86-5c95-9b1f-0724ddaccd8b", Family.COM,       false, 30));
        all.add(new WorkTypeProfile("De-Energized Visual Inspection",  "de-energized-visual-inspection", "01ad81ff-63fe-507e-beb0-305d7f67dad9", Family.PM_FORMS,  true,  19));
        all.add(new WorkTypeProfile("DGA / Fluid Sample Analysis",     "dga-fluid-sample-analysis",      "5dff8199-3579-56c6-b81d-dc4e9b4dcd3d", Family.PM_FORMS,  false, 1));
        all.add(new WorkTypeProfile("Infrared Thermography",           "infrared-thermography",          "3b732d14-461c-54a7-8e30-70391bd34dd6", Family.IR,        false, 30));
        all.add(new WorkTypeProfile("Insulation Resistance Testing",   "insulation-resistance-testing",  "d9c9efef-914f-5656-b510-e156bd07ba63", Family.PM_FORMS,  true,  17));
        all.add(new WorkTypeProfile("NETA Testing",                    "de-energized-testing",           "0d914f81-a750-5833-8c46-5c71064f676e", Family.PM_FORMS,  true,  19));
        all.add(new WorkTypeProfile("Panel Schedule Updates",          "panel-schedule-updates",         "3f92b954-8d88-5045-83a8-ee7d9ace504d", Family.SCHEDULE,  false, 2));
        all.add(new WorkTypeProfile("Shutdown (Composite)",            "composite-shutdown-emp",         "f9fb8d4a-2ccd-5bdb-baac-278dc4dc6cfb", Family.PM_FORMS,  true,  0));
        all.add(new WorkTypeProfile("UPS Maintenance",                 "ups-maintenance",                "8c5cf34c-ed04-5c5e-9bff-973410762b13", Family.PM_FORMS,  false, 1));
        GENERAL = new WorkTypeProfile("General", null, null, Family.GENERAL, false, -1);
        SERVICES = Collections.unmodifiableList(new ArrayList<>(all));
        all.add(GENERAL);
        ALL = Collections.unmodifiableList(all);

        Map<Family, String> names = new LinkedHashMap<>();
        names.put(Family.AF,        "AF_DataCollection_WO_QA_2026-07-21");
        names.put(Family.IR,        "IR_WO_QA_2026-07-21");
        names.put(Family.COM,       "COM_WO_QA_2026-07-21");
        names.put(Family.CHECKLIST, "AFLabel_Checklist_WO_QA_2026-07-21");
        names.put(Family.SCHEDULE,  "PanelSchedule_WO_QA_2026-07-21");
        names.put(Family.PM_FORMS,  "Cleaning_WO_QA_2026-07-20");
        Z1_FIXTURE_WO_NAMES = Collections.unmodifiableMap(names);

        Map<Family, String> ids = new LinkedHashMap<>();
        ids.put(Family.AF,        "9286797b-f332-478b-94d8-a09bdcb1b94c");
        ids.put(Family.IR,        "2b217cd3-a05d-447f-979a-45f069331510");
        ids.put(Family.COM,       "7a12c5e2-c83d-4a47-86cc-c5d37a9b2439");
        ids.put(Family.CHECKLIST, "7b1f9d0b-b3fe-49ec-8a28-b201d0667bbe");
        ids.put(Family.SCHEDULE,  "fecde93a-ce99-4d39-bdc0-c3c0988f28c8");
        ids.put(Family.PM_FORMS,  "33c471bd-029a-447f-989a-85d525a41eb1");
        Z1_FIXTURE_SESSION_IDS = Collections.unmodifiableMap(ids);
    }

    public static WorkTypeProfile byName(String name) {
        for (WorkTypeProfile p : ALL) if (p.name.equals(name)) return p;
        throw new IllegalArgumentException("Unknown work type '" + name + "' — catalog drifted? Re-pull /api/procedures-v2/services");
    }

    public static WorkTypeProfile byKey(String apiKey) {
        for (WorkTypeProfile p : SERVICES) if (p.apiKey.equals(apiKey)) return p;
        throw new IllegalArgumentException("Unknown service key '" + apiKey + "' — catalog drifted? Re-pull /api/procedures-v2/services");
    }

    public static List<WorkTypeProfile> byFamily(Family family) {
        List<WorkTypeProfile> out = new ArrayList<>();
        for (WorkTypeProfile p : ALL) if (p.family == family) out.add(p);
        return out;
    }

    /** The de-energized services (Auto-Schedule semantics differ for these per the ZP-3000 dev videos). */
    public static List<WorkTypeProfile> deEnergizedServices() {
        List<WorkTypeProfile> out = new ArrayList<>();
        for (WorkTypeProfile p : SERVICES) if (p.deEnergized) out.add(p);
        return out;
    }

    /**
     * One cheapest representative per service family (for expensive full-scope E2E flows):
     * AF, Checklist, COM, IR, Schedule get their only member; PM Forms gets Cleaning (has Z1 fixture).
     */
    public static List<WorkTypeProfile> familyRepresentatives() {
        return Arrays.asList(
                byName("Arc Flash Data Collection"),
                byName("Arc Flash Label Placement"),
                byName("Condition Assessment"),
                byName("Infrared Thermography"),
                byName("Panel Schedule Updates"),
                byName("Cleaning"));
    }

    /** Exact expected dropdown option labels in display order — for pinning the option list itself. */
    public static List<String> expectedOptionLabels() {
        List<String> out = new ArrayList<>();
        for (WorkTypeProfile p : ALL) out.add(p.name);
        return out;
    }

    private WorkTypeCatalog() {}
}
