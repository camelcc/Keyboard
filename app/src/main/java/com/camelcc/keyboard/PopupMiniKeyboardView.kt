package com.camelcc.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class PopupMiniKeyboardView: View {
    lateinit var key: PreviewTextKey

    var clickListener: PopupMiniKeyboardViewListener? =null

    private val mPaint = Paint()
    private val keyHeight = Keyboard.theme.popupKeyHeight


    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        background = context.getDrawable(R.drawable.roundcornor_rect)
        clipToOutline = true

        mPaint.isAntiAlias = true
        mPaint.textSize = .0f
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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }
}

interface PopupMiniKeyboardViewListener {
    fun onText(text: String)
}
