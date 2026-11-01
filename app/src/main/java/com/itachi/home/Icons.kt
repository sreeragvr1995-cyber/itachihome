package com.itachi.home

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun logoOnly(d: Drawable): Drawable {
    val s = 128
    val src = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    d.setBounds(0, 0, s, s)
    d.draw(Canvas(src))
    val px = IntArray(s * s)
    src.getPixels(px, 0, s, 0, 0, s, s)
    var opaque = 0
    for (p in px) if (Color.alpha(p) > 200) opaque++
    val boxed = opaque > s * s * 0.85
    val bg = px[(s / 2) * s + 3]
    for (i in px.indices) {
        val p = px[i]
        val a = if (boxed) {
            val dist = max(abs(Color.red(p) - Color.red(bg)), max(abs(Color.green(p) - Color.green(bg)), abs(Color.blue(p) - Color.blue(bg))))
            min(255, dist * 3) * Color.alpha(p) / 255
        } else Color.alpha(p)
        px[i] = (a shl 24) or 0xFFFFFF
    }
    val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    out.setPixels(px, 0, s, 0, 0, s, s)
    return BitmapDrawable(Resources.getSystem(), out)
}
