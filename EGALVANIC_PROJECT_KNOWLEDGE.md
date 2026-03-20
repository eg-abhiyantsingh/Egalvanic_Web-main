# eGalvanic Platform — Comprehensive Project Knowledge Base

## 1. Company & Product Overview

**eGalvanic** (Egalvanic) is a cloud-based **electrical infrastructure management platform** designed for facility managers, electrical contractors, and service companies. It digitizes the management of electrical distribution systems in commercial and industrial buildings.

- **URL**: `https://acme.qa.egalvanic.ai` (QA environment, "acme" tenant)
- **Current Version**: Web v1.18.2
- **Tech Stack**: React SPA (Vite bundled), MUI (Material-UI) components, Zustand/context state management
- **Monitoring**: Sentry (qa environment), Beamer (release notes), DevRev (customer feedback widget)
- **Auth**: JWT-based with refresh tokens, subdomain-based tenant isolation
- **Mobile**: iOS app exists ("Z Platform-QA" .ipa seen in attachments)

---

## 2. Application Architecture

### 2.1 Navigation Structure

```
DASHBOARDS
├── Site Overview          /dashboard
├── Arc Flash Readiness    /arc-flash
├── Condition Assessment   /pm-readiness
└── Equipment Insights     /equipment-insights

DATA
├── SLDs                   /slds
├── Assets                 /assets
├── Connections            /connections
├── Locations              /locations
├── Tasks                  /tasks
├── Issues                 /issues
└── Attachments            /attachments

OPERATIONS
├── Work Order Planning    /planning
├── Service Agreements     /jobs-v2
├── Work Orders            /sessions
└── Scheduling             /scheduling

SALES
├── Opportunities          /opportunities
└── Accounts               /accounts

ADMIN
├── Settings               /admin
└── Audit Log              /admin/audit-log
```

### 2.2 Global Header Components
- **Site Selector**: Combobox "Select facility" — selects which SLD/site to view (e.g., "test site")
- **Role Selector**: Combobox "Select role" — switches user role context (Admin, Project Manager, Technician, Facility Manager, Client Portal)
- **User Profile Button**: Shows initials + name (e.g., "AP Avani Patel")
- **Release Updates**: Bell icon with badge count
- **Z University**: Link to `/z-university` — learning center

### 2.3 User Roles (RBAC)
Observed in Admin > Users:
| Role | Description |
|------|-------------|
| **Admin** | Full access to all modules including Settings, Users, Classes, PM, Forms |
| **Project Manager** | Manages projects, work orders, SLDs |
| **Technician** | Field worker role — performs tasks, work orders |
| **Facility Manager** | Building/facility-level oversight |
| **Client Portal** | Read-only external client view |
| **No Role** | Unassigned users |

---

## 3. Module Deep Dive

### 3.1 Site Overview Dashboard (`/dashboard`)
**Purpose**: High-level KPI dashboard for a selected facility.

**KPI Cards** (6 cards):
| KPI | Sample Value | Description |
|-----|-------------|-------------|
| Total Assets | 1,849 | Count of all electrical assets in the facility |
| Unresolved Issues | 38 | Open issues (NEC violations, thermal anomalies, etc.) |
| Pending Tasks | 32 | Tasks not yet completed |
| Active Work Orders | 27 | Work orders in Open/In Progress status |
| Opportunities Value | $0.0k | Total dollar value of sales opportunities |
| Equipment at Risk | $2555.4k | Replacement cost of equipment flagged at risk |

**Charts**:
- **Assets by Type** (pie/donut): ATS, Busway, Capacitor, Circuit Breaker, Disconnect Switch, Fuse, MCC Bucket, PDU, Switchboard
- **Issues by Type** (pie/donut): NEC Violation, NFPA 70B Violation, OSHA Violation, Repair Needed, Thermal Anomaly, Ultrasonic Anomaly

### 3.2 Arc Flash Readiness (`/arc-flash`)
**Purpose**: NFPA 70E compliance tracking — measures how ready a facility is for an arc flash study.

**Tabs**: Overview | Asset Details | Source/Target Connections | Connection Details

**Overview Tab** — Three radial progress indicators:
- **Asset Details** (7%): "Percentage of required asset fields that are filled out across all assets"
- **Source/Target Connections** (0%): "Percentage of assets requiring a source connection that have an incoming connection"
- **Connection Details** (0%): "Percentage of required connection fields that are filled out across all connections"

**Per-Asset-Class Progress Cards**:
| Asset Class | Progress | Completion |
|-------------|----------|------------|
| ATS | 28% | 2/9 |
| Busway | 100% | 443/443 |
| Capacitor | 100% | 3/3 |
| Circuit Breaker | 34% | 84/251 |
| Default | 100% | 2/2 |
| Disconnect Switch | 0% | 0/17 |
| Fuse | 0% | 0/54 |
| Generator | 100% | 2/2 |
| Loadcenter | 0% | 0/1 |
| MCC | 0% | 0/4 |
| MCC Bucket | 100% | 8/8 |
| Motor | 0% | 0/1 |
| Other (OCP) | 100% | 2/2 |
| Panelboard | 0% | 0/3 |
| PDU | 0% | 0/501 |
| Relay | 100% | 4/4 |
| Switchboard | 0% | 0/500 |
| Unknown | 100% | 44/44 |

### 3.3 Condition Assessment / PM Readiness (`/pm-readiness`)
**Purpose**: Tracks preventive maintenance readiness of electrical assets.

**Tabs**: Overview | Asset Details

Uses similar radial progress indicators to Arc Flash Readiness but focused on PM-specific attributes (condition scores, last maintenance dates, maintenance intervals).

### 3.4 Equipment Insights (`/equipment-insights`)
**Purpose**: NETA testing results and analytics by equipment designation. Tracks replacement cost and lead times.

**Grid Columns**: Designation | Manufacturer | Type | Style | Count | Lead Time | Replacement Cost

**Sample Data** (Circuit Breakers from major manufacturers):
| Manufacturer | Type | Style | Count | Lead Time | Cost |
|-------------|------|-------|-------|-----------|------|
| Square D | LVPCB | PowerPact H | 847 | 12-16 weeks | $2,450 |
| Square D | LVMCCB | QO | 3,421 | 4-6 weeks | $180 |
| Square D | LVMCCB | Pow-R-Line C | 1,289 | 6-8 weeks | $850 |
| Siemens | LVMCCB | Sentron WL | 956 | 8-10 weeks | $920 |
| Siemens | LVPCB | Sentron 3WL | 623 | 14-18 weeks | $3,200 |
| Eaton | LVMCCB | Series C | 2,134 | 6-8 weeks | $790 |
| Eaton | LVPCB | Magnum DS | 712 | 12-16 weeks | $2,800 |
| Eaton | LVICCB | Pow-R-Way III | 445 | 10-14 weeks | $4,500 |
| ABB | LVMCCB | Tmax T7 | 1,567 | 8-12 weeks | $1,150 |

**Total**: 46 equipment designations (paginated 25 per page)

### 3.5 SLDs — Single Line Diagrams (`/slds`)
**Purpose**: Visual graph editor for electrical single-line diagrams. This is the CORE module of the platform.

**Key Features**:
- **View Selector**: Dropdown to select which view to load (views contain subsets of nodes)
- **Graph Editor**: Interactive node-edge diagram (React Flow based)
- **Toolbar Controls**:
  - Filter nodes
  - Search nodes
  - Show Status Badges (toggle)
  - Highlight Selection (toggle)
  - Trace Lineage — Upstream/Downstream Path (toggle)
  - Show Edge Labels (toggle)
  - Show MiniMap (toggle)
  - Fit View
  - Lock Graph (toggle, default: locked)
- **Export**: Export functionality
- **Help**: Show help button
- **Refresh**: Refresh SLD data

**Data Model**:
- **Nodes** = Assets (electrical equipment) — represented as visual blocks on the diagram
- **Edges** = Connections between assets (source → target power flow)
- **Views** = Named subsets of nodes in an SLD (e.g., a specific electrical room or panel)
- **33 active SLDs** in test site

### 3.6 Assets (`/assets`)
**Purpose**: CRUD management for electrical assets/equipment.

**Asset Classes** (22 types):
ATS, Busway, Cable, CT (Current Transformer), Capacitor, Circuit Breaker, Disconnect Switch (DS), Fuse, Generator, Junction Box (JB), Load Center (LC), MCC, MCC Bucket, MCCB, Motor, OCP (Other), Panelboard, PDU, Relay, Switchboard (SWB), Transformer (TRF), UPS, Utility (UTL), VFD

**Each asset has**:
- Common fields: Name, Location, Manufacturer, Model, etc.
- Class-specific **Core Attributes** (e.g., Motor has HP, RPM, Frame Size, Voltage, etc.)
- **Subtypes** within classes (e.g., Transformer subtypes: Dry Type, Oil Filled, Pad Mount, etc.)
- Required fields vary by class (toggle to show only required fields)

### 3.7 Connections (`/connections`)
**Purpose**: Manage source-target relationships between electrical assets.

**Grid Columns**: Source | Target | Connection Type
**Features**: Create, Edit, Delete connections, Search/Filter, MUI DataGrid with autocomplete fields

### 3.8 Locations (`/locations`)
**Purpose**: Hierarchical location management (Building → Floor → Room).

**Tree Structure**: Buildings contain Floors contain Rooms
**CRUD**: Create/Edit/Delete at each level

### 3.9 Tasks (`/tasks`)
**Purpose**: Task management for maintenance and inspection activities.

**Status Summary Cards**:
- Pending: 32 tasks
- Completed: 3 tasks
- Due Soon (Next 30 Days): 0 tasks
- Overdue: 32 tasks

**Views**: List view | Calendar view

**Grid Columns**: Title | Asset | Location | Type | Created | Due Date | Work Order | Status | Actions

**Task Types**: PM (Preventive Maintenance), others
**Task Statuses**: Pending, Scheduled, Completed
**Actions**: Edit Task, Delete Task

### 3.10 Issues (`/issues`)
**Purpose**: Track electrical code violations, anomalies, and repair needs.

**Issue Classes/Types**:
- NEC Violation (National Electrical Code)
- NFPA 70B Violation (Recommended Practice for Electrical Equipment Maintenance)
- OSHA Violation (Occupational Safety & Health)
- Repair Needed
- Thermal Anomaly
- Ultrasonic Anomaly

**Features**: Create, Edit, Delete, Photos, Activate Jobs (convert issue to work order), Detail tabs

### 3.11 Attachments (`/attachments`)
**Purpose**: File management for facility documentation.

**Grid Columns**: File Name | Type | Site | Size | Created | Actions (View/Download, Download)

**Attachment Types**:
- Third-Party Report
- General Documentation
- SLD
- Product Manual
- Floor Plan
- Reference

**Supported formats**: PDF, PNG, JPG, JPEG, WEBM, IPA

### 3.12 Work Order Planning (`/planning`)
**Purpose**: Create and manage work order plans/quotes.

**Grid Columns**: Title | Type | Description | Facility | Created | Total | Status | Actions
**Features**: Create Plan, Search plans

### 3.13 Service Agreements (`/jobs-v2`)
**Purpose**: Manage service agreements/contracts with facilities.

### 3.14 Work Orders (`/sessions`)
**Purpose**: Track field work sessions/orders.

**Grid Columns**: Created | Priority | Work Order | SA / Plan | Facility | Est. Hours | Due Date | Scheduled | Status

**Work Order Statuses**: Open, In Progress, Completed, Closed
**Priority Levels**: High, Medium, Low
**Filterable**: Status column has active filter capability
**Current data**: 27 active work orders (mostly SmokeTest_WO_* test data)

### 3.15 Scheduling (`/scheduling`)
**Purpose**: Calendar-based scheduling of work orders and technician assignments.

### 3.16 Opportunities (`/opportunities`)
**Purpose**: Sales opportunity tracking for electrical service proposals.

### 3.17 Accounts (`/accounts`)
**Purpose**: Customer/client account management.

### 3.18 Admin > Settings (`/admin`)
**Purpose**: System administration.

**Settings Tabs**: Sites | Users | Classes | PM | Forms

**Users Management**:
- Grid: Name | Email | Job Title | Role | Status | Actions
- Actions: Edit User, Resend Invitation Email
- Create User button
- Search users
- User statuses: Active, Inactive

### 3.19 Admin > Audit Log (`/admin/audit-log`)
**Purpose**: Track all user actions and system changes for compliance.

---

## 4. Domain Knowledge — Electrical Infrastructure

### 4.1 Single Line Diagrams (SLDs)
A **Single Line Diagram** (also called One-Line Diagram) is a simplified schematic of an electrical power distribution system. It shows:
- **Power sources** (utility feeds, generators)
- **Distribution equipment** (switchboards, panelboards, transformers)
- **Protective devices** (circuit breakers, fuses, disconnect switches)
- **Loads** (motors, PDUs, etc.)
- **Connections** (cables, busways) showing power flow direction

SLDs are essential for:
- **Arc flash studies** (NFPA 70E compliance)
- **Short circuit analysis**
- **Coordination studies**
- **Facility maintenance planning**

### 4.2 Arc Flash & NFPA 70E
**Arc Flash**: An electrical explosion caused by a fault condition. Can generate temperatures up to 35,000°F and blast pressures.

**NFPA 70E**: Standard for Electrical Safety in the Workplace. Requires:
- Arc flash hazard analysis
- Equipment labeling with incident energy levels
- PPE (Personal Protective Equipment) categories
- Arc flash boundaries
- Working distance specifications

**Arc Flash Readiness** in eGalvanic measures:
1. Are all asset details filled out? (needed for short-circuit calculations)
2. Are all connections documented? (needed to trace power flow paths)
3. Are connection details complete? (cable sizes, lengths, impedances)

### 4.3 Electrical Asset Types
| Type | Full Name | Purpose |
|------|-----------|---------|
| **ATS** | Automatic Transfer Switch | Switches between normal and emergency power |
| **Busway** | Bus Duct / Busway | Enclosed conductor system for power distribution |
| **Cable** | Power Cable | Conducts power between equipment |
| **CT** | Current Transformer | Measures current for metering/protection |
| **DS** | Disconnect Switch | Manual isolation device |
| **Fuse** | Fuse | Overcurrent protection (one-time) |
| **Generator** | Generator | Backup/emergency power source |
| **JB** | Junction Box | Wiring junction/splice point |
| **LC** | Load Center | Small distribution panel (residential/light commercial) |
| **MCC** | Motor Control Center | Controls and protects multiple motors |
| **MCCB** | Molded Case Circuit Breaker | Fixed-frame circuit breaker |
| **Motor** | Electric Motor | Converts electrical to mechanical energy |
| **OCP** | Overcurrent Protection | Generic overcurrent protective device |
| **Panelboard** | Panelboard | Branch circuit distribution panel |
| **PDU** | Power Distribution Unit | Data center power distribution |
| **Relay** | Protective Relay | Monitors and trips on fault conditions |
| **SWB** | Switchboard | Main/secondary power distribution |
| **TRF** | Transformer | Steps voltage up/down |
| **UPS** | Uninterruptible Power Supply | Battery backup for critical loads |
| **UTL** | Utility | Utility power feed (grid connection) |
| **VFD** | Variable Frequency Drive | Controls motor speed |

### 4.4 Issue Types — Regulatory Context
| Issue Type | Standard | Consequence |
|-----------|----------|-------------|
| **NEC Violation** | NFPA 70 (National Electrical Code) | Code compliance failure — can fail inspection |
| **NFPA 70B Violation** | NFPA 70B (Electrical Equipment Maintenance) | Maintenance deficiency — increased failure risk |
| **OSHA Violation** | OSHA 29 CFR 1910 Subpart S | Safety violation — can result in fines/shutdown |
| **Repair Needed** | General | Equipment needs physical repair |
| **Thermal Anomaly** | Infrared thermography inspection | Hot spot indicating loose connection or overload |
| **Ultrasonic Anomaly** | Ultrasonic inspection | Arcing/tracking/corona detected by sound |

### 4.5 Condition Assessment
Condition assessment evaluates the health of electrical equipment based on:
- **Age** vs. expected lifespan
- **Visual inspection** results
- **Infrared (IR) thermography** — thermal imaging for hot spots
- **Ultrasonic testing** — detecting partial discharge/arcing
- **NETA testing** — InterNational Electrical Testing Association standardized tests
- **Maintenance history** — last service date, frequency

### 4.6 Work Order Lifecycle
```
Plan → Create Work Order → Assign Technician → Schedule →
In Progress → Complete Tasks → Close Work Order
```

Work orders can be linked to:
- Service Agreements (recurring contracts)
- Plans (one-time project scopes)
- Tasks (individual work items)
- Issues (problems that triggered the work)

---

## 5. Technical Details

### 5.1 UI Framework
- **React** with Vite build (`index-CnGxCKdJ.js` bundled)
- **MUI (Material-UI)** components: DataGrid, Drawer, Accordion, Autocomplete, Combobox
- **React Flow** for SLD graph editor
- **Chart.js/Recharts** for dashboard charts (pie/donut)

### 5.2 API Architecture
- **REST API** at `/api/*`
- **Auth**: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/resend-code`
- **SLDs**: `/api/sld/{sldId}/views`, `/api/sld/{sldId}/edges`
- **Nodes**: Fetched per SLD, with classes and subtypes
- **Sessions**: `/api/sessions` for work orders

### 5.3 State Management
- `mainAPIService` — centralized API service layer
- `LookupService` — caches nodes, tasks, attachments per SLD
- `UI Store` — manages user context, SLD views
- `Auth Store` — handles subdomain detection, token refresh
- `Layout` — tracks site state and current sldId

### 5.4 Key Patterns
- **SLD-scoped data**: Most data modules (Tasks, Attachments, Issues) are scoped to a selected SLD
- **Lazy loading**: Data only fetches when an SLD is selected (log: "No sldId provided, showing error message")
- **Node classes & subtypes**: Dynamic — fetched from API, not hardcoded
- **Edge classes**: Connection types fetched dynamically
- **Shortcuts**: Quick-access mappings fetched per SLD

### 5.5 External Integrations
- **Sentry**: Error monitoring (qa environment)
- **Beamer**: In-app release notes / changelog
- **DevRev**: Customer feedback widget (fails to load in QA — `plug-platform.devrev.ai/static/plug.js` returns ERR_FAILED)
- **Service Worker**: Registered for offline/PWA capabilities

---

## 6. Test Environment Details

### 6.1 Credentials
| Account | Email | Role |
|---------|-------|------|
| Primary (Admin) | abhiyant.singh+admin@egalvanic.com | Admin |
| Admin 2 | abhiyant.singh+admin@egalvanic.com | Admin |
| Project Manager | abhiyant.singh+projectmanger@egalvanic.com | Project Manager |
| Technician | abhiyant.singh+technician@egalvanic.com | Technician |
| Facility Manager | abhiyant.singh+fm@egalvanic.com | Facility Manager |
| Client Portal | abhiyant.singh+cp@egalvanic.com | Client Portal |

**Password**: Stored in `AppConstants.VALID_PASSWORD` (currently `RP@egalvanic123` but may change — was invalid earlier and reset)

### 6.2 Test Data
- **33 SLDs** (active) in "test site"
- **1,849 assets** across all classes
- **38 unresolved issues**
- **32 pending tasks** (all overdue)
- **27 active work orders** (mostly SmokeTest data)
- **11 attachments**
- **0 plans** (work order planning is empty)
- **46 equipment designations** in insights

### 6.3 Known Console Errors
1. `DevRev SDK failed to load` — external widget, not blocking
2. `Beamer user_id PII leak` — sends full email in Beamer init URL (security concern)
3. `SLD views fetch failure` — occasional 403/500 on `/api/sld/{id}/views`

---

## 7. Observed Bugs & Potential Test Areas

### 7.1 UI Bugs Noticed
1. **Duplicate legend items** in Issues by Type chart — "NEC Violation" appears twice with different colors, same for NFPA 70B Violation, OSHA Violation, Repair Needed, Thermal Anomaly
2. **Equipment at Risk displays as "$2555.4k"** — inconsistent formatting vs "$0.0k" (should be "$2,555.4k"?)
3. **Legal Agreement modal** blocks interaction on first load — could cause issues if auto-accepted in previous session but appears again
4. **DevRev SDK error** logged on every page load (console noise)
5. **"No rows" display** in Work Order Planning — should show a proper empty state message

### 7.2 Data Integrity Concerns
1. **All 32 tasks are overdue** — Due dates in Nov 2025 but current date is Mar 2026
2. **Work Orders all "SmokeTest_*"** — Test data pollution from previous automation runs
3. **User "abhiyant.singh@egalvanic.com" has "No Role"** — should they have a role?
4. **PDU has 501 assets at 0% arc flash readiness** — largest incomplete category
5. **Switchboard has 500 assets at 0%** — second largest incomplete category

### 7.3 Security Observations
1. **Beamer init URL contains user PII** (firstname, lastname, email in query params)
2. **No CSRF tokens visible** in REST API calls
3. **JWT refresh token flow** — need to verify token expiry handling
4. **Role switching** — verify backend enforces role-based access (not just UI hiding)

### 7.4 Potential Edge Cases for Testing
1. **SLD with no nodes** — "Empty view received" is logged frequently
2. **Very large SLDs** — What happens with 500+ nodes? (PDU and Switchboard counts suggest this is realistic)
3. **Concurrent role switching** while data is loading
4. **Site switching** during mid-operation
5. **Offline capabilities** (service worker registered — does offline mode work?)
6. **File upload limits** — Largest attachment is 91.64 MB (webm video)
7. **Date format consistency** — Some dates are "14/11/2025" (DD/MM/YYYY), others "Mar 17, 2026"
8. **Pagination** — Grid defaults to 25 rows per page

---

## 8. Module Coverage vs Test Automation

### 8.1 Already Automated (in this project)
| Module | Test File | TCs |
|--------|-----------|-----|
| Authentication | AuthenticationTestNG.java | 38 |
| Connection (Smoke) | ConnectionSmokeTestNG.java | - |
| Connection (Full) | ConnectionTestNG.java | 45 |
| Site Selection (Smoke) | SiteSelectionSmokeTestNG.java | - |
| Site Selection (Full) | SiteSelectionTestNG.java | 52 |
| Asset (Smoke) | AssetSmokeTestNG.java | - |
| Asset Part 1 | AssetPart1TestNG.java | 30 |
| Asset Part 2 | AssetPart2TestNG.java | 40 |
| Asset Part 3 | AssetPart3TestNG.java | 76 |
| Asset Part 4 | AssetPart4TestNG.java | 62 |
| Asset Part 5 | AssetPart5TestNG.java | 65 |
| Location (Smoke) | LocationSmokeTestNG.java | - |
| Location (Full) | LocationTestNG.java | 55 |
| Issue (Smoke) | IssuesSmokeTestNG.java | - |
| Issue (Full) | IssueTestNG.java | 70 |
| Work Order (Smoke) | WorkOrderSmokeTestNG.java | - |
| Bug Hunt & Security | BugHuntTestNG.java | 30 |
| API Auth | AuthenticationAPITest.java | - |
| API User | UserAPITest.java | - |
| API Security | APISecurityTest.java | - |
| API Performance | APIPerformanceTest.java | - |

### 8.2 Not Yet Automated
| Module | Priority | Complexity |
|--------|----------|------------|
| **SLDs** (Graph Editor) | HIGH | VERY HIGH — interactive graph, drag/drop, view switching |
| **Tasks** | HIGH | MEDIUM — standard CRUD with calendar view |
| **Attachments** | MEDIUM | MEDIUM — file upload/download |
| **Work Order Planning** | MEDIUM | MEDIUM — CRUD grid |
| **Service Agreements** | MEDIUM | MEDIUM — contract management |
| **Work Orders (Full)** | HIGH | HIGH — complex lifecycle, scheduling integration |
| **Scheduling** | MEDIUM | HIGH — calendar UI |
| **Opportunities** | LOW | MEDIUM — sales tracking |
| **Accounts** | LOW | LOW — account management |
| **Dashboards** | MEDIUM | MEDIUM — verify KPI calculations, chart data |
| **Arc Flash Readiness** | HIGH | MEDIUM — verify progress calculations |
| **Condition Assessment** | HIGH | MEDIUM — verify PM readiness metrics |
| **Equipment Insights** | MEDIUM | LOW — read-only grid with search |
| **Admin Settings** | HIGH | HIGH — user management, classes config, PM config, forms |
| **Audit Log** | MEDIUM | LOW — read-only log with search/filter |
| **Role-Based Access** | HIGH | HIGH — verify each role sees/accesses correct modules |

---

## 9. Glossary

| Term | Definition |
|------|-----------|
| **SLD** | Single Line Diagram — schematic of electrical distribution |
| **NFPA 70E** | Standard for Electrical Safety in the Workplace (arc flash) |
| **NFPA 70B** | Recommended Practice for Electrical Equipment Maintenance |
| **NEC** | National Electrical Code (NFPA 70) |
| **OSHA** | Occupational Safety and Health Administration |
| **NETA** | InterNational Electrical Testing Association |
| **Arc Flash** | Electrical explosion from fault condition |
| **PPE** | Personal Protective Equipment |
| **LVPCB** | Low Voltage Power Circuit Breaker |
| **LVMCCB** | Low Voltage Molded Case Circuit Breaker |
| **LVICCB** | Low Voltage Insulated Case Circuit Breaker |
| **PM** | Preventive Maintenance |
| **IR** | Infrared (thermography) |
| **SA** | Service Agreement |
| **WO** | Work Order |
| **RBAC** | Role-Based Access Control |
