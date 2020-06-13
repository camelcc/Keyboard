package com.camelcc.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View

class CandidateView: View {
    interface CandidateViewListener {
        fun onSuggestion(text: String, index: Int, fromCompletion: Boolean)
    }

    var listener: CandidateViewListener? = null

    private val paint: Paint = Paint()
    private val moreDrawable: Drawable

    private var mShowComposing = false
    private var mShowDropDownAction = false

    private var mComposing = ""

    private var mActiveIndex = -1
    private var mFromCompletion = false
    private var mSuggestions = listOf<String>()
    private var mSuggestionsTextSize = KeyboardTheme.candidateTextSize
    private var mSuggestionsWidth = mutableListOf<Int>()

    private var mTypedWordValid = true

    constructor(context: Context): super(context) {
        setBackgroundColor(Color.TRANSPARENT)

        moreDrawable = context.getDrawable(R.drawable.ic_keyboard_arrow_down_24dp)!!

        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.textSize = KeyboardTheme.candidateTextSize
        paint.strokeWidth = .0f
        setWillNotDraw(false)
//        setPadding(KeyboardTheme.candidateViewPadding, 0, KeyboardTheme.candidateViewPadding, 0)
    }

    fun resetDisplayStyle(showComposing: Boolean, showDropDownAction: Boolean) {
        mShowComposing = showComposing
        mShowDropDownAction = showDropDownAction

        mActiveIndex = -1
        mSuggestions = listOf()
        mComposing = ""
        mSuggestionsWidth = mutableListOf()
        invalidate()
    }

    fun setSuggestions(suggestions: List<String>, fromCompletion: Boolean, typedWordValid: Boolean, composing: String = "") {
        mFromCompletion = fromCompletion
        mSuggestions = suggestions
        mTypedWordValid = typedWordValid
        mComposing = composing
        invalidate()
        requestLayout() // relayout fake children (text) views
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d("[SK]", "CandidateView onMeasure")

        val composingHeight = (2*KeyboardTheme.pinyinComposingPadding +
                KeyboardTheme.pinyinComposingTextSize).toInt().coerceAtLeast(KeyboardTheme.pinyinComposingHeight)
        val candidateHeight = (paddingTop + paddingBottom +
                2*KeyboardTheme.candidateTextPadding +
                KeyboardTheme.candidateTextSize).toInt().coerceAtLeast(KeyboardTheme.candidateHeight)
        setMeasuredDimension(resolveSize(50, widthMeasureSpec),
            resolveSize(composingHeight + candidateHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d("[SK]", "CandidateView onLayout")
        super.onLayout(changed, left, top, right, bottom)
        if (mSuggestions.isEmpty()) {
            mSuggestionsWidth = mutableListOf()
            return
        }
        var availableWidth = right-left-2*KeyboardTheme.candidateViewPadding

        if (!mShowDropDownAction) {
            var texts = if (mSuggestions.size > 3) {
                listOf(mSuggestions[0], mSuggestions[1], mSuggestions[2]) } else mSuggestions
            var textSize = KeyboardTheme.candidateTextSize
            while (!fit(texts, textSize, availableWidth)) {
                if (textSize <= KeyboardTheme.candidateMinTextSize) {
                    textSize = KeyboardTheme.candidateTextSize
                    if (texts.size == 1) {
                        textSize = KeyboardTheme.candidateMinTextSize
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
        } else {
            paint.textSize = KeyboardTheme.pinyinTextSize
            var pickedIndex = 0
            var usedWidth = 0
            while (usedWidth <= availableWidth && pickedIndex < mSuggestions.size) {
                var width = (2*KeyboardTheme.pinyinTextPadding + paint.measureText(mSuggestions[pickedIndex])).toInt()
                if (pickedIndex > 0) {
                    width += 1
                }
                if (usedWidth + width > availableWidth) {
                    break
                }
                usedWidth += width
                pickedIndex++
            }
            assert(pickedIndex <= mSuggestions.size)
            // leave space for more key
            if (pickedIndex < mSuggestions.size) {
                val moreWidth = moreDrawable.intrinsicWidth + 2*KeyboardTheme.candidateTextSize
                availableWidth -= moreWidth.toInt()
            }
            if (pickedIndex == 0 || pickedIndex == 1) { // rare case, should not happen if engine is good
                mSuggestions = listOf(mSuggestions[0])
                mSuggestionsWidth = mutableListOf(availableWidth)
                return
            }
            // have at least one candidate, back one suggestion is no space, only possible when more key
            if (usedWidth > availableWidth && pickedIndex > 1) {
                pickedIndex--
                var width = (2*KeyboardTheme.pinyinTextPadding + paint.measureText(mSuggestions[pickedIndex])).toInt()
                if (pickedIndex > 0) {
                    width += 1
                }
                usedWidth -= width
            }
            // picked >= 1, real layout each candidate
            mSuggestionsWidth = mutableListOf()
            val more = (availableWidth-usedWidth).coerceAtLeast(0)/pickedIndex
            for (i in 0 until pickedIndex) {
                var width = (2*KeyboardTheme.pinyinTextPadding + paint.measureText(mSuggestions[i])).toInt()
                if (pickedIndex > 0) {
                    width += 1
                }
                width += more
                mSuggestionsWidth.add(width)
            }
        }
    }

    private fun fit(texts: List<String>, textSize: Float, width: Int): Boolean {
        if (texts.isEmpty()) {
            return true
        }
        paint.textSize = textSize
        val maxWidth = texts.map { paint.measureText(it)+2*KeyboardTheme.candidateTextPadding }.max() ?: .0f
        return maxWidth*texts.size + (texts.size - 1) * 1.dp2px <= width
    }

    override fun onDraw(canvas: Canvas) {
        Log.d("[SK]", "CandidateView onDraw")
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        val composingHeight = (2*KeyboardTheme.pinyinComposingPadding +
                KeyboardTheme.pinyinComposingTextSize).toInt().coerceAtLeast(KeyboardTheme.pinyinComposingHeight)

        if (mShowComposing && mComposing.isNotBlank()) {
            paint.color = KeyboardTheme.background
            canvas.drawRect(.0f, .0f, 2*KeyboardTheme.pinyinComposingPadding + paint.measureText(mComposing), composingHeight.toFloat(), paint)

            paint.color = Color.BLACK
            paint.textSize = KeyboardTheme.pinyinComposingTextSize
            canvas.drawText(mComposing, KeyboardTheme.pinyinComposingPadding.toFloat(),
                composingHeight/2-(paint.descent()+paint.ascent())/2, paint)
        }

        paint.color = KeyboardTheme.background
        canvas.drawRect(.0f, composingHeight.toFloat(), width.toFloat(), height.toFloat(), paint)
        if (mSuggestions.isNullOrEmpty()) {
            return
        }

        var x = paddingLeft + KeyboardTheme.candidateViewPadding
        if (!mShowDropDownAction) {
            paint.textSize = KeyboardTheme.candidateTextSize
            val w = (width - paddingLeft - paddingRight - (mSuggestions.size-1)*1.dp2px)/mSuggestions.size
            paint.textSize = mSuggestionsTextSize
            for (i in mSuggestions.indices) {
                if (i == mActiveIndex) {
                    paint.color = KeyboardTheme.candidatePickedColor
                    canvas.drawRect(x.toFloat(), composingHeight.toFloat(), (x+w).toFloat(), height.toFloat(), paint)
                }

                paint.color = Color.BLACK
                val tw = paint.measureText(mSuggestions[i])
                canvas.drawText(mSuggestions[i], x + w/2.0f-tw/2.0f,
                    composingHeight + (height-composingHeight)/2-(paint.descent()+paint.ascent())/2, paint)
                x += w

                if (i != mSuggestions.size-1) {
                    canvas.drawLine(x.toFloat(), composingHeight+KeyboardTheme.candidateVerticalPadding.toFloat(), x+1.dp2px.toFloat(), height.toFloat()-KeyboardTheme.candidateVerticalPadding, paint)
                    x += 1.dp2px
                }
            }
        } else {
            paint.textSize = KeyboardTheme.pinyinTextSize
            for (i in mSuggestionsWidth.indices) {
                if (i == mActiveIndex) {
                    paint.color = KeyboardTheme.candidatePickedColor
                    canvas.drawRect(x.toFloat(), composingHeight.toFloat(), (x+mSuggestionsWidth[i]).toFloat(), height.toFloat(), paint)
                }

                paint.color = Color.BLACK
                val tw = paint.measureText(mSuggestions[i]).coerceAtMost(mSuggestionsWidth[i].toFloat())
                val ty = composingHeight + (height-composingHeight)/2 - (paint.descent()+paint.ascent())/2
                canvas.drawText(mSuggestions[i], x + mSuggestionsWidth[i]/2.0f-tw/2.0f, ty, paint)

                x += mSuggestionsWidth[i]

                if (i != mSuggestions.size-1) {
                    canvas.drawLine(x.toFloat(), composingHeight + KeyboardTheme.candidateVerticalPadding.toFloat(), x+1.dp2px.toFloat(), height.toFloat()- KeyboardTheme.candidateVerticalPadding, paint)
                    x += 1.dp2px
                }
            }
            if (mSuggestionsWidth.size < mSuggestions.size) {
                val drawableX = (x + (width-x-moreDrawable.intrinsicWidth)/2).toFloat()
                val drawableY = (composingHeight + (height-composingHeight-moreDrawable.intrinsicHeight)/2).toFloat()
                canvas.translate(drawableX, drawableY)
                moreDrawable.setBounds(0, 0, moreDrawable.intrinsicWidth, moreDrawable.intrinsicHeight)
                moreDrawable.draw(canvas)
                canvas.translate(-drawableX, -drawableY)
            }
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val composingHeight = (2*KeyboardTheme.pinyinComposingPadding +
                KeyboardTheme.pinyinComposingTextSize).toInt().coerceAtLeast(KeyboardTheme.pinyinComposingHeight)
        val x = ev.x
        val y = ev.y
        var index = -1
        if (!mShowDropDownAction) {
            val w = (width - paddingLeft - paddingRight)/mSuggestions.size
            index = x.toInt()/w
        } else {
            var w = 0
            for (i in mSuggestionsWidth.indices) {
                if (w + mSuggestionsWidth[i] >= x) {
                    index = i
                    break
                } else {
                    w += mSuggestionsWidth[i]
                }
            }
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // do not handle first wrong place click
                if (y < composingHeight) {
                    return false
                }

                if (mActiveIndex != index) {
                    mActiveIndex = index
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (index == mActiveIndex) {
                    if (index == -1) {
                        // TODO: drop down action
                    } else {
                        listener?.onSuggestion(mSuggestions[index], index, mFromCompletion)
                    }
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
