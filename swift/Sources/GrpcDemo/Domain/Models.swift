// swift/Sources/GrpcDemo/Domain/Models.swift
//
// Pure domain models — no generated proto types leak beyond the data layer.

import Foundation

struct Product: Identifiable, Equatable {
    let id: String
    let name: String
    let pricePaise: Int64
    let imageURL: URL?
    let inStock: Bool
    let category: String

    /// Display price: ₹2499.00
    var displayPrice: String {
        let rupees = pricePaise / 100
        let paise  = pricePaise % 100
        return "₹\(rupees).\(String(format: "%02d", paise))"
    }
}

struct PriceUpdate {
    let productId: String
    let pricePaise: Int64
    let currency: String
}
