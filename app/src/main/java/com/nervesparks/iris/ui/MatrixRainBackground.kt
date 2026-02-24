package com.nervesparks.iris.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.MainViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

// ── Matrix green palette ──────────────────────────────────────────────────────
val MatrixGreen        = Color(0xFF00FF41)
val MatrixGreenDark    = Color(0xFF003B00)
val MatrixGreenMid     = Color(0xFF008F11)
val MatrixGreenBright  = Color(0xFF39FF14)
val MatrixBlack        = Color(0xFF000000)
val MatrixBg           = Color(0xFF010D01)   // very dark green-black

// All chars that can appear in the rain
private val MATRIX_CHARS: List<Char> =
    (0x30A0..0x30FF).map { it.toChar() } +   // Katakana
    ('0'..'9').toList() +
    ('A'..'Z').toList() +
    listOf('$', '#', '@', '&', '%', '|', '/', '\\', '<', '>', '?', '!', '~')

private data class RainColumn(
    val x: Float,
    val charHeight: Float,
    var headY: Float,         // Y of the leading (brightest) character
    val speed: Float,
    val chars: MutableList<Char>,
    val trailLength: Int      // how many glowing chars trail behind
)

/**
 * Full-screen Matrix digital rain background.
 * Efficient single-Canvas approach — no Compose State per char.
 */
@Composable
fun MatrixRainBackground(modifier: Modifier = Modifier) {
    var tick by remember { mutableLongStateOf(0L) }
    val columns = remember { mutableStateOf<List<RainColumn>>(emptyList()) }
    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }

    // Drive the animation at ~30 fps
    LaunchedEffect(Unit) {
        while (true) {
            delay(33)
            tick++
            // Advance each column head
            columns.value = columns.value.map { col ->
                val newHeadY = col.headY + col.speed
                // randomise the leading char
                val idx = (col.headY / col.charHeight).toInt().coerceIn(0, col.chars.size - 1)
                col.chars[idx] = MATRIX_CHARS.random()
                // wrap around
                val wrapped = if (newHeadY - col.trailLength * col.charHeight > canvasH) {
                    -col.charHeight * Random.nextInt(5, 30).toFloat()
                } else newHeadY
                col.copy(headY = wrapped)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBg)
    ) {
        // One-time init
        if (columns.value.isEmpty() && size.width > 0f) {
            canvasW = size.width
            canvasH = size.height
            val charH = 22f
            val colW  = 20f
            val numCols = (size.width / colW).toInt() + 1
            columns.value = List(numCols) { i ->
                RainColumn(
                    x          = i * colW,
                    charHeight = charH,
                    headY      = -charH * Random.nextInt(0, 60).toFloat(),
                    speed      = 3f + Random.nextFloat() * 7f,
                    chars      = MutableList(80) { MATRIX_CHARS.random() },
                    trailLength = Random.nextInt(8, 22)
                )
            }
        }

        // Draw all columns
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 20f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            for (col in columns.value) {
                for (t in 0..col.trailLength) {
                    val cy = col.headY - t * col.charHeight
                    if (cy < -col.charHeight || cy > size.height) continue
                    val charIdx = ((cy / col.charHeight).toInt().coerceAtLeast(0)) % col.chars.size
                    val char = col.chars[charIdx].toString()
                    // Head char = bright white-green, trail fades to dark
                    val alpha = when {
                        t == 0     -> 1f
                        t == 1     -> 0.85f
                        t <= 3     -> 0.55f
                        t <= 6     -> 0.30f
                        else       -> (1f - t.toFloat() / col.trailLength) * 0.15f
                    }.coerceIn(0f, 1f)
                    paint.color = when (t) {
                        0    -> android.graphics.Color.argb((alpha * 255).toInt(), 220, 255, 220)
                        1    -> android.graphics.Color.argb((alpha * 255).toInt(), 100, 255, 100)
                        else -> android.graphics.Color.argb((alpha * 255).toInt(), 0, 180, 20)
                    }
                    canvas.nativeCanvas.drawText(char, col.x, cy, paint)
                }
            }
        }
    }
}

// ── 1. DOWNLOADING ANIMATION — binary data stream flowing left→right ──────────
@Composable
fun DownloadingAnimation(progress: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dl")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dlOffset"
    )

    var binaryRows by remember { mutableStateOf(List(5) { randomBinaryRow() }) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            binaryRows = List(5) { randomBinaryRow() }
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF001000), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rowH = h / 6f
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 13f
                typeface = android.graphics.Typeface.MONOSPACE
            }

            // Draw scrolling binary rows
            drawIntoCanvas { canvas ->
                for (row in 0..4) {
                    val y = rowH * (row + 1)
                    val rowStr = binaryRows.getOrElse(row) { randomBinaryRow() }
                    val textW = rowStr.length * 8f
                    val startX = -textW * offset
                    // draw twice for seamless loop
                    for (pass in 0..1) {
                        val x = startX + pass * textW
                        rowStr.forEachIndexed { i, c ->
                            val brightness = if (c == '1') 255 else 80
                            paint.color = android.graphics.Color.argb(200, 0, brightness, 0)
                            canvas.nativeCanvas.drawText(c.toString(), x + i * 8f, y, paint)
                        }
                    }
                }
                // Progress bar overlay
                val barY = h - 10f
                paint.color = android.graphics.Color.argb(80, 0, 100, 0)
                canvas.nativeCanvas.drawRect(0f, barY - 6f, w, barY + 6f, paint)
                paint.color = android.graphics.Color.argb(255, 0, 255, 65)
                canvas.nativeCanvas.drawRect(0f, barY - 6f, w * progress, barY + 6f, paint)
                // Glowing head of progress
                if (progress > 0.01f) {
                    paint.color = android.graphics.Color.argb(255, 200, 255, 200)
                    canvas.nativeCanvas.drawRect(
                        w * progress - 3f, barY - 8f,
                        w * progress + 3f, barY + 8f, paint
                    )
                }
            }
        }
        Text(
            text = "DOWNLOADING ${(progress * 100).toInt()}%",
            color = MatrixGreenBright,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
        )
    }
}

private fun randomBinaryRow(): String =
    (1..64).map { if (Random.nextBoolean()) '1' else '0' }.joinToString("")

// ── 2. LOADING ANIMATION — Matrix vortex / spinning code ring ────────────────
@Composable
fun LoadingMatrixAnimation(label: String = "LOADING MODEL", modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "load")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadAngle"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var spinChars by remember { mutableStateOf(List(24) { MATRIX_CHARS.random() }) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            val idx = Random.nextInt(spinChars.size)
            spinChars = spinChars.toMutableList().also { it[idx] = MATRIX_CHARS.random() }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r1 = minOf(cx, cy) * 0.72f
            val r2 = minOf(cx, cy) * 0.50f
            val r3 = minOf(cx, cy) * 0.30f

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                // Outer ring — 16 chars
                for (i in 0 until 16) {
                    val a = Math.toRadians((angle + i * 22.5).toDouble())
                    val x = cx + r1 * Math.cos(a).toFloat()
                    val y = cy + r1 * Math.sin(a).toFloat()
                    val brightness = (0.4f + 0.6f * ((i % 4) / 3f)) * pulse
                    paint.color = android.graphics.Color.argb(
                        (brightness * 255).toInt(), 0, 220, 30)
                    paint.textSize = 16f
                    canvas.nativeCanvas.drawText(spinChars[i].toString(), x, y, paint)
                }
                // Middle ring — 8 chars (counter-rotate)
                for (i in 0 until 8) {
                    val a = Math.toRadians((-angle * 1.5 + i * 45.0))
                    val x = cx + r2 * Math.cos(a).toFloat()
                    val y = cy + r2 * Math.sin(a).toFloat()
                    paint.color = android.graphics.Color.argb(
                        (pulse * 255).toInt(), 60, 255, 80)
                    paint.textSize = 18f
                    canvas.nativeCanvas.drawText(spinChars[i + 16].toString(), x, y, paint)
                }
                // Core glow
                paint.color = android.graphics.Color.argb((pulse * 180).toInt(), 0, 255, 60)
                paint.textSize = 28f
                canvas.nativeCanvas.drawText(">_", cx - 18f, cy + 10f, paint)
            }
        }
        Text(
            text = label,
            color = MatrixGreenBright.copy(alpha = pulse),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        )
    }
}

// ── 3. INSTALLING ANIMATION — char fill-up from bottom (like a loading bar) ──
@Composable
fun InstallingAnimation(modifier: Modifier = Modifier) {
    var fillLevel by remember { mutableStateOf(0f) }
    var grid by remember { mutableStateOf(List(8) { List(20) { MATRIX_CHARS.random() } }) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60)
            fillLevel = (fillLevel + 0.012f).coerceAtMost(1f)
            if (fillLevel >= 1f) { delay(600); fillLevel = 0f }
            // Mutate random chars in the filled area
            grid = grid.mapIndexed { row, cols ->
                cols.mapIndexed { col, c ->
                    val rowFraction = 1f - (row.toFloat() / 7f)
                    if (rowFraction <= fillLevel && Random.nextFloat() < 0.15f)
                        MATRIX_CHARS.random()
                    else c
                }
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF001000), RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 14f
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val cellW = size.width  / 20f
                val cellH = size.height / 9f
                for (row in 0 until 8) {
                    for (col in 0 until 20) {
                        val rowFraction = 1f - (row.toFloat() / 7f)
                        val filled = rowFraction <= fillLevel
                        if (filled) {
                            val brightness = if (rowFraction > fillLevel - 0.05f) 255 else 160
                            paint.color = android.graphics.Color.argb(220, 0, brightness, 20)
                        } else {
                            paint.color = android.graphics.Color.argb(40, 0, 80, 0)
                        }
                        canvas.nativeCanvas.drawText(
                            grid[row][col].toString(),
                            col * cellW + 2f,
                            row * cellH + cellH,
                            paint
                        )
                    }
                }
                // Scan line at fill frontier
                val scanY = size.height * (1f - fillLevel)
                paint.color = android.graphics.Color.argb(200, 160, 255, 160)
                canvas.nativeCanvas.drawRect(0f, scanY - 2f, size.width, scanY + 2f, paint)
            }
        }
        Text(
            text = "INSTALLING ${(fillLevel * 100).toInt()}%",
            color = MatrixGreenBright,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        )
    }
}

// ── Loading modal with Matrix vortex ─────────────────────────────────────────
@Composable
fun MatrixLoadingModal(viewModel: MainViewModel) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape  = RoundedCornerShape(16.dp),
            color  = Color(0xFF000D00),
            shadowElevation = 24.dp,
            modifier = Modifier
                .size(280.dp, 220.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000D00))
            ) {
                // subtle matrix rain in the background of the dialog
                Box(modifier = Modifier.fillMaxSize()) {
                    LoadingMatrixAnimation(
                        label = "LOADING MODEL",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = viewModel.loadedModelName.value,
                    color = MatrixGreen.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                )
            }
        }
    }
}
