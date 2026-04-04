package com.demo.grpcdemo.domain.model

data class PriceUpdate(
    val productId: String,
    val pricePaise: Long,
    val currency: String,
)
