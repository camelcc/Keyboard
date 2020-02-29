package com.camelcc.keyboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable

open class Key(val theme: KeyboardTheme) {
    var x = .0f
    var y = .0f
    var width = .0f
    var height = .0f

    var keyColor = theme.keyColor

    fun paint(canvas: Canvas, paint: Paint) {
        canvas.translate(x, y)

        val style = paint.style
        val color = paint.color
        paint.style = Paint.Style.STROKE
        paint.color = theme.keyBorderColor
        paint.strokeWidth = theme.keyBorderWidth.toFloat()
        canvas.drawRoundRect(1.0f, 1.0f,
            width-1.0f, height+1.0f,
            theme.keyBorderRadius.toFloat(), theme.keyBorderRadius.toFloat(), paint)
        paint.style = Paint.Style.FILL
        paint.color = keyColor
        paint.strokeWidth = .0f
        canvas.drawRoundRect(.0f, .0f,
            width, height,
            theme.keyBorderRadius.toFloat(), theme.keyBorderRadius.toFloat(), paint)
        paint.style = style
        paint.color = color

        drawContent(canvas, paint)

        canvas.translate(-x, -y)
    }

    open fun drawContent(canvas: Canvas, paint: Paint) {}
}

class TextKey(theme: KeyboardTheme, private val text: String): Key(theme) {
    var textSize = theme.keyTextSize.toFloat()
    var bold = false

    override fun drawContent(canvas: Canvas, paint: Paint) {
        paint.textSize = textSize
        if (bold) {
            paint.typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(text,
            width/2,
            height/2-(paint.descent()+paint.ascent())/2, paint)
        if (bold) {
            paint.typeface = Typeface.DEFAULT
        }
    }
}

class IconKey(theme: KeyboardTheme, private val icon: Drawable): Key(theme) {
    override fun drawContent(canvas: Canvas, paint: Paint) {
        val drawableX = (width-icon.intrinsicWidth)/2
        val drawableY = (height-icon.intrinsicHeight)/2
        canvas.translate(drawableX, drawableY)
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(canvas)
        canvas.translate(-drawableX, -drawableY)
    }
}

class TextUpperKey(theme: KeyboardTheme,
                   private val text: String,
                   private val upper: String): Key(theme) {
    var textSize = theme.keyTextSize.toFloat()
    var upperSize = theme.keyUpperTextSize.toFloat()

    override fun drawContent(canvas: Canvas, paint: Paint) {
        paint.textSize = textSize
        canvas.drawText(text,
            width/2,
            height/2-(paint.descent()+paint.ascent())/2, paint)


        paint.textSize = upperSize
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(upper,
            width - paint.measureText(upper),
            paint.textSize, paint)
        paint.typeface = Typeface.DEFAULT
    }
}
