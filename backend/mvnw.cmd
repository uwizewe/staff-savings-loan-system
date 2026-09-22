@echo off
setlocal
set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_DIR=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%"
set "ARCHIVE=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip"
if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  echo Maven is not installed. Downloading Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%ARCHIVE%'; Expand-Archive -Path '%ARCHIVE%' -DestinationPath '%BASE_DIR%.mvn' -Force"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_DIR%\bin\mvn.cmd" %*
endlocal

