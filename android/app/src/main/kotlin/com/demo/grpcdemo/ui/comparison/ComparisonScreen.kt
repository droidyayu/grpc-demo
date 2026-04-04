package com.demo.grpcdemo.ui.comparison

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.grpcdemo.data.comparison.TransportStats
import kotlin.math.max

// ─────────────────────────────────────────────────────────────
// Colour tokens
// ─────────────────────────────────────────────────────────────
private val RestColor  = Color(0xFFE53935)   // red  — "danger / legacy"
private val GrpcColor  = Color(0xFF00897B)   // teal — "modern / efficient"
private val CardBg     = Color(0xFF1E1E2E)
private val SurfaceBg  = Color(0xFF13131F)

// ─────────────────────────────────────────────────────────────
// ComparisonScreen
// ─────────────────────────────────────────────────────────────
@Composable
fun ComparisonScreen(viewModel: ComparisonViewModel) {
    val benchmark by viewModel.benchmark.collectAsState()
    val live      by viewModel.live.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ──────────────────────────────────────────
        Text(
            text       = "REST  vs  gRPC",
            fontSize   = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
            modifier   = Modifier.fillMaxWidth(),
            textAlign  = TextAlign.Center,
        )
        Text(
            text      = "The Problem ↔ The Fix",
            fontSize  = 14.sp,
            color     = Color.Gray,
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        // ── Section 1: one-shot benchmark ───────────────────
        SectionHeader("📦  20 Sequential Requests")
        BenchmarkCard(
            state     = benchmark,
            onRunBenchmark = viewModel::runBenchmark,
        )

        // ── Section 2: live comparison ───────────────────────
        SectionHeader("⚡  Live Price Updates")
        LiveComparisonCard(
            state    = live,
            onStart  = { viewModel.startLiveComparison("p001") },
            onStop   = viewModel::stopLiveComparison,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Benchmark card
// ─────────────────────────────────────────────────────────────
@Composable
fun BenchmarkCard(
    state: BenchmarkState,
    onRunBenchmark: () -> Unit,
) {
    DemoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Row: REST vs gRPC column headers
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TransportLabel("REST", RestColor, Modifier.weight(2f))
                TransportLabel("gRPC", GrpcColor, Modifier.weight(2f))
            }

            // Time row — total wall-clock time for 20 sequential requests
            MetricRow(
                label    = "Time (20 req)",
                restVal  = state.rest?.let { "${it.durationMs} ms" } ?: "—",
                grpcVal  = state.grpc?.let { "${it.durationMs} ms" } ?: "—",
                restBar  = state.rest?.durationMs?.toFloat() ?: 0f,
                grpcBar  = state.grpc?.durationMs?.toFloat() ?: 0f,
                higherIsBad = true,
            )

            // Bytes row — total bytes across 20 requests
            MetricRow(
                label    = "Total bytes",
                restVal  = state.rest?.let { formatBytes(it.bodyBytes) } ?: "—",
                grpcVal  = state.grpc?.let { formatBytes(it.bodyBytes) } ?: "—",
                restBar  = state.rest?.bodyBytes?.toFloat() ?: 0f,
                grpcBar  = state.grpc?.bodyBytes?.toFloat() ?: 0f,
            )

            // Connections callout — explains WHY gRPC is faster
            ConnectionsCallout()

            // Savings callout
            if (state.rest != null && state.grpc != null) {
                val savings = state.rest.bodyBytes - state.grpc.bodyBytes
                if (savings > 0) {
                    SavingsCallout(
                        text = "gRPC payload is ${((savings.toFloat() / state.rest.bodyBytes) * 100).toInt()}% smaller " +
                               "— ${formatBytes(savings)} saved across 20 requests"
                    )
                }
            }

            // Error callouts (shown if a side failed)
            state.restError?.let { ErrorCallout("REST error: $it") }
            state.grpcError?.let { ErrorCallout("gRPC error: $it") }

            Button(
                onClick  = onRunBenchmark,
                enabled  = !state.isRunning,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4ACF)),
            ) {
                if (state.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color    = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Running…")
                } else {
                    Text("▶  Run Benchmark")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Live comparison card
// ─────────────────────────────────────────────────────────────
@Composable
fun LiveComparisonCard(
    state: LiveComparisonState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isRunning = state.rest.isRunning || state.grpc.isRunning

    DemoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LiveSidePanel(
                    label     = "🐢  REST Polling",
                    subtitle  = "1 HTTP call every 2 s",
                    side      = state.rest,
                    color     = RestColor,
                    modifier  = Modifier.weight(1f),
                )
                LiveSidePanel(
                    label     = "⚡  gRPC Stream",
                    subtitle  = "1 connection, push-based",
                    side      = state.grpc,
                    color     = GrpcColor,
                    modifier  = Modifier.weight(1f),
                )
            }

            // Cumulative bytes savings
            if (state.rest.cumulativeBytes > 0 && state.grpc.cumulativeBytes > 0) {
                val saved = state.rest.cumulativeBytes - state.grpc.cumulativeBytes
                if (saved > 0) {
                    SavingsCallout(
                        text = "gRPC saved ${formatBytes(saved)} over " +
                               "${state.grpc.updateCount} updates"
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick  = onStart,
                    enabled  = !isRunning,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = GrpcColor),
                ) { Text("▶  Start") }

                OutlinedButton(
                    onClick  = onStop,
                    enabled  = isRunning,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RestColor),
                ) { Text("■  Stop") }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Live side panel (REST or gRPC column)
// ─────────────────────────────────────────────────────────────
@Composable
fun LiveSidePanel(
    label:    String,
    subtitle: String,
    side:     LiveSideState,
    color:    Color,
    modifier: Modifier = Modifier,
) {
    val priceColor by animateColorAsState(
        targetValue   = if (side.isRunning && side.pricePaise > 0) color else Color.Gray,
        animationSpec = tween(300),
        label         = "priceColor",
    )

    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label,    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(subtitle, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(Modifier.height(4.dp))

        // Animated price display
        AnimatedContent(targetState = side.pricePaise, label = "price") { price ->
            Text(
                text       = if (price > 0) "₹${price / 100}.${price % 100}" else "—",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = priceColor,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(4.dp))
        Divider(color = Color.DarkGray, thickness = 0.5.dp)

        MetricLine("Updates",   "${side.updateCount}")
        MetricLine("Per frame", if (side.lastFrameBytes > 0) formatBytes(side.lastFrameBytes) else "—")
        MetricLine("Total",     if (side.cumulativeBytes > 0) formatBytes(side.cumulativeBytes) else "—")
    }
}

// ─────────────────────────────────────────────────────────────
// Reusable small components
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text       = text,
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color      = Color.White.copy(alpha = 0.85f),
    )
}

@Composable
private fun DemoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content,
        )
    }
}

@Composable
private fun TransportLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text       = text,
        color      = color,
        fontWeight = FontWeight.Bold,
        fontSize   = 13.sp,
        textAlign  = TextAlign.Center,
        modifier   = modifier,
    )
}

@Composable
private fun MetricRow(
    label:       String,
    restVal:     String,
    grpcVal:     String,
    restBar:     Float,
    grpcBar:     Float,
    higherIsBad: Boolean = true,   // if true, the larger bar is drawn in the "bad" color
) {
    val maxVal   = max(restBar, grpcBar).coerceAtLeast(1f)
    // For time: higher = worse, so a bigger REST bar stays red and gRPC teal looks good.
    // For bytes: same idea — larger REST bar is already the "bad" color.
    val restBarColor = if (higherIsBad && restBar > grpcBar) RestColor else RestColor
    val grpcBarColor = GrpcColor
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
            Text(restVal, fontSize = 12.sp, color = RestColor, modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center)
            Text(grpcVal, fontSize = 12.sp, color = GrpcColor, modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center)
        }
        // Bar chart
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.weight(2f).height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray)) {
                if (restBar > 0) Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(restBar / maxVal)
                        .clip(RoundedCornerShape(4.dp))
                        .background(restBarColor)
                )
            }
            Box(Modifier.weight(2f).height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray)) {
                if (grpcBar > 0) Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(grpcBar / maxVal)
                        .clip(RoundedCornerShape(4.dp))
                        .background(grpcBarColor)
                )
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ConnectionsCallout() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("20", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = RestColor)
            Text("TCP handshakes", fontSize = 10.sp, color = Color.Gray)
            Text("REST", fontSize = 10.sp, color = RestColor)
        }
        Text("vs", fontSize = 16.sp, color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterVertically))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("1", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = GrpcColor)
            Text("HTTP/2 connection", fontSize = 10.sp, color = Color.Gray)
            Text("gRPC", fontSize = 10.sp, color = GrpcColor)
        }
    }
}

@Composable
private fun ErrorCallout(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RestColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("⚠️", fontSize = 14.sp)
        Text(text, fontSize = 11.sp, color = RestColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SavingsCallout(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GrpcColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🎉", fontSize = 16.sp)
        Text(text, fontSize = 12.sp, color = GrpcColor, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────
// Formatters
// ─────────────────────────────────────────────────────────────
private fun formatBytes(bytes: Int): String = when {
    bytes < 1_024 -> "$bytes B"
    else          -> "${"%.1f".format(bytes / 1_024f)} KB"
}
