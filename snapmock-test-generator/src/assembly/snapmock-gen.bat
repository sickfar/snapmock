@echo off

set JAR_FILE=%~dp0snapmock-test-generator.jar

if not exist "%JAR_FILE%" (
    echo Error: JAR file not found: %JAR_FILE%
    exit /b 1
)

java -jar "%JAR_FILE%" %*
