plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fptcampus.lostfoundfptcampus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fptcampus.lostfoundfptcampus"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // --- ROOM DATABASE ---
    val room_version = "2.6.1" // hoặc 2.5.2 nếu muốn ổn định hơn cho API 26
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // cho coroutine hoặc tiện ích DAO

    // --- RETROFIT + GSON ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // bản ổn định hơn 2.0.2
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // --- JWT TOKEN (chỉ decode trên Android) ---
    implementation("com.auth0.android:jwtdecode:2.0.2")

    // --- OSMDroid (bản đồ open source) ---
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("org.osmdroid:osmbonuspack:6.9.0") // hỗ trợ tìm đường, route, marker mở rộng

    // --- ZXing (QR code scanner/generator) ---
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.2")

    // --- Google Play Services (Location) ---
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- SwipeRefreshLayout ---
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // --- ANDROID CORE UI ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // --- TEST ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}