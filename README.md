# gRPC Demo

A multi-platform gRPC demo with Android, Flutter, Swift, and a Kotlin/Ktor backend. All platforms share a single `.proto` contract.

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java | 17+ | `brew install --cask temurin` |
| Android Studio | Hedgehog+ | [developer.android.com](https://developer.android.com/studio) |
| Android Emulator | API 26+ | Via Android Studio SDK Manager |
| grpcurl *(optional)* | any | `brew install grpcurl` |

> **No Docker needed.** The backend runs directly via the Gradle wrapper.

---

## Project Structure

```
grpc-demo/
├── proto/
│   └── product_catalog.proto        ← single source of truth for all platforms
│
├── backend/                         ← Kotlin/Ktor server (gRPC :50051 + REST :8080)
│   └── src/main/kotlin/com/demo/grpc/
│       ├── Application.kt
│       ├── service/CatalogServiceImpl.kt
│       └── model/ProductRepository.kt
│
├── android/                         ← Kotlin + Jetpack Compose + Hilt
│   └── app/src/main/kotlin/com/demo/grpcdemo/
│       ├── domain/                      ← pure Kotlin, no framework deps
│       │   ├── model/                   ← Product, PriceUpdate
│       │   ├── exception/               ← CatalogException
│       │   ├── repository/              ← CatalogRepository (interface)
│       │   └── usecase/                 ← ListProducts, GetProduct, WatchPrice
│       ├── data/                        ← gRPC transport + proto↔domain mapping
│       │   ├── GrpcCatalogDataSource.kt
│       │   ├── CatalogRepository.kt
│       │   └── comparison/ComparisonDataSource.kt
│       ├── di/                          ← Hilt modules
│       └── ui/                          ← ViewModel + Compose screens
│
├── flutter/                         ← Dart + Riverpod
│   └── lib/
│       ├── domain/
│       ├── data/catalog_repository_impl.dart
│       ├── di/providers.dart
│       └── ui/
│
├── swift/                           ← Swift + SwiftUI + grpc-swift (iOS 17+)
│   └── Sources/GrpcDemo/
│       ├── Domain/
│       ├── Data/CatalogRepository.swift
│       └── UI/
│
└── docker-compose.yml
```

---

## Running the Backend

```bash
cd backend
./gradlew run
```

You'll see:
```
gRPC server started on :50051
REST  server started on :8080
```

Both servers run side by side from the same codebase.

**Stop the backend:**
```bash
lsof -ti:50051 -ti:8080 | xargs kill -9
```

---

## Running the Android App

1. Open `android/` in Android Studio
2. Start an emulator (API 26+)
3. Click **Run** or:

```bash
cd android && ./gradlew installDebug
```

> The app uses `10.0.2.2` as the host — the Android emulator's alias for `localhost` on your Mac.  
> On a **physical device**, update `GRPC_HOST` and `REST_BASE_URL` in `android/app/build.gradle.kts` to your machine's LAN IP.

The app has two tabs:
- **Catalog** — streams products via gRPC with a live price ticker
- **REST vs gRPC** — side-by-side benchmark screen

---

## Testing with grpcurl

```bash
# List available services
grpcurl -plaintext localhost:50051 list

# Unary — get one product
grpcurl -plaintext \
  -d '{"product_id": "p001"}' \
  localhost:50051 catalog.CatalogService/GetProduct

# Server-streaming — list all products
grpcurl -plaintext \
  -d '{"category": "electronics"}' \
  localhost:50051 catalog.CatalogService/ListProducts

# Server-streaming — live price updates (Ctrl+C to stop)
grpcurl -plaintext \
  -d '{"product_id": "p001"}' \
  localhost:50051 catalog.CatalogService/WatchPrice

# REST equivalent
curl http://localhost:8080/api/v1/products/p001 | jq .
```

---

## Architecture

The same clean architecture pattern is used on all three platforms.

```
proto/product_catalog.proto
         │
         ▼  (code generation)
   data layer          ← only layer that knows about proto types
         │  maps to domain models
         ▼
  domain layer         ← pure Kotlin/Dart/Swift, zero framework deps
         │
         ▼
   ui layer            ← ViewModel + Compose / Flutter widgets / SwiftUI
```

### Cross-platform comparison

| Concept | Android (Kotlin) | Flutter (Dart) | iOS (Swift) |
|---|---|---|---|
| **Proto generation** | `protoc-gen-grpc-kotlin` via Gradle | `protoc_plugin` via `protoc` | `protoc-gen-swift` + `protoc-gen-grpc-swift` |
| **gRPC runtime** | `grpc-kotlin-stub` | `package:grpc` | `grpc-swift` |
| **Domain model** | Kotlin `data class` | Dart `class` | Swift `struct` |
| **Repository interface** | `interface` | `abstract interface class` | `protocol` |
| **DI** | Hilt `@Module` | Riverpod `Provider` | Manual in `@main` |
| **State container** | `StateFlow` + `ViewModel` | `AsyncNotifier` | `@Observable` class |
| **Streaming** | `Flow<T>` | `Stream<T>` | `AsyncThrowingStream<T>` |
| **UI framework** | Jetpack Compose | Flutter widgets | SwiftUI |

---

## Generating Proto Stubs

```bash
# Android — handled automatically by Gradle on every build
cd android && ./gradlew generateProto

# Flutter
dart pub global activate protoc_plugin
protoc --dart_out=grpc:flutter/lib/generated -I proto proto/product_catalog.proto

# Swift (requires grpc-swift toolchain)
brew install swift-protobuf grpc-swift
protoc \
  --swift_out=swift/Sources/GrpcDemo/Generated \
  --grpc-swift_out=Client=true,Server=false:swift/Sources/GrpcDemo/Generated \
  -I proto proto/product_catalog.proto
```

---

## Feature Flag Migration Pattern

Both old REST and new gRPC implement the same `CatalogRepository` interface.  
Swap at runtime with zero ViewModel changes:

```kotlin
// di/RepositoryModule.kt
@Provides
fun provideCatalogRepository(
    grpcImpl: CatalogRepositoryImpl,
    restImpl: LegacyRestCatalogRepository,
    remoteConfig: RemoteConfig,
): CatalogRepository =
    if (remoteConfig.getBoolean("use_grpc")) grpcImpl else restImpl
```
