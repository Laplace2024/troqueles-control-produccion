@echo off
REM Lanza la app tras recompilar (asi los cambios en codigo se reflejan siempre).
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compilar-y-ejecutar.ps1"
if errorlevel 1 pause
