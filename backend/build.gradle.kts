// backend/build.gradle.kts
plugins {
    kotlin("jvm")                          version "1.9.23"
    kotlin("plugin.serialization")         version "1.9.23"
    id("io.ktor.plugin")                   version "2.3.10"
    id("com.google.protobuf")              version "0.9.4"
}

group   = "com.demo.grpc"
version = "1.0.0"

application {
    mainClass.set("com.demo.grpc.ApplicationKt")
}

repositories {
    mavenCentral()
}

val grpcVersion      = "1.63.0"
val grpcKotinVersion = "1.4.1"
val protobufVersion  = "3.25.3"

dependencies {
    // gRPC
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotinVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Ktor (optional REST layer for the "side-by-side" demo)
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotinVersion:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
                create("grpckt")
            }
            task.builtins {
                create("kotlin")
            }
        }
    }
}

// Proto source directory — use afterEvaluate so the protobuf plugin's
// dynamic 'proto' extension is registered before we access it.
afterEvaluate {
    (sourceSets["main"] as org.gradle.api.plugins.ExtensionAware)
        .extensions.getByName("proto")
        .let { (it as org.gradle.api.file.SourceDirectorySet).srcDir("../proto") }
}
