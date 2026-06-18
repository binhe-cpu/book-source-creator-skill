@echo off
REM setup-adb.bat - Download Android SDK Platform-Tools for this validator package

setlocal enabledelayedexpansion

set "INSTALL_DIR=%~dp0tools\platform-tools"
set "ADB=%INSTALL_DIR%\adb.exe"
set "ZIP=%TEMP%\platform-tools-latest-windows.zip"
set "EXTRACT=%TEMP%\platform-tools-extract"

if exist "%ADB%" (
    echo adb already installed:
    "%ADB%" version
    exit /b 0
)

echo Downloading Android SDK Platform-Tools...

REM Try primary URL (Google), then fallback mirrors
set URLS[0]=https://dl.google.com/android/repository/platform-tools-latest-windows.zip
set URLS[1]=https://mirrors.cloud.tencent.com/AndroidSDK/repository/platform-tools-latest-windows.zip
set URLS[2]=https://ghproxy.com/https://github.com/nicholasburns/platform-tools/releases/latest/download/platform-tools-latest-windows.zip

set SUCCESS=0
for /l %%i in (0,1,2) do (
    if !SUCCESS!==0 (
        set URL=!URLS[%%i]!
        echo [%%i] !URL!
        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
          "$ErrorActionPreference = 'Stop';" ^
          "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
          "try { Invoke-WebRequest -Uri '!URL!' -OutFile '%ZIP%' -TimeoutSec 120 } catch { exit 1 };" ^
          "if (Test-Path '%EXTRACT%') { Remove-Item '%EXTRACT%' -Recurse -Force };" ^
          "Expand-Archive -Path '%ZIP%' -DestinationPath '%EXTRACT%' -Force;" ^
          "if (Test-Path '%INSTALL_DIR%') { Remove-Item '%INSTALL_DIR%' -Recurse -Force };" ^
          "New-Item -ItemType Directory -Path '%INSTALL_DIR%' -Force | Out-Null;" ^
          "Move-Item -Path (Join-Path '%EXTRACT%' 'platform-tools') -Destination '%INSTALL_DIR%';" ^
        && set SUCCESS=1
        if !SUCCESS!==0 (
            echo Failed, trying next mirror...
            if exist "%ZIP%" del /f "%ZIP%"
        )
    )
)

if exist "%ZIP%" del /f "%ZIP%" >nul 2>&1
if exist "%EXTRACT%" rmdir /s /q "%EXTRACT%" >nul 2>&1

if !SUCCESS!==0 (
    echo.
    echo ERROR: All download mirrors failed.
    echo You can manually download adb from:
    echo   https://developer.android.com/studio/releases/platform-tools
    echo Extract adb.exe to: %INSTALL_DIR%
    exit /b 1
)

if not exist "%ADB%" (
    echo.
    echo ERROR: adb.exe was not found after install at %ADB%
    exit /b 1
)

echo.
echo adb installed successfully:
"%ADB%" version

endlocal
