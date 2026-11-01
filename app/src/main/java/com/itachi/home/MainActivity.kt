package com.itachi.home

import android.app.Activity
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private val M = ViewGroup.LayoutParams.MATCH_PARENT
    private val W = ViewGroup.LayoutParams.WRAP_CONTENT
    private lateinit var wallA: ImageView
    private lateinit var wallB: ImageView
    private lateinit var glow: View
    private lateinit var eyes: EyesView
    private lateinit var row: LinearLayout
    private lateinit var overlay: FrameLayout
    private lateinit var shar: SharinganView
    private lateinit var hm: TextView
    private lateinit var date: TextView
    private lateinit var d1: DialDigit
    private lateinit var d2: DialDigit
    private var wallIdx = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private val screenOn = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { wake() }
    }
    private val tick = object : Runnable {
        override fun run() {
            val c = Calendar.getInstance()
            hm.text = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            date.text = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(c.time)
            val s = c.get(Calendar.SECOND)
            d1.set(s / 10); d2.set(s % 10)
            h.postDelayed(this, 250)
        }
    }
    private val wallLoop = object : Runnable {
        override fun run() { nextWall(); h.postDelayed(this, 300000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        wallA = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        wallB = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; alpha = 0f }
        root.addView(wallA, M, M); root.addView(wallB, M, M)
        val dim = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x33000000, 0x00000000, 0xDD000000.toInt()))
        }
        root.addView(dim, M, M)

        glow = View(this).apply {
            alpha = 0f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = dp(350).toFloat()
                colors = intArrayOf(0x40FFFFFF, 0x00FFFFFF)
            }
        }
        root.addView(glow, FrameLayout.LayoutParams(dp(700), dp(700)))

        eyes = EyesView(this)
        root.addView(eyes, FrameLayout.LayoutParams(dp(320), dp(90), Gravity.TOP or Gravity.START).apply { setMargins(dp(48), dp(32), 0, 0) })

        hm = TextView(this).apply {
            setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        d1 = DialDigit(this); d2 = DialDigit(this)
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        timeRow.addView(hm)
        timeRow.addView(d1, LinearLayout.LayoutParams(dp(22), dp(34)).apply { leftMargin = dp(6); bottomMargin = dp(8) })
        timeRow.addView(d2, LinearLayout.LayoutParams(dp(22), dp(34)).apply { bottomMargin = dp(8) })
        date = TextView(this).apply { setTextColor(0x99FFFFFF.toInt()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); gravity = Gravity.END }
        val clock = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
        clock.addView(timeRow); clock.addView(date, LinearLayout.LayoutParams(M, W))
        root.addView(clock, FrameLayout.LayoutParams(W, W, Gravity.TOP or Gravity.END).apply { setMargins(0, dp(28), dp(48), 0) })

        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_HORIZONTAL; clipChildren = false }
        val scroll = HorizontalScrollView(this).apply {
            clipChildren = false; clipToPadding = false; isHorizontalScrollBarEnabled = false; isFillViewport = true
            setPadding(dp(60), dp(50), dp(60), dp(50))
        }
        scroll.addView(row, FrameLayout.LayoutParams(M, W))
        root.addView(scroll, FrameLayout.LayoutParams(M, W, Gravity.BOTTOM).apply { bottomMargin = dp(40) })

        shar = SharinganView(this)
        overlay = FrameLayout(this).apply { setBackgroundColor(Color.BLACK); visibility = View.GONE }
        overlay.addView(shar, FrameLayout.LayoutParams(dp(520), dp(520), Gravity.CENTER))
        root.addView(overlay, M, M)

        setContentView(root)
        loadApps()
        root.post { wake() }
    }

    override fun onStart() { super.onStart(); registerReceiver(screenOn, IntentFilter(Intent.ACTION_SCREEN_ON)) }
    override fun onStop() { unregisterReceiver(screenOn); super.onStop() }
    override fun onResume() { super.onResume(); h.post(tick); h.post(wallLoop) }
    override fun onPause() { h.removeCallbacks(tick); h.removeCallbacks(wallLoop); super.onPause() }
    override fun onBackPressed() {}

    private fun wake() {
        overlay.visibility = View.VISIBLE; overlay.alpha = 1f
        shar.play { overlay.animate().alpha(0f).setDuration(400).withEndAction { overlay.visibility = View.GONE }.start() }
    }

    private fun loadApps() {
        val pm = packageManager
        var list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        if (list.isEmpty()) list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        list.filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .forEach { addItem(it) }
        row.getChildAt(0)?.requestFocus()
    }

    private fun white(d: Drawable): Drawable {
        val base = if (d is AdaptiveIconDrawable) (d.foreground ?: d) else d
        return base.mutate().apply { colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }
    }

    private fun addItem(ri: ResolveInfo) {
        val pm = packageManager
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            isFocusable = true; isClickable = true; clipChildren = false
            setPadding(dp(16), 0, dp(16), 0)
        }
        val icon = ImageView(this).apply { setImageDrawable(white(ri.loadIcon(pm))); alpha = 0.6f }
        val label = TextView(this).apply {
            text = ri.loadLabel(pm); setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            alpha = 0f; gravity = Gravity.CENTER; maxLines = 1
        }
        item.addView(icon, LinearLayout.LayoutParams(dp(96), dp(96)))
        item.addView(label, LinearLayout.LayoutParams(dp(120), W).apply { topMargin = dp(6) })
        item.setOnFocusChangeListener { v, f ->
            val s = if (f) 1.4f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(180).start()
            label.animate().alpha(if (f) 1f else 0f).setDuration(180).start()
            icon.animate().alpha(if (f) 1f else 0.6f).setDuration(180).start()
            if (f) track(v)
        }
        item.setOnClickListener { open(ri, it) }
        row.addView(item)
    }

    private fun track(v: View) {
        fun apply() {
            val l = IntArray(2); v.getLocationOnScreen(l)
            val cx = l[0] + v.width / 2f; val cy = l[1] + v.height / 2f
            glow.animate().alpha(1f).x(cx - glow.width / 2f).y(cy - glow.height / 2f).setDuration(350).start()
            eyes.lookAt(cx, cy)
        }
        apply()
        v.postDelayed({ if (v.hasFocus()) apply() }, 250)
    }

    private fun open(ri: ResolveInfo, v: View) {
        val i = Intent(Intent.ACTION_MAIN).setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i, ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.width, v.height).toBundle())
    }

    private fun nextWall() {
        val dir = File(getExternalFilesDir(null), "wallpapers").apply { mkdirs() }
        val fs = dir.listFiles()?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
            ?.sortedBy { it.name } ?: return
        if (fs.isEmpty()) return
        val opts = BitmapFactory.Options().apply { inSampleSize = 2; inPreferredConfig = Bitmap.Config.RGB_565 }
        val bmp = BitmapFactory.decodeFile(fs[wallIdx % fs.size].path, opts) ?: return
        wallIdx++
        val back = if (wallA.alpha >= 0.5f) wallB else wallA
        val front = if (back === wallA) wallB else wallA
        back.setImageBitmap(bmp)
        back.animate().alpha(1f).setDuration(1500).start()
        front.animate().alpha(0f).setDuration(1500).start()
    }
}
