package com.camelcc.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import android.view.View

class CandidateView: View {
    interface CandidateViewListener {
        fun onSuggestion(text: String, index: Int, fromCompletion: Boolean)
    }

    var listener: CandidateViewListener? = null

    private var contentHeight = 0
    private val paint: Paint = Paint()

    private var mFromCompletion = false
    private var mTypedWordValid = true
    private var mSuggestions = listOf<String>()
    private var mSuggestionsTextSize = Keyboard.theme.candidateTextSize

    private var mActiveIndex = -1

    constructor(context: Context): super(context) {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.textSize = Keyboard.theme.candidateTextSize.toFloat()
        paint.strokeWidth = .0f
        setWillNotDraw(false)
        setPadding(Keyboard.theme.candidateViewPadding, 0, Keyboard.theme.candidateViewPadding, 0)
    }

    fun setSuggestions(suggestions: List<String>, fromCompletion: Boolean, typedWordValid: Boolean) {
        mFromCompletion = fromCompletion
        mSuggestions = suggestions
        mTypedWordValid = typedWordValid
        invalidate()
        requestLayout() // relayout fake children (text) views
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d("[SK]", "CandidateView onMeasure")

        contentHeight = (paddingTop + paddingBottom + paint.textSize).toInt().coerceAtLeast(Keyboard.theme.candidateHeight)
        setMeasuredDimension(resolveSize(50, widthMeasureSpec),
            resolveSize(contentHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d("[SK]", "CandidateView onLayout")
        super.onLayout(changed, left, top, right, bottom)
        if (mSuggestions.isEmpty()) {
            return
        }
        val availableWidth = right-left-paddingLeft-paddingRight
        var texts = if (mSuggestions.size > 3) {
            listOf(mSuggestions[0], mSuggestions[1], mSuggestions[2]) } else mSuggestions
        var textSize = Keyboard.theme.candidateTextSize
        while (!fit(texts, textSize, availableWidth)) {
            if (textSize <= Keyboard.theme.candidateMinTextSize) {
                textSize = Keyboard.theme.candidateTextSize
                if (texts.size == 1) {
                    textSize = Keyboard.theme.candidateMinTextSize
                    var text = texts[0]
                    val i = text.indexOf("...")
                    if (i < 0) {
                        text = text.replaceRange((text.length/2-2).coerceAtLeast(0), (text.length/2+2).coerceAtMost(text.length), "...")
                    } else {
                        text = text.replaceRange((i-1).coerceAtLeast(0), (i+4).coerceAtMost(text.length), "...")
                    }
                    texts = listOf(text)
                } else if (texts.size == 2) {
                    texts = listOf(texts[0])
                } else {
                    texts = listOf(texts[0], texts[1])
                }
            } else {
                textSize -= 1.dp2px
            }
        }
        mSuggestionsTextSize = textSize
        mSuggestions = texts
    }

    private fun fit(texts: List<String>, textSize: Float, width: Int): Boolean {
        if (texts.isEmpty()) {
            return true
        }
        paint.textSize = textSize
        val maxWidth = texts.map { paint.measureText(it)+2*Keyboard.theme.candidateTextPadding }.max() ?: .0f
        return maxWidth*texts.size + (texts.size - 1) * 1.dp2px <= width
    }

    override fun onDraw(canvas: Canvas) {
        Log.d("[SK]", "CandidateView onDraw")
        super.onDraw(canvas)

        val textPadding = Keyboard.theme.candidateTextPadding
        val top = height-contentHeight
        paint.style = Paint.Style.FILL
        paint.color = Keyboard.theme.background
        canvas.drawRect(.0f, top.toFloat(), width.toFloat(), height.toFloat(), paint)

        if (mSuggestions.isNullOrEmpty()) {
            return
        }

        var x = paddingLeft
        val w = (width - paddingLeft - paddingRight - (mSuggestions.size-1)*1.dp2px)/mSuggestions.size
        paint.textSize = mSuggestionsTextSize
        paint.color = Color.BLACK
        for (i in mSuggestions.indices) {
            if (i == mActiveIndex) {
                paint.color = Keyboard.theme.candidatePickedColor
                canvas.drawRect(x.toFloat(), top.toFloat(), (x+w).toFloat(), height.toFloat(), paint)
            }

            paint.color = Color.BLACK
            if ((i == 1 && !mTypedWordValid) || (i == 0 && mTypedWordValid)) {
                paint.isFakeBoldText = true
            }
            val tw = paint.measureText(mSuggestions[i])
            canvas.drawText(mSuggestions[i], x + w/2.0f-tw/2.0f,
                top + height/2-(paint.descent()+paint.ascent())/2, paint)
            paint.isFakeBoldText = false
            x += w

            if (i != mSuggestions.size-1) {
                canvas.drawLine(x.toFloat(), top+Keyboard.theme.candidateVerticalPadding.toFloat(), x+1.dp2px.toFloat(), height.toFloat()-Keyboard.theme.candidateVerticalPadding, paint)
                x += 1.dp2px
            }
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val w = (width - paddingLeft - paddingRight)/mSuggestions.size
        val index = ev.x.toInt() / w

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if (mActiveIndex != index) {
                    mActiveIndex = index
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (index == mActiveIndex) {
                    listener?.onSuggestion(mSuggestions[index], index, mFromCompletion)
                }
                mActiveIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                mActiveIndex = -1
                invalidate()
            }
            else -> {}
        }
        return true
    }
}
