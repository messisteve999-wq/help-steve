package com.steve.helpsteve

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Fond animé : petits robots futuristes qui se déplacent et échangent des impulsions lumineuses.
class RobotBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Bot(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        val color: Int,
        val phase: Float
    )

    private val bots = mutableListOf<Bot>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private var time = 0f
    private var areaW = 0
    private var areaH = 0
    private val palette = intArrayOf(0xFF00E5FF.toInt(), 0xFFB26BFF.toInt(), 0xFFFF4FD8.toInt())

    private val ticker = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { step(0.016f); invalidate() }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        areaW = w
        areaH = h
        bots.clear()
        repeat(6) { i ->
            bots += Bot(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h,
                vx = (Random.nextFloat() - 0.5f) * 80f,
                vy = (Random.nextFloat() - 0.5f) * 80f,
                color = palette[i % palette.size],
                phase = Random.nextFloat() * 6.28f
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ticker.start()
    }

    override fun onDetachedFromWindow() {
        ticker.cancel()
        super.onDetachedFromWindow()
    }

    private fun step(dt: Float) {
        time += dt
        for (b in bots) {
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (b.x < 40f || b.x > areaW - 40f) b.vx = -b.vx
            if (b.y < 80f || b.y > areaH - 80f) b.vy = -b.vy
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0A1F.toInt(), 0xFF150B2E.toInt(), 0xFF050510.toInt()),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Grille de circuits
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = 0x2200E5FF
        var gx = 0f
        while (gx < w) { canvas.drawLine(gx, 0f, gx, h, paint); gx += 60f }
        var gy = 0f
        while (gy < h) { canvas.drawLine(0f, gy, w, gy, paint); gy += 60f }

        // Échanges lumineux entre robots proches
        for (i in bots.indices) for (j in i + 1 until bots.size) {
            val a = bots[i]
            val b = bots[j]
            val dx = a.x - b.x
            val dy = a.y - b.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < 260f) {
                val alpha = ((1f - d / 260f) * 120).toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = Color.argb(alpha, Color.red(a.color), Color.green(a.color), Color.blue(a.color))
                canvas.drawLine(a.x, a.y, b.x, b.y, paint)

                // Impulsion qui voyage d'un robot à l'autre
                val t = (time * 0.6f + a.phase) % 1f
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                paint.alpha = alpha
                canvas.drawCircle(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, 4f, paint)
            }
        }

        for (b in bots) drawBot(canvas, b)
    }

    private fun drawBot(c: Canvas, b: Bot) {
        val w = 84f
        val h = 96f
        val rect = RectF(b.x - w / 2, b.y - h / 2, b.x + w / 2, b.y + h / 2)

        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = Color.argb(45, Color.red(b.color), Color.green(b.color), Color.blue(b.color))
        c.drawRoundRect(rect, 18f, 18f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = b.color
        c.drawRoundRect(rect, 18f, 18f, paint)
        c.drawLine(b.x, rect.top, b.x, rect.top - 22f, paint)

        paint.style = Paint.Style.FILL
        c.drawCircle(b.x, rect.top - 24f, 5f, paint)

        // Yeux qui clignotent
        val blink = sin(time * 2f + b.phase) > 0.96f
        val eyeRadius = if (blink) 2f else 9f
        paint.color = Color.WHITE
        c.drawCircle(b.x - 18f, b.y - 10f, eyeRadius, paint)
        c.drawCircle(b.x + 18f, b.y - 10f, eyeRadius, paint)

        // Barre lumineuse en guise de bouche
        paint.color = b.color
        c.drawRoundRect(RectF(b.x - 20f, b.y + 18f, b.x + 20f, b.y + 26f), 4f, 4f, paint)
    }
}
