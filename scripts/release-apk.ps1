#!/usr/bin/env pwsh
#Requires -Version 7.0

# Release APK builder and GitHub release helper
# Requirements:
# - Java + Android SDK properly set up
# - ./gradlew present in repo root
# - gh CLI authenticated (`gh auth status`)
# - git with push access

param(
    [string]$Module = "app",
    [string]$Variant = "release",
    [string]$Tag = "",
    [string]$Title = "",
    [string]$Notes = "",
    [string]$NotesFile = "",
    [switch]$Draft,
    [switch]$Prerelease,
    [switch]$SkipBuild,
    [switch]$SkipTag,
    [string]$UploadPattern = "",
    [string]$Repo = "",
    [switch]$Yes,
    [switch]$Help
)

# Help function
function Show-Help {
    @"
Usage: $(Split-Path $PSCommandPath -Leaf) [options]

Build a signed APK/AAB, create a git tag, open/update a GitHub release, and upload artifact(s).

Options:
  -Module NAME         Gradle module (default: app)
  -Variant NAME        Build variant (default: release)
  -Tag TAG             Tag name. Default: v<versionName> from Gradle.
  -Title TITLE         Release title (default: same as tag)
  -Notes TEXT          Release notes text
  -NotesFile FILE      Release notes from file (markdown supported)
  -Draft               Create as draft release
  -Prerelease          Mark release as prerelease
  -SkipBuild           Skip Gradle build (use existing artifact)
  -SkipTag             Do not create/push git tag
  -UploadPattern PATTERN  Glob for artifact(s) to upload. If omitted, auto-detect APK/AAB in module build outputs.
  -Repo OWNER/NAME     GitHub repo override (default: deduced from git remote)
  -Yes                 Non-interactive; assume yes for prompts
  -Help                Show this help
"@
}

if ($Help) {
    Show-Help
    exit 0
}

# Helper function to check if command exists
function Test-Command {
    param([string]$Command)
    return [bool](Get-Command $Command -ErrorAction SilentlyContinue)
}

# Check required commands
if (-not (Test-Command "git")) {
    Write-Error "Missing required command: git"
    exit 1
}

if (-not (Test-Path "./gradlew")) {
    Write-Error "Missing required command: ./gradlew"
    exit 1
}

if (-not (Test-Command "gh")) {
    Write-Error "Missing required command: gh"
    exit 1
}

# Check GitHub CLI authentication
try {
    gh auth status -h github.com | Out-Null
} catch {
    Write-Error "GitHub CLI not authenticated. Run: gh auth login"
    exit 1
}

# Determine repo
if ([string]::IsNullOrEmpty($Repo)) {
    try {
        $OriginUrl = git remote get-url origin 2>$null
        if ([string]::IsNullOrEmpty($OriginUrl)) {
            Write-Error "Cannot determine origin remote. Use -Repo OWNER/NAME."
            exit 1
        }
        
        if ($OriginUrl -match "github\.com[:/](.+/.+?)(\.git)?$") {
            $Repo = $matches[1]
        } else {
            Write-Error "Origin remote is not a GitHub URL. Use -Repo OWNER/NAME."
            exit 1
        }
    } catch {
        Write-Error "Cannot determine origin remote. Use -Repo OWNER/NAME."
        exit 1
    }
}

$CurrentBranch = git rev-parse --abbrev-ref HEAD

# Ensure clean working tree
$GitStatus = git status --porcelain
if ($GitStatus) {
    Write-Error "Working tree has uncommitted changes. Commit or stash first."
    exit 1
}

# Optionally build
if (-not $SkipBuild) {
    Write-Host "Running Gradle assemble for $Module`:$Variant ..."
    & ./gradlew ":$Module`:assemble$($Variant.Substring(0,1).ToUpper() + $Variant.Substring(1))" --stacktrace
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Gradle build failed"
        exit 1
    }
}

# Auto-detect version from Gradle
if ([string]::IsNullOrEmpty($Tag)) {
    $VersionName = & ./gradlew -q ":$Module`:properties" | Select-String "^versionName:" | ForEach-Object { $_.ToString().Split(': ')[1] }
    if ([string]::IsNullOrEmpty($VersionName)) {
        Write-Error "Could not determine versionName from Gradle. Use -Tag."
        exit 1
    }
    $Tag = "v$VersionName"
}

if ([string]::IsNullOrEmpty($Title)) {
    $Title = $Tag
}

# Determine artifacts
$Artifacts = @()
if (-not [string]::IsNullOrEmpty($UploadPattern)) {
    $Artifacts = Get-ChildItem -Path $UploadPattern -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
} else {
    # Try APK first then AAB
    $ApkDir = "$Module/build/outputs/apk/$Variant"
    $AabDir = "$Module/build/outputs/bundle/$Variant"
    
    if (Test-Path $ApkDir) {
        $Artifacts = Get-ChildItem -Path $ApkDir -Filter "*.apk" | Sort-Object LastWriteTime -Descending | ForEach-Object { $_.FullName }
    }
    
    if ($Artifacts.Count -eq 0 -and (Test-Path $AabDir)) {
        $Artifacts = Get-ChildItem -Path $AabDir -Filter "*.aab" | Sort-Object LastWriteTime -Descending | ForEach-Object { $_.FullName }
    }
}

if ($Artifacts.Count -eq 0) {
    Write-Error "No artifacts found. Use -UploadPattern PATTERN or build step."
    exit 1
}

# Prepare release notes
if (-not [string]::IsNullOrEmpty($NotesFile)) {
    $NotesContent = Get-Content $NotesFile -Raw
} else {
    $NotesContent = $Notes
}

# Confirm
Write-Host "Repository: $Repo"
Write-Host "Branch: $CurrentBranch"
Write-Host "Tag: $Tag"
Write-Host "Title: $Title"
Write-Host "Draft: $Draft  Prerelease: $Prerelease"
Write-Host "Artifacts (upload order):"
foreach ($a in $Artifacts) {
    Write-Host "  - $a"
}

if (-not $Yes) {
    $Response = Read-Host "Proceed? [y/N]"
    if ($Response -notmatch "^[Yy]$") {
        Write-Host "Aborted."
        exit 1
    }
}

# Tagging
if (-not $SkipTag) {
    $TagExists = git rev-parse $Tag 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Tag $Tag already exists locally. Skipping create."
    } else {
        git tag -a $Tag -m $Title
    }
    git push origin $Tag
}

# Create or get release id
$ExistingJson = $null
try {
    $ExistingJson = gh release view $Tag --repo $Repo --json id,htmlUrl 2>$null
} catch {
    # Release doesn't exist
}

if ([string]::IsNullOrEmpty($ExistingJson)) {
    Write-Host "Creating GitHub release $Tag ..."
    $GhArgs = @(
        "release", "create", $Tag,
        "--repo", $Repo
    )
    
    if ($Draft) { $GhArgs += "--draft" }
    if ($Prerelease) { $GhArgs += "--prerelease" }
    $GhArgs += "--title", $Title
    if (-not [string]::IsNullOrEmpty($NotesContent)) {
        $GhArgs += "--notes", $NotesContent
    }
    
    & gh @GhArgs 2>$null
} else {
    Write-Host "Updating existing release $Tag ..."
    $GhArgs = @(
        "release", "edit", $Tag,
        "--repo", $Repo
    )
    
    if ($Draft) { $GhArgs += "--draft" }
    if ($Prerelease) { $GhArgs += "--prerelease" }
    $GhArgs += "--title", $Title
    if (-not [string]::IsNullOrEmpty($NotesContent)) {
        $GhArgs += "--notes", $NotesContent
    }
    
    & gh @GhArgs
}

# Upload artifacts (retry on conflict)
foreach ($artifact in $Artifacts) {
    if (-not (Test-Path $artifact)) {
        Write-Error "Missing artifact: $artifact"
        exit 1
    }
    
    Write-Host "Uploading: $artifact"
    $UploadSuccess = $false
    $RetryCount = 0
    $MaxRetries = 2
    
    while (-not $UploadSuccess -and $RetryCount -lt $MaxRetries) {
        try {
            & gh release upload $Tag $artifact --repo $Repo --clobber
            $UploadSuccess = $true
        } catch {
            $RetryCount++
            if ($RetryCount -lt $MaxRetries) {
                Write-Host "Retrying upload for $artifact after a brief delay..."
                Start-Sleep -Seconds 2
            } else {
                Write-Error "Failed to upload $artifact after $MaxRetries attempts"
                exit 1
            }
        }
    }
}

$ReleaseUrl = gh release view $Tag --repo $Repo --json htmlUrl -q .htmlUrl
Write-Host "Done. View release: $ReleaseUrl"
