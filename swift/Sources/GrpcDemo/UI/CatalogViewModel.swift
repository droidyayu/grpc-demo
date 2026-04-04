// swift/Sources/GrpcDemo/UI/CatalogViewModel.swift
//
// @Observable macro (iOS 17+) mirrors Kotlin's StateFlow:
//   published properties automatically trigger SwiftUI redraws.

import Foundation
import Observation

@Observable
final class CatalogViewModel {

    // ── State ──────────────────────────────────────────────
    private(set) var products: [Product] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var priceTicker: PriceUpdate?
    private(set) var isWatchingPrice = false

    // ── Dependencies (injected — never self-constructed) ───
    private let repository: CatalogRepositoryProtocol

    init(repository: CatalogRepositoryProtocol) {
        self.repository = repository
    }

    // ── Load products ──────────────────────────────────────
    @MainActor
    func loadProducts(category: String = "") async {
        isLoading = true
        errorMessage = nil
        do {
            for try await product in repository.listProducts(category: category, page: 1) {
                products.append(product)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    // ── Live price ticker ──────────────────────────────────
    private var priceWatchTask: Task<Void, Never>?

    @MainActor
    func startWatchingPrice(productId: String) {
        priceWatchTask?.cancel()
        isWatchingPrice = true
        priceWatchTask = Task {
            do {
                for try await update in repository.watchPrice(productId: productId) {
                    priceTicker = update
                }
            } catch {
                isWatchingPrice = false
            }
        }
    }

    func stopWatchingPrice() {
        priceWatchTask?.cancel()
        isWatchingPrice = false
        priceTicker = nil
    }

    deinit {
        priceWatchTask?.cancel()
    }
}
