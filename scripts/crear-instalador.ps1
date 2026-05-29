# Crea el instalador de Troqueles en dist/ usando JDK local + Maven portable + jpackage.
# Uso (desde la raiz del proyecto):
#   .\scripts\crear-instalador.ps1
#   .\scripts\crear-instalador.ps1 -PackageType app-image -SkipTests
param(
    [ValidateSet('auto', 'exe', 'msi', 'app-image')]
    [string]$PackageType = 'auto',
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $ProjectRoot

function Find-JdkHome {
    $candidates = @(
        $env:JAVA_HOME,
        'C:\Program Files\Eclipse Adoptium\jdk-21*',
        'C:\Program Files\Java\jdk-21*',
        'C:\Program Files\Eclipse Adoptium\jdk-17*',
        'C:\Program Files\Java\jdk-17'
    )
    foreach ($pattern in $candidates) {
        if (-not $pattern) { continue }
        if ($pattern -match '\*') {
            $resolved = Get-ChildItem -Path ($pattern -replace '\\jdk-\d+\*','') -Filter (Split-Path $pattern -Leaf) -Directory -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending |
                Select-Object -First 1
            if ($resolved) { return $resolved.FullName }
        } elseif (Test-Path -LiteralPath $pattern) {
            return $pattern
        }
    }
    throw 'No se encontro JDK 17+ con jpackage. Instala Temurin 21 o JDK 17 en Program Files\Java.'
}

function Ensure-Maven {
    param([string]$ToolsDir)
    $mavenRoot = Join-Path $ToolsDir 'apache-maven-3.9.9'
    $mvnCmd = Join-Path $mavenRoot 'bin\mvn.cmd'
    if (Test-Path -LiteralPath $mvnCmd) {
        return $mvnCmd
    }

    $zipUrl = 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip'
    $zipPath = Join-Path $env:TEMP 'apache-maven-3.9.9-bin.zip'
    Write-Host "Descargando Maven 3.9.9..."
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath -UseBasicParsing
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
    Expand-Archive -LiteralPath $zipPath -DestinationPath $ToolsDir -Force
    Remove-Item -LiteralPath $zipPath -Force -ErrorAction SilentlyContinue
    if (-not (Test-Path -LiteralPath $mvnCmd)) {
        throw "Maven no quedo disponible en $mvnCmd"
    }
    return $mvnCmd
}

$jdkHome = Find-JdkHome
$jdkMajor = [int]((Get-Content (Join-Path $jdkHome 'release') | Where-Object { $_ -match '^JAVA_VERSION=' }) -replace 'JAVA_VERSION="(\d+).*','$1')
if ($jdkMajor -lt 17) {
    throw "JDK demasiado antiguo ($jdkHome). Se requiere 17 o superior."
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;" + ($env:Path -replace [regex]::Escape("$jdkHome\bin;"), '')

$toolsDir = Join-Path $ProjectRoot '.tools'
$mvnCmd = Ensure-Maven -ToolsDir $toolsDir

$mvnArgs = @(
    '-B', 'package',
    '-DskipTests'
)
if (-not $SkipTests) {
    $mvnArgs = @('-B', 'package')
}
if ($jdkMajor -lt 21) {
    Write-Host "JDK $jdkMajor detectado: compilando con release 17."
    $mvnArgs += @(
        '-Dmaven.compiler.release=17',
        '-Dmaven.compiler.source=17',
        '-Dmaven.compiler.target=17'
    )
}

Write-Host "Compilando con Maven..."
& $mvnCmd @mvnArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven finalizo con codigo $LASTEXITCODE."
}

$env:Path = (Join-Path (Split-Path $mvnCmd -Parent) '') + ';' + $env:Path

$jpackageArgs = @{
    PackageType = $PackageType
}
if ($SkipTests) {
    $jpackageArgs.SkipTests = $true
}

& (Join-Path $PSScriptRoot 'jpackage.ps1') @jpackageArgs
