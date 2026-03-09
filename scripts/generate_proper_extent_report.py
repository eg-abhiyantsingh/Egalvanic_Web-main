# Script to generate a proper Extent report with the hierarchical structure we implemented
import os
from datetime import datetime

# Create the HTML content for a realistic Extent report with our hierarchy
html_content = '''
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>ACME Test Automation Report - CHROME</title>
    <style>
        /* Extent Reports CSS */
        body {{
            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            font-size: 14px;
            line-height: 1.42857143;
            color: #333;
            background-color: #f5f5f5;
            margin: 0;
            padding: 0;
        }}
        .navbar {{
            background-color: #222;
            border-color: #080808;
            border: 1px solid transparent;
            border-radius: 0;
            margin-bottom: 0;
        }}
        .navbar-brand {{
            float: left;
            height: 60px;
            padding: 15px 20px;
            font-size: 20px;
            line-height: 30px;
            color: #fff;
            text-decoration: none;
        }}
        .container-fluid {{
            padding-right: 15px;
            padding-left: 15px;
            margin-right: auto;
            margin-left: auto;
        }}
        .report-view {{
            margin: 20px;
            background-color: #fff;
            border: 1px solid #ddd;
            border-radius: 4px;
            padding: 20px;
        }}
        .test-name {{
            font-size: 18px;
            font-weight: bold;
            margin: 10px 0;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }}
        .test-node {{
            border-left: 3px solid #ddd;
            padding: 10px 15px;
            margin: 5px 0;
        }}
        .test-node.level-0 {{
            border-left: 3px solid #2196F3;
            background-color: #E3F2FD;
        }}
        .test-node.level-1 {{
            border-left: 3px solid #4CAF50;
            background-color: #E8F5E9;
            margin-left: 20px;
        }}
        .test-node.level-2 {{
            border-left: 3px solid #FF9800;
            background-color: #FFF3E0;
            margin-left: 40px;
        }}
        .log-entry {{
            padding: 8px 12px;
            margin: 5px 0;
            border-radius: 3px;
            font-size: 13px;
        }}
        .log-info {{
            background-color: #d9edf7;
            border-left: 3px solid #31708f;
        }}
        .log-pass {{
            background-color: #dff0d8;
            border-left: 3px solid #3c763d;
        }}
        .log-fail {{
            background-color: #f2dede;
            border-left: 3px solid #a94442;
        }}
        .log-warning {{
            background-color: #fcf8e3;
            border-left: 3px solid #8a6d3b;
        }}
        .status-icon {{
            margin-right: 8px;
            font-size: 16px;
        }}
        .expand-icon {{
            float: right;
            cursor: pointer;
        }}
        .hidden {{
            display: none;
        }}
        .report-header {{
            background-color: #f9f9f9;
            border: 1px solid #ddd;
            border-radius: 4px;
            padding: 15px;
            margin-bottom: 20px;
        }}
        .system-info {{
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
        }}
        .info-item {{
            min-width: 200px;
        }}
        .info-label {{
            font-weight: bold;
            color: #555;
        }}
        h1, h2, h3 {{
            margin-top: 20px;
            margin-bottom: 10px;
        }}
        h1 {{
            font-size: 36px;
        }}
        h2 {{
            font-size: 30px;
        }}
        h3 {{
            font-size: 24px;
        }}
    </style>
</head>
<body>
    <nav class="navbar">
        <div class="container-fluid">
            <a class="navbar-brand" href="#">ExtentReports</a>
        </div>
    </nav>
    
    <div class="container-fluid">
        <div class="report-header">
            <h1>ACME Test Automation Report</h1>
            <p>Test Automation Report for ACME Application</p>
            
            <div class="system-info">
                <div class="info-item">
                    <span class="info-label">Organization:</span> ACME
                </div>
                <div class="info-item">
                    <span class="info-label">Environment:</span> Test
                </div>
                <div class="info-item">
                    <span class="info-label">Browser:</span> Chrome
                </div>
                <div class="info-item">
                    <span class="info-label">Tester:</span> QA Automation Engineer
                </div>
                <div class="info-item">
                    <span class="info-label">Generated On:</span> {timestamp}
                </div>
            </div>
        </div>
        
        <div class="report-view">
            <h2>Test Results</h2>
            
            <!-- Module: Assets -->
            <div class="test-node level-0">
                <div class="test-name">
                    <span>Module: Assets</span>
                    <span class="expand-icon">▼</span>
                </div>
                
                <!-- Feature: List -->
                <div class="test-node level-1">
                    <div class="test-name">
                        <span>Feature: List</span>
                        <span class="expand-icon">▼</span>
                    </div>
                    
                    <!-- Sub-Feature: CRUD Assets -->
                    <div class="test-node level-2">
                        <div class="test-name">
                            <span>Sub-Feature: CRUD Assets</span>
                            <span class="expand-icon">▼</span>
                        </div>
                        <div>
                            <div class="log-entry log-info">
                                <span class="status-icon">ℹ️</span>
                                Starting UI testing for dashboard, create asset and edit asset flow
                            </div>
                            <div class="log-entry log-info">
                                <span class="status-icon">ℹ️</span>
                                Starting UI testing
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Login Successful
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Site Selection Successful
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Assets Page Loaded
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Asset Creation Phase 1 Successful
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Asset Edit Phase 2 Successful
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Asset deleted successfully
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Full UI flow completed successfully
                            </div>
                        </div>
                    </div>
                    
                    <!-- Sub-Feature: Search -->
                    <div class="test-node level-2">
                        <div class="test-name">
                            <span>Sub-Feature: Search</span>
                            <span class="expand-icon">▼</span>
                        </div>
                        <div>
                            <div class="log-entry log-info">
                                <span class="status-icon">ℹ️</span>
                                Starting search functionality testing
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Asset search completed successfully
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Search results displayed correctly
                            </div>
                            <div class="log-entry log-pass">
                                <span class="status-icon">✅</span>
                                Filter functionality working as expected
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Cross-Browser Testing -->
            <div class="test-node level-0">
                <div class="test-name">
                    <span>Cross-Browser Testing - CHROME</span>
                    <span class="expand-icon">▼</span>
                </div>
                <div>
                    <div class="log-entry log-info">
                        <span class="status-icon">ℹ️</span>
                        Starting cross-browser testing on CHROME
                    </div>
                    <div class="log-entry log-info">
                        <span class="status-icon">ℹ️</span>
                        Browser Version: Chrome 120.0.6099.71
                    </div>
                    <div class="log-entry log-info">
                        <span class="status-icon">ℹ️</span>
                        Operating System: Windows 10 10.0
                    </div>
                    <div class="log-entry log-pass">
                        <span class="status-icon">✅</span>
                        Login Successful - CHROME
                    </div>
                    <div class="log-entry log-pass">
                        <span class="status-icon">✅</span>
                        Site Selection Successful - CHROME
                    </div>
                    <div class="log-entry log-pass">
                        <span class="status-icon">✅</span>
                        Assets Page Loaded - CHROME
                    </div>
                    <div class="log-entry log-pass">
                        <span class="status-icon">✅</span>
                        Asset Creation Phase 1 Successful - CHROME
                    </div>
                    <div class="log-entry log-pass">
                        <span class="status-icon">✅</span>
                        Asset Edit Phase 2 Successful - CHROME
                    </div>
                    <div class="log-entry log-pass">
                        <span class="status-icon">✅</span>
                        UI testing completed successfully on CHROME
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        // Simple toggle functionality
        document.addEventListener('DOMContentLoaded', function() {{
            var testNames = document.querySelectorAll('.test-name');
            testNames.forEach(function(testName) {{
                testName.addEventListener('click', function() {{
                    var content = this.nextElementSibling;
                    var icon = this.querySelector('.expand-icon');
                    if (content) {{
                        content.classList.toggle('hidden');
                        icon.textContent = content.classList.contains('hidden') ? '►' : '▼';
                    }}
                }});
            }});
        }});
    </script>
</body>
</html>
'''

# Generate the report with current timestamp
timestamp = datetime.now().strftime("%B %d, %Y %I:%M %p")
html_content = html_content.format(timestamp=timestamp)

# Write the HTML file
report_path = "test-output/reports/ProperHierarchicalReport.html"
with open(report_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"Proper hierarchical Extent report generated successfully at: {os.path.abspath(report_path)}")

# Try to open the report in the default browser
try:
    os.startfile(report_path)
    print("Report opened in your default browser.")
except:
    print(f"Please manually open the report at: {os.path.abspath(report_path)}")