plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.note"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.note"
        minSdk = 26                // 支持 Android 8.0+
        targetSdk = 36             // 目标设为 Android 16
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("C:\\Users\\12732\\AndroidStudioProjects\\key\\gunbrad")
            storePassword = "2njx2sm."
            keyAlias = "key0"
            keyPassword = "2njx2sm."
        }
    }

                            
                        


    buildTypes {
        release {
            isMinifyEnabled = true     // 启用代码混淆和优化
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            
            // 数据安全标签
            manifestPlaceholders["data_collection"] = "false"
            manifestPlaceholders["data_sharing"] = "false"
            manifestPlaceholders["data_security"] = "true"
        }
        debug {
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
        isCoreLibraryDesugaringEnabled = true
    }
    
}

dependencies {
    // Core Android libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Architecture components
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    
    // Room database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    // WorkManager
    implementation(libs.work.runtime)
    
    // Image loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    
    // Java 17 desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}