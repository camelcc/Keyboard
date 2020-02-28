package com.camelcc.keyboard

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable

interface Key {
    var x: Int
    var y: Int
    var width: Int
    var height: Int

    fun paint(canvas: Canvas, paint: Paint)
}

open class TextKey(val text: CharSequence): Key {
    override var x: Int = 0
    override var y: Int = 0
    override var height: Int = 0
    override var width: Int = 0

    val radius = 4.dp2px
    var size = 26.dp2px

    override fun paint(canvas: Canvas, paint: Paint) {
        canvas.translate(x.toFloat(), y.toFloat())

        val style = paint.style
        val color = paint.color

        paint.style = Paint.Style.STROKE
        paint.color = 0xFFA9ABAD.toInt()
        paint.strokeWidth = 2.0f
        canvas.drawRoundRect(1.0f, 1.0f,
            width.toFloat()-1.0f, height.toFloat()+1.0f,
            radius.toFloat(), radius.toFloat(), paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.strokeWidth = .0f
        canvas.drawRoundRect(.0f, .0f,
            width.toFloat(), height.toFloat(),
            radius.toFloat(), radius.toFloat(), paint)

        paint.color = color
        paint.style = style
        paint.textSize = size.toFloat()
        canvas.drawText(text.toString(),
            width.toFloat()/2,
            height/2-(paint.descent()+paint.ascent())/2, paint)
        canvas.translate(-x.toFloat(), -y.toFloat())
    }
}

open class IconKey(val icon: Drawable): Key {
    override var x: Int = 0
    override var y: Int = 0
    override var height: Int = 0
    override var width: Int = 0

    private val radius = 4.dp2px

    override fun paint(canvas: Canvas, paint: Paint) {
        canvas.translate(x.toFloat(), y.toFloat())

        val style = paint.style
        val color = paint.color

        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRoundRect(1.0f, 1.0f,
            width.toFloat()-1.0f, height.toFloat()+1.0f,
            radius.toFloat(), radius.toFloat(), paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRoundRect(.0f, .0f, width.toFloat(), height.toFloat(), 4.dp2px.toFloat(), 4.dp2px.toFloat(), paint)

        paint.color = color
        paint.style = style
        val drawableX = (width-icon.intrinsicWidth)/2
        val drawableY = (height-icon.intrinsicHeight)/2
        canvas.translate(drawableX.toFloat(), drawableY.toFloat())
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(canvas)
        canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())
        canvas.translate(-x.toFloat(), -y.toFloat())
    }
}

class TextUpperKey(text: CharSequence, private val upperText: CharSequence): TextKey(text) {
    override fun paint(canvas: Canvas, paint: Paint) {
        super.paint(canvas, paint)
    }
}

class EnterKey(icon: Drawable): IconKey(icon) {
    override fun paint(canvas: Canvas, paint: Paint) {
        super.paint(canvas, paint)
    }
}

class EmojiKey(icon: Drawable, private val text: CharSequence): IconKey(icon) {
    var size = 26.dp2px

    override fun paint(canvas: Canvas, paint: Paint) {
        canvas.translate(x.toFloat(), y.toFloat())

        val style = paint.style
        val color = paint.color
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRoundRect(.0f, .0f, width.toFloat(), height.toFloat(), 4.dp2px.toFloat(), 4.dp2px.toFloat(), paint)

        paint.color = color
        paint.style = style
        val drawableX = (width-icon.intrinsicWidth)/2
        val drawableY = (height/2-icon.intrinsicHeight)/2
        canvas.translate(drawableX.toFloat(), drawableY.toFloat())
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(canvas)
        canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())

        paint.textSize = size.toFloat()
        canvas.drawText(text.toString(),
            width.toFloat()/2,
            height/2+(size-paint.descent()), paint)
        canvas.translate(-x.toFloat(), -y.toFloat())
    }
}

class SpaceKey(text: CharSequence): TextKey(text) {
    override fun paint(canvas: Canvas, paint: Paint) {
        val paintColor = paint.color
        paint.color = Color.LTGRAY
        paint.color = paintColor
    }
}
