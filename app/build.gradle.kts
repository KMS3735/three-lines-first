import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Gemini·지도 API 키는 local.properties에만 두고 Git에 올리지 않는다
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
        versionName = "1.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
        // 지도 키는 매니페스트 메타데이터로 들어가야 한다. 키가 없으면 장소 보기 버튼을 숨긴다
        val mapsKey = localProps.getProperty("MAPS_API_KEY", "")
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        buildConfigField("boolean", "HAS_MAPS_KEY", mapsKey.isNotEmpty().toString())
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
    implementation(libs.recyclerview)

    // 촬영: 화면 위에 가이드 틀을 그리려고 CameraX 미리보기 사용
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // 글자 인식 (한국어, 기기 안에서 처리)
    implementation(libs.mlkit.text.korean)

    // AI 요약: Gemini REST API 호출
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // 저장: Room (Java라서 ksp 대신 annotationProcessor)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // 장소 지도 보기: 지도와 내 위치
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
}
