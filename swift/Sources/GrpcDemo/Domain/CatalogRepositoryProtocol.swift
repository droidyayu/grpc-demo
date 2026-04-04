// swift/Sources/GrpcDemo/Domain/CatalogRepositoryProtocol.swift

protocol CatalogRepositoryProtocol {
    func getProduct(id: String) async throws -> Product
    func listProducts(category: String, page: Int) -> AsyncThrowingStream<Product, Error>
    func watchPrice(productId: String) -> AsyncThrowingStream<PriceUpdate, Error>
}
