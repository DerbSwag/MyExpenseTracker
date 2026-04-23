// ═══════════════════════════════════════════════════════════════
//  S25UltraFeatures.kt — S Pen Handwriting Input
//  วาดตัวเลขด้วย S Pen / นิ้ว → ML Kit OCR → แปลงเป็นจำนวนเงิน
// ═══════════════════════════════════════════════════════════════
package com.example.myexpensetracker

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

private data class DrawnLine(val points: List<Offset>)

/**
 * S Pen / Stylus handwriting input for amount.
 * วาดตัวเลขบน canvas → กด "แปลงตัวเลข" → ML Kit OCR อ่าน → callback จำนวนเงิน
 *
 * Usage in AddEditScreen:
 *   SPenAmountInput(onRecognized = { amount = it })
 */
@Composable
fun SPenAmountInput(
    onRecognized: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lines by remember { mutableStateOf(listOf<DrawnLine>()) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    var showHint by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(0.4f), RoundedCornerShape(14.dp))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> showHint = false; current = listOf(offset) },
                            onDrag = { change, _ -> current = current + change.position },
                            onDragEnd = {
                                if (current.size > 1) lines = lines + DrawnLine(current)
                                current = emptyList()
                            }
                        )
                    }
            ) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()

                val strokeStyle = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val inkColor = Color(0xFF1A73E8)

                (lines.map { it.points } + listOf(current)).forEach { pts ->
                    if (pts.size > 1) {
                        val path = Path().apply {
                            pts.forEachIndexed { i, pt -> if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y) }
                        }
                        drawPath(path, inkColor, style = strokeStyle)
                    }
                }
            }

            if (showHint) {
                Text("✍️ เขียนจำนวนเงินด้วย S Pen", Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), fontSize = 14.sp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { lines = emptyList(); current = emptyList(); showHint = true },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("ล้าง", fontSize = 13.sp) }

            Button(
                onClick = {
                    if (lines.isEmpty() || canvasWidth <= 0) return@Button
                    isProcessing = true

                    // Render lines to bitmap
                    val bmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        strokeWidth = 6f
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    lines.forEach { line ->
                        if (line.points.size > 1) {
                            val path = android.graphics.Path()
                            line.points.forEachIndexed { i, pt ->
                                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                            }
                            canvas.drawPath(path, paint)
                        }
                    }

                    // ML Kit OCR
                    val image = InputImage.fromBitmap(bmp, 0)
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                        .addOnSuccessListener { result ->
                            isProcessing = false
                            // Extract numbers from recognized text
                            val nums = Regex("\\d+\\.?\\d*").findAll(result.text)
                                .mapNotNull { it.value.toDoubleOrNull() }
                                .filter { it > 0 }
                                .toList()
                            if (nums.isNotEmpty()) {
                                // ใช้ตัวเลขที่ใหญ่ที่สุด (กรณีเขียนหลายตัว)
                                onRecognized(nums.max().toLong().toString())
                            }
                            bmp.recycle()
                        }
                        .addOnFailureListener {
                            isProcessing = false
                            bmp.recycle()
                        }
                },
                modifier = Modifier.weight(1f),
                enabled = !isProcessing && lines.isNotEmpty()
            ) {
                if (isProcessing) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) }
                else { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("แปลงตัวเลข", fontSize = 13.sp) }
            }
        }
    }
}
