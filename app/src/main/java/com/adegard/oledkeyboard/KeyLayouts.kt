package com.adegard.oledkeyboard

enum class KeyType { CHAR, SHIFT, BACKSPACE, SPACE, ENTER, TOGGLE_LAYOUT }

/**
 * A single key. [code] is the committed text for CHAR keys (lowercase form).
 * [longPress] holds the accented alternatives shown on long press.
 */
data class Key(
    val type: KeyType,
    val id: String,
    var label: String,
    val code: String? = null,
    val weight: Float = 1f,
    val longPress: List<String> = emptyList()
)

/** Italian QWERTY letter rows and their accented long-press alternatives. */
object ItalianLayout {

    val letters = listOf(
        "QWERTYUIOP",
        "ASDFGHJKL",
        "ZXCVBNM"
    )

    val accents = mapOf(
        "a" to listOf("à", "â", "ä"),
        "e" to listOf("è", "é", "ê", "ë"),
        "i" to listOf("ì", "î", "ï"),
        "o" to listOf("ò", "ó", "ô", "ö"),
        "u" to listOf("ù", "ú", "û", "ü"),
        "c" to listOf("ç", "ć"),
        "n" to listOf("ñ")
    )
}

object KeyLayouts {

    private val shiftKey = Key(KeyType.SHIFT, "shift", "⇧", weight = 1.5f)
    private val backspaceKey = Key(KeyType.BACKSPACE, "backspace", "⌫", weight = 1.5f)
    private val commaKey = Key(KeyType.CHAR, "comma", ",", code = ",", longPress = listOf(","))
    private val dotKey = Key(KeyType.CHAR, "dot", ".", code = ".", longPress = listOf(".", "…"))

    fun letters(symbolLabel: String): List<List<Key>> {
        val rows = ArrayList<List<Key>>()
        ItalianLayout.letters.forEachIndexed { index, rowStr ->
            if (index == 2) {
                rows.add(
                    listOf(shiftKey) +
                        rowStr.map(::letterKey) +
                        listOf(backspaceKey)
                )
            } else {
                rows.add(rowStr.map(::letterKey))
            }
        }
        rows.add(
            listOf(
                Key(KeyType.TOGGLE_LAYOUT, "toggle", symbolLabel, weight = 1.3f),
                commaKey,
                Key(KeyType.SPACE, "space", "", weight = 5f),
                dotKey,
                Key(KeyType.ENTER, "enter", "⏎", weight = 1.5f)
            )
        )
        return rows
    }

    private fun letterKey(ch: Char): Key {
        val lower = ch.lowercaseChar().toString()
        return Key(
            type = KeyType.CHAR,
            id = "char_$lower",
            label = lower,
            code = lower,
            longPress = ItalianLayout.accents[lower] ?: emptyList()
        )
    }

    fun symbols(symbolLabel: String): List<List<Key>> {
        val digits = "1234567890".map { ch ->
            Key(KeyType.CHAR, "digit_$ch", ch.toString(), code = ch.toString())
        }
        val symbols = "@#\$%&*-+()".map { ch ->
            Key(KeyType.CHAR, "sym_$ch", ch.toString(), code = ch.toString())
        }
        val extra = listOf(";", ":", "\"", "'", "!", "?", "€", "/", "\\").map { ch ->
            Key(KeyType.CHAR, "sym_$ch", ch, code = ch)
        }
        return listOf(
            digits,
            symbols,
            listOf(Key(KeyType.TOGGLE_LAYOUT, "toggle", symbolLabel, weight = 1.4f)) +
                extra +
                listOf(backspaceKey),
            listOf(
                Key(KeyType.TOGGLE_LAYOUT, "toggle", symbolLabel, weight = 1.3f),
                commaKey,
                Key(KeyType.SPACE, "space", "", weight = 5f),
                dotKey,
                Key(KeyType.ENTER, "enter", "⏎", weight = 1.5f)
            )
        )
    }
}
