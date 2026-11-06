plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val ghRepoEnv = System.getenv("GITHUB_REPOSITORY")?.takeIf { it.contains("/") }
val ghOwner = ghRepoEnv?.substringBefore("/") ?: "xboyrex"
val ghRepo = ghRepoEnv?.substringAfter("/") ?: "Untrack"

android {
    namespace = "com.xboyrex.untrack"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xboyrex.untrack"
        minSdk = 24
        targetSdk = 36
        versionCode = 100
        versionName = "1.0.0"

        buildConfigField("String", "GITHUB_OWNER", "\"$ghOwner\"")
        buildConfigField("String", "GITHUB_REPO", "\"$ghRepo\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")?.takeIf { file("release.keystore").exists() }
                ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/libandroidx.graphics.path.so"
        }
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/com/android/build/**",
                "META-INF/version-control-info.textproto",
                "META-INF/*.version",
                "DebugProbesKt.bin",
                "META-INF/**/LICENSE.txt",
                "META-INF/**/LICENSE"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.google.re2j:re2j:1.8")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("javax.inject:javax.inject:1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
