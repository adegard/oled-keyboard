package com.adegard.oledkeyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences(OledKeyboardService.PREFS, MODE_PRIVATE) }
    private var btnOled: Button? = null
    private var btnWhite: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        updateSelection()
    }

    private fun buildUi() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.BLACK)
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(24), dp(24), dp(24), dp(24))
        setContentView(root)

        val title = TextView(this)
        title.text = getString(R.string.app_name)
        title.setTextColor(Color.WHITE)
        title.textSize = 26f
        title.setTypeface(null, Typeface.BOLD)
        root.addView(title, params(-2, -2, 0, dp(20)))

        val subtitle = TextView(this)
        subtitle.text = getString(R.string.subtitle)
        subtitle.setTextColor(Color.rgb(150, 150, 150))
        subtitle.textSize = 14f
        subtitle.gravity = Gravity.CENTER
        root.addView(subtitle, params(-1, -2, 0, dp(10)))

        val hint = TextView(this)
        hint.text = getString(R.string.enable_hint)
        hint.setTextColor(Color.rgb(120, 120, 120))
        hint.textSize = 13f
        hint.gravity = Gravity.CENTER
        root.addView(hint, params(-1, -2, 0, dp(14)))

        val btnEnable = Button(this)
        btnEnable.text = getString(R.string.enable_btn)
        btnEnable.setTextColor(Color.BLACK)
        btnEnable.setTypeface(null, Typeface.BOLD)
        btnEnable.minHeight = dp(52)
        btnEnable.setBackgroundColor(Color.rgb(154, 154, 154))
        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        root.addView(btnEnable, params(-1, -2, 0, dp(22)))

        val themeTitle = TextView(this)
        themeTitle.text = getString(R.string.theme_title)
        themeTitle.setTextColor(Color.WHITE)
        themeTitle.textSize = 18f
        themeTitle.setTypeface(null, Typeface.BOLD)
        root.addView(themeTitle, params(-1, -2, 0, dp(30)))

        btnOled = themeButton(getString(R.string.theme_oled))
        btnWhite = themeButton(getString(R.string.theme_white))
        root.addView(btnOled, params(-1, -2, 0, dp(10)))
        root.addView(btnWhite, params(-1, -2, 0, dp(10)))

        val note = TextView(this)
        note.text = getString(R.string.note)
        note.setTextColor(Color.rgb(100, 100, 100))
        note.textSize = 12f
        note.gravity = Gravity.CENTER
        root.addView(note, params(-1, -2, 0, dp(24)))
    }

    private fun themeButton(label: String): Button {
        val b = Button(this)
        b.text = label
        b.minHeight = dp(54)
        b.setTextColor(Color.rgb(200, 200, 200))
        b.setTypeface(null, Typeface.BOLD)
        b.setOnClickListener {
            val mode = if (b === btnOled) OledKeyboardService.MODE_OLED else OledKeyboardService.MODE_WHITE
            prefs.edit().putInt(OledKeyboardService.KEY_MODE, mode).apply()
            updateSelection()
        }
        return b
    }

    private fun updateSelection() {
        val mode = prefs.getInt(OledKeyboardService.KEY_MODE, OledKeyboardService.MODE_OLED)
        applyThemeButton(btnOled!!, mode == OledKeyboardService.MODE_OLED)
        applyThemeButton(btnWhite!!, mode == OledKeyboardService.MODE_WHITE)
    }

    private fun applyThemeButton(b: Button, selected: Boolean) {
        val bg = GradientDrawable()
        bg.cornerRadius = dp(12).toFloat()
        if (selected) {
            bg.setColor(Color.rgb(154, 154, 154))
            bg.setStroke(0, Color.TRANSPARENT)
            b.setTextColor(Color.BLACK)
        } else {
            bg.setColor(Color.rgb(15, 15, 15))
            bg.setStroke(1, Color.rgb(80, 80, 80))
            b.setTextColor(Color.rgb(200, 200, 200))
        }
        b.background = bg
    }

    private fun params(w: Int, h: Int, top: Int, bottom: Int): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(w, h)
        lp.topMargin = top
        lp.bottomMargin = bottom
        return lp
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
