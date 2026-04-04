package com.demo.grpcdemo.data

import com.demo.grpc.generated.CatalogServiceGrpcKt
import com.demo.grpc.generated.getProductRequest
import com.demo.grpc.generated.listProductsRequest
import com.demo.grpc.generated.watchPriceRequest
import com.demo.grpc.generated.Product as ProtoProduct
import com.demo.grpc.generated.PriceUpdate as ProtoPriceUpdate
import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Raw transport layer — only speaks proto types.
// Nothing outside this class should know about gRPC stubs.
class GrpcCatalogDataSource @Inject constructor(channel: ManagedChannel) {

    private val stub = CatalogServiceGrpcKt.CatalogServiceCoroutineStub(channel)

    suspend fun getProduct(productId: String): ProtoProduct =
        stub.getProduct(getProductRequest { this.productId = productId })

    fun listProducts(category: String, page: Int, pageSize: Int): Flow<ProtoProduct> =
        stub.listProducts(listProductsRequest {
            this.category = category
            this.page     = page
            this.pageSize = pageSize
        })

    fun watchPrice(productId: String): Flow<ProtoPriceUpdate> =
        stub.watchPrice(watchPriceRequest { this.productId = productId })
}
