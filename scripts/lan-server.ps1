# Arranca el servidor LAN de Troqueles para uso multiusuario por WiFi.
# Requiere PostgreSQL accesible con la configuracion de DbSettings.
param(
    [string]$Host = "0.0.0.0",
    [int]$Port = 9010
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

Write-Host "Iniciando servidor LAN en $Host:$Port ..."
Write-Host "Healthcheck: http://$Host:$Port/health"
& mvn -B -DskipTests compile exec:java -Dexec.args="--lan-server --host $Host --port $Port"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

