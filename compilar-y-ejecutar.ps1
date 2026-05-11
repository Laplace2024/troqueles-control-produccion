# Compila todos los .java y arranca la aplicacion sin ventana de consola persistente.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force -Path 'target\classes' | Out-Null
$sources = @(Get-ChildItem -Recurse -Filter '*.java' -Path 'src\main\java' | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    Write-Host 'No se encontraron fuentes en src\main\java'
    exit 1
}
& javac -encoding UTF-8 -d 'target\classes' @sources
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Error de compilacion. Revisa los mensajes arriba.'
    exit $LASTEXITCODE
}
Start-Process -FilePath 'javaw' -ArgumentList '-cp', 'target\classes', 'com.trabajo.troqueles.Main' -WorkingDirectory $PSScriptRoot
