package com.camelcc.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class PopupMiniKeyboardView: View {
    var clickListener: PopupMiniKeyboardViewListener? =null

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        background = ColorDrawable(Color.DKGRAY)
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        Log.i("[SK]", "[PopupMiniKeyboardView] onTouchEvent: ${ev.action.action}, x: ${ev.x}, y: ${ev.y}")
        return super.dispatchTouchEvent(ev)
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
}

interface PopupMiniKeyboardViewListener {
    fun onText(text: String)
}
