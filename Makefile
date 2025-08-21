# Makefile for XSnowWallpaper2
# Usage: make release

.PHONY: help clean build release release-draft release-prerelease release-skip-build build-rename

# Default target
help:
	@echo "Available targets:"
	@echo "  help              - Show this help message"
	@echo "  clean             - Clean the project"
	@echo "  build             - Build the project"
	@echo "  release           - Create a release (build, tag, and upload to GitHub)"
	@echo "  release-draft     - Create a draft release"
	@echo "  release-prerelease - Mark release as prerelease"
	@echo "  release-skip-build - Create release without building (use existing APK)"
	@echo "  build-rename      - Build and rename APK with custom naming"

# Clean the project
clean:
	.\gradlew.bat clean

# Build the project
build:
	.\gradlew.bat assembleRelease

# Create a release
release:
	powershell -ExecutionPolicy Bypass -File scripts/release-apk.ps1

# Create a release with custom options
release-draft:
	powershell -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -Draft

release-prerelease:
	powershell -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -Prerelease

release-skip-build:
	powershell -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -SkipBuild

# Build and rename APK with custom naming
build-rename:
	.\gradlew.bat renameApk
