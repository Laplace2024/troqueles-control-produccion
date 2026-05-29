# Genera un instalador nativo de Troqueles con jpackage (JDK 21+).
# En Windows, .exe/.msi requieren WiX Toolset 3.x en PATH (candle.exe y light.exe).
# Sin WiX, el script genera una carpeta portable (--type app-image).
param(
    [ValidateSet('auto', 'exe', 'msi', 'app-image')]
    [string]$PackageType = 'auto',
    [switch]$SkipTests,
    [string]$AppVersion,
    [string]$DestDir = 'dist'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $ProjectRoot

function Get-ProjectVersion {
    param([string]$Override)
    if ($Override) {
        return $Override
    }
    [xml]$pom = Get-Content -LiteralPath (Join-Path $ProjectRoot 'pom.xml')
    return [string]$pom.project.version
}

function Test-WixToolset {
    return $null -ne (Get-Command candle.exe -ErrorAction SilentlyContinue) -and
        $null -ne (Get-Command light.exe -ErrorAction SilentlyContinue)
}

function Resolve-PackageType {
    param(
        [string]$Requested,
        [bool]$WixAvailable
    )
    if ($Requested -ne 'auto') {
        if (($Requested -eq 'exe' -or $Requested -eq 'msi') -and -not $WixAvailable) {
            throw "El tipo '$Requested' requiere WiX Toolset 3.x (candle.exe y light.exe en PATH)."
        }
        return $Requested
    }
    if ($WixAvailable) {
        return 'exe'
    }
    Write-Host 'WiX no detectado: se generara una carpeta portable (app-image).'
    return 'app-image'
}

function Test-JpackageSupportsAppContent {
    $help = & jpackage --help 2>&1 | Out-String
    return $help -match 'app-content'
}

function Prepare-InputDir {
    param(
        [string]$JarPath,
        [string]$InputDir
    )

    if (-not (Test-Path -LiteralPath $JarPath)) {
        throw "No se encontro el JAR de aplicacion: $JarPath"
    }

    if (Test-Path -LiteralPath $InputDir) {
        Remove-Item -LiteralPath $InputDir -Recurse -Force
    }

    New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
    Copy-Item -LiteralPath $JarPath -Destination (Join-Path $InputDir 'troqueles-app.jar') -Force
}

function Install-DashboardIntoPackages {
    param(
        [string]$PackageRoot,
        [string]$DashboardSource,
        [string]$AppName
    )

    $targets = @(
        (Join-Path $PackageRoot $AppName),
        $PackageRoot
    )
    foreach ($target in $targets) {
        if (-not (Test-Path -LiteralPath $target)) {
            continue
        }
        $dest = Join-Path $target 'dashboard'
        if (Test-Path -LiteralPath $dest) {
            Remove-Item -LiteralPath $dest -Recurse -Force
        }
        Copy-Item -Path $DashboardSource -Destination $dest -Recurse -Force
        Write-Host "Dashboard copiado en: $dest"
    }
}

function Publish-PackageOutput {
    param(
        [string]$SourceDir,
        [string]$TargetDir
    )

    if (Test-Path -LiteralPath $TargetDir) {
        Remove-Item -LiteralPath $TargetDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

    Get-ChildItem -LiteralPath $SourceDir | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $TargetDir -Recurse -Force
    }
}

$resolvedVersion = Get-ProjectVersion -Override $AppVersion
$wixAvailable = Test-WixToolset
$resolvedType = Resolve-PackageType -Requested $PackageType -WixAvailable $wixAvailable

$mvnArgs = @('-B', 'package')
if ($SkipTests) {
    $mvnArgs += '-DskipTests'
}
Write-Host "Compilando proyecto (mvn $($mvnArgs -join ' '))..."
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven finalizo con codigo $LASTEXITCODE."
}

$jarPath = Join-Path $ProjectRoot 'target\troqueles-app.jar'
$dashboardSource = Join-Path $ProjectRoot 'dashboard'
if (-not (Test-Path -LiteralPath $dashboardSource)) {
    throw "No se encontro la carpeta dashboard requerida por la aplicacion."
}

$buildRoot = Join-Path $env:TEMP ('troqueles-jpackage-' + [Guid]::NewGuid().ToString('N'))
$inputDir = Join-Path $buildRoot 'input'
$workDest = Join-Path $buildRoot 'out'
$appName = 'Troqueles'
$supportsAppContent = Test-JpackageSupportsAppContent
$iconPath = Join-Path $ProjectRoot 'docs\icon.ico'
$stagedIconPath = Join-Path $buildRoot 'icon.ico'

try {
    Prepare-InputDir -JarPath $jarPath -InputDir $inputDir
    New-Item -ItemType Directory -Force -Path $workDest | Out-Null

    $jpackageArgs = @(
        '--input', $inputDir,
        '--main-jar', 'troqueles-app.jar',
        '--main-class', 'com.trabajo.troqueles.Main',
        '--name', $appName,
        '--app-version', $resolvedVersion,
        '--vendor', 'Trabajo Troqueles',
        '--description', 'Control de produccion de troqueles',
        '--dest', $workDest,
        '--type', $resolvedType,
        '--java-options', '-Dfile.encoding=UTF-8'
    )

    if ($supportsAppContent) {
        $appContentPath = Join-Path $inputDir 'dashboard'
        Copy-Item -Path $dashboardSource -Destination $appContentPath -Recurse -Force
        $jpackageArgs += @('--app-content', $appContentPath)
    }

    if (Test-Path -LiteralPath $iconPath) {
        Copy-Item -LiteralPath $iconPath -Destination $stagedIconPath -Force
        $jpackageArgs += @('--icon', $stagedIconPath)
    }

    if ($resolvedType -eq 'exe' -or $resolvedType -eq 'msi') {
        $jpackageArgs += @('--win-dir-chooser', '--win-menu', '--win-shortcut')
    }

    Write-Host "Ejecutando jpackage ($resolvedType)..."
    & jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage finalizo con codigo $LASTEXITCODE."
    }

    if (-not $supportsAppContent) {
        Install-DashboardIntoPackages -PackageRoot $workDest -DashboardSource $dashboardSource -AppName $appName
    }

    $destPath = Join-Path $ProjectRoot $DestDir
    Publish-PackageOutput -SourceDir $workDest -TargetDir $destPath
    if (-not $supportsAppContent) {
        Install-DashboardIntoPackages -PackageRoot $destPath -DashboardSource $dashboardSource -AppName $appName
    }
    Write-Host "Empaquetado completado en: $destPath"
}
finally {
    if (Test-Path -LiteralPath $buildRoot) {
        Remove-Item -LiteralPath $buildRoot -Recurse -Force
    }
}
