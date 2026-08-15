package com.adegard.oledkeyboard

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class OledKeyboardService : InputMethodService() {

    companion object {
        const val PREFS = "oled_kb"
        const val KEY_MODE = "theme_mode"
        const val MODE_OLED = 0
        const val MODE_WHITE = 1
    }

    private val prefs: SharedPreferences by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var keyboard: KeyboardView? = null

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_MODE) {
            keyboard?.theme = KeyboardTheme.fromMode(prefs.getInt(KEY_MODE, MODE_OLED))
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        keyboard = view
        view.theme = KeyboardTheme.fromMode(prefs.getInt(KEY_MODE, MODE_OLED))
        view.enterLabel = enterLabel()
        view.onChar = { text -> commitText(text) }
        view.onSpace = { commitText(" ") }
        view.onBackspace = { deleteChar() }
        view.onBackspaceRepeat = { deleteChar() }
        view.onEnter = { performEnter() }
        window?.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        return view
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        window?.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        keyboard?.enterLabel = enterLabel()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    // ---- input helpers --------------------------------------------------

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun deleteChar() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1, 0)
        if (before.isNullOrEmpty()) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun performEnter() {
        val ic = currentInputConnection ?: return
        val ime = currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
        val action = ime and EditorInfo.IME_MASK_ACTION
        if (action != EditorInfo.IME_ACTION_UNSPECIFIED && action != EditorInfo.IME_ACTION_NONE) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun enterLabel(): String {
        val ime = currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
        return when (ime and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "Cerca"
            EditorInfo.IME_ACTION_SEND -> "Invia"
            EditorInfo.IME_ACTION_NEXT -> "Avanti"
            EditorInfo.IME_ACTION_DONE -> "OK"
            EditorInfo.IME_ACTION_GO -> "Vai"
            else -> "⏎"
        }
    }
}
