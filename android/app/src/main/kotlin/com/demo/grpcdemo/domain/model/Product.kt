package com.demo.grpcdemo.domain.model

data class Product(
    val id: String,
    val name: String,
    val pricePaise: Long,
    val imageUrl: String,
    val inStock: Boolean,
    val category: String,
)
