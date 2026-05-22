plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.bgremover.vqzpt"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD") ?: "bgwrap123"
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "bgwrap123"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("generateReleaseKeystore") {
    doLast {
        val keystoreFile = file("${rootDir}/my-upload-key.jks")
        if (!keystoreFile.exists()) {
            println("Generating release keystore at: ${keystoreFile.absolutePath}")
            val cmd = listOf(
                "keytool", "-genkeypair",
                "-alias", "upload",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-keystore", keystoreFile.absolutePath,
                "-storepass", "bgwrap123",
                "-keypass", "bgwrap123",
                "-dname", "CN=BGWrap, OU=AIStudio, O=Google, L=MountainView, S=California, C=US"
            )
            println("Running command: ${cmd.joinToString(" ")}")
            val process = ProcessBuilder(cmd).start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("Release keystore generated successfully!")
            } else {
                val errorStream = process.errorStream.bufferedReader().readText()
                println("Error generating keystore. Exit code: $exitCode. Error: $errorStream")
            }
        } else {
            println("Release keystore already exists.")
        }
    }
}

tasks.register("copyReleaseBinaries") {
    dependsOn("assembleRelease", "bundleRelease", "assembleDebug")
    doLast {
        val apkFile = file("${project.layout.buildDirectory.get().asFile}/outputs/apk/release/app-release.apk")
        val aabFile = file("${project.layout.buildDirectory.get().asFile}/outputs/bundle/release/app-release.aab")
        val debugApkFile = file("${project.layout.buildDirectory.get().asFile}/outputs/apk/debug/app-debug.apk")
        
        // These are the files at the absolute root '/' which corresponds to the IDE workspace root
        val apkDestRoot = file("/BGWrap_release.apk")
        val aabDestRoot = file("/BGWrap_release.aab")
        val debugApkDestRoot = file("/BGWrap_debug.apk")
        
        fun printSize(f: java.io.File, name: String) {
            val sizeInMb = f.length().toDouble() / (1024 * 1024)
            println("FILE INFO: $name is %.2f MB (${f.length()} bytes) at ${f.absolutePath}")
        }
        
        if (apkFile.exists()) {
            apkFile.copyTo(apkDestRoot, overwrite = true)
            println("SUCCESS: Copied signed release APK to absolute root /")
            printSize(apkDestRoot, "apkDestRoot")
        } else {
            println("WARNING: Release APK not found at: ${apkFile.absolutePath}")
        }
        
        if (aabFile.exists()) {
            aabFile.copyTo(aabDestRoot, overwrite = true)
            println("SUCCESS: Copied signed release AAB to absolute root /")
            printSize(aabDestRoot, "aabDestRoot")
        } else {
            println("WARNING: Release AAB not found at: ${aabFile.absolutePath}")
        }

        if (debugApkFile.exists()) {
            debugApkFile.copyTo(debugApkDestRoot, overwrite = true)
            println("SUCCESS: Copied debug APK to absolute root /")
            printSize(debugApkDestRoot, "debugApkDestRoot")
        } else {
            println("WARNING: Debug APK not found at: ${debugApkFile.absolutePath}")
        }
    }
}


