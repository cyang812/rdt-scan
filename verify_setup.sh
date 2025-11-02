#!/bin/bash

# Build verification script for RDT-Scan Android project
# This script checks that all required tools and dependencies are properly configured

echo "=== RDT-Scan Build Environment Verification ==="
echo ""

# Check Java version
echo "Checking Java version..."
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -ge 17 ]; then
    echo "✓ Java $java_version detected (JDK 17+ required)"
else
    echo "✗ Java $java_version detected - JDK 17 or later required"
    echo "  Download from: https://adoptium.net/"
fi
echo ""

# Check Android SDK
echo "Checking Android SDK..."
if [ -n "$ANDROID_HOME" ] || [ -n "$ANDROID_SDK_ROOT" ]; then
    sdk_path="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
    echo "✓ Android SDK found at: $sdk_path"
    
    # Check for required SDK components
    if [ -d "$sdk_path/platforms/android-34" ]; then
        echo "✓ Android SDK Platform 34 installed"
    else
        echo "✗ Android SDK Platform 34 not found"
        echo "  Install via SDK Manager: Android 14.0 (API 34)"
    fi
    
    if [ -d "$sdk_path/build-tools" ] && [ -n "$(ls -A $sdk_path/build-tools 2>/dev/null)" ]; then
        latest_build_tools=$(ls -1 $sdk_path/build-tools | sort -V | tail -1)
        echo "✓ Build Tools installed (latest: $latest_build_tools)"
    else
        echo "✗ Build Tools not found"
        echo "  Install via SDK Manager: Android SDK Build-Tools"
    fi
    
    if [ -d "$sdk_path/ndk" ] && [ -n "$(ls -A $sdk_path/ndk 2>/dev/null)" ]; then
        latest_ndk=$(ls -1 $sdk_path/ndk | sort -V | tail -1)
        echo "✓ NDK installed (version: $latest_ndk)"
    else
        echo "✗ NDK not found"
        echo "  Install via SDK Manager: NDK (Side by side)"
    fi
else
    echo "✗ Android SDK not found"
    echo "  Set ANDROID_HOME or ANDROID_SDK_ROOT environment variable"
    echo "  Or install Android Studio: https://developer.android.com/studio"
fi
echo ""

# Check Gradle wrapper
echo "Checking Gradle..."
if [ -f "./gradlew" ]; then
    echo "✓ Gradle wrapper found"
    gradle_version=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\(.*\)-all.zip/\1/')
    echo "  Version: $gradle_version"
    if [ "$gradle_version" == "8.7" ]; then
        echo "✓ Gradle version is correct (8.7)"
    fi
else
    echo "✗ Gradle wrapper not found"
    echo "  Run from project root directory"
fi
echo ""

# Check network connectivity
echo "Checking network connectivity..."
if curl -s --head --request GET https://maven.google.com | grep "200 OK" > /dev/null; then
    echo "✓ maven.google.com is accessible"
else
    echo "✗ maven.google.com is not accessible"
    echo "  Check your internet connection and firewall settings"
fi

if curl -s --head --request GET https://repo1.maven.org | grep "200 OK" > /dev/null; then
    echo "✓ repo1.maven.org (Maven Central) is accessible"
else
    echo "✗ repo1.maven.org (Maven Central) is not accessible"
    echo "  Check your internet connection and firewall settings"
fi
echo ""

# Summary
echo "=== Summary ==="
echo "If all checks pass, you can build the project with:"
echo "  ./gradlew clean"
echo "  ./gradlew assembleDebug"
echo ""
echo "For detailed build requirements, see: README.md"
echo "For migration details, see: MIGRATION_NOTES.md"
