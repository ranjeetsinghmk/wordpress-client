# WordPress Client Android Library

WordPress Client is an Android library written in Kotlin that provides a news app theme with WordPress REST API integration. The library includes post listing, detailed post view with text-to-speech functionality, bookmarks, and customizable themes.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Environment Requirements
- **Java 11** - CRITICAL: Use Java 11, not Java 17. Java 17 causes Groovy compatibility issues with Gradle 5.6.4
- **Android SDK with API Level 29** - Required for compilation  
- **Network access to Google Maven repositories** - Required for downloading Android Gradle Plugin and dependencies

### Bootstrap and Build Commands

**IMPORTANT: Network and Environment Constraints**
- The build **REQUIRES** network access to `dl.google.com` and Google Maven repositories
- In network-restricted environments, the build **WILL FAIL** with "No address associated with hostname" errors
- **DO NOT** attempt to build without network access to Google services - it is guaranteed to fail

#### In environments with full network access:
```bash
# Set Java 11 (NEVER use Java 17)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
java -version  # Should show Java 11

# Build the project (takes 10-15 minutes on first run, NEVER CANCEL)
cd demo
./gradlew clean build  # NEVER CANCEL: Initial build takes 10-15 minutes. Set timeout to 30+ minutes.

# Run unit tests (takes 2-3 minutes, NEVER CANCEL)  
./gradlew test  # NEVER CANCEL: Takes 2-3 minutes. Set timeout to 10+ minutes.

# Run instrumented tests (requires Android device/emulator)
./gradlew connectedAndroidTest
```

#### In network-restricted environments:
```bash
# These commands WILL FAIL due to network restrictions:
cd demo && ./gradlew clean build  # FAILS: Cannot download Android Gradle Plugin from dl.google.com
cd demo && ./gradlew test         # FAILS: Cannot resolve dependencies
cd demo && ./gradlew tasks        # FAILS: Cannot configure project
```

**Build Status: Network-dependent** - The project only builds in environments with unrestricted access to Google Maven repositories.

## Project Structure

The project is organized as a multi-module Android library:

### Core Modules
- **wordpress-core**: Main library with UI components (fragments, adapters, view models)
- **wordpress-api**: REST API client using Retrofit for WordPress REST API
- **webview**: Custom WebView components with video support
- **logger**: Logging utility using SLF4J
- **demo**: Sample Android app demonstrating the library usage

### Key Dependencies
- **Kotlin 1.3.61** with Android Extensions
- **Android SDK 29** (compileSdk and targetSdk)
- **AndroidX** libraries (appcompat, core-ktx, constraintlayout, material)
- **Room Database 2.2.4** for local data storage
- **Retrofit 2.4.0** for REST API calls
- **Glide 4.9.0** for image loading
- **Gradle 5.6.4** (via wrapper)

## Validation and Testing

### When Build is Successful:
```bash
# Always run these validation steps after making changes:
cd demo && ./gradlew lint                    # Takes 1-2 minutes
cd demo && ./gradlew test                    # Takes 2-3 minutes  
cd demo && ./gradlew connectedAndroidTest    # Takes 5-10 minutes (requires device)
```

### Manual Testing Scenarios:
After building successfully, **ALWAYS** test these core user scenarios:

1. **Post List Loading**: Launch demo app, verify posts load from https://sikhsiyasat.net
2. **Post Detail Navigation**: Tap on a post to navigate to detail view
3. **Text-to-Speech**: In post detail, test text-to-speech functionality
4. **Bookmark Functionality**: Test bookmarking/unbookmarking posts
5. **Share Feature**: Test post sharing options
6. **Font Options**: Test font customization in post detail view

### CI/CD Workflow
The repository includes `.github/workflows/blank.yml` which runs:
```bash
cd demo && ./gradlew clean build
```

## Build Time Expectations

**NEVER CANCEL these operations:**
- **Initial build**: 10-15 minutes (downloads dependencies, compiles all modules)
- **Clean build**: 8-12 minutes  
- **Incremental build**: 30-60 seconds
- **Unit tests**: 2-3 minutes
- **Lint check**: 1-2 minutes
- **Connected tests**: 5-10 minutes (with Android device/emulator)

**Always set timeouts of 30+ minutes for build commands and 10+ minutes for test commands.**

## Troubleshooting Common Issues

### Java Version Issues:
```bash
# If you see Groovy compatibility errors:
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64  # Use Java 11, not 17
```

### Network Issues:
```bash
# If you see "dl.google.com: No address associated with hostname":
# This indicates network restrictions - the build cannot proceed without Google Maven access
# Resolution: Use an environment with unrestricted internet access
```

### Android SDK Issues:
```bash
# If you see "Android SDK not found":
# Install Android SDK with API level 29
# Set ANDROID_HOME environment variable
export ANDROID_HOME=/path/to/android-sdk
```

## Limitations in Restricted Environments

**Cannot be performed without network access to Google services:**
- Building the project (`./gradlew build`)
- Running tests (`./gradlew test`)  
- Listing Gradle tasks (`./gradlew tasks`)
- Installing dependencies
- Generating APK files

**Can be performed offline:**
- Code analysis using IDE tools
- Static code review
- Manual inspection of source files
- Documentation updates

## Code Organization

### Key Source Directories:
```
wordpress-core/src/main/java/com/sikhsiyasat/wordpress/
├── ui/list/           # Post list fragment and view model
├── ui/detail/         # Post detail fragment and view model  
├── models/            # Data models (Post, DisplayablePost)
├── WordpressRepository.kt    # Main repository class
└── LocalStorageService.kt    # Room database service

wordpress-api/src/main/java/com/sikhsiyasat/wordpress/api/
├── WebService.kt      # REST API interface
├── WebServiceImpl.kt  # Retrofit implementation
└── Models.kt          # API response models

demo/app/src/main/java/com/sikhsiyasat/wordpress/demo/
└── MainActivity.kt    # Demo app entry point
```

### Main Entry Points:
- **PostsFragment**: Displays list of WordPress posts with pagination
- **PostFragment**: Shows detailed post view with text-to-speech and bookmarks
- **WordpressRepository**: Coordinates between API and local storage
- **WebService**: Handles WordPress REST API calls

### Testing Files:
- Unit tests: `src/test/java/**/*Test.kt`
- Instrumented tests: `src/androidTest/java/**/*Test.kt`
- Most test files are currently minimal examples

## Common Development Tasks

### Adding New Features:
1. **Always verify build works first** in an unrestricted network environment
2. Make changes to the appropriate module (core, api, webview, logger)
3. Add corresponding tests
4. Build and test: `./gradlew clean build test`
5. Test manually using the demo app

### Working with WordPress API:
- API client is in `wordpress-api` module using Retrofit
- Base URL is configurable (demo uses "https://sikhsiyasat.net")
- Supports WordPress REST API v2 endpoints

### Database Schema:
- Uses Room database for local storage
- Schema location: `wordpress-core/schemas/`
- Main entities: Posts, Authors, Categories, Tags, Media

**Remember: All build operations require unrestricted network access to Google Maven repositories.**