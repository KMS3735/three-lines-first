import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Gemini API 키는 local.properties에만 두고 Git에 올리지 않는다
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.kms3735.threelinesfirst"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kms3735.threelinesfirst"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
}
