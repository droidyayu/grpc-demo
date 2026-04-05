package com.demo.grpcdemo.ui.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.grpcdemo.data.comparison.ComparisonDataSource
import com.demo.grpcdemo.data.comparison.TransportStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// State for the one-shot benchmark card
// ─────────────────────────────────────────────────────────────
data class BenchmarkState(
    val isRunning: Boolean = false,
    val rest: TransportStats? = null,
    val grpc: TransportStats? = null,
    val restError: String? = null,
    val grpcError: String? = null,
)

// ─────────────────────────────────────────────────────────────
// State for one side of the live comparison
// ─────────────────────────────────────────────────────────────
data class LiveSideState(
    val pricePaise: Long = 0L,
    val lastFrameBytes: Int = 0,
    val cumulativeBytes: Int = 0,
    val updateCount: Int = 0,
    val isRunning: Boolean = false,
    val error: String? = null,
)

data class LiveComparisonState(
    val rest: LiveSideState = LiveSideState(),
    val grpc: LiveSideState = LiveSideState(),
)

// ─────────────────────────────────────────────────────────────
// ComparisonViewModel
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class ComparisonViewModel @Inject constructor(
    private val dataSource: ComparisonDataSource,
) : ViewModel() {

    private val _benchmark = MutableStateFlow(BenchmarkState())
    val benchmark: StateFlow<BenchmarkState> = _benchmark.asStateFlow()

    private val _live = MutableStateFlow(LiveComparisonState())
    val live: StateFlow<LiveComparisonState> = _live.asStateFlow()

    private var liveJob: Job? = null

    // ── One-shot payload benchmark ────────────────────────────
    fun runBenchmark() {
        if (_benchmark.value.isRunning) return
        _benchmark.value = BenchmarkState(isRunning = true)
        viewModelScope.launch {
            // Warm up both transports first so we measure data transfer,
            // not TCP + HTTP/2 handshake overhead
            runCatching { dataSource.warmUp() }

            // Run sequentially for a fair comparison
            val restResult = runCatching { dataSource.benchmarkRest() }
            val grpcResult = runCatching { dataSource.benchmarkGrpc() }
            _benchmark.value = BenchmarkState(
                isRunning  = false,
                rest       = restResult.getOrNull(),
                grpc       = grpcResult.getOrNull(),
                restError  = restResult.exceptionOrNull()?.message,
                grpcError  = grpcResult.exceptionOrNull()?.message,
            )
        }
    }

    // ── Start side-by-side live comparison ───────────────────
    fun startLiveComparison(productId: String = "p001") {
        if (_live.value.rest.isRunning) return
        _live.value = LiveComparisonState(
            rest = LiveSideState(isRunning = true),
            grpc = LiveSideState(isRunning = true),
        )
        liveJob = viewModelScope.launch {
            // REST polling coroutine
            launch {
                dataSource.pollPriceViaRest(productId)
                    .catch { e ->
                        _live.update { state ->
                            state.copy(rest = state.rest.copy(
                                isRunning = false,
                                error     = e.message ?: "REST error",
                            ))
                        }
                    }
                    .collect { frame ->
                        _live.update { state ->
                            state.copy(rest = LiveSideState(
                                pricePaise       = frame.pricePaise,
                                lastFrameBytes   = frame.frameBytes,
                                cumulativeBytes  = frame.cumulativeBytes,
                                updateCount      = frame.updateCount,
                                isRunning        = true,
                            ))
                        }
                    }
            }
            // gRPC streaming coroutine
            launch {
                dataSource.streamPriceViaGrpc(productId)
                    .catch { /* stop silently on error */ }
                    .collect { frame ->
                        _live.update { state ->
                            state.copy(grpc = LiveSideState(
                                pricePaise       = frame.pricePaise,
                                lastFrameBytes   = frame.frameBytes,
                                cumulativeBytes  = frame.cumulativeBytes,
                                updateCount      = frame.updateCount,
                                isRunning        = true,
                            ))
                        }
                    }
            }
        }
    }

    fun stopLiveComparison() {
        liveJob?.cancel()
        liveJob = null
        _live.update {
            it.copy(
                rest = it.rest.copy(isRunning = false),
                grpc = it.grpc.copy(isRunning = false),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveJob?.cancel()
    }
}
