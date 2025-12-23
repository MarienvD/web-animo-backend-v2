@echo off

podman compose up -d

if %errorlevel% equ 0 (
    echo Successfully installed
) else (
    echo Installation failed
)

exit /b
