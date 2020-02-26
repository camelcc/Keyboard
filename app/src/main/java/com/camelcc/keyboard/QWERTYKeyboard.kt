package com.camelcc.keyboard

import android.graphics.drawable.Drawable

class QWERTYKeyboard: Keyboard {
    override val keys = mutableListOf<Key>()
    override var width: Int
    override var height: Int = 0

    private val kQ = initKey(text = "q")
    private val kW = initKey(text = "w")
    private val kE = initKey(text = "e")
    private val kR = initKey(text = "r")
    private val kT = initKey(text = "t")
    private val kY = initKey(text = "y")
    private val kU = initKey(text = "u")
    private val kI = initKey(text = "i")
    private val kO = initKey(text = "o")
    private val kP = initKey(text = "p")
    private val kA = initKey(text = "a")
    private val kS = initKey(text = "s")
    private val kD = initKey(text = "d")
    private val kF = initKey(text = "f")
    private val kG = initKey(text = "g")
    private val kH = initKey(text = "h")
    private val kJ = initKey(text = "j")
    private val kK = initKey(text = "k")
    private val kL = initKey(text = "l")
    private val kShift = initKey(text = "^")
    private val kZ = initKey(text = "z")
    private val kX = initKey(text = "x")
    private val kC = initKey(text = "c")
    private val kV = initKey(text = "v")
    private val kB = initKey(text = "b")
    private val kN = initKey(text = "n")
    private val kM = initKey(text = "m")
    private val kDelete = initKey(text = "<")
    private val kNumber = initKey(text = "?123")
    private val kEmoji = initKey(text = ",")
    private val kLang = initKey(text = "O")
    private val kSpace = initKey(text = "English")
    private val kSymbol = initKey(text = ".")
    private val kEnter = initKey(text = "-")

    constructor(with: Int) {
        this.width = with
        keys.add(kQ)
        keys.add(kW)
        keys.add(kE)
        keys.add(kR)
        keys.add(kT)
        keys.add(kY)
        keys.add(kU)
        keys.add(kI)
        keys.add(kO)
        keys.add(kP)
        keys.add(kA)
        keys.add(kS)
        keys.add(kD)
        keys.add(kF)
        keys.add(kG)
        keys.add(kH)
        keys.add(kJ)
        keys.add(kK)
        keys.add(kL)
        keys.add(kShift)
        keys.add(kZ)
        keys.add(kX)
        keys.add(kC)
        keys.add(kV)
        keys.add(kB)
        keys.add(kN)
        keys.add(kM)
        keys.add(kDelete)
        keys.add(kNumber)
        keys.add(kEmoji)
        keys.add(kLang)
        keys.add(kSpace)
        keys.add(kSymbol)
        keys.add(kEnter)
        layoutKeys()
    }

    override fun resize(w: Int, h: Int) {
        width = w
        layoutKeys()
    }

    private fun initKey(text: CharSequence? = null, icon: Drawable? = null): Key {
        return Key(x = 0, y = 0, width = 0, height = 0, text = text, icon = icon)
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
        val normalHeight = 50.dp2px
        var x = 0
        var y = 0

        layoutKey(x, y, normalWidth, normalHeight, kQ)
        x += kQ.width
        layoutKey(x, y, normalWidth, normalHeight, kW)
        x += kW.width
        layoutKey(x, y, normalWidth, normalHeight, kE)
        x += kE.width
        layoutKey(x, y, normalWidth, normalHeight, kR)
        x += kR.width
        layoutKey(x, y, normalWidth, normalHeight, kT)
        x += kT.width
        layoutKey(x, y, normalWidth, normalHeight, kY)
        x += kY.width
        layoutKey(x, y, normalWidth, normalHeight, kU)
        x += kU.width
        layoutKey(x, y, normalWidth, normalHeight, kI)
        x += kI.width
        layoutKey(x, y, normalWidth, normalHeight, kO)
        x += kO.width
        layoutKey(x, y, normalWidth, normalHeight, kP)
        x += kP.width
        x = 0
        y += normalHeight

        x = (width*0.05).toInt()
        layoutKey(x, y, normalWidth, normalHeight, kA)
        x += kA.width
        layoutKey(x, y, normalWidth, normalHeight, kS)
        x += kS.width
        layoutKey(x, y, normalWidth, normalHeight, kD)
        x += kD.width
        layoutKey(x, y, normalWidth, normalHeight, kF)
        x += kF.width
        layoutKey(x, y, normalWidth, normalHeight, kG)
        x += kG.width
        layoutKey(x, y, normalWidth, normalHeight, kH)
        x += kH.width
        layoutKey(x, y, normalWidth, normalHeight, kJ)
        x += kJ.width
        layoutKey(x, y, normalWidth, normalHeight, kK)
        x += kK.width
        layoutKey(x, y, normalWidth, normalHeight, kL)
        x += kL.width
        x = 0
        y += normalHeight


        layoutKey(x, y, controlWidth, normalHeight, kShift)
        x += kShift.width
        layoutKey(x, y, normalWidth, normalHeight, kZ)
        x += kZ.width
        layoutKey(x, y, normalWidth, normalHeight, kX)
        x += kX.width
        layoutKey(x, y, normalWidth, normalHeight, kC)
        x += kC.width
        layoutKey(x, y, normalWidth, normalHeight, kV)
        x += kV.width
        layoutKey(x, y, normalWidth, normalHeight, kB)
        x += kB.width
        layoutKey(x, y, normalWidth, normalHeight, kN)
        x += kN.width
        layoutKey(x, y, normalWidth, normalHeight, kM)
        x += kM.width
        layoutKey(x, y, normalWidth, normalHeight, kDelete)
        x += kDelete.width
        x = 0
        y += normalHeight

        layoutKey(x, y, controlWidth, normalHeight, kNumber)
        x += kNumber.width
        layoutKey(x, y, normalWidth, normalHeight, kEmoji)
        x += kEmoji.width
        layoutKey(x, y, normalWidth, normalHeight, kLang)
        x += kLang.width
        layoutKey(x, y, spaceWidth, normalHeight, kSpace)
        x += kSpace.width
        layoutKey(x, y, normalWidth, normalHeight, kSymbol)
        x += kSymbol.width
        layoutKey(x, y, normalWidth, normalHeight, kEnter)
        x += kEnter.width
        y += normalHeight
        height = y
    }
}
