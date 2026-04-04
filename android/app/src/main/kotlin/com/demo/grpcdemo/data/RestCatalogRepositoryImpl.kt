package com.demo.grpcdemo.data

import com.demo.grpcdemo.BuildConfig
import com.demo.grpcdemo.domain.exception.CatalogException
import com.demo.grpcdemo.domain.model.PriceUpdate
import com.demo.grpcdemo.domain.model.Product
import com.demo.grpcdemo.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Named

/**
 * REST implementation of [CatalogRepository].
 *
 * ── DEMO SLIDE NOTE ──────────────────────────────────────────────────────────
 * This is the "BEFORE" state — what most production apps look like today.
 *
 * Problems visible in this code vs CatalogRepositoryImpl (gRPC):
 *  1. Manual JSON parsing  — brittle, no compile-time safety
 *  2. HttpURLConnection    — verbose, no streaming support
 *  3. Polling for prices   — polls every 2s, wastes battery + bandwidth
 *  4. No type safety       — JSONObject field names are plain strings
 *  5. Price updates are PULL — client asks repeatedly; server can't push
 *
 * The interface (CatalogRepository) is IDENTICAL — the ViewModel never changed.
 * Swapping back to this is one line in RepositoryModule.
 * ─────────────────────────────────────────────────────────────────────────────
 */
class RestCatalogRepositoryImpl @Inject constructor(
    @Named("restBaseUrl") private val baseUrl: String,
) : CatalogRepository {

    // ── Unary: fetch one product ──────────────────────────────
    override suspend fun getProduct(productId: String): Result<Product> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = get("$baseUrl/api/v1/products/$productId")
                JSONObject(body).toDomainProduct()
            }.mapRestError()
        }

    // ── "Streaming": paginated REST — not real streaming ─────
    // Returns all products as a Flow but fetches them in one blocking HTTP call.
    // There is no server push — client must call again to get updates.
    override fun listProducts(category: String, page: Int): Flow<Product> = flow {
        val body = withContext(Dispatchers.IO) {
            get("$baseUrl/api/v1/products")
        }
        val array = JSONArray(body)
        for (i in 0 until array.length()) {
            emit(array.getJSONObject(i).toDomainProduct())
        }
    }

    // ── Price "streaming": polling every 2 seconds ───────────
    // Simulates live updates by repeatedly calling the REST endpoint.
    // Compare to gRPC: one open connection, server pushes each change instantly.
    override fun watchPrice(productId: String): Flow<PriceUpdate> = flow {
        while (true) {
            val body = withContext(Dispatchers.IO) {
                get("$baseUrl/api/v1/products/$productId/price")
            }
            val json = JSONObject(body)
            emit(
                PriceUpdate(
                    productId  = json.getString("productId"),
                    pricePaise = json.getLong("pricePaise"),
                    currency   = "INR",
                )
            )
            delay(2_000) // poll every 2s — battery/bandwidth wasted even if price didn't change
        }
    }

    // ── HTTP helper ───────────────────────────────────────────
    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout    = 5_000
        // Required for ngrok free tier — without this header ngrok returns an
        // HTML browser-warning page instead of the JSON API response.
        conn.setRequestProperty("ngrok-skip-browser-warning", "true")
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}

// ── JSON → Domain mappers ─────────────────────────────────────
// Notice: field names are plain strings — a typo here is a runtime crash,
// not a compile error. Proto-generated code has no such risk.
private fun JSONObject.toDomainProduct() = Product(
    id         = getString("id"),
    name       = getString("name"),
    pricePaise = getLong("pricePaise"),
    imageUrl   = optString("imageUrl", ""),
    inStock    = optBoolean("inStock", true),
    category   = optString("category", ""),
)

// ── Error mapping ─────────────────────────────────────────────
private fun <T> Result<T>.mapRestError(): Result<T> = onFailure { e ->
    throw when (e) {
        is java.net.ConnectException -> CatalogException.Unavailable(e)
        is java.io.IOException       -> CatalogException.Unavailable(e)
        else                         -> CatalogException.Unknown(e)
    }
}
