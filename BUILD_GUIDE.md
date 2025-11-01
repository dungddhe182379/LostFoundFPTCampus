# 🔨 BUILD & DEPLOYMENT GUIDE

## 📦 Build APK

### Debug Build (cho test)

```bash
1. Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Hoặc chạy lệnh:
   ./gradlew assembleDebug
3. APK location:
   app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (production)

```bash
1. Tạo keystore (lần đầu):
   keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000

2. Cấu hình signing trong build.gradle.kts:
   signingConfigs {
       create("release") {
           storeFile = file("my-release-key.keystore")
           storePassword = "password"
           keyAlias = "my-key-alias"
           keyPassword = "password"
       }
   }

3. Build:
   ./gradlew assembleRelease

4. APK location:
   app/build/outputs/apk/release/app-release.apk
```

---

## 📱 Install APK

### Via USB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via File Manager

```
1. Copy APK to phone
2. Open file manager
3. Click APK file
4. Allow "Install from unknown sources"
5. Install
```

---

## 🚀 Deploy to Google Play Store

### 1. Prepare

- [ ] Tạo app icon (512x512 PNG)
- [ ] Tạo screenshots (phone + tablet)
- [ ] Viết description
- [ ] Tạo privacy policy URL
- [ ] Build signed release APK
- [ ] Test thoroughly

### 2. Create App Bundle (AAB)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### 3. Upload

```
1. Login to Google Play Console
2. Create new app
3. Upload app-release.aab
4. Fill in store listing
5. Set pricing & distribution
6. Submit for review
```

---

## 🔧 Environment Configuration

### Development

```java
// ApiClient.java
private static final String BASE_URL = "http://10.0.2.2:8080/Vietsuky2/";
private static final boolean DEBUG = true;
```

### Production

```java
// ApiClient.java
private static final String BASE_URL = "https://vietsuky.com/Vietsuky2/";
private static final boolean DEBUG = false;
```

### Build Variants (Recommended)

```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/Vietsuky2/\"")
            buildConfigField("Boolean", "DEBUG_MODE", "true")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://vietsuky.com/Vietsuky2/\"")
            buildConfigField("Boolean", "DEBUG_MODE", "false")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

---

## 🔐 Security Checklist

### Before Release

- [ ] Remove all `Log.d()` / `Log.e()` statements
- [ ] Disable OkHttp logging interceptor
- [ ] Change JWT secret key
- [ ] Enable ProGuard/R8
- [ ] Enable certificate pinning
- [ ] Remove test accounts
- [ ] Check for hardcoded credentials
- [ ] Test with release build

### ProGuard Rules

```proguard
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.fptcampus.lostfoundfptcampus.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
```

---

## 📊 Build Optimization

### Reduce APK Size

```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
    
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}
```

### Enable R8 Optimization

```
# gradle.properties
android.enableR8.fullMode=true
```

---

## 🧪 Testing Before Release

### 1. Functional Testing

- [ ] Login/Register
- [ ] Create item
- [ ] View items
- [ ] Filter items
- [ ] GPS location
- [ ] Offline mode
- [ ] Logout

### 2. Performance Testing

- [ ] App starts in < 3 seconds
- [ ] Smooth scrolling
- [ ] No memory leaks
- [ ] Battery efficient

### 3. Device Testing

- [ ] Phone (small screen)
- [ ] Tablet (large screen)
- [ ] Different Android versions (8.0 - 14)
- [ ] Different manufacturers (Samsung, Xiaomi, etc.)

---

## 📈 Version Management

### Version Code & Name

```kotlin
// build.gradle.kts
defaultConfig {
    versionCode = 1      // Increment for each release
    versionName = "1.0.0" // Semantic versioning
}
```

### Git Tags

```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

---

## 🐛 Debug Tools

### Android Studio Profiler

```
View → Tool Windows → Profiler
→ CPU, Memory, Network, Energy
```

### Database Inspector

```
View → Tool Windows → App Inspection
→ Database Inspector
```

### Layout Inspector

```
Tools → Layout Inspector
```

### Logcat Filters

```
# Show only app logs
package:mine level:debug

# Show errors
level:error

# Custom tag
tag:ApiClient
```

---

## 📦 APK Analyzer

```bash
# Android Studio
Build → Analyze APK
→ Select app-release.apk

# Command line
./gradlew :app:analyzeReleaseBundle
```

---

## 🔄 CI/CD Setup (Optional)

### GitHub Actions

```yaml
# .github/workflows/android.yml
name: Android CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Build with Gradle
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v2
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📝 Release Checklist

- [ ] Update version code & name
- [ ] Update CHANGELOG.md
- [ ] Run all tests
- [ ] Build release APK/AAB
- [ ] Test release build on device
- [ ] Create git tag
- [ ] Upload to Play Store
- [ ] Update documentation
- [ ] Notify users

---

**Build Time:** ~30 seconds (debug)  
**APK Size:** ~8-15 MB (without obfuscation)  
**Min SDK:** 26 (Android 8.0)  
**Target SDK:** 36

---

**Last Updated:** November 1, 2025
