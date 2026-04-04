// backend/src/main/kotlin/com/demo/grpc/Application.kt
package com.demo.grpc

import com.demo.grpc.service.CatalogServiceImpl
import io.grpc.ServerBuilder
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    // ── 1. Start the gRPC server on port 50051 ────────────────
    val grpcServer = ServerBuilder
        .forPort(50051)
        .addService(CatalogServiceImpl())
        .build()
        .start()

    println("gRPC server started on :50051")

    // ── 2. Start the Ktor REST server on port 8080 ────────────
    //    (This runs ALONGSIDE gRPC — "side-by-side" demo slide)
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true })
        }
        configureRestRoutes()
    }.start(wait = false)

    println("REST server started on :8080")

    // Keep the JVM alive until interrupted
    Runtime.getRuntime().addShutdownHook(Thread {
        grpcServer.shutdown()
        println("Servers shut down.")
    })
    grpcServer.awaitTermination()
}

// ── Legacy REST routes (kept alive during migration) ──────────
fun Application.configureRestRoutes() {
    routing {
        get("/api/v1/products/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                io.ktor.http.HttpStatusCode.BadRequest, "Missing id"
            )
            // Same ProductRepository — shared business logic
            val product = ProductRepository.findById(id)
                ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound, "Not found")
            call.respond(product.toRestDto())
        }

        get("/api/v1/products") {
            val category = call.request.queryParameters["category"] ?: ""
            call.respond(ProductRepository.list(category).map { it.toRestDto() })
        }

        // ── Live-price endpoint — added for the REST-vs-gRPC demo ──────
        // Simulates the same ±5-rupee fluctuation the gRPC WatchPrice RPC does,
        // so both approaches return genuinely changing prices.
        // The point: REST forces the client to poll; gRPC pushes without asking.
        get("/api/v1/products/{id}/price") {
            val id = call.parameters["id"] ?: return@get call.respond(
                io.ktor.http.HttpStatusCode.BadRequest, "Missing id"
            )
            val product = ProductRepository.findById(id)
                ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound, "Not found")
            val fluctuated = product.pricePaise + (-500..500L).random()
            call.respond(LivePriceDto(productId = id, pricePaise = fluctuated))
        }
    }
}
