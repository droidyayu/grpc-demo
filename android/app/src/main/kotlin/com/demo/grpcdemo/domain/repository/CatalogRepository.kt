package com.demo.grpcdemo.domain.repository

import com.demo.grpcdemo.domain.model.PriceUpdate
import com.demo.grpcdemo.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    suspend fun getProduct(productId: String): Result<Product>
    fun listProducts(category: String = "", page: Int = 1): Flow<Product>
    fun watchPrice(productId: String): Flow<PriceUpdate>
}
