package com.demo.grpcdemo.domain.exception

sealed class CatalogException(message: String) : Exception(message) {
    class NotFound(id: String)           : CatalogException("Product '$id' not found")
    class Unavailable(cause: Throwable?) : CatalogException("Service unavailable: ${cause?.message}")
    class Unauthenticated                : CatalogException("Session expired — please log in again")
    class Unknown(cause: Throwable?)     : CatalogException("Unknown error: ${cause?.message}")
}
