package com.mapchina.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.roundToInt

object PhotoMarkerRenderer {
    private const val TAG = "PhotoMarkerRenderer"
    private const val SIZE_DP = 56
    private const val RADIUS_DP = 8
    private const val BADGE_SIZE_DP = 20
    private const val STACK_OFFSET_DP = 4

    fun render(context: Context, imagePath: String, count: Int): Bitmap? {
        val density = context.resources.displayMetrics.density
        val size = (SIZE_DP * density).roundToInt()
        val radius = RADIUS_DP * density
        val badgeSize = (BADGE_SIZE_DP * density).roundToInt()
        val stackOffset = (STACK_OFFSET_DP * density).roundToInt()

        val totalWidth = size + stackOffset * (count.coerceAtMost(2) - 1)
        val totalHeight = size + stackOffset * (count.coerceAtMost(2) - 1) + (8 * density).roundToInt()

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val image = loadImage(context, imagePath, size)
        if (image == null) {
            Log.w(TAG, "Failed to load image: $imagePath, using placeholder")
            return renderPlaceholder(size, radius, count, density)
        }

        val layers = count.coerceAtMost(2)
        for (i in (layers - 1) downTo 0) {
            val offsetX = i * stackOffset.toFloat()
            val offsetY = i * stackOffset.toFloat()
            val rect = RectF(offsetX, offsetY, offsetX + size, offsetY + size)

            val clipPath = Path()
            clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)

            if (i == 0) {
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(image, offsetX, offsetY, null)
                canvas.restore()
                val borderPaint = Paint().apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f * density; isAntiAlias = true }
                canvas.drawRoundRect(rect, radius, radius, borderPaint)
            } else {
                val bgPaint = Paint().apply { color = 0xFFE8E5DD.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
                canvas.drawRoundRect(rect, radius, radius, bgPaint)
                val borderPaint = Paint().apply { color = 0xFFD0CCC4.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f * density; isAntiAlias = true }
                canvas.drawRoundRect(rect, radius, radius, borderPaint)
            }
        }
        image.recycle()

        if (count > 1) {
            val badgeX = totalWidth - badgeSize.toFloat() - 2 * density
            val badgeY = 2 * density
            val badgePaint = Paint().apply { color = 0xFFE94560.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
            canvas.drawCircle(badgeX + badgeSize / 2f, badgeY + badgeSize / 2f, badgeSize / 2f, badgePaint)
            val textPaint = Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 11f * density; isAntiAlias = true; textAlign = Paint.Align.CENTER }
            val text = if (count > 99) "99+" else count.toString()
            canvas.drawText(text, badgeX + badgeSize / 2f, badgeY + badgeSize / 2f + 4f * density, textPaint)
        }

        return bitmap
    }

    private fun loadImage(context: Context, path: String, size: Int): Bitmap? {
        return try {
            val data = if (File(path).exists()) {
                Uri.fromFile(File(path))
            } else {
                path
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
            Log.e(TAG, "Error loading image $path", e)
            null
        }
    }

    private fun renderPlaceholder(size: Int, radius: Float, count: Int, density: Float): Bitmap? {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply { color = 0xFFE8E5DD.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, bgPaint)
        // Draw a simple camera-like geometric icon
        val iconPaint = Paint().apply { color = 0xFF9E9E9E.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f * density; isAntiAlias = true }
        val cx = size / 2f
        val cy = size / 2f
        val bodyW = 16f * density
        val bodyH = 12f * density
        canvas.drawRoundRect(RectF(cx - bodyW, cy - bodyH, cx + bodyW, cy + bodyH), 3f * density, 3f * density, iconPaint)
        val lensR = 6f * density
        iconPaint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, lensR, iconPaint)
        // Small bump on top (flash)
        val flashPaint = Paint().apply { color = 0xFF9E9E9E.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(RectF(cx - 4f * density, cy - bodyH - 4f * density, cx + 2f * density, cy - bodyH), 1f * density, 1f * density, flashPaint)
        return bitmap
    }
}
