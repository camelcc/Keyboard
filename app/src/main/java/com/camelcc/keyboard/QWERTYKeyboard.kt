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

    constructor(context: Context) {
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
        val normalWidth = width/10
        val controlWidth = (width*0.15).toInt()
        val spaceWidth = (width*0.4).toInt()
        val normalHeight = 60.dp2px
        var x = 0
        var y = 0

        layoutKey(x, y, normalWidth, normalHeight, q)
        x += q.width
        layoutKey(x, y, normalWidth, normalHeight, w)
        x += w.width
        layoutKey(x, y, normalWidth, normalHeight, e)
        x += e.width
        layoutKey(x, y, normalWidth, normalHeight, r)
        x += r.width
        layoutKey(x, y, normalWidth, normalHeight, t)
        x += t.width
        layoutKey(x, y, normalWidth, normalHeight, this.y)
        x += this.y.width
        layoutKey(x, y, normalWidth, normalHeight, u)
        x += u.width
        layoutKey(x, y, normalWidth, normalHeight, i)
        x += i.width
        layoutKey(x, y, normalWidth, normalHeight, o)
        x += o.width
        layoutKey(x, y, normalWidth, normalHeight, p)
        x += p.width
        y += normalHeight

        x = (width*0.05).toInt()
        layoutKey(x, y, normalWidth, normalHeight, a)
        x += a.width
        layoutKey(x, y, normalWidth, normalHeight, s)
        x += s.width
        layoutKey(x, y, normalWidth, normalHeight, d)
        x += d.width
        layoutKey(x, y, normalWidth, normalHeight, f)
        x += f.width
        layoutKey(x, y, normalWidth, normalHeight, g)
        x += g.width
        layoutKey(x, y, normalWidth, normalHeight, h)
        x += h.width
        layoutKey(x, y, normalWidth, normalHeight, j)
        x += j.width
        layoutKey(x, y, normalWidth, normalHeight, k)
        x += k.width
        layoutKey(x, y, normalWidth, normalHeight, l)
        x += l.width
        x = 0
        y += normalHeight


        layoutKey(x, y, controlWidth, normalHeight,shift)
        x += shift.width
        layoutKey(x, y, normalWidth, normalHeight, z)
        x += z.width
        layoutKey(x, y, normalWidth, normalHeight, this.x)
        x += this.x.width
        layoutKey(x, y, normalWidth, normalHeight, c)
        x += c.width
        layoutKey(x, y, normalWidth, normalHeight, v)
        x += v.width
        layoutKey(x, y, normalWidth, normalHeight, b)
        x += b.width
        layoutKey(x, y, normalWidth, normalHeight, n)
        x += n.width
        layoutKey(x, y, normalWidth, normalHeight, m)
        x += m.width
        layoutKey(x, y, normalWidth, normalHeight, delete)
        x += delete.width
        x = 0
        y += normalHeight

        layoutKey(x, y, controlWidth, normalHeight, number)
        x += number.width
        layoutKey(x, y, normalWidth, normalHeight, emoji)
        x += emoji.width
        layoutKey(x, y, normalWidth, normalHeight, lang)
        x += lang.width
        layoutKey(x, y, spaceWidth, normalHeight, space)
        x += space.width
        layoutKey(x, y, normalWidth, normalHeight, symbol)
        x += symbol.width
        layoutKey(x, y, normalWidth, normalHeight, enter)
        x += enter.width
        y += normalHeight
        height = y
    }
}
