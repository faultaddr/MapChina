package com.mapchina.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.roundToInt

object AttractionMarkerRenderer {
    private const val TAG = "AttractionMarkerRenderer"
    private const val SIZE_DP = 48
    private const val RADIUS_DP = 8
    private const val TAIL_HEIGHT_DP = 10
    private const val BORDER_DP = 2

    fun render(
        context: Context,
        imageUrl: String?,
        name: String,
        visited: Boolean
    ): Bitmap? {
        val density = context.resources.displayMetrics.density
        val size = (SIZE_DP * density).roundToInt()
        val tailH = (TAIL_HEIGHT_DP * density).roundToInt()
        val borderW = (BORDER_DP * density).roundToInt()
        val radius = RADIUS_DP * density

        val totalW = size + borderW * 2
        val totalH = size + tailH + borderW * 2

        val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Load image or fall back to text placeholder
        val image = imageUrl?.let { loadImage(context, it, size) }

        // Draw tail (pointed bottom)
        val tailPaint = Paint().apply {
            color = if (visited) 0xFFC84530.toInt() else 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val path = Path()
        path.moveTo(totalW / 2f - 6 * density, size + borderW.toFloat())
        path.lineTo(totalW / 2f, totalH.toFloat())
        path.lineTo(totalW / 2f + 6 * density, size + borderW.toFloat())
        path.close()
        canvas.drawPath(path, tailPaint)

        // Rounded rect body
        val bodyRect = RectF(
            borderW.toFloat(), borderW.toFloat(),
            (size + borderW).toFloat(), (size + borderW).toFloat()
        )

        // White border
        val borderPaint = Paint().apply {
            color = if (visited) 0xFFC84530.toInt() else 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(bodyRect, radius, radius, borderPaint)

        // Image or placeholder
        val innerRect = RectF(
            (borderW * 2).toFloat(), (borderW * 2).toFloat(),
            (size - borderW).toFloat(), (size - borderW).toFloat()
        )
        val innerRadius = (radius - borderW).coerceAtLeast(0f)

        if (image != null) {
            canvas.save()
            val clipPath = Path()
            clipPath.addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            canvas.drawBitmap(image, innerRect.left, innerRect.top, null)
            canvas.restore()
            image.recycle()
        } else {
            // Gradient placeholder with initial
            val bgPaint = Paint().apply {
                shader = android.graphics.LinearGradient(
                    innerRect.left, innerRect.top, innerRect.right, innerRect.bottom,
                    if (visited) 0xFFE8A090.toInt() else 0xFFE0F5F5.toInt(),
                    if (visited) 0xFFC84530.toInt() else 0xFF0D7377.toInt(),
                    android.graphics.Shader.TileMode.CLAMP
                )
                isAntiAlias = true
            }
            canvas.drawRoundRect(innerRect, innerRadius, innerRadius, bgPaint)

            val textPaint = Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 18f * density
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val textY = innerRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(name.take(1), innerRect.centerX(), textY, textPaint)
        }

        // Visited dot indicator
        if (visited) {
            val dotR = 4 * density
            val dotX = innerRect.right - dotR * 1.5f
            val dotY = innerRect.top + dotR * 1.5f
            val dotPaint = Paint().apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(dotX, dotY, dotR + 1.5f * density, dotPaint)
            val innerDotPaint = Paint().apply {
                color = 0xFFC84530.toInt()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(dotX, dotY, dotR, innerDotPaint)
        }

        return bitmap
    }

    private fun loadImage(context: Context, imageUrl: String, size: Int): Bitmap? {
        return try {
            val data = if (File(imageUrl).exists()) {
                Uri.fromFile(File(imageUrl))
            } else {
                imageUrl
            }
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(data)
                .size(size, size)
                .allowHardware(false)
                .build()
            val result = runBlocking { loader.execute(request) }
            result.image?.toBitmap()?.let { scaled ->
                if (scaled.width != size || scaled.height != size) {
                    val scaled2 = Bitmap.createScaledBitmap(scaled, size, size, true)
                    if (scaled2 !== scaled) scaled.recycle()
                    scaled2
                } else scaled
            }
        } catch (e: Exception) {
            null
        }
    }
}
