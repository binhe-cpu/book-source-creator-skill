@echo off
REM setup-adb.bat - Download Android SDK Platform-Tools for this validator package

setlocal

set "INSTALL_DIR=%~dp0tools\platform-tools"
set "ADB=%INSTALL_DIR%\adb.exe"
set "ZIP=%TEMP%\platform-tools-latest-windows.zip"
set "EXTRACT=%TEMP%\platform-tools-%RANDOM%%RANDOM%"
set "URL=https://dl.google.com/android/repository/platform-tools-latest-windows.zip"

if exist "%ADB%" (
    echo adb already installed:
    "%ADB%" version
    exit /b 0
)

echo Downloading Android SDK Platform-Tools from Google...
echo %URL%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
  "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP%';" ^
  "if (Test-Path '%EXTRACT%') { Remove-Item '%EXTRACT%' -Recurse -Force };" ^
  "Expand-Archive -Path '%ZIP%' -DestinationPath '%EXTRACT%' -Force;" ^
  "if (Test-Path '%INSTALL_DIR%') { Remove-Item '%INSTALL_DIR%' -Recurse -Force };" ^
  "New-Item -ItemType Directory -Path (Split-Path '%INSTALL_DIR%') -Force | Out-Null;" ^
  "Move-Item -Path (Join-Path '%EXTRACT%' 'platform-tools') -Destination '%INSTALL_DIR%';" ^
  "Remove-Item '%ZIP%' -Force;" ^
  "Remove-Item '%EXTRACT%' -Recurse -Force;"

if errorlevel 1 (
    echo.
    echo ERROR: Failed to download or extract Platform-Tools.
    exit /b 1
)

if not exist "%ADB%" (
    echo.
    echo ERROR: adb.exe was not found after install.
    exit /b 1
)

echo.
echo adb installed to:
echo %ADB%
"%ADB%" version

endlocal
