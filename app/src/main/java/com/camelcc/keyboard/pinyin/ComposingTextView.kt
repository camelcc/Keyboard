package com.camelcc.keyboard.pinyin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.camelcc.keyboard.KeyboardTheme

class ComposingTextView : View {
    private val paint: Paint = Paint()

    var composing: String = ""
        set(value) {
            field = value
            requestLayout()
        }

    val calculatedWidth: Int
        get() = (2*KeyboardTheme.pinyinComposingPadding + paint.measureText(composing)).toInt()
    val calculatedHeight: Int
        get() = (2*KeyboardTheme.pinyinComposingPadding +
                KeyboardTheme.pinyinComposingTextSize).toInt().coerceAtLeast(KeyboardTheme.pinyinComposingHeight)

    constructor(context: Context): super(context) {
        setBackgroundColor(KeyboardTheme.background)

        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.textSize = KeyboardTheme.pinyinComposingTextSize
        paint.strokeWidth = .0f
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(calculatedWidth, calculatedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawText(composing, KeyboardTheme.pinyinComposingPadding.toFloat(),
                height/2-(paint.descent()+paint.ascent())/2, paint)
    }
}
