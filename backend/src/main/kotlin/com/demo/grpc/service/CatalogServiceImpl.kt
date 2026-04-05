// backend/src/main/kotlin/com/demo/grpc/service/CatalogServiceImpl.kt
package com.demo.grpc.service

import com.demo.grpc.generated.CatalogServiceGrpcKt
import com.demo.grpc.generated.GetProductRequest
import com.demo.grpc.generated.ListProductsRequest
import com.demo.grpc.generated.PriceUpdate
import com.demo.grpc.generated.Product
import com.demo.grpc.generated.WatchPriceRequest
import com.demo.grpc.generated.priceUpdate
import com.demo.grpc.generated.product
import com.demo.grpc.ProductRepository
import com.demo.grpc.toProto
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// ─────────────────────────────────────────────────────────────
// CatalogServiceImpl
//
// This is the class you SHOW LIVE during the talk.
// It extends the generated abstract base class — the compiler
// enforces every RPC method is implemented.
// ─────────────────────────────────────────────────────────────
class CatalogServiceImpl : CatalogServiceGrpcKt.CatalogServiceCoroutineImplBase() {

    // ── Unary RPC ─────────────────────────────────────────────
    override suspend fun getProduct(request: GetProductRequest): Product {
        val found = ProductRepository.findById(request.productId)
            ?: throw StatusException(
                Status.NOT_FOUND.withDescription("Product ${request.productId} not found")
            )
        return found.toProto()
    }

    // ── Server-streaming RPC ──────────────────────────────────
    // Returns a Flow<Product>; gRPC-Kotlin streams each emission
    // to the client automatically.
    override fun listProducts(request: ListProductsRequest): Flow<Product> = flow {
        val items = ProductRepository.list(request.category)
        val page     = request.page.takeIf { it > 0 } ?: 1
        val pageSize = request.pageSize.takeIf { it > 0 } ?: 20

        items
            .drop((page - 1) * pageSize)
            .take(pageSize)
            .forEach { item ->
                emit(item.toProto())
                delay(50) // simulate DB / network latency
            }
    }

    // ── Server-streaming RPC: live price ticker ───────────────
    override fun watchPrice(request: WatchPriceRequest): Flow<PriceUpdate> = flow {
        val product = ProductRepository.findById(request.productId)
            ?: throw StatusException(
                Status.NOT_FOUND.withDescription("Product ${request.productId} not found")
            )

        // Stream indefinitely until the client cancels
        var currentPrice = product.pricePaise
        while (true) {
            currentPrice += (-500..500L).random()   // ±5 rupee swing
            emit(priceUpdate {
                productId  = request.productId
                pricePaise = currentPrice
                currency   = "INR"
            })
            delay(2_000)
        }
    }
}
