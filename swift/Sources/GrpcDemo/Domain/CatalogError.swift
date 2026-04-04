// swift/Sources/GrpcDemo/Domain/CatalogError.swift

enum CatalogError: LocalizedError {
    case notFound(id: String)
    case unavailable(underlying: Error?)
    case unknown(underlying: Error?)

    var errorDescription: String? {
        switch self {
        case .notFound(let id):   return "Product '\(id)' not found"
        case .unavailable(let e): return "Service unavailable: \(e?.localizedDescription ?? "?")"
        case .unknown(let e):     return "Unknown error: \(e?.localizedDescription ?? "?")"
        }
    }
}
