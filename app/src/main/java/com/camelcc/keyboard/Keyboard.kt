package com.camelcc.keyboard

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import kotlin.math.max
import kotlin.math.min

abstract class Keyboard(private val context: Context) {
    companion object {
        const val NORMAL = 1
        const val UPPER = 2
        const val STICKY_UPPER = 3
        const val PUNCTUATION = 4
        const val SYMBOL = 5
    }

    var width = 0
    var height = 0
    var keyHeight = 0
    var keyboardListener: KeyboardListener? = null

    var layout: QWERTYLayout
    var mode = NORMAL
    var keys = listOf<Key>()

    init {
        val dm = context.resources.displayMetrics
        val dw = min(dm.widthPixels, dm.heightPixels)
        val dh = max(dm.widthPixels, dm.heightPixels)
        width = dm.widthPixels
        height = 0

        layout = QWERTYLayout()
        layout.displayWidth = dw
        layout.displayHeight = dh
    }

    fun buildLayout() {
        buildLayoutForMode()
        layout.layout(width, height)
        height = layout.height
        keyHeight = layout.keyHeight
        keyboardListener?.onLayoutChanged()
    }

    abstract fun buildLayoutForMode()
    abstract fun onDoubleClick(key: Key): Boolean
    abstract fun onClick(key: Key)

    fun resize(w: Int, h: Int) {
        Log.i("[SK]", "[Keyboard] resize w: $w, h: $h")
        layout.layout(w, h)
        width = layout.width
        height = layout.height
    }

    fun getKey(x: Int, y: Int): Key? {
        val nearKeys = layout.getNearestKeys(x, y)
        var closestDist = Int.MAX_VALUE
        var closestKey:Key? = null
        for (key in nearKeys) {
            if (key.isInside(x, y)) {
                return key
            }
            val dis = key.squareDistanceFrom(x, y)
            if (dis < closestDist) {
                closestDist = dis
                closestKey = key
            }
        }
        return closestKey
    }
}

class EnglishKeyboard(private val context: Context): Keyboard(context) {
    fun showUpper() {
        mode = UPPER
        buildLayout()
    }

    fun showStickyUpper() {
        mode = STICKY_UPPER
        buildLayout()
    }

    override fun buildLayoutForMode() {
        when (mode) {
            NORMAL -> buildQWERTY()
            UPPER -> buildQWERTY(true)
            STICKY_UPPER -> buildQWERTY(upperCase = true, stickShift = true)
            PUNCTUATION -> buildPunctuation()
            SYMBOL -> buildSymbol()
        }
    }

    private fun buildQWERTY(upperCase: Boolean = false, stickShift: Boolean = false) {
        val q = PreviewTextKey(if (upperCase) 'Q' else 'q', '1', listOf('1'))
        val w = PreviewTextKey(if (upperCase) 'W' else 'w', '2', listOf('2'))
        val e = PreviewTextKey(if (upperCase) 'E' else 'e', '3', if (upperCase)
            listOf('\u0112', '\u00CA', '\u00CB', '\u00C8', '3', '\u00C9') else
            listOf('\u0113', '\u00EA', '\u00EB', '\u00E8', '3', '\u00E9'), 4)
        val r = PreviewTextKey(if (upperCase) 'R' else 'r', '4', listOf('4'))
        val t = PreviewTextKey(if (upperCase) 'T' else 't', '5', listOf('5'))
        val y = PreviewTextKey(if (upperCase) 'Y' else 'y', '6', listOf('6'))
        val u = PreviewTextKey(if (upperCase) 'U' else 'u', '7', if (upperCase)
            listOf('\u016A', '\u00DC', '\u00D9', '\u00DB', '7', '\u00DA') else
            listOf('\u016B', '\u00FC', '\u00F9', '\u00FB', '7', '\u00FA'), 4)
        val i = PreviewTextKey(if (upperCase) 'I' else 'i', '8', if (upperCase)
            listOf('\u00CC', '\u00CF', '\u012A', '\u00CE', '8', '\u00CD') else
            listOf('\u00EC', '\u00EF', '\u012B', '\u00EE', '8', '\u00ED'), 4)
        val o = PreviewTextKey(if (upperCase) 'O' else 'o', '9', if (upperCase)
            listOf('\u0000', '\u00D5', '\u014C', '\u0152', '\u00D8', '\u00D2', '\u00D6', '\u00D4', '9', '\u00D3') else
            listOf('\u0000', '\u00F5', '\u014D', '\u0153', '\u00F8', '\u00F2', '\u00F6', '\u00F4', '9', '\u00F3'), 8)
        val p = PreviewTextKey(if (upperCase) 'P' else 'p', '0', listOf('0'))
        val a = PreviewTextKey(if (upperCase) 'A' else 'a', '\u0000', if (upperCase)
            listOf('\u00C6', '\u00C3', '\u00C5', '\u0100', '\u00C0', '\u00C1', '\u00C2', '\u00C4') else
            listOf('\u00E6', '\u00E3', '\u00E5', '\u0101', '\u00E0', '\u00E1', '\u00E2', '\u00E4'), 4)
        val s = PreviewTextKey(if (upperCase) 'S' else 's', '\u0000', if (upperCase) listOf('\u1E9E') else listOf('\u00DF'))
        val d = PreviewTextKey(if (upperCase) 'D' else 'd')
        val f = PreviewTextKey(if (upperCase) 'F' else 'f')
        val g = PreviewTextKey(if (upperCase) 'G' else 'g')
        val h = PreviewTextKey(if (upperCase) 'H' else 'h')
        val j = PreviewTextKey(if (upperCase) 'J' else 'j')
        val k = PreviewTextKey(if (upperCase) 'K' else 'k')
        val l = PreviewTextKey(if (upperCase) 'L' else 'l')
        val shift = ShiftKey(context.getDrawable(if (!upperCase) R.drawable.ic_up_24dp else {if (stickShift) R.drawable.ic_upsticky_24dp else R.drawable.ic_upper_24dp })!!)
        shift.keyColor = KeyboardTheme.keyControlBackground
        shift.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val z = PreviewTextKey(if (upperCase) 'Z' else 'z')
        val x = PreviewTextKey(if (upperCase) 'X' else 'x')
        val c = PreviewTextKey(if (upperCase) 'C' else 'c', '\u0000', if (upperCase) listOf('\u00C7') else listOf('\u00E7'))
        val v = PreviewTextKey(if (upperCase) 'V' else 'v')
        val b = PreviewTextKey(if (upperCase) 'B' else 'b')
        val n = PreviewTextKey(if (upperCase) 'N' else 'n', '\u0000', if (upperCase) listOf('\u00D1') else listOf('\u00F1'))
        val m = PreviewTextKey(if (upperCase) 'M' else 'm')
        val delete = DeleteKey(context.getDrawable(R.drawable.ic_delete_24dp)!!)
        delete.repeatable = true
        delete.keyColor = KeyboardTheme.keyControlBackground
        delete.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val number = NumberKey("?123")
        number.keyColor = KeyboardTheme.keyControlBackground
        number.textSize = 18.dp2px.toFloat()
        number.bold = true
        number.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val emoji = PreviewTextKey(',')
        emoji.keyColor = KeyboardTheme.keyControlBackground
        emoji.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        val lang = LangKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = SpaceKey("English")
        space.textSize = KeyboardTheme.keySpaceTextSize.toFloat()
        space.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val period = PreviewTextKey('.')
        period.keyColor = KeyboardTheme.keyControlBackground
        period.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        period.miniKeys = listOf('&', '%', '+', '\'', '-', ':', '\'', '@', ';', '/', '(', ')', '#', '!', ',', '?')
        period.initMiniKeyIndex = 14
        val enter = DoneKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = KeyboardTheme.keyEnterBackground
        enter.keyPressedColor = KeyboardTheme.keyEnterPressedBackground

        layout.rows = arrayOf(
            arrayOf<Key>(q, w, e, r, t, y ,u, i, o, p),
            arrayOf<Key>(a, s, d, f, g, h, j, k, l),
            arrayOf<Key>(shift, z, x, c, v, b, n, m, delete),
            arrayOf<Key>(number, emoji, lang, space, period, enter)
        )

        keys = listOf(
            q, w, e, r, t, y ,u, i, o, p,
            a, s, d, f, g, h, j, k, l,
            shift, z, x, c, v, b, n, m, delete,
            number, emoji, lang, space, period, enter)
    }

    private fun buildPunctuation() {
        val p1 = PreviewTextKey('1', '\u0000',
            listOf('\u2159', '\u2150', '\u215B', '\u2151', '\u2152', '\u00B9', '\u00BD', '\u2153', '\u00BC', '\u2155'), 5)
        val p2 = PreviewTextKey('2', '\u0000',
            listOf('\u2156', '\u00B2', '\u2154'), 1)
        val p3 = PreviewTextKey('3', '\u0000',
            listOf('\u2157', '\u00B3', '\u00BE', '\u215C'), 1)
        val p4 = PreviewTextKey('4', '\u0000',
            listOf('\u2074', '\u2158'), 0)
        val p5 = PreviewTextKey('5', '\u0000',
            listOf('\u215D', '\u2075', '\u215A'), 1)
        val p6 = PreviewTextKey('6', '\u0000',
            listOf('\u2076'), 0)
        val p7 = PreviewTextKey('7', '\u0000',
            listOf('\u2077', '\u215E'), 0)
        val p8 = PreviewTextKey('8', '\u0000', listOf('\u2078'), 0)
        val p9 = PreviewTextKey('9', '\u0000', listOf('\u2079'), 0)
        val p0 = PreviewTextKey('0', '\u0000', listOf('\u2205', '\u207F', '\u2070'), 2)
        val at = PreviewTextKey('@')
        val sharp = PreviewTextKey('#', '\u0000', listOf('\u2116'))
        val dollar = PreviewTextKey('$', '\u0000', listOf('\u20B1', '\u20AC', '\u00A2', '\u00A3', '\u00A5'), 2)
        val underscore = PreviewTextKey('_')
        val and = PreviewTextKey('&')
        val minus = PreviewTextKey('-', '\u0000', listOf('\u2014', '\u005F', '\u2013', '\u00B7'), 1)
        val plus = PreviewTextKey('+', '\u0000', listOf('\u00B1'))
        val leftParenthesis = PreviewTextKey('(', '\u0000', listOf('[', '<', '{'), 1)
        val rightParenthesis = PreviewTextKey(')', '\u0000', listOf(']', '>', '}'), 1)
        val slash = PreviewTextKey('/')

        val symbol = SymbolKey("=\\<")
        symbol.keyColor = KeyboardTheme.keyControlBackground
        symbol.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        symbol.textSize = 18.dp2px.toFloat()
        symbol.bold = true
        val asterisk = PreviewTextKey('*', '\u0000', listOf('\u2605', '\u2020', '\u2021'), 1)
        val quote = PreviewTextKey('"', '\u0000', listOf('\u201E', '\u201C', '\u201D', '\u00AB', '\u00BB'), 2)
        val singleQuote = PreviewTextKey('\'', '\u0000', listOf('\u201A', '\u2018', '\u2019', '\u2039', '\u203A'), 2)
        val colon = PreviewTextKey(':')
        val semicolon = PreviewTextKey(';')
        val exclamation = PreviewTextKey('!', '\u0000', listOf('\u00A1'))
        val question = PreviewTextKey('?', '\u0000', listOf('\u00BF', '\u203D'))
        val delete = DeleteKey(context.getDrawable(R.drawable.ic_delete_24dp)!!)
        delete.repeatable = true
        delete.keyColor = KeyboardTheme.keyControlBackground
        delete.keyPressedColor = KeyboardTheme.keyControlPressedBackground

        val char = QWERTYKey("ABC")
        char.keyColor = KeyboardTheme.keyControlBackground
        char.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        char.textSize = 18.dp2px.toFloat()
        char.bold = true
        val comma = PreviewTextKey(',')
        comma.keyColor = KeyboardTheme.keyControlBackground
        comma.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        val lang = LangKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = SpaceKey("English")
        space.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        space.textSize = KeyboardTheme.keySpaceTextSize.toFloat()
        val period = PreviewTextKey('.')
        period.keyColor = KeyboardTheme.keyControlBackground
        period.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        period.miniKeys = listOf('\u2026')
        val enter = DoneKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = KeyboardTheme.keyEnterBackground
        enter.keyPressedColor = KeyboardTheme.keyEnterPressedBackground

        layout.rows = arrayOf(
            arrayOf<Key>(p1, p2, p3, p4, p5, p6, p7, p8, p9, p0),
            arrayOf<Key>(at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash),
            arrayOf<Key>(symbol, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete),
            arrayOf<Key>(char, comma, lang, space, period, enter)
        )

        keys = listOf(
            p1, p2, p3, p4, p5, p6, p7, p8, p9, p0,
            at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash,
            symbol, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete,
            char, comma, lang, space, period, enter
        )
    }

    private fun buildSymbol() {
        val p1 = PreviewTextKey('\u007E')
        val p2 = PreviewTextKey('\u0060')
        val p3 = PreviewTextKey('\u007C')
        val p4 = PreviewTextKey('\u2022', '\u0000', listOf('\u2663', '\u2660', '\u266A', '\u2665', '\u2666'), 2)
        val p5 = PreviewTextKey('\u221A')
        val p6 = PreviewTextKey('\u03C0', '\u0000', listOf('\u03A9', '\u03A0', '\u03BC'), 1)
        val p7 = PreviewTextKey('\u00F7')
        val p8 = PreviewTextKey('\u00D7')
        val p9 = PreviewTextKey('\u00B6', '\u0000', listOf('\u00A7'))
        val p0 = PreviewTextKey('\u2206')

        val at = PreviewTextKey('\u00A3')
        val sharp = PreviewTextKey('\u00A2')
        val dollar = PreviewTextKey('\u20AC')
        val underscore = PreviewTextKey('\u00A5')
        val and = PreviewTextKey('\u005E', '\u0000', listOf('\u2190', '\u2191', '\u2193', '\u2192'), 1)
        val minus = PreviewTextKey('\u00B0', '\u0000', listOf('\u2032', '\u2033'))
        val plus = PreviewTextKey('\u003D', '\u0000', listOf('\u221E', '\u2260', '\u2248'), 1)
        val leftParenthesis = PreviewTextKey('\u007B', '\u0000', listOf('\u0028'))
        val rightParenthesis = PreviewTextKey('\u007D', '\u0000', listOf('\u0029'))
        val slash = PreviewTextKey('\u005C')

        val punctuation = PunctuationKey("?123")
        punctuation.keyColor = KeyboardTheme.keyControlBackground
        punctuation.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        punctuation.textSize = 18.dp2px.toFloat()
        punctuation.bold = true
        val asterisk = PreviewTextKey('\u0025', '\u0000', listOf('\u2030', '\u2105'))
        val quote = PreviewTextKey('\u00A9')
        val singleQuote = PreviewTextKey('\u00AE')
        val colon = PreviewTextKey('\u2122')
        val semicolon = PreviewTextKey('\u2713')
        val exclamation = PreviewTextKey('\u005B')
        val question = PreviewTextKey('\u005D')
        val delete = DeleteKey(context.getDrawable(R.drawable.ic_delete_24dp)!!)
        delete.repeatable = true
        delete.keyColor = KeyboardTheme.keyControlBackground
        delete.keyPressedColor = KeyboardTheme.keyControlPressedBackground

        val char = QWERTYKey("ABC")
        char.keyColor = KeyboardTheme.keyControlBackground
        char.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        char.textSize = 18.dp2px.toFloat()
        char.bold = true
        val comma = PreviewTextKey('\u003C')
        comma.keyColor = KeyboardTheme.keyControlBackground
        comma.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        comma.miniKeys = listOf('\u00AB', '\u2264', '\u2039', '\u27E8')
        comma.initMiniKeyIndex = 1
        val lang = LangKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = SpaceKey("English")
        space.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        space.textSize = KeyboardTheme.keySpaceTextSize.toFloat()
        val period = PreviewTextKey('\u003E')
        period.keyColor = KeyboardTheme.keyControlBackground
        period.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        period.miniKeys = listOf('\u27E9', '\u00BB', '\u2265', '\u203A')
        period.initMiniKeyIndex = 2
        val enter = DoneKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = KeyboardTheme.keyEnterBackground
        enter.keyPressedColor = KeyboardTheme.keyEnterPressedBackground

        layout.rows = arrayOf(
            arrayOf<Key>(p1, p2, p3, p4, p5, p6, p7, p8, p9, p0),
            arrayOf<Key>(at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash),
            arrayOf<Key>(punctuation, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete),
            arrayOf<Key>(char, comma, lang, space, period, enter)
        )

        keys = listOf(
            p1, p2, p3, p4, p5, p6, p7, p8, p9, p0,
            at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash,
            punctuation, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete,
            char, comma, lang, space, period, enter
        )
    }

    override fun onClick(key: Key) {
        Log.i("[SK]", "[Keyboard] click: $key")

        var updated = false
        when (key) {
            is ShiftKey -> {
                if (mode == NORMAL) {
                    mode = UPPER
                    updated = true
                } else if (mode == UPPER || mode == STICKY_UPPER) {
                    mode = NORMAL
                    updated = true
                }
            }
            is NumberKey -> {
                if (mode == NORMAL || mode == UPPER || mode == STICKY_UPPER) {
                    mode = PUNCTUATION
                    updated = true
                }
            }
            is QWERTYKey -> {
                if (mode == PUNCTUATION || mode == SYMBOL) {
                    mode = NORMAL
                    updated = true
                }
            }
            is SymbolKey -> {
                if (mode == PUNCTUATION) {
                    mode = SYMBOL
                    updated = true
                }
            }
            is PunctuationKey -> {
                if (mode == SYMBOL) {
                    mode = PUNCTUATION
                    updated = true
                }
            }
            is DeleteKey -> {
                Log.i("[SK]", "[Keyboard] delete")
                keyboardListener?.onKey(KeyEvent.KEYCODE_DEL)
            }
            is SpaceKey -> {
                Log.i("[SK]", "[Keyboard] space")
                keyboardListener?.onKey(KeyEvent.KEYCODE_SPACE)
            }
            is DoneKey -> {
                Log.i("[SK]", "[Keyboard] done")
                keyboardListener?.onKey(KeyEvent.KEYCODE_ENTER)
            }
            is LangKey -> {
                Log.i("[SK]", "[Keyboard] lang switch")
                keyboardListener?.onSwitchLang()
            }
            else -> {
                if (mode == UPPER) {
                    mode = NORMAL
                    updated = true
                }

                if (key is TextKey) {
                    keyboardListener?.onChar(key.keyCode)
                }
            }
        }
        if (updated) {
            buildLayout()
        }
    }

    override fun onDoubleClick(key: Key): Boolean {
        return when (key) {
            is ShiftKey -> {
                if (mode == UPPER) {
                    mode = STICKY_UPPER
                    buildLayout()
                }
                true
            }
            else -> {
                false
            }
        }
    }
}

class PinyinKeyboard(private val context: Context): Keyboard(context) {
    override fun buildLayoutForMode() {
        when (mode) {
            NORMAL -> buildQWERTY()
            PUNCTUATION -> buildPunctuation()
            SYMBOL -> buildSymbol()
        }
    }

    private fun buildQWERTY() {
        val q = PreviewTextKey('q', '1', listOf('1', 'q', 'Q'), 0)
        val w = PreviewTextKey('w', '2', listOf('W', '2', 'w'), 1)
        val e = PreviewTextKey('e', '3', listOf(
            '\u011b', '\u0113', '\u00E9', '\u00E8', 'E', '3', 'e', '\u00EA'), 5)
        val r = PreviewTextKey('r', '4', listOf('R', '4', 'r'), 1)
        val t = PreviewTextKey('t', '5', listOf('T', '5', 't'), 1)
        val y = PreviewTextKey('y', '6', listOf('Y', '6', 'y'), 1)
        val u = PreviewTextKey('u', '7', listOf(
            '\u00F9', '\u00FA', '\u01D4', '\u0000', 'U', '7', 'u', '\u016B'), 5)
        val i = PreviewTextKey('i', '8', listOf(
            '\u00EC', '\u00ED', '\u01D0', '\u0000', 'I', '8', 'i', '\u012B'), 5)
        val o = PreviewTextKey('o', '9', listOf(
            '\u0000', '\u00F2', '\u00F3', '\u01D2', '\u014D', 'O', '9', 'o'), 6)
        val p = PreviewTextKey('p', '0', listOf('P', 'p', '0'), 2)
        val a = PreviewTextKey('a', '\u0000', listOf(
            '\u00E1', '\u01CE', '\u00E0', 'a', 'A', '\u0101'), 3)
        val s = PreviewTextKey('s', '\u0000', listOf('s', 'S'), 0)
        val d = PreviewTextKey('d', '\u0000', listOf('d', 'D'), 0)
        val f = PreviewTextKey('f', '\u0000', listOf('f', 'F'), 0)
        val g = PreviewTextKey('g', '\u0000', listOf('u', 'U'), 0)
        val h = PreviewTextKey('h', '\u0000', listOf('h', 'H'), 0)
        val j = PreviewTextKey('j', '\u0000', listOf('j', 'J'), 0)
        val k = PreviewTextKey('k', '\u0000', listOf('k', 'K'), 0)
        val l = PreviewTextKey('l', '\u0000', listOf('L', 'l'), 0)
        val hanci = HanCiKey("'词")
        hanci.keyColor = KeyboardTheme.keyControlBackground
        hanci.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val z = PreviewTextKey('z', '\u0000', listOf('z', 'Z'), 0)
        val x = PreviewTextKey('x', '\u0000', listOf('x', 'X'), 0)
        val c = PreviewTextKey('c', '\u0000', listOf('c', 'C'), 0)
        val v = PreviewTextKey('v', '\u0000', listOf(
            '\u01DC', '\u01D8', '\u01DA', '\u0000', '\u00FC', 'v', 'V', '\u016D'), 5)
        val b = PreviewTextKey('b', '\u0000', listOf('b', 'B'), 0)
        val n = PreviewTextKey('n', '\u0000', listOf('n', 'N'), 0)
        val m = PreviewTextKey('m', '\u0000', listOf('m', 'M'), 0)
        val delete = DeleteKey(context.getDrawable(R.drawable.ic_delete_24dp)!!)
        delete.repeatable = true
        delete.keyColor = KeyboardTheme.keyControlBackground
        delete.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val number = NumberKey("?123")
        number.keyColor = KeyboardTheme.keyControlBackground
        number.textSize = 18.dp2px.toFloat()
        number.bold = true
        number.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val emoji = PreviewTextKey(',')
        emoji.keyColor = KeyboardTheme.keyControlBackground
        emoji.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        val lang = LangKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = SpaceKey("拼音")
        space.textSize = KeyboardTheme.keySpaceTextSize.toFloat()
        space.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        val period = PreviewTextKey('。')
        period.keyColor = KeyboardTheme.keyControlBackground
        period.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        period.miniKeys = listOf('\uFF0C', '\u3010', '\u3011', '\u300A', '\u300B', '&', '$', '\u005E', '\u0025', '\u005F', '\uFF1B', '\uFF1A', '\u3001', '\uFF01', '\u2026', '\uFF1F')
        period.initMiniKeyIndex = 14
        val enter = DoneKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = KeyboardTheme.keyEnterBackground
        enter.keyPressedColor = KeyboardTheme.keyEnterPressedBackground

        layout.rows = arrayOf(
            arrayOf<Key>(q, w, e, r, t, y ,u, i, o, p),
            arrayOf<Key>(a, s, d, f, g, h, j, k, l),
            arrayOf<Key>(hanci, z, x, c, v, b, n, m, delete),
            arrayOf<Key>(number, emoji, lang, space, period, enter)
        )

        keys = listOf(
            q, w, e, r, t, y ,u, i, o, p,
            a, s, d, f, g, h, j, k, l,
            hanci, z, x, c, v, b, n, m, delete,
            number, emoji, lang, space, period, enter)
    }

    private fun buildPunctuation() {
        val p1 = PreviewTextKey('1', '\u0000',
            listOf('\u2159', '\u2150', '\u215B', '\u2151', '\u2152', '\u00B9', '\u00BD', '\u2153', '\u00BC', '\u2155'), 5)
        val p2 = PreviewTextKey('2', '\u0000',
            listOf('\u2156', '\u00B2', '\u2154'), 1)
        val p3 = PreviewTextKey('3', '\u0000',
            listOf('\u2157', '\u00B3', '\u00BE', '\u215C'), 1)
        val p4 = PreviewTextKey('4', '\u0000',
            listOf('\u2074', '\u2158'), 0)
        val p5 = PreviewTextKey('5', '\u0000',
            listOf('\u215D', '\u2075', '\u215A'), 1)
        val p6 = PreviewTextKey('6', '\u0000',
            listOf('\u2076'), 0)
        val p7 = PreviewTextKey('7', '\u0000',
            listOf('\u2077', '\u215E'), 0)
        val p8 = PreviewTextKey('8', '\u0000', listOf('\u2078'), 0)
        val p9 = PreviewTextKey('9', '\u0000', listOf('\u2079'), 0)
        val p0 = PreviewTextKey('0', '\u0000', listOf('\u2205', '\u207F', '\u2070'), 2)
        val at = PreviewTextKey('@')
        val sharp = PreviewTextKey('#', '\u0000', listOf('\u2116'))
        val dollar = PreviewTextKey('$', '\u0000', listOf('\u20B1', '\u20AC', '\u00A2', '\u00A3', '\u00A5'), 2)
        val underscore = PreviewTextKey('_')
        val and = PreviewTextKey('&')
        val minus = PreviewTextKey('-', '\u0000', listOf('\u2014', '\u005F', '\u2013', '\u00B7'), 1)
        val plus = PreviewTextKey('+', '\u0000', listOf('\u00B1'))
        val leftParenthesis = PreviewTextKey('\uFF08', '\u0000',
            listOf('\u3010', '{', '(', '<', '\uFF3B'), 2)
        val rightParenthesis = PreviewTextKey('\uFF09', '\u0000',
            listOf('\u3011', '\uFF3D', '}', ')', '>'), 3)
        val slash = PreviewTextKey('/', '\u0000', listOf('\uFF0F'), 0)

        val symbol = SymbolKey("=\\<")
        symbol.keyColor = KeyboardTheme.keyControlBackground
        symbol.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        symbol.textSize = 18.dp2px.toFloat()
        symbol.bold = true
        val asterisk = PreviewTextKey('\u3001')
        val quote = PreviewTextKey('\u201C', '\u0000', listOf('\u0022', '\u300A'), 0)
        val singleQuote = PreviewTextKey('\u201D', '\u0000', listOf('\u201E', '\u300B'), 0)
        val colon = PreviewTextKey('\uFF1A', '\u0000', listOf('\u003A'), 0)
        val semicolon = PreviewTextKey('\uFF1B', '\u0000', listOf('\u003B'), 0)
        val exclamation = PreviewTextKey('\uFF01', '\u0000', listOf('!', '\u00A1'))
        val question = PreviewTextKey('\uFF1F', '\u0000', listOf('?', '\u00BF'))
        val delete = DeleteKey(context.getDrawable(R.drawable.ic_delete_24dp)!!)
        delete.repeatable = true
        delete.keyColor = KeyboardTheme.keyControlBackground
        delete.keyPressedColor = KeyboardTheme.keyControlPressedBackground

        val char = QWERTYKey("返回")
        char.keyColor = KeyboardTheme.keyControlBackground
        char.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        char.textSize = 18.dp2px.toFloat()
        char.bold = true
        val comma = PreviewTextKey(',')
        comma.keyColor = KeyboardTheme.keyControlBackground
        comma.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        val lang = LangKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = SpaceKey("拼音")
        space.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        space.textSize = KeyboardTheme.keySpaceTextSize.toFloat()
        val period = PreviewTextKey('.')
        period.keyColor = KeyboardTheme.keyControlBackground
        period.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        period.miniKeys = listOf('\u2026')
        val enter = DoneKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = KeyboardTheme.keyEnterBackground
        enter.keyPressedColor = KeyboardTheme.keyEnterPressedBackground

        layout.rows = arrayOf(
            arrayOf<Key>(p1, p2, p3, p4, p5, p6, p7, p8, p9, p0),
            arrayOf<Key>(at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash),
            arrayOf<Key>(symbol, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete),
            arrayOf<Key>(char, comma, lang, space, period, enter)
        )

        keys = listOf(
            p1, p2, p3, p4, p5, p6, p7, p8, p9, p0,
            at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash,
            symbol, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete,
            char, comma, lang, space, period, enter
        )
    }

    private fun buildSymbol() {
        val p1 = PreviewTextKey('\u007E', '\u0000', listOf('\uFF5E'), 0)
        val p2 = PreviewTextKey('\u0060')
        val p3 = PreviewTextKey('\u007C')
        val p4 = PreviewTextKey('\u2022', '\u0000', listOf('\u2663', '\u2660', '\u266A', '\u2665', '\u2666'), 2)
        val p5 = PreviewTextKey('\u221A')
        val p6 = PreviewTextKey('\u03C0', '\u0000', listOf('\u03A9', '\u03A0', '\u03BC'), 1)
        val p7 = PreviewTextKey('\u00F7')
        val p8 = PreviewTextKey('\u00D7')
        val p9 = PreviewTextKey('\u002A', '\u0000', listOf('\u2605', '\u2020', '\u2021'), 1)
        val p0 = PreviewTextKey('\u00B6', '\u0000', listOf('\u00A7'), 0)

        val at = PreviewTextKey('\u00A3')
        val sharp = PreviewTextKey('\u00A2')
        val dollar = PreviewTextKey('\u20AC')
        val underscore = PreviewTextKey('\u00A5')
        val and = PreviewTextKey('\u005E', '\u0000', listOf('\u2190', '\u2191', '\u2193', '\u2192'), 1)
        val minus = PreviewTextKey('\u00B0', '\u0000', listOf('\u2032', '\u2033'))
        val plus = PreviewTextKey('\u003D', '\u0000', listOf('\u221E', '\u2260', '\u2248'), 1)
        val leftParenthesis = PreviewTextKey('\u300C', '\u0000', listOf('\u300E'))
        val rightParenthesis = PreviewTextKey('\u300D', '\u0000', listOf('\u300F'))
        val slash = PreviewTextKey('\u005C')

        val punctuation = PunctuationKey("?123")
        punctuation.keyColor = KeyboardTheme.keyControlBackground
        punctuation.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        punctuation.textSize = 18.dp2px.toFloat()
        punctuation.bold = true
        val asterisk = PreviewTextKey('\u0025', '\u0000', listOf('\u2030', '\u2105'))
        val quote = PreviewTextKey('\u2018', '\u0000', listOf('\u0027', '\u3008'))
        val singleQuote = PreviewTextKey('\u2019', '\u0000', listOf('\u201A', '\u3009'))
        val colon = PreviewTextKey('\u2122')
        val semicolon = PreviewTextKey('\u2713')
        val exclamation = PreviewTextKey('\uFF3B', '\u0000', listOf('\u005B'))
        val question = PreviewTextKey('\uFF3D', '\u0000', listOf('\u005D'))
        val delete = DeleteKey(context.getDrawable(R.drawable.ic_delete_24dp)!!)
        delete.repeatable = true
        delete.keyColor = KeyboardTheme.keyControlBackground
        delete.keyPressedColor = KeyboardTheme.keyControlPressedBackground

        val char = QWERTYKey("返回")
        char.keyColor = KeyboardTheme.keyControlBackground
        char.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        char.textSize = 18.dp2px.toFloat()
        char.bold = true
        val comma = PreviewTextKey('\u300A')
        comma.keyColor = KeyboardTheme.keyControlBackground
        comma.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        comma.miniKeys = listOf('\u00AB', '\u2264', '\u2039', '\u27E8')
        comma.initMiniKeyIndex = 1
        val lang = LangKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = SpaceKey("拼音")
        space.keyPressedColor = KeyboardTheme.keyControlPressedBackground
        space.textSize = KeyboardTheme.keySpaceTextSize.toFloat()
        val period = PreviewTextKey('\u300B')
        period.keyColor = KeyboardTheme.keyControlBackground
        period.textSize = KeyboardTheme.keySymbolTextSize.toFloat()
        period.miniKeys = listOf('\u27E9', '\u00BB', '\u2265', '\u203A')
        period.initMiniKeyIndex = 2
        val enter = DoneKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = KeyboardTheme.keyEnterBackground
        enter.keyPressedColor = KeyboardTheme.keyEnterPressedBackground

        layout.rows = arrayOf(
            arrayOf<Key>(p1, p2, p3, p4, p5, p6, p7, p8, p9, p0),
            arrayOf<Key>(at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash),
            arrayOf<Key>(punctuation, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete),
            arrayOf<Key>(char, comma, lang, space, period, enter)
        )

        keys = listOf(
            p1, p2, p3, p4, p5, p6, p7, p8, p9, p0,
            at, sharp, dollar, underscore, and, minus, plus, leftParenthesis, rightParenthesis, slash,
            punctuation, asterisk, quote, singleQuote, colon, semicolon, exclamation, question, delete,
            char, comma, lang, space, period, enter
        )
    }

    override fun onClick(key: Key) {
        Log.i("[SK]", "[Keyboard] click: $key")

        var updated = false
        when (key) {
            is NumberKey -> {
                mode = PUNCTUATION
                updated = true
            }
            is QWERTYKey -> {
                mode = NORMAL
                updated = true
            }
            is SymbolKey -> {
                if (mode == PUNCTUATION) {
                    mode = SYMBOL
                    updated = true
                }
            }
            is PunctuationKey -> {
                if (mode == SYMBOL) {
                    mode = PUNCTUATION
                    updated = true
                }
            }
            is DeleteKey -> {
                Log.i("[SK]", "[Keyboard] delete")
                keyboardListener?.onKey(KeyEvent.KEYCODE_DEL)
            }
            is SpaceKey -> {
                Log.i("[SK]", "[Keyboard] space")
                keyboardListener?.onKey(KeyEvent.KEYCODE_SPACE)
            }
            is DoneKey -> {
                Log.i("[SK]", "[Keyboard] done")
                keyboardListener?.onKey(KeyEvent.KEYCODE_ENTER)
            }
            is LangKey -> {
                Log.i("[SK]", "[Keyboard] lang switch")
                keyboardListener?.onSwitchLang()
            }
            else -> {
                if (key is TextKey) {
                    keyboardListener?.onChar(key.keyCode)
                }
            }
        }
        if (updated) {
            buildLayout()
        }
    }

    override fun onDoubleClick(key: Key): Boolean {
        return false
    }
}

interface KeyboardListener {
    fun onSwitchLang()
    fun onLayoutChanged()
    fun onChar(c: Char)
    fun onKey(keyCode: Int)
}

class QWERTYLayout {
    companion object {
        const val GRID_WIDTH = 20
        const val GRID_HEIGHT = 8
    }
    var width = 0
    var height = 0
    var keyHeight = 0
    var rows = arrayOf(arrayOf<Key>())

    var displayWidth = 0
    var displayHeight = 0

    private var px = .0f
    private var py = .0f

    var gw = 0
    var gh = 0
    var grids = arrayOf(arrayOf<Key>())

    fun layout(wid: Int, hei: Int) {
        var keys = arrayOf<Key>()
        val horizontal = wid > displayWidth
        val verticalPadding = if (horizontal) KeyboardTheme.keyPaddingHorizontal else KeyboardTheme.keyPadding
        width = wid
        val normalWidth = (width - KeyboardTheme.paddingLeft- KeyboardTheme.paddingRight-11* KeyboardTheme.keyGap)/10.toFloat()
        val controlWidth = (width - KeyboardTheme.paddingLeft- KeyboardTheme.paddingRight-10* KeyboardTheme.keyGap-7*normalWidth)/2.toFloat()
        val spaceWidth = 4*normalWidth+3* KeyboardTheme.keyGap.toFloat()
        keyHeight = if (horizontal) KeyboardTheme.keyHeightHorizontal else KeyboardTheme.keyHeight
        val normalHeight = keyHeight.toFloat()
        px = (KeyboardTheme.paddingLeft+ KeyboardTheme.keyGap).toFloat()
        py = (KeyboardTheme.paddingTop+ verticalPadding).toFloat()

        keys += rows[0]
        rows[0].forEach {
            layoutKey(px, py, normalWidth, normalHeight, it)
        }
        py += normalHeight + verticalPadding

        py += verticalPadding
        px = if (rows[1].size == 9) {
            (width-9*normalWidth-8* KeyboardTheme.keyGap)/2
        } else {
            (KeyboardTheme.paddingLeft+ KeyboardTheme.keyGap).toFloat()
        }
        keys += rows[1]
        rows[1].forEach {
            layoutKey(px, py, normalWidth, normalHeight, it)
        }
        py += normalHeight + verticalPadding

        px = (KeyboardTheme.paddingLeft+ KeyboardTheme.keyGap).toFloat()
        py += verticalPadding
        keys += rows[2]
        rows[2].forEachIndexed { index, key ->
            if (index == 0 || index == 8) {
                layoutKey(px, py, controlWidth, normalHeight, key)
            } else {
                layoutKey(px, py, normalWidth, normalHeight, key)
            }
        }
        py += normalHeight + verticalPadding

        px = (KeyboardTheme.paddingLeft+ KeyboardTheme.keyGap).toFloat()
        py += verticalPadding
        keys += rows[3]
        rows[3].forEachIndexed { index, key ->
            if (index == 0 || index == 5) {
                layoutKey(px, py, controlWidth, normalHeight, key)
            } else if (index == 3) {
                layoutKey(px, py, spaceWidth, normalHeight, key)
            } else {
                layoutKey(px, py, normalWidth, normalHeight, key)
            }
        }
        py += normalHeight + verticalPadding + KeyboardTheme.paddingBottom
        height = py.toInt()

        // Round-up so we don't have any pixels outside the grid
        gw = (width+GRID_WIDTH-1)/ GRID_WIDTH
        gh = (height+ GRID_HEIGHT)/ GRID_HEIGHT
        val threshold = (width/10) * 1.8
        computeNearestNeighbors((threshold*threshold).toInt(), keys)
    }

    fun getNearestKeys(x: Int, y: Int): Array<Key> {
        val px = if (x < 0) 0 else if (x >= width) width-1 else x
        val py = if (y < 0) 0 else if (y >= height) height-1 else y
        val index = (py/gh)* GRID_WIDTH+(px/gw)
        assert(index < grids.size)
        return grids[index]
    }

    private fun computeNearestNeighbors(threshold: Int, keys: Array<Key>) {
        grids = arrayOf()
        for (y in 0 until height step gh) {
            for (x in 0 until width step gw) {
                var grid = arrayOf<Key>()
                for (key in keys) {
                    if (key.squareDistanceFrom(x, y) < threshold ||
                        key.squareDistanceFrom(x+gw-1, y) < threshold ||
                        key.squareDistanceFrom(x+gw-1, y+gh-1) < threshold ||
                        key.squareDistanceFrom(x, y+gh-1) < threshold ||
                        key.isInside(x, y) ||
                        key.isInside(x+gw-1, y) ||
                        key.isInside(x+gw-1, y+gh-1) ||
                        key.isInside(x, y+gh-1)) {
                        grid += key
                    }
                }
                grids += grid
            }
        }
    }

    private fun layoutKey(x: Float, y: Float, width: Float, height: Float, key: Key) {
        key.x = x
        key.y = y
        key.width = width
        key.height = height

        px += width + KeyboardTheme.keyGap
    }
}
