package com.demo.grpcdemo.domain.usecase

import com.demo.grpcdemo.domain.model.Product
import com.demo.grpcdemo.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ListProductsUseCase @Inject constructor(private val repository: CatalogRepository) {
    operator fun invoke(category: String = "", page: Int = 1): Flow<Product> =
        repository.listProducts(category, page)
}
