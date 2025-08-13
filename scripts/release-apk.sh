#!/usr/bin/env bash
set -euo pipefail

# Release APK builder and GitHub release helper
# Requirements:
# - Java + Android SDK properly set up
# - ./gradlew present in repo root
# - gh CLI authenticated (`gh auth status`)
# - git with push access

# Defaults
MODULE="app"
VARIANT="release"
TAG=""
TITLE=""
NOTES_FILE=""
NOTES=""
DRAFT=false
PRERELEASE=false
SKIP_BUILD=false
SKIP_TAG=false
UPLOAD_PATTERN=""
REPO=""

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Build a signed APK/AAB, create a git tag, open/update a GitHub release, and upload artifact(s).

Options:
  -m, --module NAME         Gradle module (default: app)
  -v, --variant NAME        Build variant (default: release)
  -t, --tag TAG             Tag name. Default: v<versionName> from Gradle.
  -T, --title TITLE         Release title (default: same as tag)
  -n, --notes TEXT          Release notes text
  -f, --notes-file FILE     Release notes from file (markdown supported)
  -d, --draft               Create as draft release
  -p, --prerelease          Mark release as prerelease
  -s, --skip-build          Skip Gradle build (use existing artifact)
  --skip-tag                Do not create/push git tag
  -u, --upload PATTERN      Glob for artifact(s) to upload. If omitted, auto-detect APK/AAB in module build outputs.
  -r, --repo OWNER/NAME     GitHub repo override (default: deduced from git remote)
  -y, --yes                 Non-interactive; assume yes for prompts
  -h, --help                Show this help
EOF
}

CONFIRM_YES=false

for arg in "$@"; do
  case "$arg" in
    -h|--help) usage; exit 0;;
  esac
done

while [[ $# -gt 0 ]]; do
  case "$1" in
    -m|--module) MODULE="$2"; shift 2;;
    -v|--variant) VARIANT="$2"; shift 2;;
    -t|--tag) TAG="$2"; shift 2;;
    -T|--title) TITLE="$2"; shift 2;;
    -n|--notes) NOTES="$2"; shift 2;;
    -f|--notes-file) NOTES_FILE="$2"; shift 2;;
    -d|--draft) DRAFT=true; shift;;
    -p|--prerelease) PRERELEASE=true; shift;;
    -s|--skip-build) SKIP_BUILD=true; shift;;
    --skip-tag) SKIP_TAG=true; shift;;
    -u|--upload) UPLOAD_PATTERN="$2"; shift 2;;
    -r|--repo) REPO="$2"; shift 2;;
    -y|--yes) CONFIRM_YES=true; shift;;
    --) shift; break;;
    *) echo "Unknown option: $1" >&2; usage; exit 1;;
  esac
done

require_cmd() { command -v "$1" >/dev/null 2>&1 || { echo "Missing required command: $1" >&2; exit 1; }; }

require_cmd git
require_cmd ./gradlew
require_cmd gh

if ! gh auth status -h github.com >/dev/null 2>&1; then
  echo "GitHub CLI not authenticated. Run: gh auth login" >&2
  exit 1
fi

# Determine repo
if [[ -z "$REPO" ]]; then
  ORIGIN_URL=$(git remote get-url origin 2>/dev/null || true)
  if [[ -z "$ORIGIN_URL" ]]; then
    echo "Cannot determine origin remote. Use --repo OWNER/NAME." >&2
    exit 1
  fi
  if [[ "$ORIGIN_URL" =~ github.com[:/](.+/.+?)(\.git)?$ ]]; then
    REPO="${BASH_REMATCH[1]}"
  else
    echo "Origin remote is not a GitHub URL. Use --repo OWNER/NAME." >&2
    exit 1
  fi
fi

current_branch=$(git rev-parse --abbrev-ref HEAD)

# Ensure clean working tree
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Working tree has uncommitted changes. Commit or stash first." >&2
  exit 1
fi

# Optionally build
if [[ "$SKIP_BUILD" != true ]]; then
  echo "Running Gradle assemble for $MODULE:$VARIANT ..."
  ./gradlew ":$MODULE:assemble${VARIANT^}" --stacktrace
fi

# Auto-detect version from Gradle
if [[ -z "$TAG" ]]; then
  VERSION_NAME=$(./gradlew -q ":$MODULE:properties" | awk -F ': ' '/^versionName:/ {print $2; exit}')
  if [[ -z "$VERSION_NAME" ]]; then
    echo "Could not determine versionName from Gradle. Use --tag." >&2
    exit 1
  fi
  TAG="v$VERSION_NAME"
fi

if [[ -z "$TITLE" ]]; then
  TITLE="$TAG"
fi

# Determine artifacts
declare -a ARTIFACTS
if [[ -n "$UPLOAD_PATTERN" ]]; then
  # shellcheck disable=SC2206
  ARTIFACTS=( $(compgen -G "$UPLOAD_PATTERN" || true) )
else
  # Try APK first then AAB
  APK_DIR="$MODULE/build/outputs/apk/$VARIANT"
  AAB_DIR="$MODULE/build/outputs/bundle/$VARIANT"
  if [[ -d "$APK_DIR" ]]; then
    mapfile -t ARTIFACTS < <(find "$APK_DIR" -type f -name "*.apk" -printf "%T@ %p\n" | sort -nr | awk '{print $2}' )
  fi
  if [[ ${#ARTIFACTS[@]} -eq 0 && -d "$AAB_DIR" ]]; then
    mapfile -t ARTIFACTS < <(find "$AAB_DIR" -type f -name "*.aab" -printf "%T@ %p\n" | sort -nr | awk '{print $2}' )
  fi
fi

if [[ ${#ARTIFACTS[@]} -eq 0 ]]; then
  echo "No artifacts found. Use --upload PATTERN or build step." >&2
  exit 1
fi

# Prepare release notes
if [[ -n "$NOTES_FILE" ]]; then
  NOTES_CONTENT=$(cat "$NOTES_FILE")
else
  NOTES_CONTENT="$NOTES"
fi

# Confirm
echo "Repository: $REPO"
echo "Branch: $current_branch"
echo "Tag: $TAG"
echo "Title: $TITLE"
echo "Draft: $DRAFT  Prerelease: $PRERELEASE"
echo "Artifacts (upload order):"
for a in "${ARTIFACTS[@]}"; do echo "  - $a"; done

if [[ "$CONFIRM_YES" != true ]]; then
  read -r -p "Proceed? [y/N] " ans
  if [[ ! "$ans" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
  fi
fi

# Tagging
if [[ "$SKIP_TAG" != true ]]; then
  if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "Tag $TAG already exists locally. Skipping create."
  else
    git tag -a "$TAG" -m "$TITLE"
  fi
  git push origin "$TAG"
fi

# Create or get release id
set +e
EXISTING_JSON=$(gh release view "$TAG" --repo "$REPO" --json id,htmlUrl 2>/dev/null)
status=$?
set -e

if [[ $status -ne 0 || -z "$EXISTING_JSON" ]]; then
  echo "Creating GitHub release $TAG ..."
  gh release create "$TAG" \
    --repo "$REPO" \
    ${DRAFT:+--draft} \
    ${PRERELEASE:+--prerelease} \
    --title "$TITLE" \
    ${NOTES_CONTENT:+--notes "$NOTES_CONTENT"} \
    2>/dev/null || true
else
  echo "Updating existing release $TAG ..."
  gh release edit "$TAG" \
    --repo "$REPO" \
    ${DRAFT:+--draft} \
    ${PRERELEASE:+--prerelease} \
    --title "$TITLE" \
    ${NOTES_CONTENT:+--notes "$NOTES_CONTENT"}
fi

# Upload artifacts (retry on conflict)
for artifact in "${ARTIFACTS[@]}"; do
  if [[ ! -f "$artifact" ]]; then
    echo "Missing artifact: $artifact" >&2
    exit 1
  fi
  echo "Uploading: $artifact"
  if ! gh release upload "$TAG" "$artifact" --repo "$REPO" --clobber; then
    echo "Retrying upload for $artifact after a brief delay..."
    sleep 2
    gh release upload "$TAG" "$artifact" --repo "$REPO" --clobber
  fi
done

echo "Done. View release: $(gh release view "$TAG" --repo "$REPO" --json htmlUrl -q .htmlUrl)"
