package com.demo.grpcdemo.domain.usecase

import com.demo.grpcdemo.domain.model.Product
import com.demo.grpcdemo.domain.repository.CatalogRepository
import javax.inject.Inject

class GetProductUseCase @Inject constructor(private val repository: CatalogRepository) {
    suspend operator fun invoke(productId: String): Result<Product> =
        repository.getProduct(productId)
}
