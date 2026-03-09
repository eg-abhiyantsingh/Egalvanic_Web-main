@echo off
echo Cleaning and compiling the project...
mvn clean compile

echo Running the Egalvanic automation test...
mvn exec:java -Dexec.mainClass="Egalvanic"

echo Test execution completed!
pause