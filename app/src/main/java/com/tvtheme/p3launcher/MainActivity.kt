package com.tvtheme.p3launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.*
import java.util.Date

class MainActivity : Activity() {
    private lateinit var sitesRow: LinearLayout
    private lateinit var appsRow: LinearLayout
    private lateinit var timeTv: TextView
    private lateinit var dateTv: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { updateClock(); handler.postDelayed(this, 15000) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(s: String, sp: Float, color: Int) = TextView(this).apply {
        text = s; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(color)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        letterSpacing = 0.06f
    }

    private fun rowHolder(): Pair<HorizontalScrollView, LinearLayout> {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; clipChildren = false
            setPadding(dp(8), dp(14), dp(8), dp(14))
        }
        val sv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; clipChildren = false; clipToPadding = false; addView(row)
        }
        return Pair(sv, row)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = FrameLayout(this)
        root.addView(BackgroundView(this))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; clipChildren = false
            setPadding(dp(56), dp(32), dp(40), dp(16))
        }
        timeTv = tv("", 54f, 0xFFFFFFFF.toInt())
        dateTv = tv("", 18f, ACCENT)
        col.addView(timeTv); col.addView(dateTv)
        col.addView(tv("SITES", 16f, ACCENT).apply { setPadding(0, dp(28), 0, 0) })
        val s = rowHolder(); sitesRow = s.second; col.addView(s.first)
        col.addView(tv("APPS", 16f, ACCENT).apply { setPadding(0, dp(10), 0, 0) })
        val a = rowHolder(); appsRow = a.second; col.addView(a.first)
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        buildSites(-1)
        buildApps()
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* home screen: stay put */ }

    private fun updateClock() {
        val now = Date()
        timeTv.text = DateFormat.format(if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm", now)
        dateTv.text = DateFormat.format("EEE   MM/dd", now).toString().uppercase()
    }

    private fun addTile(row: LinearLayout, t: Tile) {
        row.addView(t, LinearLayout.LayoutParams(dp(190), dp(135)).apply { setMargins(dp(10), dp(10), dp(14), dp(10)) })
    }

    private fun buildSites(focusIdx: Int) {
        sitesRow.removeAllViews()
        val add = Tile(this, "Add site", null, "+")
        add.setOnClickListener { addSite() }
        addTile(sitesRow, add)
        for (s in Sites.load(this)) {
            val t = Tile(this, s.name, null, s.name.take(1).uppercase())
            t.setOnClickListener { startActivity(Intent(this, WebActivity::class.java).putExtra("url", s.url)) }
            t.setOnLongClickListener { confirmRemove(s); true }
            addTile(sitesRow, t)
        }
        if (focusIdx >= 0) sitesRow.getChildAt(focusIdx)?.requestFocus()
    }

    private fun addSite() {
        val et = EditText(this).apply {
            hint = "example.com"; setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(this).setTitle("Add site (hold OK on a site to remove it)").setView(et)
            .setPositiveButton("Add") { _, _ ->
                val u = Sites.normalize(et.text.toString())
                if (u == null) {
                    Toast.makeText(this, "Not a valid address", Toast.LENGTH_SHORT).show()
                } else {
                    val l = Sites.load(this); l.add(Sites.Site(Sites.nameFor(u), u)); Sites.save(this, l)
                    buildSites(l.size)
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun confirmRemove(s: Sites.Site) {
        AlertDialog.Builder(this).setTitle("Remove ${s.name}?")
            .setPositiveButton("Remove") { _, _ ->
                val l = Sites.load(this); l.removeAll { it.url == s.url && it.name == s.name }
                Sites.save(this, l); buildSites(0)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun buildApps() {
        appsRow.removeAllViews()
        val pm = packageManager
        val seen = HashSet<String>()
        val found = ArrayList<ResolveInfo>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            for (ri in pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                if (ri.activityInfo.packageName != packageName && seen.add(ri.activityInfo.packageName)) found.add(ri)
            }
        }
        found.sortBy { it.loadLabel(pm).toString().lowercase() }
        val si = Intent(Settings.ACTION_SETTINGS)
        val st = Tile(this, "Settings", pm.resolveActivity(si, 0)?.loadIcon(pm), "S")
        st.setOnClickListener { startActivity(si) }
        addTile(appsRow, st)
        for (ri in found) {
            val pkg = ri.activityInfo.packageName
            val t = Tile(this, ri.loadLabel(pm).toString(), ri.loadIcon(pm), null)
            t.setOnClickListener {
                val i = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
                if (i != null) startActivity(i)
            }
            addTile(appsRow, t)
        }
    }
}
