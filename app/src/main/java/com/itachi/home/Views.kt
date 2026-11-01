package com.itachi.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.*
import kotlin.random.Random

/** Itachi's eyes: iris follows lookAt(), random blinks. */
class EyesView(c: Context) : View(c) {
    private val sclera = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B0B0F.toInt() }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB3121B.toInt() }
    private val blk = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private var dx = 0f
    private var dy = 0f
    private var open = 1f
    private val h = Handler(Looper.getMainLooper())
    private val blinkRun = object : Runnable {
        override fun run() {
            ValueAnimator.ofFloat(1f, 0.05f, 1f).apply {
                duration = 180
                addUpdateListener { open = it.animatedValue as Float; invalidate() }
                start()
            }
            h.postDelayed(this, 1800L + Random.nextLong(4500))
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); h.postDelayed(blinkRun, 2000) }
    override fun onDetachedFromWindow() { h.removeCallbacks(blinkRun); super.onDetachedFromWindow() }

    fun lookAt(sx: Float, sy: Float) {
        val l = IntArray(2); getLocationOnScreen(l)
        val ex = sx - (l[0] + width / 2f)
        val ey = sy - (l[1] + height / 2f)
        val d = hypot(ex, ey).coerceAtLeast(1f)
        val tx = ex / d; val ty = ey / d; val x0 = dx; val y0 = dy
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            addUpdateListener { val t = it.animatedValue as Float; dx = x0 + (tx - x0) * t; dy = y0 + (ty - y0) * t; invalidate() }
            start()
        }
    }

    override fun onDraw(cv: Canvas) {
        val eyeW = width * 0.46f
        val cy = height / 2f
        val eyeH = eyeW * 0.5f * open
        for (i in 0..1) {
            val cx = width * (0.25f + 0.5f * i)
            val p = Path().apply {
                moveTo(cx - eyeW / 2, cy)
                quadTo(cx, cy - eyeH, cx + eyeW / 2, cy)
                quadTo(cx, cy + eyeH, cx - eyeW / 2, cy)
                close()
            }
            cv.drawPath(p, sclera)
            cv.save(); cv.clipPath(p)
            val ix = cx + dx * eyeW * 0.14f
            val iy = cy + dy * eyeW * 0.07f
            val r = eyeW * 0.28f
            cv.drawCircle(ix, iy, r, red)
            cv.drawCircle(ix, iy, r * 0.3f, blk)
            for (k in 0..2) {
                val a = Math.toRadians(90.0 + k * 120)
                cv.drawCircle(ix + (r * 0.6f * cos(a)).toFloat(), iy + (r * 0.6f * sin(a)).toFloat(), r * 0.17f, blk)
            }
            cv.restore()
            cv.drawPath(p, edge)
        }
    }
}

/** Sharingan that spins in, used on wake. */
class SharinganView(c: Context) : View(c) {
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB3121B.toInt() }
    private val blk = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = min(width, height) / 2f * 0.95f
        ring.strokeWidth = r * 0.03f
        cv.drawCircle(cx, cy, r, red)
        cv.drawCircle(cx, cy, r * 0.72f, ring)
        cv.drawCircle(cx, cy, r * 0.18f, blk)
        for (i in 0..2) {
            val a = Math.toRadians(90.0 + i * 120)
            cv.drawCircle(cx + (r * 0.5f * cos(a)).toFloat(), cy + (r * 0.5f * sin(a)).toFloat(), r * 0.12f, blk)
        }
    }

    fun play(end: () -> Unit) {
        rotation = 0f; scaleX = 0.5f; scaleY = 0.5f
        animate().rotation(1080f).scaleX(1f).scaleY(1f).setDuration(1700)
            .setInterpolator(DecelerateInterpolator()).withEndAction(end).start()
    }
}

/** One clock digit that rolls like a dial when it changes. */
class DialDigit(c: Context) : View(c) {
    private var cur = 0
    private var prev = 0
    private var t = 1f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }

    fun set(d: Int) {
        if (d == cur) return
        prev = cur; cur = d
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            addUpdateListener { t = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(cv: Canvas) {
        p.textSize = height * 0.8f
        val base = height * 0.78f
        cv.save(); cv.clipRect(0, 0, width, height)
        cv.drawText(prev.toString(), width / 2f, base - t * height, p)
        cv.drawText(cur.toString(), width / 2f, base + (1 - t) * height, p)
        cv.restore()
    }
}
