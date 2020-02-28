package com.camelcc.keyboard

import android.content.Context
import android.graphics.drawable.Drawable

class QWERTYKeyboard: Keyboard {
    override var width: Int
    override var height: Int = 0

    private val q = initKey(text = "q")
    private val w = initKey(text = "w")
    private val e = initKey(text = "e")
    private val r = initKey(text = "r")
    private val t = initKey(text = "t")
    private val y = initKey(text = "y")
    private val u = initKey(text = "u")
    private val i = initKey(text = "i")
    private val o = initKey(text = "o")
    private val p = initKey(text = "p")
    private val a = initKey(text = "a")
    private val s = initKey(text = "s")
    private val d = initKey(text = "d")
    private val f = initKey(text = "f")
    private val g = initKey(text = "g")
    private val h = initKey(text = "h")
    private val j = initKey(text = "j")
    private val k = initKey(text = "k")
    private val l = initKey(text = "l")
    private val z = initKey(text = "z")
    private val x = initKey(text = "x")
    private val c = initKey(text = "c")
    private val v = initKey(text = "v")
    private val b = initKey(text = "b")
    private val n = initKey(text = "n")
    private val m = initKey(text = "m")
    private val shift = initKey(text = "^")
    private val delete = initKey(text = "<")
    private val number = initKey(text = "?123")
    private val emoji: Key
    private val lang: Key
    private val space = initKey(text = "English")
    private val symbol = initKey(text = ".")
    private val enter = initKey(text = "-")
    override val keys: List<Key>

    private val theme: KeyboardTheme

    constructor(context: Context) {
        theme = KeyboardTheme(context)
        width = context.resources.displayMetrics.widthPixels
        emoji = EmojiKey(context.getDrawable(R.drawable.ic_emoji_12dp)!!, ",")
        lang = IconKey(context.getDrawable(R.drawable.ic_lang_24dp)!!)
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

    private fun initKey(text: CharSequence? = null, icon: Drawable? = null): Key {
        return TextKey(text = text!!)
    }

    private fun layoutKey(x: Int, y: Int, width: Int, height: Int, key: Key) {
        key.x = x
        key.y = y
        key.width = width
        key.height = height
    }

    private fun layoutKeys() {
        val normalWidth = (width-theme.paddingLeft-theme.paddingRight-11*theme.keyPadding)/10
        val controlWidth = (width-theme.paddingLeft-theme.paddingRight-10*theme.keyPadding-7*normalWidth)/2
        val spaceWidth = 4*normalWidth+3*theme.keyPadding
        val normalHeight = theme.keyHeight
        var x = theme.paddingLeft+theme.keyPadding
        var y = theme.paddingTop+theme.keyPadding

        layoutKey(x, y, normalWidth, normalHeight, q)
        x += q.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, w)
        x += w.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, e)
        x += e.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, r)
        x += r.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, t)
        x += t.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, this.y)
        x += this.y.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, u)
        x += u.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, i)
        x += i.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, o)
        x += o.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, p)
        x += p.width + theme.keyPadding
        y += normalHeight + theme.keyPadding

        y += theme.keyPadding
        x = (width-9*normalWidth-8*theme.keyPadding)/2
        layoutKey(x, y, normalWidth, normalHeight, a)
        x += a.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, s)
        x += s.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, d)
        x += d.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, f)
        x += f.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, g)
        x += g.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, h)
        x += h.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, j)
        x += j.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, k)
        x += k.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, l)
        x += l.width + theme.keyPadding
        y += normalHeight + theme.keyPadding

        x = theme.keyPadding
        y += theme.keyPadding
        layoutKey(x, y, controlWidth, normalHeight,shift)
        x += shift.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, z)
        x += z.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, this.x)
        x += this.x.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, c)
        x += c.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, v)
        x += v.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, b)
        x += b.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, n)
        x += n.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, m)
        x += m.width + theme.keyPadding
        layoutKey(x, y, controlWidth, normalHeight, delete)
        x += delete.width + theme.keyPadding
        y += normalHeight + theme.keyPadding

        x = theme.keyPadding
        y += theme.keyPadding
        layoutKey(x, y, controlWidth, normalHeight, number)
        x += number.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, emoji)
        x += emoji.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, lang)
        x += lang.width + theme.keyPadding
        layoutKey(x, y, spaceWidth, normalHeight, space)
        x += space.width + theme.keyPadding
        layoutKey(x, y, normalWidth, normalHeight, symbol)
        x += symbol.width + theme.keyPadding
        layoutKey(x, y, controlWidth, normalHeight, enter)
        x += enter.width + theme.keyPadding
        y += normalHeight + theme.keyPadding + theme.paddingBottom
        height = y
    }
}
