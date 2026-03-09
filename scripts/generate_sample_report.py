# Script to generate a sample Extent report with the hierarchical structure
import os
from datetime import datetime

# Create the HTML content for the sample report
html_content = '''
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Sample Extent Report - Hierarchical Structure</title>
    <style>
        body {{
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }}
        .container {{
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        .header {{
            background: linear-gradient(135deg, #4CAF50, #45a049);
            color: white;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
            text-align: center;
        }}
        .test-node {{
            border: 1px solid #ddd;
            margin: 15px 0;
            border-radius: 5px;
            overflow: hidden;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }}
        .node-header {{
            padding: 15px 20px;
            cursor: pointer;
            font-weight: bold;
            display: flex;
            justify-content: space-between;
            align-items: center;
            transition: background-color 0.3s;
        }}
        .node-header:hover {{
            opacity: 0.9;
        }}
        .node-header.module {{
            background: linear-gradient(to right, #2196F3, #1976D2);
            color: white;
        }}
        .node-header.feature {{
            background: linear-gradient(to right, #4CAF50, #388E3C);
            color: white;
        }}
        .node-header.subfeature {{
            background: linear-gradient(to right, #FF9800, #F57C00);
            color: white;
        }}
        .node-content {{
            padding: 0 20px;
            max-height: 0;
            overflow: hidden;
            transition: max-height 0.3s ease-out, padding 0.3s ease;
        }}
        .node-content.expanded {{
            padding: 20px;
            max-height: 1000px;
        }}
        .log-entry {{
            padding: 12px 15px;
            margin: 8px 0;
            border-radius: 4px;
            display: flex;
            align-items: center;
        }}
        .log-info {{
            background-color: #E3F2FD;
            border-left: 4px solid #2196F3;
        }}
        .log-pass {{
            background-color: #E8F5E9;
            border-left: 4px solid #4CAF50;
        }}
        .log-fail {{
            background-color: #FFEBEE;
            border-left: 4px solid #F44336;
        }}
        .log-warning {{
            background-color: #FFF3E0;
            border-left: 4px solid #FF9800;
        }}
        .status-icon {{
            margin-right: 10px;
            font-size: 18px;
        }}
        .toggle-btn {{
            background: none;
            border: none;
            color: white;
            font-size: 18px;
            cursor: pointer;
            width: 30px;
            height: 30px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
        }}
        .toggle-btn:hover {{
            background-color: rgba(255,255,255,0.2);
        }}
        .legend {{
            display: flex;
            gap: 20px;
            margin: 20px 0;
            flex-wrap: wrap;
            justify-content: center;
        }}
        .legend-item {{
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 15px;
            border-radius: 20px;
            font-weight: bold;
        }}
        .color-box {{
            width: 20px;
            height: 20px;
            border-radius: 3px;
        }}
        .timestamp {{
            text-align: right;
            color: #666;
            font-size: 14px;
            margin-top: 20px;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Extent Report - Hierarchical Structure Demo</h1>
            <p>Showing the implemented Module → Feature → Sub-Feature hierarchy</p>
        </div>

        <div class="legend">
            <div class="legend-item" style="background-color: #E3F2FD; color: #1976D2;">
                <div class="color-box" style="background: linear-gradient(to right, #2196F3, #1976D2);"></div>
                <span>Module Level</span>
            </div>
            <div class="legend-item" style="background-color: #E8F5E9; color: #388E3C;">
                <div class="color-box" style="background: linear-gradient(to right, #4CAF50, #388E3C);"></div>
                <span>Feature Level</span>
            </div>
            <div class="legend-item" style="background-color: #FFF3E0; color: #F57C00;">
                <div class="color-box" style="background: linear-gradient(to right, #FF9800, #F57C00);"></div>
                <span>Sub-Feature Level</span>
            </div>
        </div>

        <!-- Module: Assets -->
        <div class="test-node">
            <div class="node-header module" onclick="toggleNode(this)">
                <span>Module: Assets</span>
                <button class="toggle-btn">▼</button>
            </div>
            <div class="node-content expanded">
                
                <!-- Feature: List -->
                <div class="test-node">
                    <div class="node-header feature" onclick="toggleNode(this)">
                        <span>Feature: List</span>
                        <button class="toggle-btn">▼</button>
                    </div>
                    <div class="node-content expanded">
                        
                        <!-- Sub-Feature: CRUD Assets -->
                        <div class="test-node">
                            <div class="node-header subfeature" onclick="toggleNode(this)">
                                <span>Sub-Feature: CRUD Assets</span>
                                <button class="toggle-btn">▼</button>
                            </div>
                            <div class="node-content expanded">
                                <div class="log-entry log-info">
                                    <span class="status-icon">ℹ️</span>
                                    <span>INFO: Starting UI testing for dashboard, create asset and edit asset flow</span>
                                </div>
                                <div class="log-entry log-info">
                                    <span class="status-icon">ℹ️</span>
                                    <span>INFO: Starting UI testing</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Login Successful</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Site Selection Successful</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Assets Page Loaded</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Asset Creation Phase 1 Successful</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Asset Edit Phase 2 Successful</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Asset deleted successfully</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Full UI flow completed successfully</span>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Sub-Feature: Search -->
                        <div class="test-node">
                            <div class="node-header subfeature" onclick="toggleNode(this)">
                                <span>Sub-Feature: Search</span>
                                <button class="toggle-btn">▼</button>
                            </div>
                            <div class="node-content expanded">
                                <div class="log-entry log-info">
                                    <span class="status-icon">ℹ️</span>
                                    <span>INFO: Starting search functionality testing</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Asset search completed successfully</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Search results displayed correctly</span>
                                </div>
                                <div class="log-entry log-pass">
                                    <span class="status-icon">✅</span>
                                    <span>PASS: Filter functionality working as expected</span>
                                </div>
                            </div>
                        </div>
                        
                    </div>
                </div>
                
            </div>
        </div>
        
        <div class="timestamp">
            Generated on: {timestamp}
        </div>
    </div>

    <script>
        function toggleNode(element) {{
            const content = element.nextElementSibling;
            const button = element.querySelector('.toggle-btn');
            
            content.classList.toggle('expanded');
            
            if (content.classList.contains('expanded')) {{
                button.textContent = '▲';
            }} else {{
                button.textContent = '▼';
            }}
        }}
        
        // Auto-expand all nodes on page load
        document.addEventListener('DOMContentLoaded', function() {{
            const contents = document.querySelectorAll('.node-content');
            contents.forEach(content => {{
                content.classList.add('expanded');
            }});
            
            const buttons = document.querySelectorAll('.toggle-btn');
            buttons.forEach(button => {{
                button.textContent = '▲';
            }});
        }});
    </script>
</body>
</html>
'''

# Generate the report with current timestamp
timestamp = datetime.now().strftime("%B %d, %Y at %I:%M %p")
html_content = html_content.format(timestamp=timestamp)

# Write the HTML file
report_path = "test-output/reports/SampleHierarchicalReport.html"
with open(report_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"Sample hierarchical report generated successfully at: {os.path.abspath(report_path)}")

# Try to open the report in the default browser
try:
    os.startfile(report_path)
    print("Report opened in your default browser.")
except:
    print(f"Please manually open the report at: {os.path.abspath(report_path)}")