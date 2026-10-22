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

    // 촬영: 화면 위에 가이드 틀을 그리려고 CameraX 미리보기 사용
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // 글자 인식 (한국어, 기기 안에서 처리)
    implementation(libs.mlkit.text.korean)
    testImplementation(libs.junit)
}
