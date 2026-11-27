import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.lepramim"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lepramim"
        minSdk = 30
        targetSdk = 37
        versionCode = 10
        versionName = "0.5.4"
    }

    // A chave fica fora do repositório, em keystore.properties.
    val chave = rootProject.file("keystore.properties")
    signingConfigs {
        if (chave.exists()) {
            create("publicacao") {
                val p = Properties().apply { chave.inputStream().use(::load) }
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Debug e release com a mesma chave, para um instalar por cima do outro.
        debug {
            signingConfigs.findByName("publicacao")?.let { signingConfig = it }
        }
        release {
            signingConfig = signingConfigs.findByName("publicacao") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.mlkit.texto)
    implementation(libs.mlkit.scanner)
    implementation(libs.pdfbox)
    implementation(libs.androidx.documentfile)
}
