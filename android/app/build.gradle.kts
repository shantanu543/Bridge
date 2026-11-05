plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bridge.app"
    compileSdk = 35

    val googleServerClientId = providers.gradleProperty("bridge.google.serverClientId")
        .orElse(providers.environmentVariable("BRIDGE_GOOGLE_SERVER_CLIENT_ID"))
        .getOrElse("")

    defaultConfig {
        applicationId = "com.bridge.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Bridge Dev")
            resValue("string", "google_server_client_id", googleServerClientId)
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8001/\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
            buildConfigField("boolean", "ALLOW_LOCAL_DEMO", "true")
        }
        create("stag") {
            dimension = "environment"
            applicationIdSuffix = ".stag"
            versionNameSuffix = "-stag"
            resValue("string", "app_name", "Bridge Staging")
            resValue("string", "google_server_client_id", googleServerClientId)
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("String", "ENVIRONMENT", "\"stag\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api-stag.example.invalid/\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
            buildConfigField("boolean", "ALLOW_LOCAL_DEMO", "false")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Bridge")
            resValue("string", "google_server_client_id", googleServerClientId)
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.invalid/\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
            buildConfigField("boolean", "ALLOW_LOCAL_DEMO", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.credentials)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
