package com.camelcc.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import androidx.annotation.IntegerRes

class CandidateView: View {
    private var contentHeight = 0
    private val paint: Paint = Paint()

    private var mSuggestions = listOf<String>()
    private var mTypedWordValid = true

    private var mPressed = -1

    constructor(context: Context): super(context) {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.textSize = Keyboard.theme.candidateTextSize.toFloat()
        paint.strokeWidth = .0f
        setWillNotDraw(false)
    }

    fun setSuggestions(suggestions: List<String>, typedWordValid: Boolean) {
        mSuggestions = suggestions
        mTypedWordValid = typedWordValid
        invalidate()
        requestLayout() // relayout fake children (text) views
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.e("[SK]", "CandidateView onMeasure")

        val minHeight = Keyboard.theme.keyHeight + Keyboard.theme.popupMarginBottom
        contentHeight = (paddingTop + paddingBottom + paint.textSize).toInt().coerceAtLeast(Keyboard.theme.candidateHeight)
        setMeasuredDimension(resolveSize(50, widthMeasureSpec),
            resolveSize(minHeight.coerceAtLeast(contentHeight), heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val suggestionWidths = mutableListOf<Float>()
        paint.textSize = Keyboard.theme.candidateTextSize
        for (i in 0 until mSuggestions.size.coerceAtMost(3)) {
            suggestionWidths.add(paint.measureText(mSuggestions[i]) + 2*Keyboard.theme.candidateTextPadding)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val top = height-contentHeight
        paint.color = Keyboard.theme.background
        canvas.drawRect(.0f, top.toFloat(), width.toFloat(), height.toFloat(), paint)


    }
}
