package com.tvtheme.p3launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.animation.LinearInterpolator

val ACCENT = 0xFF2EC4FF.toInt()
val NAVY = 0xFF041048.toInt()

/** Slanted tile: dark blue when idle, bright white-blue with small lift when focused. */
class Tile(ctx: Context, private val label: String, private val icon: Drawable?, private val glyph: String?) : View(ctx) {
    private val d = ctx.resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * d }
    private val txt = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        textSize = 14f * d; textAlign = Paint.Align.CENTER
    }
    private val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC); textAlign = Paint.Align.CENTER
    }
    private val path = Path()

    init {
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false
        onFocusChangeListener = OnFocusChangeListener { _, f ->
            animate().scaleX(if (f) 1.1f else 1f).scaleY(if (f) 1.1f else 1f)
                .translationY(if (f) -6f * d else 0f).setDuration(140).start()
            invalidate()
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val skew = h * 0.18f
        val f = isFocused
        path.reset()
        path.moveTo(skew, 0f); path.lineTo(w, 0f); path.lineTo(w - skew, h); path.lineTo(0f, h); path.close()
        fill.color = if (f) 0xFFE9F8FF.toInt() else 0xCC0A1D6B.toInt()
        c.drawPath(path, fill)
        stroke.color = if (f) ACCENT else 0x662EC4FF
        c.drawPath(path, stroke)
        if (icon != null) {
            val s = (h * 0.46f).toInt(); val top = (h * 0.14f).toInt(); val left = ((w - s) / 2f).toInt()
            icon.setBounds(left, top, left + s, top + s); icon.draw(c)
        } else if (glyph != null) {
            gp.textSize = h * 0.42f; gp.color = if (f) NAVY else ACCENT
            c.drawText(glyph, w / 2f, h * 0.60f, gp)
        }
        txt.color = if (f) NAVY else Color.WHITE
        val t = TextUtils.ellipsize(label.uppercase(), txt, w * 0.72f, TextUtils.TruncateAt.END).toString()
        c.drawText(t, w / 2f, h * 0.88f, txt)
    }
}

/** Dark-blue gradient, glowing moon with slow rotating dashed ring, drifting diagonal light bands. */
class BackgroundView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0x552EC4FF; strokeWidth = 3f * ctx.resources.displayMetrics.density
    }
    private val band = Path()
    private var t = 0f
    private var bg: Shader? = null
    private var glow: Shader? = null
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 30000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg = LinearGradient(0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF010512.toInt(), 0xFF05143F.toInt(), 0xFF0A2A7A.toInt()),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        glow = RadialGradient(w * 0.78f, h * 0.30f, h * 0.55f, 0x552EC4FF, 0x002EC4FF, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.shader = bg; c.drawRect(0f, 0f, w, h, p)
        // drifting diagonal bands
        p.shader = null; p.color = 0x142EC4FF
        for (i in 0..2) {
            val x = ((t + i / 3f) % 1f) * w * 1.6f - w * 0.3f
            band.reset()
            band.moveTo(x, 0f); band.lineTo(x + w * 0.05f, 0f)
            band.lineTo(x + w * 0.05f - h * 0.3f, h); band.lineTo(x - h * 0.3f, h); band.close()
            c.drawPath(band, p)
        }
        // moon + glow
        val cx = w * 0.78f; val cy = h * 0.30f; val r = h * 0.17f
        p.shader = glow; c.drawRect(0f, 0f, w, h, p); p.shader = null
        p.color = 0xFFD8F6FF.toInt(); c.drawCircle(cx, cy, r, p)
        p.color = 0x22041048; c.drawCircle(cx + r * 0.3f, cy - r * 0.15f, r * 0.8f, p)
        c.save(); c.rotate(t * 360f, cx, cy)
        ring.pathEffect = DashPathEffect(floatArrayOf(r * 0.25f, r * 0.15f), 0f)
        c.drawCircle(cx, cy, r * 1.3f, ring)
        c.restore()
    }
}
