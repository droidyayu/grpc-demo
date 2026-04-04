package com.demo.grpcdemo.data

import com.demo.grpc.generated.PriceUpdate as ProtoPriceUpdate
import com.demo.grpc.generated.Product as ProtoProduct
import com.demo.grpcdemo.domain.exception.CatalogException
import com.demo.grpcdemo.domain.model.PriceUpdate
import com.demo.grpcdemo.domain.model.Product
import com.demo.grpcdemo.domain.repository.CatalogRepository
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Implements the domain CatalogRepository interface.
// Owns all proto↔domain mapping so proto types never cross this boundary.
class CatalogRepositoryImpl @Inject constructor(
    private val dataSource: GrpcCatalogDataSource,
) : CatalogRepository {

    override suspend fun getProduct(productId: String): Result<Product> =
        withContext(Dispatchers.IO) {
            runCatching {
                dataSource.getProduct(productId).toDomain()
            }.mapGrpcError()
        }

    override fun listProducts(category: String, page: Int): Flow<Product> =
        dataSource.listProducts(category, page, pageSize = 20)
            .map { it.toDomain() }
            .catch { e -> throw mapGrpcException(e) }

    override fun watchPrice(productId: String): Flow<PriceUpdate> =
        dataSource.watchPrice(productId)
            .map { it.toDomain() }
            .catch { e -> throw mapGrpcException(e) }
}

// ── Proto → Domain mappers ────────────────────────────────────
private fun ProtoProduct.toDomain() = Product(
    id         = id,
    name       = name,
    pricePaise = pricePaise,
    imageUrl   = imageUrl,
    inStock    = inStock,
    category   = category,
)

private fun ProtoPriceUpdate.toDomain() = PriceUpdate(
    productId  = productId,
    pricePaise = pricePaise,
    currency   = currency,
)

// ── gRPC status → domain exception mapping ────────────────────
private fun mapGrpcException(e: Throwable): CatalogException =
    if (e is StatusRuntimeException) {
        when (e.status.code) {
            Status.Code.NOT_FOUND       -> CatalogException.NotFound(e.status.description ?: "?")
            Status.Code.UNAVAILABLE     -> CatalogException.Unavailable(e)
            Status.Code.UNAUTHENTICATED -> CatalogException.Unauthenticated()
            else                        -> CatalogException.Unknown(e)
        }
    } else CatalogException.Unknown(e)

private fun <T> Result<T>.mapGrpcError(): Result<T> =
    onFailure { throw mapGrpcException(it) }
