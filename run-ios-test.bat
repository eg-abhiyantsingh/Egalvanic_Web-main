@echo off
echo Building qa-automation-suite to get Appium dependencies...
cd qa-automation-suite
call mvn compile
cd ..

echo Running IOSLaunchTest...
java -cp "src/main/java;qa-automation-suite/target/classes" IOSLaunchTest
pause