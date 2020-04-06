package com.camelcc.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class PopupMiniKeyboardView: View {
    var keys: List<String> = listOf()
    var activeIndex: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var clickListener: PopupMiniKeyboardViewListener? =null

    private val mPaint = Paint()

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        background = context.getDrawable(R.drawable.roundcornor_rect)
        clipToOutline = true

        mPaint.isAntiAlias = true
        mPaint.textSize = .0f
        mPaint.color = Color.BLACK
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255
        mPaint.typeface = Typeface.DEFAULT
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        Log.i("[SK]", "[PopupMiniKeyboardView] onTouchEvent: ${ev.action.action}, x: ${ev.x}, y: ${ev.y}")
        when (action) {
            MotionEvent.ACTION_MOVE -> {

            }
            MotionEvent.ACTION_UP -> {
                clickListener?.onText("q")
            }
            else -> { return false }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paint = mPaint
        val singleLine = keys.size <= 5
        val keyWidth = if (singleLine) width/keys.size else width/(keys.size/2)
        val keyHeight = Keyboard.theme.popupKeyHeight
        var x = 0
        var y = 0
        for (i in keys.indices) {
            if (keys[i].isBlank()) {
                x += keyWidth
                continue
            }
            if (!singleLine && i == keys.size/2) {
                x = 0
                y = keyHeight
            }

            val active = i == activeIndex
            paint.textSize = Keyboard.theme.popupTextSize
            if (active) {
                val style = paint.style
                val color = paint.color

                paint.style = Paint.Style.FILL
                paint.color = Color.BLUE
                paint.strokeWidth = .0f
                canvas.drawRoundRect(x.toFloat(), y.toFloat(),
                    x+keyWidth.toFloat(), y+keyHeight.toFloat(),
                    Keyboard.theme.keyBorderRadius.toFloat(), Keyboard.theme.keyBorderRadius.toFloat(), paint)
                paint.style = style
                paint.color = color

                paint.color = Color.WHITE
            } else {
                paint.color = Color.BLACK
            }
            canvas.drawText(
                keys[i],
                (x + keyWidth/2).toFloat(),
                y + keyHeight/2-(paint.descent()+paint.ascent())/2, paint)

            x += keyWidth
        }
    }
}

interface PopupMiniKeyboardViewListener {
    fun onText(text: String)
}
