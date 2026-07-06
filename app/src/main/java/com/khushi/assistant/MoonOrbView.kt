package com.khushi.assistant

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * A glowing moon with pulsing light rays around it - shown as a floating
 * overlay while Khushi is listening or speaking, similar to Siri's orb.
 */
class MoonOrbView(context: Context) : View(context) {

    private var pulsePhase = 0f
    private var active = false

    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F3E7")
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0AAFF")
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 2200
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            pulsePhase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        animator.start()
    }

    fun setActive(isActive: Boolean) {
        active = isActive
        animator.duration = if (isActive) 700L else 2200L
    }

    fun stop() {
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = minOf(width, height) / 3.2f
        val pulse = (Math.sin(pulsePhase.toDouble()) * 0.15 + 1.0).toFloat()
        val radius = baseRadius * pulse

        val glowRadius = radius * (if (active) 2.1f else 1.6f)
        glowPaint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(Color.parseColor("#807B2CBF"), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)

        val rayCount = 8
        for (i in 0 until rayCount) {
            val angle = (2 * Math.PI / rayCount * i) + pulsePhase
            val lengthBoost = if (active) 0.55f else 0.2f
            val rayLength = radius * (1.3f + lengthBoost * Math.sin(pulsePhase + i).toFloat())
            val startX = cx + (radius * 1.1f * Math.cos(angle)).toFloat()
            val startY = cy + (radius * 1.1f * Math.sin(angle)).toFloat()
            val endX = cx + (rayLength * Math.cos(angle)).toFloat()
            val endY = cy + (rayLength * Math.sin(angle)).toFloat()
            rayPaint.alpha = (150 + 100 * Math.sin(pulsePhase + i)).toInt().coerceIn(50, 255)
            canvas.drawLine(startX, startY, endX, endY, rayPaint)
        }

        canvas.drawCircle(cx, cy, radius, moonPaint)
        canvas.drawCircle(cx + radius * 0.35f, cy - radius * 0.2f, radius * 0.85f, shadowPaint)
    }
}
