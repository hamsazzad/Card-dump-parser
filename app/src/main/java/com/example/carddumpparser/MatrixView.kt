package com.example.carddumpparser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * MatrixView draws a simple binary rain animation similar to the
 * JavaScript implementation in the provided HTML. It repeatedly
 * draws random '0' and '1' characters falling down the screen. The
 * implementation intentionally avoids allocating large objects each
 * frame and uses a single Paint instance to minimise memory churn.
 */
class MatrixView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chars = charArrayOf('0', '1')
    private val fontSize = 16f * resources.displayMetrics.density // scale font size for density
    private var drops: IntArray = IntArray(0)

    private val updateRunnable = object : Runnable {
        override fun run() {
            invalidate()
            // Schedule next frame. 50ms corresponds to ~20fps which is
            // sufficient for a subtle background animation on low end devices.
            postDelayed(this, 50L)
        }
    }

    init {
        paint.color = Color.GREEN
        paint.textSize = fontSize
        paint.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Determine how many columns of characters we can draw across the
        // width. Add 1 to ensure coverage.
        val columns = (w / fontSize).toInt() + 1
        drops = IntArray(columns) { Random.nextInt((h / fontSize).toInt()) }
    }

    override fun onDraw(canvas: Canvas) {
        // Fade the canvas slightly by drawing a translucent black overlay.
        canvas.drawColor(Color.argb(25, 0, 0, 0))
        val heightPx = height.toFloat()
        val charHeight = fontSize
        // Draw each column. We choose a random character each time and
        // increment its drop position. When the drop leaves the bottom
        // randomly reset it near the top.
        for (i in drops.indices) {
            val text = chars[Random.nextInt(chars.size)].toString()
            val x = i * fontSize
            val y = drops[i] * charHeight
            canvas.drawText(text, x, y, paint)
            if (y > heightPx && Random.nextFloat() > 0.98f) {
                drops[i] = 0
            }
            drops[i]++
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Start the animation loop when the view is attached.
        removeCallbacks(updateRunnable)
        post(updateRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Stop the animation loop when the view is detached to avoid leaks.
        removeCallbacks(updateRunnable)
    }
}