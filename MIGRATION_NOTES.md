# Android Project Modernization - Migration Notes

## Overview
This document describes the changes made to modernize the rdt-scan Android project to build successfully with current Android Studio (2024+).

## Changes Made

### 1. Gradle Configuration

#### Gradle Wrapper
- **Before:** 5.6.4
- **After:** 8.7
- **Reason:** Java 17 compatibility and support for latest Android Gradle Plugin

#### Android Gradle Plugin (AGP)
- **Before:** 3.6.1
- **After:** 8.5.0
- **Reason:** Required for modern Android Studio and SDK support

### 2. SDK Versions

#### App Module
- **compileSdk:** 26 → 34
- **targetSdk:** 26 → 34
- **minSdk:** 21 (unchanged)

#### OpenCV Module
- **compileSdk:** 23 → 34
- **targetSdk:** 21 → 34
- **minSdk:** 8 → 21

### 3. Repository Configuration

- **Removed:** jcenter() (deprecated and shut down)
- **Added:** mavenCentral() as primary repository
- **Kept:** google() for Android-specific dependencies

### 4. AndroidX Migration

#### Gradle Properties
Added migration flags:
```properties
android.useAndroidX=true
android.enableJetifier=true
```

#### Namespace Declaration
AGP 8+ requires namespace declaration in build.gradle:
- App module: `namespace 'edu.washington.cs.ubicomplab.rdt_reader'`
- OpenCV module: `namespace 'org.opencv'`

#### Dependency Updates
All Android Support Library dependencies migrated to AndroidX:

| Old (android.support) | New (androidx) |
|----------------------|----------------|
| appcompat-v7:26.1.0 | appcompat:1.7.0 |
| support-v4:26.1.0 | legacy-support-v4:1.0.0 |
| design:26.1.0 | material:1.12.0 |
| constraint-layout:1.0.2 | constraintlayout:2.2.0 |
| cardview-v7:26.1.0 | cardview:1.0.0 |
| test.runner:1.0.1 | test:runner:1.6.2 |
| test.espresso:3.0.1 | test.espresso:3.6.1 |

Other dependency updates:
- junit: 4.12 → 4.13.2
- play-services-vision: 11+ → 20.1.3
- MPAndroidChart: v3.0.3 → v3.1.0
- stepview: 1.2.0 → 1.5.1

### 5. Source Code Migration

#### Java Files Updated (5 files)
All import statements migrated from `android.support.*` to `androidx.*`:

1. **MainActivity.java**
   - `android.support.v4.app.ActivityCompat` → `androidx.core.app.ActivityCompat`
   - `android.support.v4.content.ContextCompat` → `androidx.core.content.ContextCompat`
   - `android.support.v7.app.AppCompatActivity` → `androidx.appcompat.app.AppCompatActivity`

2. **ImageResultActivity.java**
   - `android.support.v7.app.AppCompatActivity` → `androidx.appcompat.app.AppCompatActivity`

3. **SettingsDialogFragment.java**
   - `android.support.v7.app.AlertDialog` → `androidx.appcompat.app.AlertDialog`

4. **ImageQualityView.java**
   - `android.support.annotation.NonNull` → `androidx.annotation.NonNull`
   - `android.support.v4.app.ActivityCompat` → `androidx.core.app.ActivityCompat`
   - `android.support.v4.content.ContextCompat` → `androidx.core.content.ContextCompat`

5. **ViewportUsingBitmap.java**
   - `android.support.annotation.ColorRes` → `androidx.annotation.ColorRes`

#### Test Files Updated (1 file)
**ExampleInstrumentedTest.java:**
- `android.support.test.InstrumentationRegistry` → `androidx.test.platform.app.InstrumentationRegistry`
- `android.support.test.runner.AndroidJUnit4` → `androidx.test.ext.junit.runners.AndroidJUnit4`
- API change: `InstrumentationRegistry.getTargetContext()` → `InstrumentationRegistry.getInstrumentation().getTargetContext()`

#### Layout XML Files Updated (4 files)
All ConstraintLayout references updated:
- `android.support.constraint.ConstraintLayout` → `androidx.constraintlayout.widget.ConstraintLayout`

Files updated:
- activity_main.xml
- activity_image_result.xml
- image_quality_view.xml
- dialog_setting.xml

## Testing Status

### Current Limitation
The build cannot be completed in the current environment due to network restrictions. The domain `dl.google.com` (Google's Maven repository) is blocked, which prevents downloading:
- Android Gradle Plugin 8.5.0
- AndroidX libraries
- Google Play Services
- Other dependencies

### Required Action
To test the build, whitelist the following domains:
- `dl.google.com`
- `maven.google.com`

Alternatively, test in an environment with unrestricted internet access.

### Expected Build Commands

Once network access is available, run:

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Compatibility

### Minimum Requirements
- **Android Studio:** Iguana (2023.2.1) or later
- **JDK:** 17 or later
- **Gradle:** 8.7
- **Android SDK:** API 34 (Android 14)

### Runtime Requirements
- **Minimum SDK:** API 21 (Android 5.0 Lollipop)
- **Target SDK:** API 34 (Android 14)

## Additional Notes

1. **Jetifier:** The `android.enableJetifier=true` flag ensures that any third-party libraries still using Android Support Library are automatically migrated to AndroidX at build time.

2. **buildToolsVersion:** No longer required in AGP 8+; it's automatically selected based on compileSdk.

3. **Native Build:** The project includes CMake native build configuration, which remains unchanged and compatible.

4. **OpenCV Module:** Updated to use the same SDK versions as the main app for consistency.

## Future Improvements

Consider these additional modernizations:
1. Migrate to Kotlin for new code
2. Adopt Jetpack Compose for UI
3. Update to latest OpenCV version if available
4. Implement dependency version management (using version catalogs)
5. Add GitHub Actions for CI/CD
6. Update minimum SDK to 24 (covers 95%+ of devices)
7. Implement Material Design 3 (Material You)

## References

- [Android Gradle Plugin Release Notes](https://developer.android.com/build/releases/gradle-plugin)
- [AndroidX Migration Guide](https://developer.android.com/jetpack/androidx/migrate)
- [Gradle 8.7 Release Notes](https://docs.gradle.org/8.7/release-notes.html)
