// swift/Sources/GrpcDemo/Generated/README.md
//
// Run the following from the repo root to generate Swift stubs:
//
//   brew install swift-protobuf grpc-swift
//
//   protoc \
//     --swift_out=swift/Sources/GrpcDemo/Generated \
//     --grpc-swift_out=Client=true,Server=false:swift/Sources/GrpcDemo/Generated \
//     -I proto \
//     proto/product_catalog.proto
//
// This produces:
//   product_catalog.pb.swift      ← message types (Product, PriceUpdate, …)
//   product_catalog.grpc.swift    ← CatalogServiceNIOClient async/await stub
