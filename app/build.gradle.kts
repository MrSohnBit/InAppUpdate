import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mrsohn.inappupdate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mrsohn.inappupdate"
        minSdk = 24
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    signingConfigs {
        create("sign") {
            storeFile = file("../keystore/cen_keystore.jks")
            storePassword = "thsdhrhd"
            keyPassword = "thsdhrhd"
            keyAlias = "sonok"
        }
    }

    buildTypes {
        debug {
            defaultConfig.versionCode = 1
            defaultConfig.versionName = "0.1.${defaultConfig.versionCode}"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sign")
        }

        release {
            defaultConfig.versionCode = 1
            defaultConfig.versionName = "0.1.${defaultConfig.versionCode}"
            isMinifyEnabled = false
            isDebuggable  = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sign")

            val dateFormat = SimpleDateFormat("yyyyMMdd")
            val currentDate = dateFormat.format(Date())

            setProperty("archivesBaseName", "InAppUpdate_" + defaultConfig.versionName+"("+defaultConfig.versionCode+")"+ "_" + currentDate)
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.android.installreferrer:installreferrer:2.2")

    implementation(libs.inapp.update)

}