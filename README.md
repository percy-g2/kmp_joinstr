# Joinstr - Kotlin Multiplatform

<img src="https://github.com/percy-g2/kmp_joinstr/blob/main/screenshots/Joinstr-Presentation.png" alt="Presentation"/>

A privacy-focused, multiplatform application for coordinating CoinJoin transactions using the Nostr protocol.

## Overview

Joinstr is a Kotlin Multiplatform project that enables users to participate in CoinJoin transactions while maintaining privacy. It leverages the Nostr protocol for decentralized pool creation and coordination, allowing users to perform CoinJoins privately using Nostr relays.

## Features

- Multiplatform support (Android, iOS, Desktop, Web)
- Decentralized CoinJoin pool creation using Nostr
- Private participation in CoinJoins
- User-friendly Compose UI
- End-to-end encryption for all communications
- Support for multiple Nostr relays

## Tech Stack

Joinstr is built using a modern and robust tech stack:

- Kotlin Multiplatform: For sharing code across platforms
- Jetbrains Compose Multiplatform: For building the UI across all platforms
- Kotlin Coroutines: For asynchronous programming
- Ktor: For networking and HTTP client
  - Includes content negotiation, logging, and WebSocket support
- Kotlinx Serialization: For JSON serialization/deserialization
- Kotlinx DateTime: For date and time handling
- KStore: For key-value data storage across platforms
- Bitcoin KMP: For Bitcoin-related operations
- Cryptography Libraries:
  - Core cryptography operations
  - PEM serialization
  - Platform-specific providers (Apple, JDK, WebCrypto)
- Secp256k1 KMP: For elliptic curve operations
- QRose: For QR code generation
- Compottie: For Lottie animations in Compose
- Logback: For logging (Classic for Desktop/iOS, Android for Android)
- Navigation Compose: For navigation in the app
- Okio: For I/O operations
- AppDirs (Desktop only): For managing application directories

## Platform-Specific Dependencies

### Android
- AndroidX Activity Compose: Provides the Compose integration with Activities
  - Implementation: implementation(libs.androidx.activity.compose)
- AndroidX Startup Runtime: For initializing components at app startup
  - Implementation: implementation(libs.androidx.startup.runtime)
- Ktor Client Android: Android-specific HTTP client engine
  - Implementation: implementation(libs.ktor.client.android)
- Ktor Client CIO: Coroutine-based I/O client engine
  - Implementation: implementation(libs.ktor.client.cio)
- KStore File: File-based storage implementation for Android
  - Implementation: implementation(libs.kstore.file)
- Secp256k1 KMP JNI Android: JNI bindings for secp256k1 cryptographic operations
  - Implementation: implementation(libs.secp256k1.kmp.jni.android)
- Cryptography Provider JDK: JDK-based cryptography provider
  - Implementation: implementation(cryptographyLibs.provider.jdk)
- SLF4J API: Logging facade
  - Implementation: implementation(libs.slf4j.api)
- Logback Android: Android-specific logging implementation
  - Implementation: implementation(libs.logback.android)

### iOS
- Ktor Client Darwin: iOS-specific HTTP client engine
  - Implementation: implementation(libs.ktor.client.darwin)
- KStore File: File-based storage implementation for iOS
  - Implementation: implementation(libs.kstore.file)
- Cryptography Provider Apple: Apple-specific cryptography provider
  - Implementation: implementation(cryptographyLibs.provider.apple)
- Logback Classic: Logging implementation for iOS
  - Implementation: implementation(libs.logback.classic)

### Desktop
- Compose Desktop: UI toolkit for desktop applications
  - Implementation: implementation(compose.desktop.currentOs)
- Ktor Client CIO: Coroutine-based I/O client engine
  - Implementation: implementation(libs.ktor.client.cio)
- KStore File: File-based storage implementation for desktop
  - Implementation: implementation(libs.kstore.file)
- Harawata AppDirs: Library for accessing platform-specific application directories
  - Implementation: implementation(libs.harawata.appdirs)
- Kotlinx Coroutines Swing: Swing-specific coroutine dispatchers
  - Implementation: implementation(libs.kotlinx.coroutines.swing)
- Cryptography Provider JDK: JDK-based cryptography provider
  - Implementation: implementation(cryptographyLibs.provider.jdk)
- Secp256k1 KMP JNI JVM: JNI bindings for secp256k1 cryptographic operations
  - Implementation: implementation(libs.secp256k1.kmp.jni.jvm)
- Logback Classic: Logging implementation for desktop
  - Implementation: implementation(libs.logback.classic)

### Web (Wasm)
- KStore Storage: Web storage-based implementation for data persistence
  - Implementation: implementation(libs.kstore.storage)
- Ktor Client JS: JavaScript-specific HTTP client engine
  - Implementation: implementation(libs.ktor.client.js)
- Cryptography Provider WebCrypto: Web Crypto API-based cryptography provider
  - Implementation: implementation(cryptographyLibs.provider.webcrypto)
- Logback Classic: Logging implementation for web
  - Implementation: implementation(libs.logback.classic)
- Noble Secp256k1: JavaScript implementation of secp256k1 cryptographic operations
  - Implementation: implementation(npm("@noble/secp256k1", "1.7.1"))

## Supported Platforms

- Android
- iOS
- Desktop (Windows, macOS, Linux)
- Web (Wasm)

## Prerequisites

- Kotlin 1.9.0+
- Android Studio Arctic Fox or newer (for Android development)
- Xcode 12.0+ (for iOS development)
- JDK 17+

## Getting Started

1. Clone the repository:
   git clone https://github.com/percy-g2/kmp_joinstr.git

2. Open the project in Android Studio or your preferred IDE.

3. Build the project:
   ./gradlew build

## Building for Different Targets

Android:
To build and run the Android app:
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

iOS:
To build the iOS app, run from the root directory:
./gradlew :composeApp:iosDeployIphone14Debug
Then open the Xcode project in the `iosApp` directory and run the app from Xcode.

Desktop:
To run the desktop app:
./gradlew :composeApp:run

To build a desktop distribution:
./gradlew :composeApp:createDistributable

Web (Wasm):
To build and run the web app:
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

## Usage

1. Launch the application on your device or browser.
2. Connect to Nostr relays of your choice.
3. Create or join a CoinJoin pool.
4. Follow the on-screen instructions to participate in a CoinJoin transaction.

## Architecture

The project follows a Kotlin Multiplatform structure with the following main source sets:
- commonMain: Common Kotlin code shared across all platforms
- androidMain: Android-specific implementation
- iosMain: iOS-specific implementation
- desktopMain: Desktop-specific implementation
- wasmJsMain: Web (Wasm) specific implementation

## Building for Different Platforms

### Android
- Build the Android app:
  ./gradlew :composeApp:assembleDebug
- Install and run on a connected device or emulator:
  ./gradlew :composeApp:installDebug

### iOS
- Build and run on an iOS simulator:
  ./gradlew :composeApp:iosDeployIphoneSimulatorDebug
- For a physical device, open the Xcode project in the `iosApp` directory and run from there
- Framework details:
  - Name: "ComposeApp"
  - Bundle ID: "invincible.privacy.joinstr.Joinstr"

### Desktop
- Run the desktop application:
  ./gradlew :composeApp:run
- Create distributable packages:
  ./gradlew :composeApp:createDistributable
- Supported formats: Windows (MSI), macOS (DMG), Linux (DEB)


### Web (Wasm)
- Build and run the web application:
  ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
- Access the application at `http://localhost:8080` by default
- Main output: "composeApp.js"

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the Project
2. Create your Feature Branch (git checkout -b feature/AmazingFeature)
3. Commit your Changes (git commit -m 'Add some AmazingFeature')
4. Push to the Branch (git push origin feature/AmazingFeature)
5. Open a Pull Request

## License

This project is licensed under the GNU General Public License v3.0 (GPLv3) - see the [LICENSE](LICENSE) file for details.

The GPLv3 is a strong copyleft license that requires anyone who distributes your code or a derivative work to make the source available under the same terms. This license is particularly suitable for software that you want to keep open and free.

Key points of the GPLv3:
- You can use the software for any purpose
- You can change the software and distribute modified versions
- You can share the software with others
- If you distribute modified versions, you must share your modifications under the GPLv3
- You must include the license and copyright notice with each copy of the software
- You must disclose your source code when you distribute the software

For the full license text, visit: https://www.gnu.org/licenses/gpl-3.0.en.html

## Disclaimer

This software is for educational and research purposes only. Use at your own risk. The authors and contributors are not responsible for any loss of funds or privacy breaches that may occur while using this software.

## Contact

For questions or support, please open an issue in the GitHub repository: https://github.com/percy-g2/kmp_joinstr/issues
