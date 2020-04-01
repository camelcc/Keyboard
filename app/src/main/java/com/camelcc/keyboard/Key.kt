package com.camelcc.keyboard

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log

open class Key {
    companion object {
        const val TAG = "[KEY]"
        const val EDGE_LEFT = 0x1
        const val EDGE_RIGHT = 0x2
        const val EDGE_TOP = 0x4
        const val EDGE_BOTTOM = 0x8
    }

    var x = .0f
    var y = .0f
    var width = .0f
    var height = .0f

    var edges = 0

    var keyColor = Keyboard.theme.keyColor
    var keyPressedColor = keyColor

    var repeatable = false
    private var pressed = false

    fun paint(canvas: Canvas, paint: Paint) {
        canvas.translate(x, y)

        val style = paint.style
        val color = paint.color
        paint.style = Paint.Style.STROKE
        paint.color = Keyboard.theme.keyBorderColor
        paint.strokeWidth = Keyboard.theme.keyBorderWidth.toFloat()
        canvas.drawRoundRect(1.0f, 1.0f,
            width-1.0f, height+1.0f,
            Keyboard.theme.keyBorderRadius.toFloat(), Keyboard.theme.keyBorderRadius.toFloat(), paint)
        paint.style = Paint.Style.FILL
//        paint.color = if (pressed) Color.RED else keyColor
        paint.color = if (pressed) keyPressedColor else keyColor
        paint.strokeWidth = .0f
        canvas.drawRoundRect(.0f, .0f,
            width, height,
            Keyboard.theme.keyBorderRadius.toFloat(), Keyboard.theme.keyBorderRadius.toFloat(), paint)
        paint.style = style
        paint.color = color

        drawContent(canvas, paint)

        canvas.translate(-x, -y)
    }

    // Returns the square of the distance between the center of the key and the given point
    open fun squareDistanceFrom(x: Int, y: Int): Int {
        val dx = this.x + width/2 - x
        val dy = this.y + height/2 - y
        return (dx*dx + dy*dy).toInt()
    }

    // Detects if a point fall inside this key
    open fun isInside(x: Int, y: Int): Boolean {
        val left = (edges and EDGE_LEFT) > 0
        val right = (edges and EDGE_RIGHT) > 0
        val top = (edges and EDGE_TOP) > 0
        val bottom = (edges and EDGE_BOTTOM) > 0
        if ((x >= this.x || (left && x <= this.x + this.width))
            && (x < this.x + this.width || (right && x >= this.x))
            && (y >= this.y || (top && y <= this.y + this.height))
            && (y < this.y + this.height || (bottom && y >= this.y))) {
            return true
        }
        return false
    }

    open fun drawContent(canvas: Canvas, paint: Paint) {}

    // Informs the key that it has been pressed, in case it needs to change its appearance or state.
    open fun onPressed() {
        Log.i(TAG, "$APP_TAG$TAG onPressed: $this")
        pressed = !pressed
    }

    // Changes the pressed state of the key
    open fun onReleased(inside: Boolean) {
        Log.i(TAG, "$APP_TAG$TAG onReleased: $this")
        pressed = !pressed
    }

    open fun onClicked() {}
    open fun onDoubleClicked(): Boolean { return false }

    override fun toString(): String {
        return this::class.java.name
    }
}

open class TextKey: Key {
    var textSize = Keyboard.theme.keyTextSize.toFloat()
    var upperSize = Keyboard.theme.keyUpperTextSize.toFloat()
    var bold = false

    val text: String
    private var superScript: String? = null

    constructor(text: String) {
        this.text = text
    }

    constructor(text: String, superScript: String) {
        this.text = text
        this.superScript = superScript
    }

    override fun drawContent(canvas: Canvas, paint: Paint) {
        paint.textSize = textSize
        if (bold) {
            paint.typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(
            text,
            width/2,
            height/2-(paint.descent()+paint.ascent())/2, paint)
        if (bold) {
            paint.typeface = Typeface.DEFAULT
        }
        superScript?.let {
            paint.textSize = upperSize
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(it,
                width - paint.measureText(it),
                paint.textSize, paint)
            paint.typeface = Typeface.DEFAULT
        }
    }

    override fun toString(): String {
        return text
    }
}

open class IconKey(private val icon: Drawable): Key() {
    override fun drawContent(canvas: Canvas, paint: Paint) {
        val drawableX = (width-icon.intrinsicWidth)/2
        val drawableY = (height-icon.intrinsicHeight)/2
        canvas.translate(drawableX, drawableY)
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(canvas)
        canvas.translate(-drawableX, -drawableY)
    }
}

open class PreviewTextKey: TextKey {
    constructor(text: String): super(text)
    constructor(text: String, superScript: String): super(text, superScript)
}

open class DeleteKey(icon: Drawable): IconKey(icon)
open class NumberKey(text: String): TextKey(text)
open class SymbolKey(text: String): TextKey(text)
open class PunctuationKey(text: String): TextKey(text)
open class QWERTYKey(text: String): TextKey(text)
open class ShiftKey(icon: Drawable): IconKey(icon)
