# Makefile for XSnowWallpaper2
# Usage: make release

.PHONY: help clean build release release-draft release-prerelease release-skip-build increment-version

# Detect OS and set PowerShell path, works with WSL and Windows
ifeq ($(OS),Windows_NT)
POWERSHELL := powershell
else
UNAME := $(shell uname -s)
ifeq ($(UNAME),Linux)
POWERSHELL := /mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe
else
POWERSHELL := powershell
endif
endif

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
	$(POWERSHELL) -ExecutionPolicy Bypass -File scripts/release-apk.ps1

# Create a release with custom options
release-draft:
	$(POWERSHELL) -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -Draft

release-prerelease:
	$(POWERSHELL) -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -Prerelease

release-skip-build:
	$(POWERSHELL) -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -SkipBuild

# Increment version number in build.gradle.kts
increment-version:
	$(POWERSHELL) -ExecutionPolicy Bypass -File scripts/release-apk.ps1 -IncrementVersion
