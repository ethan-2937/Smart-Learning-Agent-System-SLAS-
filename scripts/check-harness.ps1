$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$configPath = Join-Path $root "harness/quality-baseline.json"
$failures = [System.Collections.Generic.List[string]]::new()

function Add-Failure([string]$message) {
    $failures.Add($message)
}

function Get-LineCount([string]$path) {
    return [System.IO.File]::ReadAllLines($path).Count
}

function Get-RelativePath([string]$path) {
    return $path.Substring($root.Length + 1).Replace("\", "/")
}

$required = @(
    "AGENTS.md",
    "backend/AGENTS.md",
    "frontend/AGENTS.md",
    "docs/PRODUCT.md",
    "docs/ARCHITECTURE.md",
    "docs/QUALITY.md",
    "docs/HARNESS_GUIDE.md",
    "docs/tasks/TEMPLATE.md",
    "harness/quality-baseline.json",
    "thesis/AGENTS.md",
    "thesis/metadata.yml",
    "thesis/evidence/claim-evidence-matrix.csv",
    "thesis/experiments/registry.json",
    "scripts/check-thesis.ps1"
)

foreach ($relative in $required) {
    if (-not (Test-Path -LiteralPath (Join-Path $root $relative))) {
        Add-Failure "Missing required harness file: $relative. Restore it or update the repository map deliberately."
    }
}

if (-not (Test-Path -LiteralPath $configPath)) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

$config = Get-Content -Raw -LiteralPath $configPath | ConvertFrom-Json

Get-ChildItem -Path $root -Filter "AGENTS.md" -Recurse -File |
    Where-Object { $_.FullName -notmatch "[\\/](target|node_modules|dist)[\\/]" } |
    ForEach-Object {
        $lines = Get-LineCount $_.FullName
        if ($lines -gt $config.agentGuideMaxLines) {
            $relative = Get-RelativePath $_.FullName
            Add-Failure "$relative has $lines lines (limit $($config.agentGuideMaxLines)). Fix: keep AGENTS.md as a map and move details into docs/."
        }
    }

$sourceRoots = @(
    (Join-Path $root "backend/src/main/java"),
    (Join-Path $root "frontend/src")
)

foreach ($sourceRoot in $sourceRoots) {
    Get-ChildItem -Path $sourceRoot -Recurse -File | ForEach-Object {
        $relative = Get-RelativePath $_.FullName
        $extension = $_.Extension.ToLowerInvariant()
        $budget = $null

        $exact = $config.fileLineBudgets.PSObject.Properties[$relative]
        if ($null -ne $exact) {
            $budget = [int]$exact.Value
        } else {
            $default = $config.defaultLineBudgets.PSObject.Properties[$extension]
            if ($null -ne $default) {
                $budget = [int]$default.Value
            }
        }

        if ($null -ne $budget) {
            $lines = Get-LineCount $_.FullName
            if ($lines -gt $budget) {
                Add-Failure "$relative has $lines lines (budget $budget). Fix: extract a cohesive module, then lower the budget when the hotspot shrinks."
            }
        }
    }
}

$tests = Get-ChildItem -Path (Join-Path $root "backend/src/test/java") -Filter "*Test.java" -Recurse -File -ErrorAction SilentlyContinue
if (@($tests).Count -eq 0) {
    Add-Failure "No backend tests found. Fix: add a deterministic *Test.java under backend/src/test/java."
}

$controllerRoot = Join-Path $root "backend/src/main/java/com/hlju/learning/controller"
$forbiddenControllerImport = '^import com\.hlju\.learning\.(repository|mapper|serviceimpl|vector)\.'
Get-ChildItem -Path $controllerRoot -Filter "*.java" -Recurse -File | ForEach-Object {
    $matches = Select-String -Path $_.FullName -Pattern $forbiddenControllerImport
    foreach ($match in $matches) {
        $relative = Get-RelativePath $_.FullName
        Add-Failure "$relative imports a concrete lower layer at line $($match.LineNumber). Fix: depend on a service interface and keep provider details behind the service boundary."
    }
}

$frontendRoot = Join-Path $root "frontend/src"
Get-ChildItem -Path $frontendRoot -Include "*.ts", "*.vue" -Recurse -File |
    Where-Object { (Get-RelativePath $_.FullName) -ne "frontend/src/api.ts" } |
    ForEach-Object {
        $matches = Select-String -Path $_.FullName -Pattern '\bfetch\s*\('
        foreach ($match in $matches) {
            $relative = Get-RelativePath $_.FullName
            Add-Failure "$relative calls fetch directly at line $($match.LineNumber). Fix: place transport behavior in the API layer."
        }
    }

if ($failures.Count -gt 0) {
    Write-Host "Harness checks failed ($($failures.Count)):" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "Harness checks passed." -ForegroundColor Green
