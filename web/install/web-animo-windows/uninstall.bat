@echo off

podman compose down --volumes --rmi all

if %errorlevel% equ 0 (
    echo Successfully uninstalled
) else (
    echo Uninstall failed
)

exit /b
