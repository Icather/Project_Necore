import java.util.Properties

val majorVersion = 2
val minorVersion = 16
val patchVersion = 4

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val releaseProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "icather.pages.dev"
    compileSdk = 36

    defaultConfig {
        applicationId = "icather.pages.dev"
        minSdk = 23
        targetSdk = 36
        versionCode = majorVersion * 1000000 + minorVersion * 1000 + patchVersion
        versionName = "${majorVersion}.${minorVersion}.${patchVersion}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "version"

    productFlavors {
        create("pure") {
            dimension = "version"
            // Pure version uses only default assets
        }
        create("full") {
            dimension = "version"
            // Full version will include the plugins from the root directory
        }
    }

    sourceSets {
        getByName("full") {
            assets.srcDir("../../protocol_plugins")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(releaseProps.getProperty("RELEASE_STORE_FILE", "../necore-release.jks"))
            storePassword = releaseProps.getProperty("RELEASE_STORE_PASSWORD", "necore2026")
            keyAlias = releaseProps.getProperty("RELEASE_KEY_ALIAS", "necore")
            keyPassword = releaseProps.getProperty("RELEASE_KEY_PASSWORD", "necore2026")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.markdown)

    // Migrated libraries
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.squareup.okhttp)
    implementation(libs.google.gson)
    implementation(libs.jetbrains.kotlinx.coroutines.core)
    implementation(libs.jetbrains.kotlinx.coroutines.android)
    implementation(libs.google.android.material)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.core.splashscreen)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // D4: WorkManager for background heartbeat
    implementation(libs.androidx.work.runtime.ktx)

    // 局域网同步：轻量嵌入式 HTTP 服务器
    implementation(libs.nanohttpd)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
