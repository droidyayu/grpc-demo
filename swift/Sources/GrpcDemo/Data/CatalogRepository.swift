// swift/Sources/GrpcDemo/Data/CatalogRepository.swift
//
// Implements CatalogRepositoryProtocol.
// Proto ↔ domain mapping is entirely contained here.
//
// The `Catalog_` prefix on generated types comes from protoc-gen-swift
// using the proto package name ("catalog") as a namespace.

import Foundation
import GRPC
import NIOCore

final class CatalogRepository: CatalogRepositoryProtocol {

    private let client: Catalog_CatalogServiceAsyncClient

    init(client: Catalog_CatalogServiceAsyncClient) {
        self.client = client
    }

    // ── Unary ─────────────────────────────────────────────
    func getProduct(id: String) async throws -> Product {
        do {
            let request = Catalog_GetProductRequest.with { $0.productID = id }
            let response = try await client.getProduct(request)
            return response.toDomain()
        } catch let error as GRPCStatus {
            throw error.toCatalogError()
        }
    }

    // ── Server-streaming ──────────────────────────────────
    // Swift's AsyncThrowingStream bridges gRPC streaming responses to
    // async/await — the same pattern as Kotlin's Flow<T>.
    func listProducts(category: String = "", page: Int = 1) -> AsyncThrowingStream<Product, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    let request = Catalog_ListProductsRequest.with {
                        $0.category = category
                        $0.page     = Int32(page)
                        $0.pageSize = 20
                    }
                    for try await proto in client.listProducts(request) {
                        continuation.yield(proto.toDomain())
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }

    func watchPrice(productId: String) -> AsyncThrowingStream<PriceUpdate, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    let request = Catalog_WatchPriceRequest.with { $0.productID = productId }
                    for try await proto in client.watchPrice(request) {
                        continuation.yield(proto.toDomain())
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}

// ── Proto → domain mappers ─────────────────────────────────
private extension Catalog_Product {
    func toDomain() -> Product {
        Product(
            id:         id,
            name:       name,
            pricePaise: pricePaise,
            imageURL:   URL(string: imageURL),
            inStock:    inStock,
            category:   category
        )
    }
}

private extension Catalog_PriceUpdate {
    func toDomain() -> PriceUpdate {
        PriceUpdate(productId: productID, pricePaise: pricePaise, currency: currency)
    }
}

// ── gRPC status → domain errors ────────────────────────────
private extension GRPCStatus {
    func toCatalogError() -> CatalogError {
        switch code {
        case .notFound:    return .notFound(id: message ?? "?")
        case .unavailable: return .unavailable(underlying: nil)
        default:           return .unknown(underlying: nil)
        }
    }
}
