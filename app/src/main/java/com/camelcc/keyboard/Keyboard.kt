package com.camelcc.keyboard

import android.content.Context
import android.util.Log
import kotlin.math.max
import kotlin.math.min

class Keyboard {
    companion object {
        var theme = KeyboardTheme()
    }

    var width = 0
    var height = 0

    var keys = listOf<Key>()
    private var layout: QWERTYLayout

    private val context: Context

    constructor(context: Context) {
        this.context = context
        val dm = context.resources.displayMetrics
        val dw = min(dm.widthPixels, dm.heightPixels)
        val dh = max(dm.widthPixels, dm.heightPixels)
        layout = QWERTYLayout()
        layout.displayWidth = dw
        layout.displayHeight = dh
        buildQWERTY()
//        buildPunctuation()
        layout.layout(dm.widthPixels, 0)
        height = layout.height
    }

    private fun buildQWERTY(upperCase: Boolean = false) {
        val q = TextKey(if (upperCase) "Q" else "q", "1")
        val w = TextKey(if (upperCase) "W" else "w", "2")
        val e = TextKey(if (upperCase) "E" else "e", "3")
        val r = TextKey(if (upperCase) "R" else "r", "4")
        val t = TextKey(if (upperCase) "T" else "t", "5")
        val y = TextKey(if (upperCase) "Y" else "y", "6")
        val u = TextKey(if (upperCase) "U" else "u", "7")
        val i = TextKey(if (upperCase) "I" else "i", "8")
        val o = TextKey(if (upperCase) "O" else "o", "9")
        val p = TextKey(if (upperCase) "P" else "p", "0")
        val a = TextKey(if (upperCase) "A" else "a")
        val s = TextKey(if (upperCase) "S" else "s")
        val d = TextKey(if (upperCase) "D" else "d")
        val f = TextKey(if (upperCase) "F" else "f")
        val g = TextKey(if (upperCase) "G" else "g")
        val h = TextKey(if (upperCase) "H" else "h")
        val j = TextKey(if (upperCase) "J" else "j")
        val k = TextKey(if (upperCase) "K" else "k")
        val l = TextKey(if (upperCase) "L" else "l")
        val shift = IconKey(context.getDrawable(R.drawable.ic_emoji_12dp)!!)
        shift.keyColor = theme.keyControlBackground
        shift.keyPressedColor = theme.keyControlPressedBackground
        val z = TextKey(if (upperCase) "Z" else "z")
        val x = TextKey(if (upperCase) "X" else "x")
        val c = TextKey(if (upperCase) "C" else "c")
        val v = TextKey(if (upperCase) "V" else "v")
        val b = TextKey(if (upperCase) "B" else "b")
        val n = TextKey(if (upperCase) "N" else "n")
        val m = TextKey(if (upperCase) "M" else "m")
        val delete = IconKey(context.getDrawable(R.drawable.ic_emoji_12dp)!!)
        delete.keyColor = theme.keyControlBackground
        delete.keyPressedColor = theme.keyControlPressedBackground
        val number = TextKey("?123")
        number.keyColor = theme.keyControlBackground
        number.textSize = 18.dp2px.toFloat()
        number.bold = true
        number.keyPressedColor = theme.keyControlPressedBackground
        val emoji = TextKey(",")
        emoji.keyColor = theme.keyControlBackground
        emoji.textSize = theme.keySymbolTextSize.toFloat()
        val lang = IconKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = TextKey("English")
        space.textSize = theme.keySpaceTextSize.toFloat()
        space.keyPressedColor = theme.keyControlPressedBackground
        val period = TextKey(".")
        period.keyColor = theme.keyControlBackground
        period.textSize = theme.keySymbolTextSize.toFloat()
        val enter = IconKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = theme.keyEnterBackground
        enter.keyPressedColor = theme.keyEnterPressedBackground

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
        val p1 = TextKey("1")
        val p2 = TextKey("2")
        val p3 = TextKey("3")
        val p4 = TextKey("4")
        val p5 = TextKey("5")
        val p6 = TextKey("6")
        val p7 = TextKey("7")
        val p8 = TextKey("8")
        val p9 = TextKey("9")
        val p0 = TextKey("0")

        val at = TextKey("@")
        val sharp = TextKey("#")
        val dollar = TextKey("$")
        val underscore = TextKey("_")
        val and = TextKey("&")
        val minus = TextKey("-")
        val plus = TextKey("+")
        val leftParenthesis = TextKey("(")
        val rightParenthesis = TextKey(")")
        val slash = TextKey("/")

        val symbol = TextKey("=\\<")
        symbol.keyColor = theme.keyControlBackground
        symbol.keyPressedColor = theme.keyControlPressedBackground
        symbol.textSize = 18.dp2px.toFloat()
        symbol.bold = true
        val asterisk = TextKey("*")
        val quote = TextKey("\"")
        val singleQuote = TextKey("'")
        val colon = TextKey(":")
        val semicolon = TextKey(";")
        val exclamation = TextKey("!")
        val question = TextKey("?")
        val delete = IconKey(context.getDrawable(R.drawable.ic_emoji_12dp)!!)
        delete.keyColor = theme.keyControlBackground
        delete.keyPressedColor = theme.keyControlPressedBackground

        val char = TextKey("ABC")
        char.keyColor = theme.keyControlBackground
        char.keyPressedColor = theme.keyControlPressedBackground
        char.textSize = 18.dp2px.toFloat()
        char.bold = true
        val comma = TextKey(",")
        comma.keyColor = theme.keyControlBackground
        comma.textSize = theme.keySymbolTextSize.toFloat()
        val lang = IconKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
        val space = TextKey("English")
        space.keyPressedColor = theme.keyControlPressedBackground
        space.textSize = theme.keySpaceTextSize.toFloat()
        val period = TextKey(".")
        period.keyColor = theme.keyControlBackground
        period.textSize = theme.keySymbolTextSize.toFloat()
        val enter = IconKey(context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = theme.keyEnterBackground
        enter.keyPressedColor = theme.keyEnterPressedBackground

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

    var mode = 0 // 0 - normal, 1 - upper, 2 - punctuation
    fun test() {
//        mode = 2
//        mode = (mode+1)%3
//        if (mode == 0) {
//            buildQWERTY()
//        } else if (mode == 1) {
//            buildQWERTY(true)
//        } else if (mode == 2) {
//            buildPunctuation()
//        }
//        layout.layout(width, height)
    }

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

class QWERTYLayout {
    var width = 0
    var height = 0
    var rows = arrayOf(arrayOf<Key>())

    var displayWidth = 0
    var displayHeight = 0

    private var px = .0f
    private var py = .0f

    var gw = 0
    var gh = 0
    var grids = arrayOf(arrayOf<Key>())

    fun layout(wid: Int, hei: Int) {
        Log.i("[SK]", "[QWERTYLayout] layout: w = $wid, h = $hei")
        var keys = arrayOf<Key>()
        val horizontal = wid > displayWidth
        val verticalPadding = if (horizontal) Keyboard.theme.keyPaddingHorizontal else Keyboard.theme.keyPadding
        width = wid
        val normalWidth = (width - Keyboard.theme.paddingLeft- Keyboard.theme.paddingRight-11* Keyboard.theme.keyGap)/10.toFloat()
        val controlWidth = (width - Keyboard.theme.paddingLeft- Keyboard.theme.paddingRight-10* Keyboard.theme.keyGap-7*normalWidth)/2.toFloat()
        val spaceWidth = 4*normalWidth+3* Keyboard.theme.keyGap.toFloat()
        val normalHeight = if (horizontal) Keyboard.theme.keyHeightHorizontal.toFloat() else Keyboard.theme.keyHeight.toFloat()
        px = (Keyboard.theme.paddingLeft+ Keyboard.theme.keyGap).toFloat()
        py = (Keyboard.theme.paddingTop+ verticalPadding).toFloat()

        keys += rows[0]
        rows[0].forEach {
            layoutKey(px, py, normalWidth, normalHeight, it)
        }
        py += normalHeight + verticalPadding

        py += verticalPadding
        px = if (rows[1].size == 9) {
            (width-9*normalWidth-8* Keyboard.theme.keyGap)/2
        } else {
            (Keyboard.theme.paddingLeft+ Keyboard.theme.keyGap).toFloat()
        }
        keys += rows[1]
        rows[1].forEach {
            layoutKey(px, py, normalWidth, normalHeight, it)
        }
        py += normalHeight + verticalPadding

        px = (Keyboard.theme.paddingLeft+ Keyboard.theme.keyGap).toFloat()
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

        px = (Keyboard.theme.paddingLeft+ Keyboard.theme.keyGap).toFloat()
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
        py += normalHeight + verticalPadding + Keyboard.theme.paddingBottom
        height = py.toInt()

        // Round-up so we don't have any pixels outside the grid
        gw = width/20
        gh = height/8
        val threshold = (width/10) * 1.8
        computeNearestNeighbors((threshold*threshold).toInt(), keys)
    }

    fun getNearestKeys(x: Int, y: Int): Array<Key> {
        val px = if (x < 0) 0 else if (x >= width) width-1 else x
        val py = if (y < 0) 0 else if (y >= height) height-1 else y
        val index = (py/gh)*20+(px/gw)
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

        px += width + Keyboard.theme.keyGap
    }
}
