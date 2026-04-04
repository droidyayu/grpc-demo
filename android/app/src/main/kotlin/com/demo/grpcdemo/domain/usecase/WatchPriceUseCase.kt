package com.demo.grpcdemo.domain.usecase

import com.demo.grpcdemo.domain.model.PriceUpdate
import com.demo.grpcdemo.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WatchPriceUseCase @Inject constructor(private val repository: CatalogRepository) {
    operator fun invoke(productId: String): Flow<PriceUpdate> =
        repository.watchPrice(productId)
}
