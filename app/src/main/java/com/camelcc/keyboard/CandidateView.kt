package com.camelcc.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import com.camelcc.keyboard.pinyin.ComposingTextView

class CandidateView: View {
    interface CandidateViewListener {
        fun onSuggestion(text: String, index: Int, fromCompletion: Boolean)
        fun onMoreDismiss()
        fun onMoreExpand()
    }

    var listener: CandidateViewListener? = null

    private val paint: Paint = Paint()
    private val moreExpandDrawable: Drawable
    private val moreDismissDrawable: Drawable

    private var mShowComposing = false
    private var mShowDropDownAction = false

    private val mComposingPopup: PopupWindow
    private val mComposingView: ComposingTextView

    private var mActiveIndex = -1
    private var mFromCompletion = false
    private var mSuggestions = listOf<String>()
    private var mSuggestionsTextSize = KeyboardTheme.candidateTextSize
    private var mSuggestionsWidth = mutableListOf<Int>()

    private var mTypedWordValid = true

    private var mMoreExpanded = false

    constructor(context: Context): super(context) {
        setBackgroundColor(Color.TRANSPARENT)

        moreExpandDrawable = context.getDrawable(R.drawable.ic_keyboard_arrow_down_24dp)!!
        moreDismissDrawable = context.getDrawable(R.drawable.ic_keyboard_arrow_up_24dp)!!

        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.textSize = KeyboardTheme.candidateTextSize
        paint.strokeWidth = .0f
        setWillNotDraw(false)
//        setPadding(KeyboardTheme.candidateViewPadding, 0, KeyboardTheme.candidateViewPadding, 0)

        mComposingView = ComposingTextView(context)
        mComposingPopup = PopupWindow(context)
        mComposingPopup.isClippingEnabled = false
        mComposingPopup.setBackgroundDrawable(null)
        mComposingPopup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        mComposingPopup.isTouchable = false
        mComposingPopup.contentView = mComposingView
    }

    fun resetDisplayStyle(showComposing: Boolean, showDropDownAction: Boolean) {
        mShowComposing = showComposing
        mShowDropDownAction = showDropDownAction

        mActiveIndex = -1
        mSuggestions = listOf()
        mSuggestionsWidth = mutableListOf()
        invalidate()

        if (!mShowComposing) {
            mComposingPopup.dismiss()
        }
    }

    fun setSuggestions(suggestions: List<String>, fromCompletion: Boolean, typedWordValid: Boolean, composing: String = "") {
        mFromCompletion = fromCompletion
        mSuggestions = suggestions
        mTypedWordValid = typedWordValid
        mMoreExpanded = false

        mComposingView.setComposing(composing)

        invalidate()
        requestLayout() // relayout fake children (text) views

        if (mShowComposing) {
            mComposingPopup.height = mComposingView.calculatedHeight
            mComposingPopup.width = mComposingView.calculatedWidth

            val coordinates = IntArray(2)
            getLocationInWindow(coordinates)
            if (mComposingPopup.isShowing) {
                mComposingPopup.update(coordinates[0], coordinates[1]-mComposingPopup.height, mComposingPopup.width, mComposingPopup.height)
            } else {
                mComposingPopup.showAtLocation(this, Gravity.LEFT or Gravity.TOP, coordinates[0], coordinates[1]-mComposingPopup.height)
            }
        } else {
            mComposingPopup.dismiss()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        closing()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    private fun closing() {
        if (mComposingPopup.isShowing) {
            mComposingPopup.dismiss()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d("[SK]", "CandidateView onMeasure")

        val candidateHeight = (paddingTop + paddingBottom +
                2*KeyboardTheme.candidateTextPadding +
                KeyboardTheme.candidateTextSize).toInt().coerceAtLeast(KeyboardTheme.candidateHeight)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(candidateHeight, heightMeasureSpec))
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
                val moreWidth = moreExpandDrawable.intrinsicWidth + 2*KeyboardTheme.candidateTextSize
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
        paint.color = KeyboardTheme.background
        canvas.drawRect(.0f, .0f, width.toFloat(), height.toFloat(), paint)
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
                    canvas.drawRect(x.toFloat(), .0f, (x+w).toFloat(), height.toFloat(), paint)
                }

                paint.color = Color.BLACK
                val tw = paint.measureText(mSuggestions[i])
                canvas.drawText(mSuggestions[i], x + w/2.0f-tw/2.0f, height/2-(paint.descent()+paint.ascent())/2, paint)
                x += w

                if (i != mSuggestions.size-1) {
                    canvas.drawLine(x.toFloat(), KeyboardTheme.candidateVerticalPadding.toFloat(), x+1.dp2px.toFloat(), height.toFloat()-KeyboardTheme.candidateVerticalPadding, paint)
                    x += 1.dp2px
                }
            }
        } else {
            paint.textSize = KeyboardTheme.pinyinTextSize
            for (i in mSuggestionsWidth.indices) {
                if (i == mActiveIndex) {
                    paint.color = KeyboardTheme.candidatePickedColor
                    canvas.drawRect(x.toFloat(), .0f, (x+mSuggestionsWidth[i]).toFloat(), height.toFloat(), paint)
                }

                paint.color = Color.BLACK
                val tw = paint.measureText(mSuggestions[i]).coerceAtMost(mSuggestionsWidth[i].toFloat())
                val ty = height/2 - (paint.descent()+paint.ascent())/2
                canvas.drawText(mSuggestions[i], x + mSuggestionsWidth[i]/2.0f-tw/2.0f, ty, paint)

                x += mSuggestionsWidth[i]

                if (i != mSuggestions.size-1) {
                    canvas.drawLine(x.toFloat(), KeyboardTheme.candidateVerticalPadding.toFloat(), x+1.dp2px.toFloat(), height.toFloat()- KeyboardTheme.candidateVerticalPadding, paint)
                    x += 1.dp2px
                }
            }
            if (mSuggestionsWidth.size < mSuggestions.size) {
                val drawableX = (x + (width-x-moreExpandDrawable.intrinsicWidth)/2).toFloat()
                val drawableY = ((height-moreExpandDrawable.intrinsicHeight)/2).toFloat()
                canvas.translate(drawableX, drawableY)
                if (mMoreExpanded) {
                    moreDismissDrawable.setBounds(0, 0, moreDismissDrawable.intrinsicWidth, moreDismissDrawable.intrinsicHeight)
                    moreDismissDrawable.draw(canvas)
                } else {
                    moreExpandDrawable.setBounds(0, 0, moreExpandDrawable.intrinsicWidth, moreExpandDrawable.intrinsicHeight)
                    moreExpandDrawable.draw(canvas)
                }
                canvas.translate(-drawableX, -drawableY)
            }
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
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
                if (mActiveIndex != index) {
                    mActiveIndex = index
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (index == mActiveIndex) {
                    if (index == -1) {
                        mMoreExpanded = !mMoreExpanded
                        if (mMoreExpanded) {
                            listener?.onMoreExpand()
                        } else {
                            listener?.onMoreDismiss()
                        }
                    } else {
                        listener?.onSuggestion(mSuggestions[index], index, mFromCompletion)
                        mMoreExpanded = false
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
