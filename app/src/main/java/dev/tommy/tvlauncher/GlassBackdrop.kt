package dev.tommy.tvlauncher

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import kotlin.math.max

class GlassBackdrop(private val bitmap: Bitmap) {
    private val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val matrix = Matrix()
    private val locationOnScreen = IntArray(2)

    fun applyTo(view: View, paint: Paint) {
        val screenWidth = view.rootView.width.toFloat()
        val screenHeight = view.rootView.height.toFloat()
        val scale = max(screenWidth / bitmap.width, screenHeight / bitmap.height)
        val offsetX = (screenWidth - bitmap.width * scale) / 2f
        val offsetY = (screenHeight - bitmap.height * scale) / 2f
        view.getLocationOnScreen(locationOnScreen)
        matrix.setScale(scale, scale)
        matrix.postTranslate(offsetX - locationOnScreen[0], offsetY - locationOnScreen[1])
        shader.setLocalMatrix(matrix)
        paint.shader = shader
    }
}
