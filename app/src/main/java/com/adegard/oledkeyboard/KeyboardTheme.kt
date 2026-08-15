package com.adegard.oledkeyboard

import android.graphics.Color

/**
 * Color schemes. Both keep the background pure black (OLED off pixels)
 * and only change the key fill / label colors.
 */
enum class KeyboardTheme(
    val bg: Int,
    val keyFill: Int,
    val specialFill: Int,
    val label: Int,
    val specialLabel: Int,
    val pressedFill: Int,
    val shiftActiveFill: Int,
    val popupBg: Int,
    val popupBorder: Int
) {
    /** Dim, OLED friendly: light gray on total black. */
    OLED(
        bg = Color.BLACK,
        keyFill = Color.rgb(16, 16, 16),
        specialFill = Color.rgb(10, 10, 10),
        label = Color.rgb(154, 154, 154),
        specialLabel = Color.rgb(120, 120, 120),
        pressedFill = Color.rgb(42, 42, 42),
        shiftActiveFill = Color.rgb(60, 60, 60),
        popupBg = Color.rgb(18, 18, 18),
        popupBorder = Color.rgb(70, 70, 70)
    ),

    /** High contrast: white on black. */
    WHITE_ON_BLACK(
        bg = Color.BLACK,
        keyFill = Color.rgb(28, 28, 28),
        specialFill = Color.rgb(15, 15, 15),
        label = Color.WHITE,
        specialLabel = Color.rgb(200, 200, 200),
        pressedFill = Color.rgb(72, 72, 72),
        shiftActiveFill = Color.rgb(92, 92, 92),
        popupBg = Color.rgb(30, 30, 30),
        popupBorder = Color.rgb(130, 130, 130)
    );

    companion object {
        fun fromMode(mode: Int): KeyboardTheme =
            if (mode == OledKeyboardService.MODE_WHITE) WHITE_ON_BLACK else OLED
    }
}
