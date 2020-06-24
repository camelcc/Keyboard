package com.camelcc.keyboard.pinyin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.camelcc.keyboard.KeyboardTheme

class ComposingTextView : View {
    private val paint: Paint = Paint()
    private var mComposing = ""

    val calculatedWidth: Int
        get() = (2*KeyboardTheme.pinyinComposingPadding + paint.measureText(mComposing)).toInt()
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

    fun setComposing(composing: String) {
        mComposing = composing
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = (2*KeyboardTheme.pinyinComposingPadding +
                KeyboardTheme.pinyinComposingTextSize).toInt().coerceAtLeast(KeyboardTheme.pinyinComposingHeight)
        val width = 2*KeyboardTheme.pinyinComposingPadding + paint.measureText(mComposing)
        setMeasuredDimension(width.toInt(), height)
    }

    override fun onDraw(canvas: Canvas) {
//        canvas.drawRect(.0f, .0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawText(mComposing, KeyboardTheme.pinyinComposingPadding.toFloat(),
                height/2-(paint.descent()+paint.ascent())/2, paint)
    }
}
