#!/bin/bash

echo "📊 Consolidating Cross-Browser Test Reports..."
echo "=========================================="

# Create output directory
mkdir -p test-output/reports

# Create the consolidated report
cat > test-output/reports/ConsolidatedAutomationReport.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>ACME Consolidated Cross-Browser Test Report</title>
    <link rel="apple-touch-icon" href="https://cdn.jsdelivr.net/gh/extent-framework/extent-github-cdn@b00a2d0486596e73dd7326beacf352c639623a0e/commons/img/logo.png">
    <link rel="shortcut icon" href="https://cdn.jsdelivr.net/gh/extent-framework/extent-github-cdn@b00a2d0486596e73dd7326beacf352c639623a0e/commons/img/logo.png">
    <link href="https://cdn.jsdelivr.net/gh/extent-framework/extent-github-cdn@d6562a79075e061305ccfdb82f01e5e195e2d307/spark/css/spark-style.css" rel="stylesheet" />
    <link href="https://stackpath.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css" rel="stylesheet">
    <style type="text/css">
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f5f7fa;
            margin: 0;
            padding: 20px;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        .browser-section {
            background: white;
            margin-bottom: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .browser-header {
            padding: 20px;
            color: white;
            font-size: 1.5em;
            font-weight: bold;
        }
        .chrome-header { background: linear-gradient(135deg, #ff9966 0%, #ff5e62 100%); }
        .firefox-header { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        .safari-header { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
        .browser-content {
            padding: 20px;
        }
        .report-frame {
            width: 100%;
            height: 800px;
            border: none;
            border-radius: 5px;
        }
        .summary-stats {
            display: flex;
            justify-content: space-around;
            flex-wrap: wrap;
            margin: 20px 0;
        }
        .stat-card {
            background: white;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            flex: 1;
            min-width: 200px;
            margin: 10px;
        }
        .stat-number {
            font-size: 2.5em;
            font-weight: bold;
            margin: 10px 0;
        }
        .stat-label {
            color: #666;
        }
        .footer {
            text-align: center;
            margin-top: 30px;
            color: #666;
            padding: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>ACME Consolidated Cross-Browser Test Report</h1>
            <p>Comprehensive test results across multiple browsers</p>
        </div>
        
        <div class="summary-stats">
            <div class="stat-card">
                <div class="stat-number" id="totalTests">0</div>
                <div class="stat-label">Total Tests</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="passedTests" style="color: #4CAF50;">0</div>
                <div class="stat-label">Passed</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="failedTests" style="color: #f44336;">0</div>
                <div class="stat-label">Failed</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="browsersTested">0</div>
                <div class="stat-label">Browsers Tested</div>
            </div>
        </div>
        
        <!-- Browser sections will be inserted here -->
        
        <div class="footer">
            <p>Generated on: <span id="generatedDate"></span></p>
            <p>ACME Automation Testing Suite</p>
        </div>
    </div>
    
    <script>
        // Set generated date
        document.getElementById('generatedDate').textContent = new Date().toLocaleString();
        
        // Update summary statistics
        function updateSummaryStats() {
            document.getElementById('browsersTested').textContent = document.querySelectorAll('.browser-section').length;
        }
        
        // Call update summary stats when page loads
        window.addEventListener('load', updateSummaryStats);
    </script>
</body>
</html>
EOF

# Array of browsers (removed Edge)
browsers=("chrome" "firefox" "safari")

# Create a temporary file to hold browser sections
temp_file=$(mktemp)

# Insert browser sections into the temporary file
for browser in "${browsers[@]}"; do
    report_file="test-output/reports/AutomationReport_${browser}.html"
    
    if [ -f "$report_file" ]; then
        echo "Inserting $browser report..."
        
        # Convert browser name to uppercase for display
        browser_upper=$(echo "$browser" | tr '[:lower:]' '[:upper:]')
        
        # Add the browser section to the temporary file
        cat >> "$temp_file" << EOF
        <div class="browser-section">
            <div class="browser-header ${browser}-header">
                <i class="fa fa-${browser}" aria-hidden="true"></i> ${browser_upper} Browser Results
            </div>
            <div class="browser-content">
                <iframe class="report-frame" src="AutomationReport_${browser}.html"></iframe>
            </div>
        </div>
EOF
    else
        echo "No report found for $browser"
    fi
done

# Insert the browser sections into the main HTML file before the closing container div
sed -i '' '/<!-- Browser sections will be inserted here -->/r '"$temp_file"'' test-output/reports/ConsolidatedAutomationReport.html

# Clean up the temporary file
rm "$temp_file"

echo "✅ Consolidated report generated at: test-output/reports/ConsolidatedAutomationReport.html"
echo ""
echo "📋 Reports included:"
for browser in "${browsers[@]}"; do
    if [ -f "test-output/reports/AutomationReport_${browser}.html" ]; then
        browser_upper=$(echo "$browser" | tr '[:lower:]' '[:upper:]')
        echo "   - ${browser_upper} report"
    fi
done