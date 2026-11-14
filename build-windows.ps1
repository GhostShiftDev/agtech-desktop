# Genera el instalador de Windows en un equipo Windows 11.
# Requisitos: JDK 21 + WiX Toolset 3.x.
#
#   powershell -ExecutionPolicy Bypass -File .\build-windows.ps1
#
# Para generar también el EXE:
#   powershell -ExecutionPolicy Bypass -File .\build-windows.ps1 -Exe

param(
    [switch]$Exe
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "==> Comprobando JDK 21..." -ForegroundColor Cyan
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java no está en PATH. Instala un JDK 21 (Temurin/Microsoft OpenJDK) y vuelve a abrir PowerShell."
}
java -version 2>&1 | ForEach-Object { Write-Host "    $_" }

Write-Host "==> Comprobando WiX Toolset 3.x..." -ForegroundColor Cyan
$wix = Get-ChildItem "C:\Program Files (x86)\WiX Toolset v3.*\bin\candle.exe" -ErrorAction SilentlyContinue |
    Select-Object -First 1
if (-not $wix) {
    $cmd = Get-Command candle.exe -ErrorAction SilentlyContinue
    if ($cmd) { $wix = Get-Item $cmd.Source }
}
if (-not $wix) {
    throw @"
WiX Toolset 3.x no encontrado.
Instálalo con:
    choco install wixtoolset -y
o descárgalo de:
    https://github.com/wixtoolset/wix3/releases
"@
}
$env:PATH = "$($wix.Directory.FullName);$env:PATH"
Write-Host "    WiX: $($wix.Directory.FullName)"

if ($Exe) {
    Write-Host "==> Generando instalador EXE..." -ForegroundColor Cyan
    .\gradlew.bat --no-daemon packageExe
    $pattern = "build\compose\binaries\main\exe\*.exe"
} else {
    Write-Host "==> Generando instalador MSI..." -ForegroundColor Cyan
    .\gradlew.bat --no-daemon packageMsi
    $pattern = "build\compose\binaries\main\msi\*.msi"
}
if ($LASTEXITCODE -ne 0) { throw "La compilación falló (código $LASTEXITCODE)" }

$installer = Get-ChildItem $pattern | Select-Object -First 1
Write-Host ""
Write-Host "Instalador generado:" -ForegroundColor Green
Write-Host "    $($installer.FullName)" -ForegroundColor Green
