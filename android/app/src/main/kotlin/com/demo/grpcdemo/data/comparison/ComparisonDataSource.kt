package com.demo.grpcdemo.data.comparison

import com.demo.grpc.generated.CatalogServiceGrpcKt
import com.demo.grpc.generated.getProductRequest
import com.demo.grpc.generated.listProductsRequest
import com.demo.grpc.generated.watchPriceRequest
import io.grpc.ManagedChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Named

// ─────────────────────────────────────────────────────────────
// TransportStats — result of one benchmark run
// ─────────────────────────────────────────────────────────────
data class TransportStats(
    val itemCount: Int,
    val bodyBytes: Int,
    val durationMs: Long,
)

// ─────────────────────────────────────────────────────────────
// LivePriceFrame — single price update with metadata
// ─────────────────────────────────────────────────────────────
data class LivePriceFrame(
    val pricePaise: Long,
    val frameBytes: Int,    // bytes sent/received for this individual update
    val cumulativeBytes: Int,
    val updateCount: Int,
)

// ─────────────────────────────────────────────────────────────
// ComparisonDataSource
//
// Bypasses the domain layer intentionally — this class exists
// solely to expose raw transport metrics for the demo screen.
// Proto types stay here; sizes are exposed as plain Ints.
// ─────────────────────────────────────────────────────────────
class ComparisonDataSource @Inject constructor(
    private val channel: ManagedChannel,
    @Named("restBaseUrl") private val restBaseUrl: String,
) {
    private val stub = CatalogServiceGrpcKt.CatalogServiceCoroutineStub(channel)

    // ── Warm up both transports before benchmarking ───────────
    // gRPC: ManagedChannel is lazy — first call pays TCP + HTTP/2 handshake cost.
    // REST: HttpURLConnection to localhost also benefits from one throwaway call.
    // Calling this before starting the timer gives fair, "warm connection" numbers.
    suspend fun warmUp() = withContext(Dispatchers.IO) {
        // Trigger gRPC HTTP/2 connection establishment
        channel.getState(/* requestConnection = */ true)
        // Throwaway gRPC call so the channel is fully ready
        runCatching {
            stub.listProducts(listProductsRequest {
                this.category = ""
                this.page     = 1
                this.pageSize = 1
            }).collect {}
        }
        // Throwaway REST call to warm the TCP connection
        runCatching {
            val conn = URL("$restBaseUrl/api/v1/products").openConnection() as HttpURLConnection
            conn.connectTimeout = 3_000
            conn.readTimeout    = 3_000
            conn.setRequestProperty("ngrok-skip-browser-warning", "true")
            conn.inputStream.readBytes()
            conn.disconnect()
        }
    }

    // ── Benchmark: 20 SEQUENTIAL REST requests, new TCP connection each time ──
    // Each request carries "Connection: close" — the server tears down the socket
    // after responding.  This is the real-world cost through proxies, load
    // balancers, and CDN edges: every call pays DNS + TCP handshake + TLS (if
    // HTTPS) + HTTP round-trip before any data arrives.
    suspend fun benchmarkRest(): TransportStats = withContext(Dispatchers.IO) {
        val requests = 20
        var singleBodyBytes = 0
        val start = System.currentTimeMillis()
        repeat(requests) {
            val conn = URL("$restBaseUrl/api/v1/products/p001")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout    = 5_000
            conn.setRequestProperty("Connection", "close") // force fresh TCP each time
            conn.setRequestProperty("ngrok-skip-browser-warning", "true")
            val body = conn.inputStream.readBytes()
            conn.disconnect()
            singleBodyBytes = body.size
        }
        val duration = System.currentTimeMillis() - start
        TransportStats(itemCount = requests, bodyBytes = singleBodyBytes * requests, durationMs = duration)
    }

    // ── Benchmark: 20 SEQUENTIAL gRPC requests, ONE persistent connection ────
    // The HTTP/2 connection was established once during warmUp().  Every
    // subsequent call is a lightweight HEADERS + DATA frame — no TCP handshake,
    // no TLS negotiation, no connection setup at all.  Just request + response.
    suspend fun benchmarkGrpc(): TransportStats = withContext(Dispatchers.IO) {
        val requests = 20
        var singleProtoBytes = 0
        val start = System.currentTimeMillis()
        repeat(requests) {
            val response = stub.getProduct(getProductRequest { this.productId = "p001" })
            singleProtoBytes = response.serializedSize
        }
        val duration = System.currentTimeMillis() - start
        TransportStats(itemCount = requests, bodyBytes = singleProtoBytes * requests, durationMs = duration)
    }

    // ── Live REST: poll /price every 2 seconds ────────────────
    // Emits one LivePriceFrame per HTTP round-trip.
    // Each poll is a brand-new connection — TCP + HTTP headers every time.
    fun pollPriceViaRest(productId: String): Flow<LivePriceFrame> = flow {
        var cumulative = 0
        var count = 0
        while (true) {
            val (pricePaise, bodyBytes) = withContext(Dispatchers.IO) {
                val conn = URL("$restBaseUrl/api/v1/products/$productId/price")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 3_000
                conn.readTimeout    = 3_000
                conn.setRequestProperty("ngrok-skip-browser-warning", "true")
                val body = conn.inputStream.readBytes()
                conn.disconnect()
                val price = runCatching {
                    JSONObject(String(body)).getLong("pricePaise")
                }.getOrDefault(0L)
                Pair(price, body.size)
            }
            cumulative += bodyBytes
            count++
            emit(LivePriceFrame(pricePaise, bodyBytes, cumulative, count))
            delay(2_000) // simulate realistic polling interval
        }
    }

    // ── Live gRPC: stream WatchPrice ──────────────────────────
    // A single stream connection multiplexes all updates.
    // Each proto PriceUpdate frame is tiny binary.
    fun streamPriceViaGrpc(productId: String): Flow<LivePriceFrame> {
        var cumulative = 0
        var count = 0
        return stub.watchPrice(watchPriceRequest { this.productId = productId })
            .map { proto ->
                val frameBytes = proto.serializedSize
                cumulative += frameBytes
                count++
                LivePriceFrame(proto.pricePaise, frameBytes, cumulative, count)
            }
    }
}
