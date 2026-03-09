# PowerShell script to run IOSLaunchTest with proper dependencies
Write-Host "Building qa-automation-suite to get Appium dependencies..."
Set-Location -Path "qa-automation-suite"
mvn compile
Set-Location -Path ".."

Write-Host "Running IOSLaunchTest..."
java -cp "src/main/java;qa-automation-suite/target/classes" IOSLaunchTest
Write-Host "Press any key to continue..."
$host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")