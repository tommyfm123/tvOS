package dev.tommy.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

private const val ART_WIDTH = 576
private const val ART_HEIGHT = 324
private const val GRADIENT_TOP_COLOR = 0xFF3A3A3C.toInt()
private const val GRADIENT_BOTTOM_COLOR = 0xFF1C1C1E.toInt()
private const val GLYPH_COLOR = 0xFFFFFFFF.toInt()
private const val LABEL_COLOR = 0xE6FFFFFF.toInt()
private const val GLYPH_WIDTH = 132f
private const val GLYPH_HEIGHT = 70f
private const val GLYPH_CHAMFER = 28f
private const val GLYPH_STROKE = 9f
private const val GLYPH_CENTER_Y = 124f
private const val LABEL_BASELINE_Y = 250f
private const val LABEL_TEXT_SIZE = 46f
private const val LABEL_FONT_FAMILY = "sans-serif-medium"

object PortArtFactory {
    fun create(context: Context, label: String): Drawable {
        val bitmap = Bitmap.createBitmap(ART_WIDTH, ART_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, ART_HEIGHT.toFloat(), GRADIENT_TOP_COLOR, GRADIENT_BOTTOM_COLOR, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, ART_WIDTH.toFloat(), ART_HEIGHT.toFloat(), paint)
        paint.shader = null
        drawGlyph(canvas, paint)
        drawLabel(canvas, paint, label)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun drawGlyph(canvas: Canvas, paint: Paint) {
        val left = (ART_WIDTH - GLYPH_WIDTH) / 2f
        val top = GLYPH_CENTER_Y - GLYPH_HEIGHT / 2f
        val right = left + GLYPH_WIDTH
        val bottom = top + GLYPH_HEIGHT
        val glyph = Path().apply {
            moveTo(left, top)
            lineTo(right, top)
            lineTo(right, bottom - GLYPH_CHAMFER)
            lineTo(right - GLYPH_CHAMFER, bottom)
            lineTo(left + GLYPH_CHAMFER, bottom)
            lineTo(left, bottom - GLYPH_CHAMFER)
            close()
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = GLYPH_STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = GLYPH_COLOR
        canvas.drawPath(glyph, paint)
    }

    private fun drawLabel(canvas: Canvas, paint: Paint, label: String) {
        paint.style = Paint.Style.FILL
        paint.color = LABEL_COLOR
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = LABEL_TEXT_SIZE
        paint.typeface = Typeface.create(LABEL_FONT_FAMILY, Typeface.NORMAL)
        canvas.drawText(label, ART_WIDTH / 2f, LABEL_BASELINE_Y, paint)
    }
}
