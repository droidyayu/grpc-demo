// swift-package-manager manifest
// swift/Package.swift
//
// Proto code generation (run once from repo root):
//
//   brew install swift-protobuf grpc-swift
//
//   protoc \
//     --swift_out=swift/Sources/GrpcDemo/Generated \
//     --grpc-swift_out=Client=true,Server=false:swift/Sources/GrpcDemo/Generated \
//     -I proto \
//     proto/product_catalog.proto

// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "GrpcDemo",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "GrpcDemo", targets: ["GrpcDemo"]),
    ],
    dependencies: [
        // grpc-swift — Apple's official gRPC library
        .package(
            url: "https://github.com/grpc/grpc-swift.git",
            from: "1.23.0"
        ),
    ],
    targets: [
        .target(
            name: "GrpcDemo",
            dependencies: [
                .product(name: "GRPC", package: "grpc-swift"),
            ]
        ),
    ]
)
