// swift/Sources/GrpcDemo/GrpcDemoApp.swift
//
// App entry point + dependency wiring.
// This is the Swift equivalent of @HiltAndroidApp — dependencies are
// created once here and injected downward, never constructed inside views.

import SwiftUI
import GRPC
import NIOPosix

@main
struct GrpcDemoApp: App {

    // Build the dependency graph once at app startup
    private let viewModel: CatalogViewModel = {
        let group = MultiThreadedEventLoopGroup(numberOfThreads: 1)
        let channel = try! GRPCChannelPool.with(
            target: .host("localhost", port: 50051), // use your server IP when deployed
            transportSecurity: .plaintext,           // swap for .tls() in production
            eventLoopGroup: group
        )
        let client = Catalog_CatalogServiceAsyncClient(channel: channel)
        let repository = CatalogRepository(client: client)
        return CatalogViewModel(repository: repository)
    }()

    var body: some Scene {
        WindowGroup {
            CatalogView(viewModel: viewModel)
        }
    }
}
