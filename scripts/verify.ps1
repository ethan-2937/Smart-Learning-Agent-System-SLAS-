$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Assert-LastExitCode([string]$step) {
    if ($LASTEXITCODE -ne 0) {
        throw "$step failed with exit code $LASTEXITCODE."
    }
}

Write-Host "[1/4] Checking repository harness..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "check-harness.ps1")
if (-not $?) {
    throw "Harness checks failed."
}

Write-Host "[2/4] Checking thesis evidence..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "check-thesis.ps1")
if (-not $?) {
    throw "Thesis checks failed."
}

Write-Host "[3/4] Running backend tests..." -ForegroundColor Cyan
Push-Location (Join-Path $root "backend")
try {
    if ($env:OS -eq "Windows_NT") {
        & ".\mvnw.cmd" test
    } else {
        & "./mvnw" test
    }
    Assert-LastExitCode "Backend tests"
} finally {
    Pop-Location
}

Write-Host "[4/4] Building frontend..." -ForegroundColor Cyan
Push-Location (Join-Path $root "frontend")
try {
    if (-not (Test-Path -LiteralPath "node_modules")) {
        & npm ci
        Assert-LastExitCode "Frontend dependency install"
    }
    & npm run build
    Assert-LastExitCode "Frontend build"
} finally {
    Pop-Location
}

Write-Host "All verification steps passed." -ForegroundColor Green
