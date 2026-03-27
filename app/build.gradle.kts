plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.upermarket"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.upermarket"
        minSdk = 26
        targetSdk = 35 
        versionCode = 9
        versionName = "1.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyD1wT18QTiCkIWfGOoitiLSYSm93K7SRHk\"")
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/com.upermarket.app/AndroidManifest.xml")
            java.setSrcDirs(listOf("src/com.upermarket.app/java"))
            res.setSrcDirs(listOf("src/com.upermarket.app/res"))
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    
    // GOOGLE MAPS - DEPENDANCES DIRECTES (Zéro erreur de synchro)
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:6.4.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation(libs.coil.compose)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.datastore.preferences)
    
    // Autres libs nécessaires
    implementation(libs.androidx.compose.material.icons.extended)
}
