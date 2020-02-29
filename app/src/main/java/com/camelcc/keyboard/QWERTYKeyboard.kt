package com.camelcc.keyboard

import android.content.Context

class QWERTYKeyboard: Keyboard {
    override var width: Int
    override var height: Int = 0

    private val q: Key
    private val w: Key
    private val e: Key
    private val r: Key
    private val t: Key
    private val y: Key
    private val u: Key
    private val i: Key
    private val o: Key
    private val p: Key
    private val a: Key
    private val s: Key
    private val d: Key
    private val f: Key
    private val g: Key
    private val h: Key
    private val j: Key
    private val k: Key
    private val l: Key
    private val shift: Key
    private val z: Key
    private val x: Key
    private val c: Key
    private val v: Key
    private val b: Key
    private val n: Key
    private val m: Key
    private val delete: Key
    private val number: Key
    private val emoji: Key
    private val lang: Key
    private val space: Key
    private val symbol: Key
    private val enter: Key
    override val keys: List<Key>

    override val theme: KeyboardTheme

    constructor(context: Context) {
        width = context.resources.displayMetrics.widthPixels
        theme = KeyboardTheme(context)
        q = TextUpperKey(theme, "q", "1")
        w = TextUpperKey(theme, "w", "2")
        e = TextUpperKey(theme, "e", "3")
        r = TextUpperKey(theme, "r", "4")
        t = TextUpperKey(theme, "t", "5")
        y = TextUpperKey(theme, "y", "6")
        u = TextUpperKey(theme, "u", "7")
        i = TextUpperKey(theme, "i", "8")
        o = TextUpperKey(theme, "o", "9")
        p = TextUpperKey(theme, "p", "0")
        a = TextKey(theme, "a")
        s = TextKey(theme, "s")
        d = TextKey(theme, "d")
        f = TextKey(theme, "f")
        g = TextKey(theme, "g")
        h = TextKey(theme, "h")
        j = TextKey(theme, "j")
        k = TextKey(theme, "k")
        l = TextKey(theme, "l")
        z = TextKey(theme, "z")
        x = TextKey(theme, "x")
        c = TextKey(theme, "c")
        v = TextKey(theme, "v")
        b = TextKey(theme, "b")
        n = TextKey(theme, "n")
        m = TextKey(theme, "m")
        shift = IconKey(theme, context.getDrawable(R.drawable.ic_emoji_12dp)!!)
        shift.keyColor = theme.keyControlBackground
        delete = IconKey(theme, context.getDrawable(R.drawable.ic_emoji_12dp)!!)
        delete.keyColor = theme.keyControlBackground
        number = TextKey(theme, "?123")
        number.keyColor = theme.keyControlBackground
        number.textSize = 18.dp2px.toFloat()
        number.bold = true
        emoji = TextKey(theme, ",")
        emoji.keyColor = theme.keyControlBackground
        emoji.textSize = theme.keySymbolTextSize.toFloat()
        lang = IconKey(theme, context.getDrawable(R.drawable.ic_lang_24dp)!!)
        space = TextKey(theme, "English")
        space.textSize = theme.keySpaceTextSize.toFloat()
        symbol = TextKey(theme, ".")
        symbol.keyColor = theme.keyControlBackground
        symbol.textSize = theme.keySymbolTextSize.toFloat()
        enter = IconKey(theme, context.getDrawable(R.drawable.ic_check_24dp)!!)
        enter.keyColor = theme.keyEnterBackground
        keys = mutableListOf(
            q, w, e, r, t, y ,u, i, o, p,
            a, s, d, f, g, h, j, k, l,
            shift, z, x, c, v, b, n, m, delete,
            number, emoji, lang, space, symbol, enter)
        layoutKeys()
    }

    override fun resize(w: Int, h: Int) {
        width = w
        layoutKeys()
    }

    private fun layoutKeys() {
        val normalWidth = (width-theme.paddingLeft-theme.paddingRight-11*theme.keyGap)/10.toFloat()
        val controlWidth = (width-theme.paddingLeft-theme.paddingRight-10*theme.keyGap-7*normalWidth)/2.toFloat()
        val spaceWidth = 4*normalWidth+3*theme.keyGap.toFloat()
        val normalHeight = theme.keyHeight.toFloat()
        var x = (theme.paddingLeft+theme.keyGap).toFloat()
        var y = (theme.paddingTop+theme.keyPadding).toFloat()

        layoutKey(x, y, normalWidth, normalHeight, q)
        x += q.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, w)
        x += w.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, e)
        x += e.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, r)
        x += r.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, t)
        x += t.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, this.y)
        x += this.y.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, u)
        x += u.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, i)
        x += i.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, o)
        x += o.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, p)
        x += p.width + theme.keyGap
        y += normalHeight + theme.keyPadding

        y += theme.keyPadding
        x = (width-9*normalWidth-8*theme.keyGap)/2
        layoutKey(x, y, normalWidth, normalHeight, a)
        x += a.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, s)
        x += s.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, d)
        x += d.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, f)
        x += f.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, g)
        x += g.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, h)
        x += h.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, j)
        x += j.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, k)
        x += k.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, l)
        x += l.width + theme.keyGap
        y += normalHeight + theme.keyPadding

        x = theme.keyGap.toFloat()
        y += theme.keyPadding
        layoutKey(x, y, controlWidth, normalHeight,shift)
        x += shift.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, z)
        x += z.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, this.x)
        x += this.x.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, c)
        x += c.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, v)
        x += v.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, b)
        x += b.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, n)
        x += n.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, m)
        x += m.width + theme.keyGap
        layoutKey(x, y, controlWidth, normalHeight, delete)
        x += delete.width + theme.keyGap
        y += normalHeight + theme.keyPadding

        x = theme.keyGap.toFloat()
        y += theme.keyPadding
        layoutKey(x, y, controlWidth, normalHeight, number)
        x += number.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, emoji)
        x += emoji.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, lang)
        x += lang.width + theme.keyGap
        layoutKey(x, y, spaceWidth, normalHeight, space)
        x += space.width + theme.keyGap
        layoutKey(x, y, normalWidth, normalHeight, symbol)
        x += symbol.width + theme.keyGap
        layoutKey(x, y, controlWidth, normalHeight, enter)
        x += enter.width + theme.keyGap
        y += normalHeight + theme.keyPadding + theme.paddingBottom
        height = y.toInt()
    }

    private fun layoutKey(x: Float, y: Float, width: Float, height: Float, key: Key) {
        key.x = x
        key.y = y
        key.width = width
        key.height = height
    }
}
