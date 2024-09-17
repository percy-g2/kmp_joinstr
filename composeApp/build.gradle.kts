import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
            jvmTarget.set(JvmTarget.JVM_17)
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
            freeCompilerArgs += "-Xbinary=bundleId=invincible.privacy.joinstr.Joinstr"
        }
    }
    
    sourceSets {
        val desktopMain by getting

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            implementation(libs.kstore.file)

            implementation(cryptographyLibs.provider.apple)

            implementation(libs.secp256k1.kmp)

            implementation(libs.logback.classic)

            implementation(libs.bitcoin.kmp)
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

            implementation(libs.secp256k1.kmp)

            implementation(libs.slf4j.api)

            implementation(libs.logback.android)

            implementation(libs.bitcoin.kmp)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
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

            implementation(cryptographyLibs.core)
            implementation(cryptographyLibs.serialization.pem)

            implementation(libs.kotlinx.datetime)

            implementation(libs.navigation.compose)

            implementation(libs.kstore)

            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.okio)

            implementation(libs.qrose)

            implementation(libs.compottie)

            implementation("com.ionspin.kotlin:bignum-serialization-kotlinx:0.3.9")

            implementation("com.ionspin.kotlin:bignum:0.3.11-SNAPSHOT")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material")
            }
            implementation(libs.ktor.client.cio)

            implementation(libs.kstore.file)

            implementation(libs.harawata.appdirs)

            implementation(libs.kotlinx.coroutines.swing)

            implementation(cryptographyLibs.provider.jdk)

            implementation(libs.secp256k1.kmp.jni.jvm)

            implementation(libs.secp256k1.kmp)

            implementation(libs.logback.classic)

            implementation(libs.bitcoin.kmp)
        }

        wasmJsMain.dependencies {
            implementation(libs.kstore.storage)

            implementation(libs.ktor.client.js)

            implementation(cryptographyLibs.provider.webcrypto)

            implementation(libs.logback.classic)

            implementation(npm("@noble/secp256k1", "1.7.1"))
        }

        commonTest.dependencies {
            implementation(libs.testng)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
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
        versionName = "0.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))

        debugImplementation(compose.uiTooling)
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val buildType = variant.buildType.name
                val outputFileName = "Joinstr_v${defaultConfig.versionName}_${buildType}.apk"
                output.outputFileName = outputFileName
            }
    }
}

compose.desktop {
    application {
        buildTypes.release.proguard {
            version.set("7.4.0")
        }
        mainClass = "invincible.privacy.joinstr.MainKt"
        buildTypes.release.proguard {
            configurationFiles.from(files("compose-desktop.pro"))
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Joinstr"
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
