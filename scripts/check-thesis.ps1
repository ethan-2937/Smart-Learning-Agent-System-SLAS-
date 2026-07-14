param(
    [switch]$Release
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$thesisRoot = Join-Path $root "thesis"
$failures = [System.Collections.Generic.List[string]]::new()

function Add-Failure([string]$message) {
    $failures.Add($message)
}

function Resolve-RepoPath([string]$relativePath) {
    return Join-Path $root $relativePath.Replace("/", [IO.Path]::DirectorySeparatorChar)
}

function Get-MetadataValue([string]$content, [string]$key) {
    $pattern = "(?m)^" + [regex]::Escape($key) + ":\s*(.*)$"
    $match = [regex]::Match($content, $pattern)
    if (-not $match.Success) {
        return $null
    }
    $value = $match.Groups[1].Value.Trim()
    if ($value.Length -ge 2 -and $value.StartsWith('"') -and $value.EndsWith('"')) {
        return $value.Substring(1, $value.Length - 2)
    }
    return $value
}

$requiredPaths = @(
    "thesis/AGENTS.md",
    "thesis/README.md",
    "thesis/metadata.yml",
    "thesis/templates/manifest.json",
    "thesis/paper/outline.md",
    "thesis/evidence/claim-evidence-matrix.csv",
    "thesis/experiments/registry.json"
)

foreach ($relativePath in $requiredPaths) {
    if (-not (Test-Path -LiteralPath (Resolve-RepoPath $relativePath))) {
        Add-Failure "Missing thesis artifact: $relativePath. Restore it before making academic claims."
    }
}

$metadataPath = Resolve-RepoPath "thesis/metadata.yml"
if (Test-Path -LiteralPath $metadataPath) {
    $metadata = Get-Content -Raw -LiteralPath $metadataPath -Encoding UTF8
    $requiredMetadata = @("college", "major", "class_name", "student_name", "student_id", "supervisor", "title_zh", "title_en")
    foreach ($key in $requiredMetadata) {
        $value = Get-MetadataValue $metadata $key
        if ([string]::IsNullOrWhiteSpace($value)) {
            Add-Failure "thesis/metadata.yml is missing '$key'. Fill confirmed metadata; never invent it."
        }
    }

    foreach ($key in @("proposal_source", "design_source")) {
        $value = Get-MetadataValue $metadata $key
        if ([string]::IsNullOrWhiteSpace($value)) {
            Add-Failure "thesis/metadata.yml is missing '$key'."
        } elseif (-not (Test-Path -LiteralPath (Resolve-RepoPath $value))) {
            Add-Failure "Metadata field '$key' points to missing source '$value'."
        }
    }

    if ($Release) {
        foreach ($key in @("proposal_date", "project_start_date", "project_end_date")) {
            $value = Get-MetadataValue $metadata $key
            if ([string]::IsNullOrWhiteSpace($value)) {
                Add-Failure "Release blocked: thesis/metadata.yml field '$key' is still empty."
            }
        }
    }
}

$manifestPath = Resolve-RepoPath "thesis/templates/manifest.json"
if (Test-Path -LiteralPath $manifestPath) {
    try {
        $manifest = Get-Content -Raw -LiteralPath $manifestPath -Encoding UTF8 | ConvertFrom-Json
        foreach ($template in $manifest.templates) {
            $templatePath = Resolve-RepoPath $template.repositoryPath
            if (-not (Test-Path -LiteralPath $templatePath)) {
                Add-Failure "Missing retained template: $($template.repositoryPath)."
                continue
            }
            $item = Get-Item -LiteralPath $templatePath
            $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $templatePath).Hash.ToLowerInvariant()
            if ($hash -ne $template.sha256) {
                Add-Failure "Template hash changed: $($template.repositoryPath). Restore the retained original; generate documents from a copy."
            }
            if ($item.Length -ne [long]$template.bytes) {
                Add-Failure "Template byte size changed: $($template.repositoryPath)."
            }
        }
    } catch {
        Add-Failure "Invalid thesis/templates/manifest.json: $($_.Exception.Message)"
    }
}

$matrixPath = Resolve-RepoPath "thesis/evidence/claim-evidence-matrix.csv"
if (Test-Path -LiteralPath $matrixPath) {
    try {
        $claims = @(Import-Csv -LiteralPath $matrixPath -Encoding UTF8)
        if ($claims.Count -eq 0) {
            Add-Failure "The claim-evidence matrix is empty. Register thesis claims before drafting results."
        }
        $allowedClaimStatuses = @("supported", "partial", "planned", "rejected")
        $seenClaims = @{}
        foreach ($claim in $claims) {
            if ([string]::IsNullOrWhiteSpace($claim.claim_id) -or $seenClaims.ContainsKey($claim.claim_id)) {
                Add-Failure "Claim IDs must be non-empty and unique: '$($claim.claim_id)'."
            } else {
                $seenClaims[$claim.claim_id] = $true
            }
            if ($claim.status -notin $allowedClaimStatuses) {
                Add-Failure "Claim $($claim.claim_id) has invalid status '$($claim.status)'."
            }
            if (-not [string]::IsNullOrWhiteSpace($claim.chapter) -and -not (Test-Path -LiteralPath (Resolve-RepoPath $claim.chapter))) {
                Add-Failure "Claim $($claim.claim_id) points to missing chapter '$($claim.chapter)'."
            }
            if ($claim.status -in @("supported", "partial")) {
                if ([string]::IsNullOrWhiteSpace($claim.implementation_evidence)) {
                    Add-Failure "Claim $($claim.claim_id) is $($claim.status) but has no implementation evidence."
                } else {
                    foreach ($evidencePath in $claim.implementation_evidence.Split(';')) {
                        $trimmed = $evidencePath.Trim()
                        if (-not [string]::IsNullOrWhiteSpace($trimmed) -and -not (Test-Path -LiteralPath (Resolve-RepoPath $trimmed))) {
                            Add-Failure "Claim $($claim.claim_id) points to missing implementation evidence '$trimmed'."
                        }
                    }
                }
            }
            if ($Release -and $claim.status -in @("partial", "planned")) {
                Add-Failure "Release blocked: claim $($claim.claim_id) is still '$($claim.status)'. Support it or mark it rejected and remove it from the paper."
            }
        }
    } catch {
        Add-Failure "Invalid claim-evidence matrix: $($_.Exception.Message)"
    }
}

$registryPath = Resolve-RepoPath "thesis/experiments/registry.json"
if (Test-Path -LiteralPath $registryPath) {
    try {
        $registry = Get-Content -Raw -LiteralPath $registryPath -Encoding UTF8 | ConvertFrom-Json
        $allowedExperimentStatuses = @("planned", "ready", "running", "complete", "blocked", "rejected")
        $seenExperiments = @{}
        foreach ($experiment in $registry.experiments) {
            if ([string]::IsNullOrWhiteSpace($experiment.id) -or $seenExperiments.ContainsKey($experiment.id)) {
                Add-Failure "Experiment IDs must be non-empty and unique: '$($experiment.id)'."
            } else {
                $seenExperiments[$experiment.id] = $true
            }
            if ($experiment.status -notin $allowedExperimentStatuses) {
                Add-Failure "Experiment $($experiment.id) has invalid status '$($experiment.status)'."
            }
            if ($experiment.status -eq "complete" -and -not (Test-Path -LiteralPath (Resolve-RepoPath $experiment.summaryPath))) {
                Add-Failure "Experiment $($experiment.id) is complete but summary '$($experiment.summaryPath)' is missing."
            }
            if ($Release -and $experiment.status -notin @("complete", "rejected")) {
                Add-Failure "Release blocked: experiment $($experiment.id) is '$($experiment.status)'."
            }
        }
    } catch {
        Add-Failure "Invalid thesis/experiments/registry.json: $($_.Exception.Message)"
    }
}

if ($Release) {
    $chapterRoot = Resolve-RepoPath "thesis/paper/chapters"
    Get-ChildItem -Path $chapterRoot -Filter "*.md" -File | ForEach-Object {
        $text = Get-Content -Raw -LiteralPath $_.FullName -Encoding UTF8
        if ($text -match "(?m)^draft_status:\s*(skeleton|pending)\s*$|\bTODO\b") {
            Add-Failure "Release blocked: $($_.Name) still contains draft status or TODO markers."
        }
    }
    foreach ($relativePath in @("thesis/releases/opening-report-final.docx", "thesis/releases/thesis-final.docx")) {
        if (-not (Test-Path -LiteralPath (Resolve-RepoPath $relativePath))) {
            Add-Failure "Release blocked: missing formal deliverable '$relativePath'."
        }
    }
}

if ($failures.Count -gt 0) {
    Write-Host "Thesis checks failed ($($failures.Count)):" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

$mode = if ($Release) { "release" } else { "working" }
Write-Host "Thesis checks passed ($mode mode)." -ForegroundColor Green
