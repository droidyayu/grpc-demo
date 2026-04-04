// swift/Sources/GrpcDemo/UI/CatalogView.swift
import SwiftUI

struct CatalogView: View {
    @State private var viewModel: CatalogViewModel

    init(viewModel: CatalogViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Live price ticker banner
                if let ticker = viewModel.priceTicker {
                    PriceTickerBanner(ticker: ticker)
                }

                Group {
                    if viewModel.isLoading && viewModel.products.isEmpty {
                        ProgressView()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if let error = viewModel.errorMessage {
                        ErrorView(message: error) {
                            Task { await viewModel.loadProducts() }
                        }
                    } else {
                        ProductList(
                            products:          viewModel.products,
                            isWatching:        viewModel.isWatchingPrice,
                            watchingProductId: viewModel.priceTicker?.productId,
                            onWatchPrice:      { id in Task { @MainActor in viewModel.startWatchingPrice(productId: id) } },
                            onStopWatch:       { viewModel.stopWatchingPrice() }
                        )
                    }
                }
            }
            .navigationTitle("gRPC Product Catalog")
        }
        .task { await viewModel.loadProducts() }
    }
}

// ── Sub-views ─────────────────────────────────────────────

struct ProductList: View {
    let products: [Product]
    let isWatching: Bool
    let watchingProductId: String?
    let onWatchPrice: (String) -> Void
    let onStopWatch: () -> Void

    var body: some View {
        List(products) { product in
            ProductCard(
                product: product,
                isWatching: watchingProductId == product.id,
                onWatchPrice: { onWatchPrice(product.id) },
                onStopWatch: onStopWatch
            )
            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
        }
        .listStyle(.plain)
    }
}

struct ProductCard: View {
    let product: Product
    let isWatching: Bool
    let onWatchPrice: () -> Void
    let onStopWatch: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(product.name).font(.headline)
            Text(product.displayPrice).foregroundStyle(.secondary)
            Text(product.inStock ? "In Stock" : "Out of Stock")
                .foregroundStyle(product.inStock ? Color(hex: 0x2EC4B6) : .red)
                .font(.caption)

            Button(action: isWatching ? onStopWatch : onWatchPrice) {
                Text(isWatching ? "Stop Live Price" : "Watch Live Price")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(isWatching ? .red : .blue)
            .padding(.top, 4)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct PriceTickerBanner: View {
    let ticker: PriceUpdate

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("LIVE PRICE")
                    .font(.caption2.bold())
                    .foregroundStyle(.secondary)
                Text(ticker.productId).font(.caption)
            }
            Spacer()
            Text("\(ticker.currency) \(ticker.pricePaise / 100).\(String(format: "%02d", ticker.pricePaise % 100))")
                .font(.title2.bold())
        }
        .padding()
        .background(Color(hex: 0xE8F4F8))
        .animation(.easeInOut(duration: 0.3), value: ticker.pricePaise)
    }
}

struct ErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text(message).foregroundStyle(.red).multilineTextAlignment(.center)
            Button("Retry", action: onRetry).buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// ── Helpers ───────────────────────────────────────────────
extension Color {
    init(hex: UInt32) {
        self.init(
            red:   Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8)  & 0xFF) / 255,
            blue:  Double(hex         & 0xFF) / 255
        )
    }
}
