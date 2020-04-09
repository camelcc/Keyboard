package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider

class PopupPreviewTextView : View {
    var key: PreviewTextKey = NOT_A_KEY
        set(value) {
            field = value
            invalidate()
        }

    private val mPaint = Paint()
    private val ellipsis = "\u2026"
    private var ellipsisWidth = .0f
    private val keyHeight = Keyboard.theme.popupKeyHeight

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        background = ColorDrawable(Keyboard.theme.popupBackground)
        outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, Keyboard.theme.popupRadius.toFloat())
            }
        }
        clipToOutline = true

        mPaint.isAntiAlias = true
        mPaint.textSize = .0f
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255
        mPaint.typeface = Typeface.DEFAULT

        mPaint.textSize = Keyboard.theme.popupSubscriptionSize
        ellipsisWidth = mPaint.measureText(ellipsis)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (key != NOT_A_KEY) {
            val paint = mPaint
            paint.textSize = Keyboard.theme.popupTextSize
            canvas.drawText(key.text,
                width/2.0f,
                keyHeight/2-(paint.descent()+paint.ascent())/2, paint)

            if (!key.miniKeys.isNullOrEmpty()) {
                paint.textSize = Keyboard.theme.popupSubscriptionSize

                canvas.drawText(ellipsis,
                    width.toFloat()-ellipsisWidth,
                    keyHeight.toFloat(),
                    paint)
            }
        }
    }
}