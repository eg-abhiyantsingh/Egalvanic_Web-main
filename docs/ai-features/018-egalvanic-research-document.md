# Egalvanic Platform - Research Document & Deep Understanding

**Date**: April 10, 2026  
**Author**: AI Research (Claude Code)  
**Version**: 1.0  
**Research Sources**: Jira CSV (1,650 issues), Live Website (Playwright exploration), Codebase analysis

---

## Table of Contents

1. [Platform Overview](#1-platform-overview)
2. [Application Architecture](#2-application-architecture)
3. [Module-by-Module Analysis](#3-module-by-module-analysis)
4. [Jira Bug & Story Analysis](#4-jira-bug--story-analysis)
5. [Test Automation Architecture](#5-test-automation-architecture)
6. [CI/CD Pipeline](#6-cicd-pipeline)
7. [Key Technical Patterns](#7-key-technical-patterns)
8. [Known Issues & Patterns](#8-known-issues--patterns)
9. [Test Coverage Gaps & Recommendations](#9-test-coverage-gaps--recommendations)
10. [Glossary](#10-glossary)

---

## 1. Platform Overview

### What is Egalvanic?

Egalvanic (branded as "Project Z") is a **cloud-based electrical asset management platform** designed for facility managers, electricians, and maintenance teams. It manages the complete lifecycle of electrical equipment — from initial asset registration through maintenance, work orders, compliance tracking, and sales opportunities.

### Core Business Value

- **Asset Management**: Track 1,900+ electrical assets (transformers, switchgear, panels, etc.) with QR codes, conditions, and hierarchical location mapping
- **Single Line Diagrams (SLDs)**: Interactive graph-based electrical diagrams showing upstream/downstream connections between equipment
- **Compliance**: NFPA 70E Arc Flash Readiness tracking and Condition Assessment dashboards
- **Field Operations**: Work orders, scheduling, issue tracking with mobile-friendly workflows
- **Sales Pipeline**: Opportunity tracking and account management for service providers

### Key Metrics (Test Site)

| Metric | Value |
|--------|-------|
| Total Assets | 1,924 |
| Unresolved Issues | 41 |
| Pending Tasks | 100 |
| Active Work Orders | 161 |
| Opportunities Value | $10.1K |
| Equipment at Risk | $3,830.4K |
| Audit Mutations | 278 |
| Accounts | 8 |
| Attachments | 13 |

### Tech Stack

- **Frontend**: React.js with Material UI (MUI) component library
- **State Management**: React Context / Redux (MUI DataGrid for tables)
- **Routing**: React Router (SPA with client-side routing)
- **Backend API**: GraphQL mutations (evidenced by "Mutation Audit Log")
- **Authentication**: Email/password with company code subdomain routing
- **Hosting**: `*.qa.egalvanic.ai` (QA environment), subdomain per company (e.g., `acme.qa.egalvanic.ai`)
- **Current Version**: V1.18.2

### User Roles

| Role | Email Pattern | Access Level |
|------|--------------|--------------|
| Admin | `+admin@egalvanic.com` | Full access to all modules + Settings |
| Project Manager | `+projectmanager@egalvanic.com` | Operations & Data management |
| Technician | `+technician@egalvanic.com` | Field work: Work Orders, Issues |
| Facility Manager | `+facilitymanager@egalvanic.com` | Site-level management |
| Client Portal | `+clientportal@egalvanic.com` | Limited read access |

---

## 2. Application Architecture

### Navigation Structure (20 Pages)

The application has a left sidebar navigation organized into **5 sections**:

```
DASHBOARDS (4 pages)
  +-- Site Overview        /dashboard
  +-- Arc Flash Readiness  /arc-flash
  +-- Condition Assessment /pm-readiness
  +-- Equipment Insights   /equipment-insights

DATA (7 pages)
  +-- SLDs                 /slds
  +-- Assets               /assets
  +-- Connections          /connections
  +-- Locations            /locations
  +-- Tasks                /tasks
  +-- Issues               /issues
  +-- Attachments          /attachments

OPERATIONS (4 pages)
  +-- Work Order Planning  /planning
  +-- Service Agreements   /jobs-v2
  +-- Work Orders          /sessions
  +-- Scheduling           /scheduling

SALES (2 pages)
  +-- Opportunities        /opportunities
  +-- Accounts             /accounts

ADMIN (2 pages)
  +-- Settings             /admin
  +-- Audit Log            /admin/audit-log

EXTRA (1 page)
  +-- Z University         /z-university
```

### Common UI Patterns

1. **Header Bar**: Page title, Site selector (combobox "Select facility"), User avatar button, Release updates, Z University link
2. **Data Grids**: MUI DataGrid with pagination ("Rows per page" + "1-25 of N"), sortable columns, search/filter
3. **Create Actions**: "Create [Entity]" button in top-left of content area, opens right-side MUI Drawer
4. **Detail Views**: Click row to open detail page (e.g., `/tasks/{uuid}`)
5. **App Update Alert**: Persistent banner "A new app update is available" with UPDATE and DISMISS buttons
6. **Loading State**: Centered spinner with "Loading..." text during initial page load

### Site-Scoped vs Cross-Site Pages

| Site-Scoped (show site selector) | Cross-Site (no site selector) |
|----------------------------------|-------------------------------|
| Dashboard, SLDs, Assets, Connections, Locations, Tasks, Issues, Attachments, Work Order Planning, Service Agreements, Work Orders, Scheduling, Opportunities, Accounts | Equipment Insights, Settings, Audit Log |

---

## 3. Module-by-Module Analysis

### 3.1 Site Overview Dashboard (`/dashboard`)

**Purpose**: Executive summary of site health  
**KPI Cards**: Total Assets, Unresolved Issues, Pending Tasks, Active Work Orders, Opportunities Value, Equipment at Risk  
**Charts**: Assets by Type, Issues by Type (pie/donut charts)  
**Key Interaction**: KPI cards are clickable, CSS `text-transform: uppercase` renders labels as "TOTAL ASSETS" but DOM contains "Total Assets"

### 3.2 Arc Flash Readiness (`/arc-flash`)

**Purpose**: NFPA 70E compliance tracking  
**Layout**: 4 tabs — Overview, Asset Details, Source/Target Connections, Connection Details  
**Overview Tab**: 3 circular gauge charts showing completion percentages  
- Asset Details: 10% complete
- Source/Target Connections: 0%
- Connection Details: 0%

**Relevance**: Electrical safety compliance is a legal requirement; this dashboard tracks readiness

### 3.3 Condition Assessment (`/pm-readiness`)

**Purpose**: Preventive Maintenance readiness tracking  
**Layout**: Similar tab structure to Arc Flash  
**Jira Context**: ZP-1393 "condition assessment PM should show only PM data" (open bug)

### 3.4 Equipment Insights (`/equipment-insights`)

**Purpose**: NETA testing results and analytics by equipment designation  
**Special**: Cross-site view (no site selector)  
**Interaction**: "Click on any row to view detailed NETA testing results"

### 3.5 SLDs (`/slds`)

**Purpose**: Single Line Diagrams — interactive electrical topology viewer  
**This is the most complex module in the platform**

**Layout**: Full-screen graph canvas with toolbar  
**Controls**:
- View selector dropdown ("Select a View to Load Assets")
- Filter nodes, Search nodes
- Toggle checkboxes: Show Status Badges, Highlight Selection, Trace Lineage (Upstream/Downstream Path), Show Edge Labels, Show MiniMap
- Lock Graph (checked by default), Fit View
- Export, Refresh, Help buttons

**Jira Context**: 
- ZP-1585: "SLD forces re-login" (High priority, open)
- ZP-1108: "SLD performance" (open)
- Multiple SLD-related stories for edge labels, minimap, status badges

### 3.6 Assets (`/assets`)

**Purpose**: Central asset registry — CRUD operations on electrical equipment  
**Grid Columns**: Asset Name, QR Code, Condition, Asset Class, Subtype, Building, Floor, Room, Parent Asset  
**Actions**: Create Asset, Bulk Upload, SKM Import, SKM Import (Legacy), SKM Export (Beta), Export Site, Bulk Ops  
**Data Volume**: 1,924 assets (paginated 25 per page)  
**Create Form Fields**: Asset Name, QR Code, Asset Class, Subtype, Core Attributes (dynamic based on class), Replacement Cost  

**Jira Context**: 
- ZP-1617: "assets page should display new attributes" (open story)
- ZP-1612: "view asset should display all data" (open story)
- ZP-1500: "asset not visible after creation" (open bug)

### 3.7 Connections (`/connections`)

**Purpose**: Define electrical connections between assets (source-to-target relationships)  
**Actions**: Create Connection  
**Data**: Grid with connection details  

**Jira Context**: ZP-671: "connection links missing" (High priority, open)

### 3.8 Locations (`/locations`)

**Purpose**: Hierarchical location management — Building > Floor > Room  
**Layout**: Tree list on left, detail panel on right ("Select a location to view details")  
**Interaction**: Click location in tree to see its details and child assets

### 3.9 Tasks (`/tasks`)

**Purpose**: Preventive Maintenance task management  
**Layout**: Status filter tabs + data grid  
**Detail View**: Opens at `/tasks/{uuid}` (navigates away from grid)  
**Key Issue**: After viewing task detail, navigating back re-triggers app update alert

### 3.10 Issues (`/issues`)

**Purpose**: Track equipment issues/defects  
**Grid Columns**: Title, Issue Class, Priority, Asset, Session, Site, Created, Status, Actions  
**Actions**: Create Issue, Generate Report  
**Data**: 45 issues total  
**Create Form**: Priority (Medium default), Immediate Hazard (Yes/No), Customer Notified (Yes/No), Issue Class (required), Asset dropdown

### 3.11 Attachments (`/attachments`)

**Purpose**: File management for site documents  
**Grid Columns**: File Name, Type, Site, Size, Created, Actions  
**Actions**: Upload Attachment  
**Data**: 13 attachments

### 3.12 Work Order Planning (`/planning`)

**Purpose**: Plan work orders before execution  
**Grid Columns**: Title, Type, Description, Facility, Created, Total, Status, Actions  
**Actions**: Create Plan  
**Data**: 0 plans (empty in test site)

### 3.13 Service Agreements (`/jobs-v2`)

**Purpose**: Manage service agreements/contracts  
**Grid Columns**: Created Sort, Service Agreement, Description, Facility, Progress, Compliance  
**Search**: "Search jobs..." textbox  
**Data**: 0 agreements (empty in test site)

### 3.14 Work Orders (`/sessions`)

**Purpose**: Field work execution tracking  
**Grid Columns**: Created Sort, Priority, Work Order, SA/Plan, Facility, Est. Hours, Due Date, Scheduled, Status  
**Actions**: Create Work Order  
**Data**: 161 work orders (1 active filter applied by default)  
**Default Filter**: Shows active/open work orders

### 3.15 Scheduling (`/scheduling`)

**Purpose**: Drag-and-drop work order scheduling  
**Layout**: Dual-panel — Work Order list (left) + Calendar Month View (right)  
**Left Panel**: "New Work Order" button, Search, scrollable list of ~100+ work order cards  
**Right Panel**: Month calendar (Sun-Sat grid) for April 2026  
**Interaction**: Drag work orders from list to calendar dates

### 3.16 Opportunities (`/opportunities`)

**Purpose**: Sales pipeline management  
**Pipeline KPIs**: Qualifying ($10.1K, 3), Pending Response ($0), Closed Won ($0), Closed Lost ($0)  
**Grid Columns**: Opportunity Name, Facility, Revisions, Total Value, Created Sort, Status, Actions  
**Actions**: New Opportunity  
**Data**: 3 opportunities  
**Special Behavior**: Site selector initially shows "No sites available" when app update alert is present

### 3.17 Accounts (`/accounts`)

**Purpose**: Customer/client account management  
**Grid Columns**: Account Name, Owner, Created Sort, Actions  
**Actions**: New Account  
**Data**: 8 accounts

### 3.18 Settings (`/admin`)

**Purpose**: Administrative configuration  
**Sub-sections** (5 tabs): Sites, Users, Classes, PM, Forms  
**Sites Tab**: Create Site + site grid  
**Cross-site**: No site selector (admin-level)

### 3.19 Audit Log (`/admin/audit-log`)

**Purpose**: Track all data mutations (GraphQL operations)  
**Title**: "Mutation Audit Log"  
**Filters**: Search, Status, Entity Type, Time Range  
**Data**: 278 mutations  
**Actions**: Refresh button

### 3.20 Z University (`/z-university`)

**Purpose**: Built-in learning center / knowledge base  
**Layout**: Left navigation with 21 article categories, right content panel  
**Articles**: Welcome, Subdomains & Users, Quick Start (Role-Based), Quick Start, Workflows, Arc Flash, Condition Assessment, SLDs, Assets, Connections, Locations, Tasks (PMs), Issues, Work Order Planning, Service Agreements, Work Orders, Opportunities (Quotes), Accounts, Settings (Admin), Customization, Key Terms, Electrician Field Guide

---

## 4. Jira Bug & Story Analysis

### Overview

| Type | Total | Open | Done | Other |
|------|-------|------|------|-------|
| Bug | 591 | 156 | 435 | 0 |
| Story | 431 | 139 | 292 | 0 |
| Task | 455 | 106 | 349 | 0 |
| Sub-task | 129 | 23 | 106 | 0 |
| Epic | 44 | 9 | 35 | 0 |
| **Total** | **1,650** | **433** | **1,217** | **0** |

### Priority Distribution

| Priority | Count | Percentage |
|----------|-------|-----------|
| Medium | 1,135 | 68.8% |
| High | 274 | 16.6% |
| Low | 137 | 8.3% |
| Highest | 89 | 5.4% |
| Lowest | 15 | 0.9% |

### High-Priority Open Bugs (Top 15)

| Key | Priority | Summary |
|-----|----------|---------|
| ZP-1585 | High | SLD forces re-login |
| ZP-1577 | High | User name change not saving |
| ZP-671 | High | Connection links missing |
| ZP-1557 | High | Current value changed |
| ZP-1500 | High | Asset not visible after creation |
| ZP-1108 | Medium | SLD performance issues |
| ZP-1622 | Medium | Issue deletion not redirecting |
| ZP-1612 | Medium | View asset should display all data |
| ZP-1607 | Medium | Delete option in asset table wrong column |
| ZP-1597 | Medium | Multi-site selection issue |
| ZP-1393 | Medium | Condition assessment showing wrong data |
| ZP-1390 | Medium | PM tab data inconsistency |
| ZP-1389 | Medium | Quick filter issues |
| ZP-1367 | Medium | Work order scheduling drag issues |
| ZP-1350 | Medium | SLD edge labels overlapping |

### Bug Distribution by Module (Estimated)

| Module | Open Bugs | Percentage |
|--------|-----------|-----------|
| SLD | ~25 | 16% |
| Assets | ~22 | 14% |
| Work Orders | ~18 | 12% |
| Connections | ~15 | 10% |
| Issues | ~14 | 9% |
| Dashboard | ~12 | 8% |
| Tasks | ~12 | 8% |
| Locations | ~10 | 6% |
| Settings/Admin | ~10 | 6% |
| Opportunities | ~8 | 5% |
| Other | ~10 | 6% |

### Key Open Stories (Feature Requests)

| Key | Summary | Module |
|-----|---------|--------|
| ZP-1650 | Enhanced SLD export options | SLD |
| ZP-1617 | Assets page new attributes | Assets |
| ZP-1615 | Work order template system | Work Orders |
| ZP-1610 | Bulk operations improvements | Assets |
| ZP-1605 | Mobile-responsive scheduling | Scheduling |
| ZP-1598 | Multi-site dashboard view | Dashboard |
| ZP-1590 | API rate limiting | API |
| ZP-1580 | Condition assessment enhancements | PM Readiness |

---

## 5. Test Automation Architecture

### Framework Overview

| Component | Technology |
|-----------|-----------|
| Language | Java 11 |
| Test Runner | TestNG 7.8.0 |
| Browser Automation | Selenium WebDriver 4.29.0 |
| API Testing | REST Assured 5.4.0 |
| Reporting | ExtentReports 5.1.2 |
| Build Tool | Maven |
| AI Integration | Claude API (claude-sonnet-4-20250514) |
| CI/CD | GitHub Actions |

### Directory Structure

```
src/
  main/java/com/egalvanic/qa/
    constants/
      AppConstants.java          # All config: URLs, credentials, timeouts, module names
    pageobjects/                 # Page Object Model (7 page objects)
      LoginPage.java             # Login form with React-compatible input handling
      DashboardPage.java         # Dashboard wait/check methods
      AssetPage.java             # CRUD operations, bulk upload, SKM import
      ConnectionPage.java        # Connection CRUD
      LocationPage.java          # Hierarchical location management
      IssuePage.java             # Issue CRUD with drawer interaction
      WorkOrderPage.java         # Work order operations
    utils/
      ExtentReportManager.java   # HTML report generation
      ScreenshotUtil.java        # Failure screenshot capture
      EmailUtil.java             # Email report delivery (SMTP)
      ai/                        # AI-powered testing utilities (10 files)
        ClaudeClient.java        # Claude API client (Java HttpClient)
        SmartBugDetector.java    # AI failure classification
        AITestGenerator.java     # AI-generated test cases
        SelfHealingDriver.java   # Auto-healing broken locators
        SelfHealingElement.java  # Smart element wrapper
        SelfHealingLocator.java  # Locator strategy management
        FlakinessPrevention.java # Flakiness detection & mitigation
        SmartTestDataGenerator.java  # AI test data generation
        VisualRegressionUtil.java    # Visual diff testing
        AIPageAnalyzer.java      # AI page structure analysis
  test/java/com/egalvanic/qa/
    listeners/
      ConsoleProgressListener.java  # TestNG progress output
    testcase/                    # 42 test classes
      BaseTest.java              # Parent class: browser setup, login, site selection
      [38 test classes]          # Module tests, smoke tests, bug hunts
      api/                       # 4 API test classes
        BaseAPITest.java
        AuthenticationAPITest.java
        UserAPITest.java
        APIPerformanceTest.java
        APISecurityTest.java
```

### Test Class Categories (46 test files, ~961 test cases)

| Category | Files | Test Cases | Description |
|----------|-------|------------|-------------|
| **Core Module Tests** | 14 | ~550 | CRUD operations: Asset (5 parts), Connection (2), Location (2), Issue (2), WorkOrder (2), SLD |
| **Bug Hunt Tests** | 8 | ~200 | Regression testing for specific Jira bugs per module |
| **Smoke Tests** | 7 | ~50 | Quick validation per module (1-5 TCs each) |
| **Dashboard/Critical** | 3 | ~70 | Dashboard bugs, critical user paths |
| **AI-Powered Tests** | 4 | ~40 | AI form testing, monkey testing, visual regression, AI page analysis |
| **API Tests** | 4 | ~30 | Auth, Users, Performance, Security |
| **Special Tests** | 3 | ~20 | Authentication, Site Selection, Load testing |
| **Standalone** | 3 | — | BaseTest, SiteSelection, BugHunt (own browser sessions) |

### BaseTest Lifecycle

```
@BeforeSuite  --> Initialize ExtentReports
@BeforeClass  --> Create Chrome browser (headless in CI)
              --> Navigate to BASE_URL
              --> Login with admin credentials
              --> Wait for dashboard
              --> Dismiss app update alert (dismissBackdrops)
              --> Select "Test Site"
@BeforeMethod --> Record test start time
              --> Call dismissBackdrops()
[Test runs]
@AfterMethod  --> Capture screenshot on failure
              --> Run SmartBugDetector.analyze() on failure
              --> Log test duration
@AfterClass   --> Quit browser
@AfterSuite   --> Flush ExtentReports
              --> Send email report (if enabled)
```

### Standalone Test Classes (Own Browser Sessions)

Three test classes maintain their own browser lifecycle instead of extending BaseTest:
1. **AuthenticationTestNG** — Tests login/logout flows (needs clean browser state)
2. **SiteSelectionTestNG** — Tests site switching (needs independent session)
3. **BugHuntTestNG** — Security/XSS tests (needs isolated environment)

Each has its own `@BeforeClass` setup with driver, login, and `dismissBackdrops()`.

### Page Object Patterns

**React Input Handling**: LoginPage uses `nativeInputValueSetter` to set React-controlled input values:
```java
// Standard sendKeys doesn't trigger React state updates
// nativeInputValueSetter bypasses this by directly setting the value
// then dispatching 'input' and 'change' events
```

**MUI Interaction Patterns**:
- **JS Click** for elements blocked by MUI backdrops/overlays
- **Actions.click()** for Selenium-level click on complex MUI components
- **sendKeys()** for MUI DataGrid Quick Filter (generates real keyboard events)
- **Never use Keys.ESCAPE** on MUI Drawers (closes them instead of dismissing popups)

### AI-Powered Features

The framework includes a **10-file AI integration layer** using Claude API:

1. **SmartBugDetector**: Analyzes test failures, classifies as REAL_BUG / FLAKY_TEST / ENVIRONMENT_ISSUE / LOCATOR_CHANGE
2. **SelfHealingDriver**: Wraps WebDriver to auto-heal broken locators when elements are renamed/moved
3. **FlakinessPrevention**: Detects and mitigates test flakiness patterns
4. **AITestGenerator**: Generates test cases from page analysis
5. **AIPageAnalyzer**: Analyzes page DOM structure and suggests test scenarios
6. **VisualRegressionUtil**: Screenshot-based visual diff detection
7. **SmartTestDataGenerator**: AI-generated test data for forms
8. **ClaudeClient**: Lightweight Java HTTP client for Claude API (no SDK dependency)

---

## 6. CI/CD Pipeline

### Workflow Files

| Workflow | File | Description |
|----------|------|-------------|
| Parallel Suite | `parallel-suite.yml` | **Primary CI** — 8 parallel jobs, 961 TCs |
| Parallel Suite 2 | `parallel-suite-2.yml` | Smoke, Load, AI, Exploratory tests |
| Full Suite | `full-suite.yml` | Sequential run (4-4.5 hours) |
| Smoke Tests | `smoke-tests.yml` | Quick validation |
| Email Test | `email-test.yml` | 1 TC per module for email verification |
| Developer Smoke | `web-tests-smoke-repodeveloper.yml` | Branch-level smoke tests |

### Parallel Suite Architecture (Primary CI)

```
Trigger: workflow_dispatch (manual)
Concurrency: One run at a time (queuing, no cancellation)
Runner: ubuntu-latest
Timeout: 120 minutes per job

8 Parallel Jobs (via matrix strategy):
  Job 1: Auth + Site + Connection    (130 TCs)  ~25 min
  Job 2: Location + Task             (135 TCs)  ~30 min
  Job 3: Work Order + Issue          (234 TCs)  ~55 min  [bottleneck]
  Job 4: Asset Parts 1-2             ( 69 TCs)  ~20 min
  Job 5: Asset Part 3                ( 76 TCs)  ~20 min
  Job 6: Asset Parts 4-5            (141 TCs)  ~35 min
  Job 7: SLD Module                  ( 71 TCs)  ~15 min
  Job 8: Dashboard + BugHunt        (105 TCs)  ~30 min

Stagger: 0-70 second delays between job starts to prevent login conflicts
Speedup: ~3-4x vs sequential (1-1.5h vs 4-4.5h)
```

### TestNG Suite XML Files (25+ files)

Suite XMLs map to CI jobs. Each defines which test classes and methods to run:
- `suite-auth-site-connection.xml` -> Job 1
- `suite-location-task.xml` -> Job 2
- `suite-workorder-issue.xml` -> Job 3
- `suite-asset-1-2.xml` -> Job 4
- `suite-asset-3.xml` -> Job 5
- `suite-asset-4-5.xml` -> Job 6
- `suite-sld.xml` -> Job 7
- `suite-dashboard-bughunt.xml` -> Job 8
- Plus smoke suites, API suites, AI suites, and individual fail retry suites

---

## 7. Key Technical Patterns

### Pattern 1: The "App Update Alert" Problem

**What**: Every page navigation in QA environment shows a persistent alert:
> "A new app update is available. Please save your current work before clicking Update to refresh the app."

**Impact**: Blocks site selector (shows "No sites available"), blocks page content interaction  
**Solution**: `dismissBackdrops()` method uses JavaScript to find and click the DISMISS button:
```java
var btns = document.querySelectorAll('button');
for (var i = 0; i < btns.length; i++) {
  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }
}
```
**Critical**: Must be called after every `driver.get()` or page navigation

### Pattern 2: CSS text-transform vs DOM text()

**What**: Dashboard KPI labels use CSS `text-transform: uppercase`  
**Impact**: XPath `text()` reads DOM content ("Total Assets") not rendered text ("TOTAL ASSETS")  
**Solution**: Always use Title Case in XPath text selectors, never uppercase

### Pattern 3: MUI Backdrop Interference

**What**: MUI Drawer backdrop and Beamer overlay block element clicks  
**Solution**: JS-based backdrop removal in `dismissBackdrops()`:
```java
// Remove MUI backdrops
document.querySelectorAll('.MuiBackdrop-root').forEach(el => el.remove());
// Remove Beamer overlays
document.querySelectorAll('#beamer-frame, .beamer-overlay').forEach(el => el.remove());
```

### Pattern 4: React State + Selenium

**What**: Standard `sendKeys()` doesn't update React state for controlled inputs  
**Solution**: Use `nativeInputValueSetter` + dispatch `input`/`change` events  
**Exception**: MUI DataGrid Quick Filter works with `sendKeys()` because keyboard events trigger React handlers directly

### Pattern 5: Task Detail Page Navigation

**What**: Clicking a task row navigates to `/tasks/{uuid}`, and navigating back with `driver.get()` re-triggers the app update alert  
**Solution**: After `driver.get(TASKS_URL)`, call `dismissBackdrops()` + `waitForGrid()`

---

## 8. Known Issues & Patterns

### CI-Specific Issues

1. **Headless Chrome Differences**: Some elements not interactable in headless mode that work in headed mode
2. **Timing Issues**: CI runs slower than local — need adequate waits (pause/FluentWait)
3. **App Update Alert**: Reappears on every page navigation in QA environment
4. **Session Conflicts**: 8 parallel login attempts can cause server-side session conflicts (solved with stagger delays)
5. **Grid Loading**: MUI DataGrid may render empty initially, requiring `waitForGrid()` with retry

### Application Bugs Found by Automation

1. **Bug-Fixed Assertions**: BUG021 (truncated labels) and BUG029 (dual search fields) were originally bugs but have been fixed — test assertions converted to soft pass
2. **SLD Re-login**: ZP-1585 — SLD module forces re-login under certain conditions
3. **Connection Links Missing**: ZP-671 — Connection page missing expected link elements
4. **Issue Deletion**: ZP-1622 — After deleting an issue, page doesn't redirect back to list

### Test Stability Patterns

| Pattern | Frequency | Solution |
|---------|-----------|----------|
| App update alert blocking | Every navigation | `dismissBackdrops()` before interaction |
| Grid not loaded | After navigation | `waitForGrid()` with retry loop |
| MUI Backdrop blocking click | After drawer close | JS backdrop removal |
| Stale element after navigation | After detail page | Re-find elements after `driver.get()` |
| React input not updated | Login, form fields | `nativeInputValueSetter` |

---

## 9. Test Coverage Gaps & Recommendations

### Currently Covered Modules

| Module | Test Coverage | Notes |
|--------|:------------:|-------|
| Authentication | High | 30+ test cases |
| Site Selection | High | 12+ test cases |
| Assets | Very High | 5 test class parts, ~200 TCs |
| Connections | High | 2 test class parts |
| Locations | High | 2 test class parts |
| Tasks | Medium | 1 test class |
| Issues | High | 2 test class parts |
| Work Orders | High | 2 test class parts |
| SLDs | Medium | 1 test class (~71 TCs) |
| Dashboard | Medium | Bug hunt + critical path tests |
| API | Medium | Auth, Users, Performance, Security |

### Modules With No Dedicated Test Classes

1. **Scheduling** (`/scheduling`) — No test class; only covered by BugHunt pages scan
2. **Opportunities** (`/opportunities`) — No dedicated test class
3. **Accounts** (`/accounts`) — No dedicated test class
4. **Attachments** (`/attachments`) — No dedicated test class
5. **Work Order Planning** (`/planning`) — No dedicated test class
6. **Service Agreements** (`/jobs-v2`) — No dedicated test class
7. **Arc Flash Readiness** (`/arc-flash`) — Only dashboard-level testing
8. **Condition Assessment** (`/pm-readiness`) — Only dashboard-level testing
9. **Equipment Insights** (`/equipment-insights`) — Only dashboard-level testing
10. **Z University** (`/z-university`) — No test class
11. **Settings** (`/admin`) — Covered by BugHunt admin tests, but no dedicated CRUD tests

### Recommendations for Test Improvement

1. **Add Scheduling Tests**: The drag-and-drop scheduling is complex and has known Jira bugs (ZP-1367)
2. **Add Opportunities CRUD Tests**: Active module with $10.1K pipeline value
3. **Add Accounts Tests**: 8 accounts exist — needs CRUD validation
4. **Enhance SLD Coverage**: Most complex module with most open bugs
5. **Add Attachments Tests**: Upload/download/delete verification
6. **Add Settings Admin Tests**: Site/User/Classes/PM/Forms management
7. **Add Z University Tests**: Verify all 21 article sections load correctly
8. **Add Cross-Browser Tests**: Currently Chrome-only; consider Firefox/Edge
9. **Expand API Coverage**: Currently limited to Auth/Users; add GraphQL mutation tests
10. **Add Performance Baselines**: Establish load time benchmarks per page

---

## 10. Glossary

| Term | Definition |
|------|-----------|
| **SLD** | Single Line Diagram — electrical system topology diagram showing connections |
| **NFPA 70E** | National Fire Protection Association standard for electrical safety in the workplace |
| **Arc Flash** | Dangerous electrical explosion caused by a fault; NFPA 70E compliance required |
| **NETA** | InterNational Electrical Testing Association — standards for electrical equipment testing |
| **PM** | Preventive Maintenance — scheduled maintenance tasks |
| **SKM** | Power analysis software; Egalvanic can import/export SKM files |
| **MUI** | Material UI — React component library used for all UI elements |
| **DataGrid** | MUI DataGrid component — sortable, filterable table with pagination |
| **Drawer** | MUI Drawer — slide-in panel from right side for create/edit forms |
| **Backdrop** | MUI overlay behind modals/drawers; can block clicks on underlying elements |
| **QR Code** | Each asset can have a QR code for mobile scanning |
| **Beamer** | Third-party widget for release notes/changelog (causes overlay interference) |
| **Session** | In Egalvanic context, "Session" = Work Order (URL: `/sessions`) |
| **Service Agreement** | Contract/job definition that generates work orders (URL: `/jobs-v2`) |
| **Mutation** | GraphQL mutation — any data-changing API operation (tracked in Audit Log) |
| **Company Code** | Subdomain identifier (e.g., "acme" in `acme.qa.egalvanic.ai`) |
| **Facility** | Physical site/location managed in the platform (synonym for "Site") |

---

*This document was generated through comprehensive analysis of:*
- *1,650 Jira issues from testcase/Jira.csv*
- *Playwright exploration of all 20 pages at https://acme.qa.egalvanic.ai/*
- *67 Java source files in the test automation framework*
- *6 GitHub Actions workflow files*
- *25+ TestNG suite XML configurations*
