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
import kotlin.math.min

class CandidateView(context: Context) : View(context) {
    var listener: KeyboardListener? = null

    private val paint: Paint = Paint()

    private var showComposing = false
    private val composingPopup: PopupWindow
    private val composingView: ComposingTextView

    private var showMore = false
    private var expanded = false
    private val expandDrawable: Drawable
    private val dismissDrawable: Drawable

    private var textSize = KeyboardTheme.candidateTextSize
    private var activeIndex = -1
    private var candidates = listOf<String>()
    private var candidatesWidth = mutableListOf<Int>()

    init {
        setBackgroundColor(Color.TRANSPARENT)
        expandDrawable = context.getDrawable(R.drawable.ic_keyboard_arrow_down_24dp)!!
        dismissDrawable = context.getDrawable(R.drawable.ic_keyboard_arrow_up_24dp)!!
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.textSize = KeyboardTheme.candidateTextSize
        paint.strokeWidth = .0f
        setWillNotDraw(false)
        composingView = ComposingTextView(context)
        composingPopup = PopupWindow(context)
        composingPopup.isClippingEnabled = false
        composingPopup.setBackgroundDrawable(null)
        composingPopup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        composingPopup.isTouchable = false
        composingPopup.contentView = composingView
    }

    fun resetDisplayStyle(showComposing: Boolean, showMore: Boolean) {
        this.showMore = showMore
        this.showComposing = showComposing

        activeIndex = -1
        candidates = listOf()
        candidatesWidth = mutableListOf()
        invalidate()

        if (!showComposing) {
            closing()
        }
    }

    fun setSuggestions(suggestions: List<String>, composing: String = "") {
        candidates = suggestions
        expanded = false

        composingView.composing = composing

        invalidate()
        requestLayout() // relayout fake children (text) views

        if (showComposing) {
            composingPopup.height = composingView.calculatedHeight
            composingPopup.width = composingView.calculatedWidth

            val coordinates = IntArray(2)
            getLocationInWindow(coordinates)
            if (composingPopup.isShowing) {
                composingPopup.update(coordinates[0],
                    coordinates[1]-composingPopup.height,
                    composingPopup.width,
                    composingPopup.height)
            } else {
                composingPopup.showAtLocation(this, Gravity.START or Gravity.TOP,
                    coordinates[0],
                    coordinates[1]-composingPopup.height)
            }
        } else {
            composingPopup.dismiss()
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
        if (composingPopup.isShowing) {
            composingPopup.dismiss()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val candidateHeight = (paddingTop + paddingBottom +
                2*KeyboardTheme.candidateTextPadding +
                KeyboardTheme.candidateTextSize).toInt().coerceAtLeast(KeyboardTheme.candidateHeight)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(candidateHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d("[CandidateView]", "CandidateView onLayout")
        super.onLayout(changed, left, top, right, bottom)
        if (candidates.isEmpty()) {
            candidatesWidth = mutableListOf()
            return
        }
        textSize = KeyboardTheme.pinyinTextSize
        paint.textSize = textSize

        var availableWidth = right-left-2*KeyboardTheme.candidateViewPadding
        var pickedIndex = 0
        var textWidth = 0
        while (pickedIndex < candidates.size && textWidth <= availableWidth) {
            pickedIndex++
            textWidth += (paint.measureText(candidates[pickedIndex-1])+2*KeyboardTheme.pinyinTextPadding+1.dp2px).toInt()
        }
        if (pickedIndex < min(3, candidates.size)) {
            pickedIndex = min(3, candidates.size)
            textWidth = candidates.subList(0, pickedIndex).map { (paint.measureText(it)+2*KeyboardTheme.pinyinTextPadding).toInt() }.sum()
            textWidth += 1.dp2px * (pickedIndex-1)
        }
        // pick at least 3 if possible
        if (pickedIndex < candidates.size && showMore) {
            val moreWidth = expandDrawable.intrinsicWidth + 2*KeyboardTheme.candidateTextSize
            availableWidth -= moreWidth.toInt()
        }
        if (pickedIndex == 0 || pickedIndex == 1) { // rare case, should not happen if engine is good
            candidates = listOf(candidates[0])
            candidatesWidth = mutableListOf(availableWidth)
            return
        }
        // have at least one candidate, back one suggestion if no space, only possible when more key
        while (textWidth > availableWidth && pickedIndex > 3) {
            pickedIndex--
            textWidth = candidates.subList(0, pickedIndex).map { (paint.measureText(it)+2*KeyboardTheme.pinyinTextPadding).toInt() }.sum()
            textWidth += 1.dp2px * (pickedIndex-1)
        }
        // shrink text size if needed
        while (textWidth > availableWidth) {
            if (textSize <= KeyboardTheme.candidateMinTextSize) {
                if (pickedIndex == 1) {
                    textSize = KeyboardTheme.candidateMinTextSize
                    break
                } else {
                    textSize = KeyboardTheme.candidateTextSize
                    pickedIndex--
                }
            } else {
                textSize -= 1.dp2px
            }
            paint.textSize = textSize
            textWidth = candidates.subList(0, pickedIndex).map { (paint.measureText(it)+2*KeyboardTheme.pinyinTextPadding).toInt() }.sum()
            textWidth += 1.dp2px * (candidates.size-1)
        }

        // picked >= 1, real layout each candidate
        candidatesWidth = mutableListOf()
        val more = (availableWidth-textWidth-pickedIndex.dp2px).coerceAtLeast(0)/pickedIndex
        for (i in 0 until pickedIndex) {
            var width = (2*KeyboardTheme.pinyinTextPadding + paint.measureText(candidates[i])).toInt().coerceAtMost(availableWidth)
            width += more
            candidatesWidth.add(width)
        }
    }

    override fun onDraw(canvas: Canvas) {
        Log.d("[CandidateView]", "CandidateView onDraw")
        super.onDraw(canvas)
        if (candidatesWidth.size > candidates.size) { // should not happen
            requestLayout()
            return
        }

        paint.style = Paint.Style.FILL
        paint.color = KeyboardTheme.background
        canvas.drawRect(.0f, .0f, width.toFloat(), height.toFloat(), paint)
        if (candidates.isNullOrEmpty()) {
            return
        }

        var x = paddingLeft + KeyboardTheme.candidateViewPadding
        paint.textSize = textSize
        for (i in candidatesWidth.indices) {
            if (i > 0) {
                canvas.drawLine(x.toFloat(), KeyboardTheme.candidateVerticalPadding.toFloat(), x+1.dp2px.toFloat(), height.toFloat()- KeyboardTheme.candidateVerticalPadding, paint)
                x += 1.dp2px
            }

            if (i == activeIndex) {
                paint.color = KeyboardTheme.candidatePickedColor
                canvas.drawRect(x.toFloat(), .0f, (x+candidatesWidth[i]).toFloat(), height.toFloat(), paint)
            }

            paint.color = Color.BLACK
            val tw = paint.measureText(candidates[i]).coerceAtMost(candidatesWidth[i].toFloat())
            val ty = height/2 - (paint.descent()+paint.ascent())/2
            canvas.drawText(candidates[i], x + candidatesWidth[i]/2.0f-tw/2.0f, ty, paint)

            x += candidatesWidth[i]
        }
        if (candidatesWidth.size < candidates.size && showMore) {
            canvas.drawLine(x.toFloat(), KeyboardTheme.candidateVerticalPadding.toFloat(), x+1.dp2px.toFloat(), height.toFloat()- KeyboardTheme.candidateVerticalPadding, paint)
            x += 1.dp2px

            val drawableX = (x + (width-x-expandDrawable.intrinsicWidth)/2).toFloat()
            val drawableY = ((height-expandDrawable.intrinsicHeight)/2).toFloat()
            canvas.translate(drawableX, drawableY)
            if (expanded) {
                dismissDrawable.setBounds(0, 0, dismissDrawable.intrinsicWidth, dismissDrawable.intrinsicHeight)
                dismissDrawable.draw(canvas)
            } else {
                expandDrawable.setBounds(0, 0, expandDrawable.intrinsicWidth, expandDrawable.intrinsicHeight)
                expandDrawable.draw(canvas)
            }
            canvas.translate(-drawableX, -drawableY)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        var index = -1
        var w = 0
        for (i in candidatesWidth.indices) {
            if (w + candidatesWidth[i] >= x) {
                index = i
                break
            } else {
                w += candidatesWidth[i] + if (i > 0) 1.dp2px else 0
            }
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if (activeIndex != index) {
                    activeIndex = index
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (index == activeIndex) {
                    if (index == -1) {
                        expanded = !expanded
                        if (expanded) {
                            listener?.showMoreCandidates()
                        } else {
                            listener?.dismissMoreCandidates()
                        }
                    } else {
                        listener?.onCandidate(candidates[index], index)
                        expanded = false
                    }
                }
                activeIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                activeIndex = -1
                invalidate()
            }
            else -> {}
        }
        return true
    }
}
