# Makefile for XSnowWallpaper2
# Usage: make release

.PHONY: help clean build release release-draft release-prerelease release-skip-build increment-version

# Default target
help:
	@echo "Available targets:"
	@echo "  help              - Show this help message"
	@echo "  clean             - Clean the project"
	@echo "  build             - Build the project"
	@echo "  release           - Create a release (build both debug & release, tag, and upload to GitHub)"
	@echo "  release-draft     - Create a draft release"
	@echo "  release-prerelease - Mark release as prerelease"
	@echo "  release-skip-build - Create release without building (use existing APK)"
	@echo "  increment-version - Increment version number in build.gradle.kts"

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

# Increment version number in build.gradle.kts
increment-version:
	powershell -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -IncrementVersion
