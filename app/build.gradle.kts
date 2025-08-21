plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.roadwaffle.xsnowwallpaper2"
    compileSdk = 36
    


    defaultConfig {
        applicationId = "com.roadwaffle.xsnowwallpaper2"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Task to print version name for release script
tasks.register("printVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}

// Task to rename APK with custom naming
tasks.register("renameApk") {
    dependsOn("assembleRelease")
    doLast {
        val apkDir = file("build/outputs/apk/release")
        val originalApk = apkDir.listFiles()?.find { it.name.endsWith(".apk") }
        
        if (originalApk != null) {
            val newName = "xsnowwallpaper2-release-v${android.defaultConfig.versionName}-unsigned.apk"
            val newFile = File(apkDir, newName)
            
            if (newFile.exists()) {
                newFile.delete()
            }
            
            originalApk.renameTo(newFile)
            println("Renamed APK to: $newName")
        }
    }
}
