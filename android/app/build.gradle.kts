// android/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace  = "com.demo.grpcdemo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.demo.grpcdemo"
        minSdk        = 26
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0"

        // ── Network targets ────────────────────────────────────────────────────
        // OPTION A — WiFi LAN (emulator: 10.0.2.2, physical device: your Mac IP)
        // buildConfigField("String", "GRPC_HOST",     "\"192.168.178.54\"")
        // buildConfigField("int",    "GRPC_PORT",     "50051")
        // buildConfigField("String", "REST_BASE_URL", "\"http://192.168.178.54:8080\"")
        //
        // OPTION B — ngrok (phone on mobile data — shows real latency difference)
        // Steps:
        //   1. brew install ngrok
        //   2. ngrok tcp 50051          → copy host:port  e.g. 0.tcp.ngrok.io:12345
        //   3. ngrok http 8080          → copy https URL  e.g. https://abc123.ngrok-free.app
        //   4. Fill in the values below and rebuild
        buildConfigField("String", "GRPC_HOST",     "\"10.0.2.2\"")            // ← Emulator host (maps to your Mac's localhost)
        buildConfigField("int",    "GRPC_PORT",     "50051")
        buildConfigField("String", "REST_BASE_URL", "\"http://10.0.2.2:8080\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose      = true
        buildConfig  = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
}

val grpcVersion     = "1.63.0"
val protobufVersion = "3.25.3"
val hiltVersion     = "2.51.1"

dependencies {
    // ── gRPC ────────────────────────────────────────────────
    implementation("io.grpc:grpc-okhttp:$grpcVersion")
    implementation("io.grpc:grpc-protobuf-lite:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-kotlin-lite:$protobufVersion")

    // ── Coroutines ──────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ── Jetpack ─────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // ── Compose ─────────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Hilt ────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
}

// ── proto source directory ───────────────────────────────────
// The 'proto' extension on AndroidSourceSet is registered dynamically by the
// protobuf plugin at runtime, so the Kotlin DSL type-safe accessor doesn't
// exist. afterEvaluate guarantees the extension is registered before we access it.
afterEvaluate {
    (android.sourceSets.getByName("main") as org.gradle.api.plugins.ExtensionAware)
        .extensions.getByName("proto")
        .let { (it as org.gradle.api.file.SourceDirectorySet).srcDir("../../proto") }
}

// ── protobuf code generation ─────────────────────────────────
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java")   { option("lite") }
                create("kotlin") { option("lite") }
            }
            task.plugins {
                create("grpc")   { option("lite") }
                create("grpckt") { option("lite") }
            }
        }
    }
}

