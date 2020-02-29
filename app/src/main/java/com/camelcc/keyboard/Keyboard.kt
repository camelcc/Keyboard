package com.camelcc.keyboard

interface Keyboard {
    val theme: KeyboardTheme

    val keys: List<Key>
    val width: Int
    val height: Int

    fun resize(w: Int, h: Int)
}
