package com.camelcc.keyboard

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import java.lang.StringBuilder
import kotlin.math.min

abstract class Key {
    companion object {
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
            Keyboard.theme.keyBorderRadius, Keyboard.theme.keyBorderRadius, paint)
        paint.style = Paint.Style.FILL
        paint.color = if (pressed) keyPressedColor else keyColor
        paint.strokeWidth = .0f
        canvas.drawRoundRect(.0f, .0f,
            width, height,
            Keyboard.theme.keyBorderRadius, Keyboard.theme.keyBorderRadius, paint)
        paint.style = style
        paint.color = color

        drawContent(canvas, paint)
        canvas.translate(-x, -y)
    }

    // Returns the square of the distance between the center of the key and the given point
    fun squareDistanceFrom(x: Int, y: Int): Int {
        val dx = this.x + width/2 - x
        val dy = this.y + height/2 - y
        return (dx*dx + dy*dy).toInt()
    }

    // Detects if a point fall inside this key
    fun isInside(x: Int, y: Int): Boolean {
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

    abstract fun drawContent(canvas: Canvas, paint: Paint)

    // Informs the key that it has been pressed, in case it needs to change its appearance or state.
    fun onPressed() {
        pressed = !pressed
    }

    // Changes the pressed state of the key
    fun onReleased(inside: Boolean) {
        pressed = !pressed
    }

    open fun onClicked() {}
    open fun onDoubleClicked(): Boolean { return false }

    override fun toString(): String {
        return this::class.java.name
    }
}

open class TextKey(var text: String) : Key() {
    var textSize = Keyboard.theme.keyTextSize.toFloat()
    var upperSize = Keyboard.theme.keyUpperTextSize.toFloat()
    var bold = false

    var superScript = ""
    var miniKeys = listOf<String>()
    var initMiniKeyIndex = 0

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
        if (superScript.isNotBlank()) {
            paint.textSize = upperSize
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(superScript,
                width - paint.measureText(superScript),
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

class PreviewTextKey(text: String): TextKey(text) {
    constructor(text: String,
                superScript: String,
                miniKeys: List<String> = listOf(),
                activeIndex: Int = 0) : this(text) {
        this.superScript = superScript
        this.miniKeys = miniKeys
        this.initMiniKeyIndex = activeIndex
    }
}

val NOT_A_KEY = PreviewTextKey("")

class DeleteKey(icon: Drawable): IconKey(icon)
class NumberKey(text: String): TextKey(text)
class SymbolKey(text: String): TextKey(text)
class PunctuationKey(text: String): TextKey(text)
class QWERTYKey(text: String): TextKey(text)
class ShiftKey(icon: Drawable): IconKey(icon)
class SpaceKey(text: String): TextKey(text)
