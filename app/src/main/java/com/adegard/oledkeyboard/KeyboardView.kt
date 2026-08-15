package com.adegard.oledkeyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Renders the whole keyboard with a single [Canvas] pass and handles all
 * touch input (tap, long press popups with accented characters, backspace
 * auto-repeat, shift cycle).
 */
class KeyboardView(context: Context) : View(context) {

    var onChar: ((String) -> Unit)? = null
    var onSpace: (() -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onBackspaceRepeat: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null

    var theme: KeyboardTheme = KeyboardTheme.OLED
        set(value) {
            field = value
            invalidate()
        }

    var enterLabel: String = "⏎"
        set(value) {
            field = value
            invalidate()
        }

    private var rows: List<List<Key>> = emptyList()
    private var keyRects: List<Pair<Key, RectF>> = emptyList()
    private var symbolMode = false
    private var shiftOn = false
    private var capsLock = false

    private var viewW = 0
    private var viewH = 0
    private var bottomInset = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var longPressTask: Runnable? = null
    private var repeatTask: Runnable? = null
    private var longPressConsumed = false
    private var pressedKey: Key? = null
    private var popup: PopupWindow? = null

    init {
        rebuild()
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private fun rebuild() {
        val toggleLabel = if (symbolMode) "ABC" else "?123"
        rows = if (symbolMode) {
            KeyLayouts.symbols(toggleLabel)
        } else {
            KeyLayouts.letters(toggleLabel)
        }
        rows.forEach { row ->
            row.forEach { key ->
                when (key.type) {
                    KeyType.ENTER -> key.label = enterLabel
                    KeyType.SHIFT -> key.label = if (capsLock) "⇪" else "⇧"
                    KeyType.CHAR -> {
                        if (key.code != null && key.code!!.length == 1) {
                            key.label = if (shiftOn) key.code!!.uppercase() else key.code
                        }
                    }
                    else -> Unit
                }
            }
        }
        computeRects()
        invalidate()
    }

    private fun computeRects() {
        if (viewW <= 0 || viewH <= 0) return
        val usableH = viewH - bottomInset
        val margin = dp(4f)
        val gap = dp(3f)
        val topPad = dp(2f)
        val rowCount = rows.size
        val totalGap = gap * (rowCount - 1)
        val rowH = (usableH - topPad - totalGap) / rowCount

        keyRects = ArrayList()
        rows.forEachIndexed { r, row ->
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            var x = margin
            val y = topPad + r * (rowH + gap)
            row.forEach { key ->
                val kw = (viewW - margin * 2) * key.weight / totalWeight - gap
                keyRects += key to RectF(x, y, x + kw, y + rowH)
                x += kw + gap
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val keyH = dp(54f)
        val gap = dp(3f)
        val topPad = dp(2f)
        val rowCount = 4
        val height = (keyH * rowCount + gap * (rowCount - 1) + topPad + bottomInset).toInt()
        setMeasuredDimension(w, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewW = w
        viewH = h
        computeRects()
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        bottomInset = if (Build.VERSION.SDK_INT >= 30) {
            insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        } else {
            @Suppress("DEPRECATION")
            insets.systemWindowInsetBottom
        }
        requestLayout()
        return super.onApplyWindowInsets(insets)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(theme.bg)
        keyRects.forEach { (key, rect) ->
            val isSpecial = key.type != KeyType.CHAR && key.type != KeyType.SPACE
            val fill = when {
                pressedKey?.id == key.id -> theme.pressedFill
                key.type == KeyType.SHIFT && shiftOn -> theme.shiftActiveFill
                isSpecial -> theme.specialFill
                else -> theme.keyFill
            }
            paint.style = Paint.Style.FILL
            paint.color = fill
            canvas.drawRoundRect(rect, dp(4f), dp(4f), paint)

            paint.color = if (isSpecial) theme.specialLabel else theme.label
            paint.typeface = if (key.type == KeyType.CHAR) Typeface.DEFAULT else Typeface.DEFAULT_BOLD
            paint.textSize = if (key.type == KeyType.CHAR) dp(21f) else dp(15f)
            paint.textAlign = Paint.Align.CENTER
            val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2
            canvas.drawText(key.label, rect.centerX(), baseline, paint)
        }
    }

    // ---- input handling -------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val key = keyAt(event.x, event.y) ?: return false
                pressedKey = key
                longPressConsumed = false
                startLongPress(key)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val key = pressedKey ?: return true
                val rect = rectOf(key) ?: return true
                val slop = dp(10f)
                if (event.x < rect.left - slop || event.x > rect.right + slop ||
                    event.y < rect.top - slop || event.y > rect.bottom + slop
                ) {
                    cancelTimers()
                    if (pressedKey != null) {
                        pressedKey = null
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                cancelTimers()
                val key = pressedKey
                pressedKey = null
                invalidate()
                if (key != null && !longPressConsumed) tap(key)
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelTimers()
                pressedKey = null
                invalidate()
            }
        }
        return true
    }

    private fun keyAt(x: Float, y: Float): Key? {
        keyRects.forEach { (key, rect) ->
            if (rect.contains(x, y)) return key
        }
        return null
    }

    private fun rectOf(key: Key): RectF? =
        keyRects.firstOrNull { it.first.id == key.id }?.second

    private fun tap(key: Key) {
        when (key.type) {
            KeyType.CHAR -> {
                val code = key.code ?: return
                val out = if (shiftOn) code.uppercase() else code
                onChar?.invoke(out)
                endShiftIfNeeded()
            }

            KeyType.SPACE -> {
                onSpace?.invoke()
                endShiftIfNeeded()
            }

            KeyType.BACKSPACE -> onBackspace?.invoke()

            KeyType.ENTER -> onEnter?.invoke()

            KeyType.TOGGLE_LAYOUT -> {
                symbolMode = !symbolMode
                rebuild()
            }

            KeyType.SHIFT -> {
                when {
                    !shiftOn -> shiftOn = true
                    shiftOn && !capsLock -> {
                        capsLock = true
                        shiftOn = true
                    }
                    else -> {
                        shiftOn = false
                        capsLock = false
                    }
                }
                rebuild()
            }
        }
    }

    private fun endShiftIfNeeded() {
        if (shiftOn && !capsLock) {
            shiftOn = false
            rebuild()
        }
    }

    // ---- long press -----------------------------------------------------

    private fun startLongPress(key: Key) {
        longPressTask = Runnable {
            when (key.type) {
                KeyType.BACKSPACE -> {
                    longPressConsumed = true
                    startRepeat()
                }

                KeyType.SHIFT -> {
                    longPressConsumed = true
                    capsLock = true
                    shiftOn = true
                    rebuild()
                }

                KeyType.CHAR -> {
                    if (key.longPress.isNotEmpty()) {
                        longPressConsumed = true
                        showPopup(key)
                    }
                }

                else -> longPressConsumed = false
            }
        }
        handler.postDelayed(longPressTask!!, 450L)
    }

    private fun cancelTimers() {
        longPressTask?.let(handler::removeCallbacks)
        longPressTask = null
        repeatTask?.let(handler::removeCallbacks)
        repeatTask = null
    }

    private fun startRepeat() {
        repeatTask = Runnable {
            onBackspaceRepeat?.invoke()
            handler.postDelayed(repeatTask!!, 55L)
        }
        handler.postDelayed(repeatTask!!, 90L)
    }

    // ---- popup ----------------------------------------------------------

    private fun showPopup(key: Key) {
        popup?.dismiss()
        val options = key.longPress
        val container = LinearLayout(context)
        container.orientation = LinearLayout.HORIZONTAL
        container.gravity = Gravity.CENTER_VERTICAL

        val bg = GradientDrawable()
        bg.setColor(theme.popupBg)
        bg.cornerRadius = dp(8f)
        bg.setStroke(dp(1f).toInt(), theme.popupBorder)
        container.background = bg

        options.forEach { option ->
            val tv = TextView(context)
            tv.text = option
            tv.setTextColor(theme.label)
            tv.textSize = 22f
            tv.gravity = Gravity.CENTER
            tv.setPadding(dp(10f).toInt(), dp(6f).toInt(), dp(10f).toInt(), dp(6f).toInt())
            tv.setOnClickListener {
                popup?.dismiss()
                onChar?.invoke(option)
            }
            container.addView(tv)
        }

        val wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        container.measure(wSpec, hSpec)

        val pw = PopupWindow(container, container.measuredWidth, container.measuredHeight, true)
        pw.isOutsideTouchable = true
        popup = pw

        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val rect = rectOf(key) ?: return
        val x = (loc[0] + rect.centerX() - container.measuredWidth / 2f).toInt()
        val y = (loc[1] + rect.top - container.measuredHeight - dp(6f)).toInt()
        pw.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
    }
}
