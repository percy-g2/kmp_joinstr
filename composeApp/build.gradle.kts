
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }
    
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain by getting

        val wasmJsMain by getting

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            implementation(libs.kstore.file)

            implementation(cryptographyLibs.provider.apple)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.startup.runtime)

            implementation(libs.ktor.client.android)
            implementation(libs.ktor.client.cio)

            implementation(libs.kstore.file)
            implementation(libs.secp256k1.kmp.jni.android)
            implementation(cryptographyLibs.provider.jdk)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.secp256k1.kmp)


            implementation(cryptographyLibs.core)
            implementation(cryptographyLibs.serialization.pem)
            implementation(libs.kotlinx.datetime)

            implementation(libs.navigation.compose)

            implementation(libs.kstore)

            implementation(libs.kotlinx.coroutines.core)

            implementation("com.soywiz.korge:korlibs-crypto:5.4.0")

        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            implementation(libs.kstore.file)
            implementation(libs.harawata.appdirs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(cryptographyLibs.provider.jdk)
            implementation(libs.secp256k1.kmp.jni.jvm)
        }

        wasmJsMain.dependencies {
            implementation(libs.kstore.storage)
            implementation(libs.ktor.client.js)
            implementation(cryptographyLibs.provider.webcrypto)
            implementation(libs.secp256k1.wasm.js)
        }
    }
}

android {
    namespace = "invincible.privacy.joinstr"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "invincible.privacy.joinstr"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))

        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        mainClass = "invincible.privacy.joinstr.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "invincible.privacy.joinstr"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/macosMain/resources/AppIcon.icns"))
            }
            windows {
                iconFile.set(project.file("src/windowsMain/resources/AppIcon.ico"))
            }
            linux {
                iconFile.set(project.file("src/linuxMain/resources/AppIcon.png"))
            }
        }
    }
}
