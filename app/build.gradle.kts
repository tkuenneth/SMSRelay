import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

@Suppress("unused")
val properties = Properties().apply {
    rootProject.file("local.properties").reader().use(::load)
}

android {
    namespace = "de.thomaskuenneth.smsrelay"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.thomaskuenneth.smsrelay"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "SMTP_HOST", "\"${properties["SMTP_HOST"].toString()}\"")
        buildConfigField("String", "SMTP_PORT", "\"${properties["SMTP_PORT"].toString()}\"")
        buildConfigField("String", "SMTP_USERNAME", "\"${properties["SMTP_USERNAME"].toString()}\"")
        buildConfigField("String", "SMTP_PASSWORD", "\"${properties["SMTP_PASSWORD"].toString()}\"")
        buildConfigField("String", "SMTP_FROM", "\"${properties["SMTP_FROM"].toString()}\"")
        buildConfigField("String", "SMTP_TO", "\"${properties["SMTP_TO"].toString()}\"")
        buildConfigField("String", "TEST_NAME_01", "\"${properties["TEST_NAME_01"].toString()}\"")
        buildConfigField("String", "TEST_PHONE_01", "\"${properties["TEST_PHONE_01"].toString()}\"")
        buildConfigField("String", "TEST_PHONE_02", "\"${properties["TEST_PHONE_02"].toString()}\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.android.mail)
    implementation(libs.android.activation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    androidTestImplementation(libs.androidx.rules)
}