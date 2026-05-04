@echo off
:: ============================================================
:: update_ei_deployment.bat
::
:: Replaces the Edge Impulse C++ deployment used by the app.
::
:: Usage:
::   update_ei_deployment.bat <path-to-new-ei-export-folder>
::
:: The <path-to-new-ei-export-folder> is the folder you get
:: after extracting the "C++ Android library" zip downloaded
:: from EdgeImpulse Studio > Deployment.
::
:: Example:
::   update_ei_deployment.bat C:\Users\KTP\Downloads\imu-test-light-cpp-android-v3
:: ============================================================

setlocal

set DEST=%~dp0app\src\main\cpp\ei-deployment

if "%~1"=="" (
    echo ERROR: Please provide the path to the new EI deployment folder.
    echo Usage: update_ei_deployment.bat ^<path-to-ei-export^>
    exit /b 1
)

set SRC=%~1

if not exist "%SRC%" (
    echo ERROR: Source folder not found: %SRC%
    exit /b 1
)

:: Check it looks like a real EI deployment
if not exist "%SRC%\edge-impulse-sdk" (
    echo ERROR: "%SRC%" does not look like an Edge Impulse C++ export.
    echo Expected to find an "edge-impulse-sdk" folder inside it.
    exit /b 1
)

echo.
echo Source : %SRC%
echo Destination: %DEST%
echo.

:: Remove old deployment
if exist "%DEST%" (
    echo Removing old deployment...
    rmdir /s /q "%DEST%"
)

:: Copy new deployment
echo Copying new deployment...
xcopy /e /i /q "%SRC%" "%DEST%"

if errorlevel 1 (
    echo ERROR: Copy failed.
    exit /b 1
)

echo.
echo Done! Deployment updated.
echo Now do: Build ^> Clean Project, then Build ^> Rebuild Project in Android Studio.
echo.
endlocal
