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

## Running the Demo

### Step 1 — Start the backend

The backend runs **both** a gRPC server (`:50051`) and a REST server (`:8080`) from the same codebase.

```bash
# From the repo root
nohup ./backend/gradlew run -p ./backend </dev/null > /tmp/backend.log 2>&1 &
```

> **Why `nohup ... </dev/null`?**  
> Gradle's `:run` task tries to read stdin, which suspends the process in a terminal.  
> Redirecting stdin from `/dev/null` and using `nohup` keeps it running in the background.

**Watch the startup log:**
```bash
tail -f /tmp/backend.log
```

You should see:
```
gRPC server started on :50051
REST  server started on :8080
```

**Verify both servers are up:**
```bash
# REST
curl http://localhost:8080/api/v1/products

# gRPC
grpcurl -plaintext localhost:50051 list
```

**Stop the backend:**
```bash
lsof -ti:50051 -ti:8080 | xargs kill -9
```

---

### Step 2 — Run the Android app

1. Open `android/` in Android Studio
2. Start an emulator (API 26+)
3. Click **Run** or:

```bash
cd android && ./gradlew installDebug
```

> The app uses `10.0.2.2` as the host address — this is the Android emulator's alias for `localhost` on your Mac.  
> If running on a **physical device**, update `GRPC_HOST` and `REST_BASE_URL` in `app/build.gradle.kts` to your machine's LAN IP.

The app has two tabs:
- **Catalog** — streams products via gRPC, shows live price ticker
- **REST vs gRPC** — side-by-side benchmark and live comparison screen

---

### Step 3 — Live terminal demo (great for slides)

```bash
# Unary call — get one product
grpcurl -plaintext \
  -d '{"product_id": "p001"}' \
  localhost:50051 catalog.CatalogService/GetProduct

# Server-streaming — list all products (watch them stream in one by one)
grpcurl -plaintext \
  -d '{"category": "electronics", "page": 1, "page_size": 20}' \
  localhost:50051 catalog.CatalogService/ListProducts

# Server-streaming — live price updates (Ctrl+C to stop)
grpcurl -plaintext \
  -d '{"product_id": "p001"}' \
  localhost:50051 catalog.CatalogService/WatchPrice

# REST equivalent — compare the verbose JSON
curl -s http://localhost:8080/api/v1/products/p001 | jq .

# Size comparison
echo "REST:"; curl -s http://localhost:8080/api/v1/products | wc -c
echo "gRPC (proto bytes):"; grpcurl -plaintext \
  -d '{"category": "", "page": 1, "page_size": 20}' \
  -format-error localhost:50051 catalog.CatalogService/ListProducts 2>/dev/null | wc -c
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
| **Proto generation** | Gradle `protobuf {}` block | `protoc_plugin` | `protoc-gen-swift` |
| **gRPC runtime** | `grpc-kotlin-stub` | `package:grpc` | `grpc-swift` |
| **Domain model** | Kotlin `data class` | Dart `class` | Swift `struct` |
| **Repository interface** | `interface` | `abstract interface class` | `protocol` |
| **DI** | Hilt `@Module` | Riverpod `Provider` | Manual in `@main` |
| **State container** | `StateFlow` + `ViewModel` | `AsyncNotifier` | `@Observable` class |
| **Streaming** | `Flow<T>` | `Stream<T>` | `AsyncThrowingStream<T>` |
| **UI framework** | Jetpack Compose | Flutter widgets | SwiftUI |

---

## Generating proto stubs

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

## Demo Script

| Slide | What to show |
|-------|-------------|
| "The contract" | `proto/product_catalog.proto` |
| "Code generation" | `android/app/build.gradle.kts` → `protobuf {}` block |
| "Adapter pattern" | `backend/Application.kt` — two transports, one service |
| "Service impl" | `backend/service/CatalogServiceImpl.kt` |
| "First gRPC call" | `data/CatalogRepositoryImpl.kt` → `listProducts()` |
| "Streaming live" | Run `grpcurl WatchPrice` in terminal — prices stream live |
| "The problem" | Android app → **REST vs gRPC** tab → Run Benchmark |
| "The fix" | Android app → **REST vs gRPC** tab → Start live comparison |
| "Cross-platform" | Architecture table above + `proto/` folder open side by side |

---

## Migration Pattern (feature flag slide)

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

│
├── backend/                         ← Kotlin/Ktor server  (gRPC :50051 + REST :8080)
│   └── src/main/kotlin/com/demo/grpc/
│       ├── Application.kt               ← Starts both servers side-by-side
│       ├── service/CatalogServiceImpl.kt ← ⭐ SHOW THIS LIVE
│       └── model/ProductRepository.kt
│
├── android/                         ← Kotlin + Jetpack Compose + Hilt
│   └── app/src/main/kotlin/com/demo/grpcdemo/
│       ├── domain/                      ← Pure Kotlin, no framework
│       │   ├── model/                   ← Product, PriceUpdate
│       │   ├── exception/               ← CatalogException
│       │   ├── repository/              ← CatalogRepository (interface)
│       │   └── usecase/                 ← ListProducts, GetProduct, WatchPrice
│       ├── data/                        ← gRPC transport + proto↔domain mapping
│       │   ├── GrpcCatalogDataSource.kt
│       │   └── CatalogRepository.kt     ← CatalogRepositoryImpl  ⭐ SHOW THIS LIVE
│       ├── di/                          ← Hilt modules (AppModule, RepositoryModule)
│       └── ui/                          ← ViewModel + Compose screens
│
├── flutter/                         ← Dart + Riverpod (mirrors Android architecture)
│   └── lib/
│       ├── domain/                      ← Pure Dart models, exceptions, interfaces
│       ├── data/CatalogRepositoryImpl   ← gRPC + proto↔domain mapping
│       ├── di/providers.dart            ← Riverpod providers (≈ Hilt modules)
│       └── ui/                          ← AsyncNotifier + Widget tree
│
├── swift/                           ← Swift + SwiftUI + grpc-swift (iOS 17+)
│   └── Sources/GrpcDemo/
│       ├── Domain/                      ← Pure Swift structs + protocol
│       ├── Data/CatalogRepository.swift ← grpc-swift client + proto↔domain mapping
│       └── UI/                          ← @Observable ViewModel + SwiftUI views
│
└── docker-compose.yml               ← `docker compose up --build` to deploy backend
```

---

## Running the Demo

### 1. Start the backend

```bash
cd backend
./gradlew run
```

You'll see:
```
gRPC server started on :50051
REST  server started on :8080
```

Both servers run **side by side** — this is your demo for the "Adapter Pattern" slide.

### 2. Test gRPC with grpcurl (great for live demos)

Install: `brew install grpcurl`

```bash
# List available services
grpcurl -plaintext localhost:50051 list

# Unary call — get one product
grpcurl -plaintext \
  -d '{"product_id": "p001"}' \
  localhost:50051 catalog.CatalogService/GetProduct

# Server-streaming — list all products (watch them stream in)
grpcurl -plaintext \
  -d '{"category": "electronics"}' \
  localhost:50051 catalog.CatalogService/ListProducts

# Server-streaming — live price updates (run this during the streaming slide)
grpcurl -plaintext \
  -d '{"product_id": "p001"}' \
  localhost:50051 catalog.CatalogService/WatchPrice
```

### 3. Test the legacy REST layer (comparison slide)

```bash
# Same data, REST transport — notice the JSON verbosity
curl http://localhost:8080/api/v1/products/p001 | jq .

# Compare payload size vs gRPC binary
curl -s http://localhost:8080/api/v1/products/p001 | wc -c
# grpcurl equivalent is ~30-40% smaller in binary wire format
```

### 4. Run the Android app

```bash
cd android
./gradlew installDebug
```

Make sure an emulator is running. The app uses `10.0.2.2:50051` to reach the host machine from the emulator.

---

## Cross-Platform Architecture Comparison

The same clean architecture pattern is used on all three platforms.  
Point this table out during the "Cross-platform" slide.

| Concept | Android (Kotlin) | Flutter (Dart) | iOS (Swift) |
|---|---|---|---|
| **Proto generation** | `protoc-gen-grpc-kotlin` via Gradle | `protoc_plugin` via `protoc` | `protoc-gen-swift` + `protoc-gen-grpc-swift` |
| **gRPC runtime** | `grpc-kotlin-stub` | `package:grpc` | `grpc-swift` |
| **Domain model** | Kotlin `data class` | Dart `class` | Swift `struct` |
| **Repository interface** | `interface` | `abstract interface class` | `protocol` |
| **DI** | Hilt `@Module` | Riverpod `Provider` | Manual in `@main` |
| **State container** | `StateFlow` + `ViewModel` | `AsyncNotifier` (Riverpod) | `@Observable` class |
| **Streaming** | `Flow<T>` | `Stream<T>` | `AsyncThrowingStream<T>` |
| **UI framework** | Jetpack Compose | Flutter widgets | SwiftUI |

**The proto is the contract.** Generate once, run everywhere.

---

## Generating proto stubs per platform

```bash
# Android — handled by Gradle (build.gradle.kts → protobuf block)
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

## Demo Script (what to show, when)

| Slide | What to open |
|-------|-------------|
| "Define the .proto" | `proto/product_catalog.proto` |
| "Android build.gradle" | `android/app/build.gradle.kts` |
| "First gRPC call" | `android/…/data/CatalogRepository.kt` |
| "Adapter pattern" | `backend/…/Application.kt` — show both servers starting |
| "Service impl" | `backend/…/service/CatalogServiceImpl.kt` |
| "Streaming demo" | Run `grpcurl WatchPrice` in terminal — prices stream live |
| "Cross-platform" | Open the architecture table above + `proto/` side by side |

---

## Feature Flag Pattern (migration slide)

To swap DataSource at runtime without redeploying:

```kotlin
// In your DI / AppModule
val useGrpc = RemoteConfig.getBoolean("use_grpc_catalog")  // Firebase / LaunchDarkly

val catalogRepository = if (useGrpc) {
    CatalogRepository(GrpcChannelProvider.channel)
} else {
    LegacyRestCatalogRepository()   // your old Retrofit-based repo
}
```

Both implement the same interface. ViewModel never changes.
