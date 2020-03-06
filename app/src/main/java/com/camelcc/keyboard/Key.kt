package com.camelcc.keyboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable

open class Key {
    var x = .0f
    var y = .0f
    var width = .0f
    var height = .0f

    var keyColor = Keyboard.theme.keyColor

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
        paint.color = keyColor
        paint.strokeWidth = .0f
        canvas.drawRoundRect(.0f, .0f,
            width, height,
            Keyboard.theme.keyBorderRadius.toFloat(), Keyboard.theme.keyBorderRadius.toFloat(), paint)
        paint.style = style
        paint.color = color

        drawContent(canvas, paint)

        canvas.translate(-x, -y)
    }

    open fun drawContent(canvas: Canvas, paint: Paint) {}
}

open class TextKey: Key {
    var textSize = Keyboard.theme.keyTextSize.toFloat()
    var upperSize = Keyboard.theme.keyUpperTextSize.toFloat()
    var bold = false


    private val text: String
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
}

class IconKey(private val icon: Drawable): Key() {
    override fun drawContent(canvas: Canvas, paint: Paint) {
        val drawableX = (width-icon.intrinsicWidth)/2
        val drawableY = (height-icon.intrinsicHeight)/2
        canvas.translate(drawableX, drawableY)
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(canvas)
        canvas.translate(-drawableX, -drawableY)
    }
}
