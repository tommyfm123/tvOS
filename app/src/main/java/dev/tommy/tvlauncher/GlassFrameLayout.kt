package dev.tommy.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout

const val PILL_RADIUS = -1f
private const val SHADOW_ALPHA = 0.6f

open class GlassFrameLayout @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
) : FrameLayout(context, attributes) {
    var cornerRadius = PILL_RADIUS
        set(value) {
            field = value
            invalidateOutline()
            invalidate()
        }

    private val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private val borderBounds = RectF()
    private var backdrop: GlassBackdrop? = null

    init {
        setWillNotDraw(false)
        tintPaint.color = context.getColor(R.color.glass_tint)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = resources.getDimension(R.dimen.glass_stroke_width)
        borderPaint.color = context.getColor(R.color.glass_border)
        elevation = resources.getDimension(R.dimen.glass_elevation)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, resolveRadius())
                outline.alpha = SHADOW_ALPHA
            }
        }
    }

    open fun setBackdrop(bitmap: Bitmap) {
        backdrop = GlassBackdrop(bitmap)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val radius = resolveRadius()
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        backdrop?.let {
            it.applyTo(this, backdropPaint)
            canvas.drawRoundRect(bounds, radius, radius, backdropPaint)
        }
        canvas.drawRoundRect(bounds, radius, radius, tintPaint)
        val inset = borderPaint.strokeWidth / 2f
        borderBounds.set(bounds)
        borderBounds.inset(inset, inset)
        canvas.drawRoundRect(borderBounds, radius - inset, radius - inset, borderPaint)
    }

    private fun resolveRadius() = if (cornerRadius == PILL_RADIUS) height / 2f else cornerRadius
}
