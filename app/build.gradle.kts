plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.smapra.exambro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smapra.exambro"
        targetSdk = 35
        versionCode = 10103
        versionName = "1.1.3"
        vectorDrawables.useSupportLibrary = true
    }

    // ── Two product flavors: same code, different minSdk ──
    flavorDimensions += "platform"
    productFlavors {
        // Full-featured modern build: Android 8.0+
        // Kiosk DPC, adaptive icons, WindowInsetsController, edge-to-edge
        create("modern") {
            dimension = "platform"
            minSdk = 26
            buildConfigField("boolean", "IS_LEGACY", "false")
            buildConfigField("int", "PLATFORM_MIN_API", "26")
        }
        // Legacy build: Android 5.0+ with runtime graceful degradation
        // Same UI, same features, security auto-adapts via Build.VERSION checks
        create("legacy") {
            dimension = "platform"
            minSdk = 21
            applicationIdSuffix = ".legacy"
            buildConfigField("boolean", "IS_LEGACY", "true")
            buildConfigField("int", "PLATFORM_MIN_API", "21")
        }
    }

    // ── APK naming: CBT-SMAPRA-v{version}-Android{X}+-{buildType}.apk ──
    applicationVariants.all {
        val variant = this
        val androidVersion = if (variant.flavorName == "modern") "8+" else "5+"
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "CBT-SMAPRA-v${variant.versionName}-Android${androidVersion}-${variant.buildType.name}.apk"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("smapra-release.jks")
            storePassword = "smapra_cbt_pass_2026"
            keyAlias = "smapra_cbt"
            keyPassword = "smapra_cbt_pass_2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Don't fail on NewApi — we handle it with runtime checks
        disable += "NewApi"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    ksp(libs.hilt.android.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
