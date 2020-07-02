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

    private val paint = Paint()
    private val ellipsis = "\u2026"
    private var ellipsisWidth = .0f
    private val keyHeight: Int get() = height-KeyboardTheme.popupMarginBottom

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        background = ColorDrawable(KeyboardTheme.popupBackground)
        outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, KeyboardTheme.popupRadius.toFloat())
            }
        }
        clipToOutline = true

        paint.isAntiAlias = true
        paint.textSize = .0f
        paint.textAlign = Paint.Align.CENTER
        paint.alpha = 255
        paint.typeface = Typeface.DEFAULT

        paint.textSize = KeyboardTheme.popupSubscriptionSize
        ellipsisWidth = paint.measureText(ellipsis)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (key != NOT_A_KEY) {
            val paint = paint
            paint.textSize = KeyboardTheme.popupTextSize
            canvas.drawText(key.text,
                width/2.0f,
                keyHeight/2-(paint.descent()+paint.ascent())/2, paint)

            if (!key.miniKeys.isNullOrEmpty()) {
                paint.textSize = KeyboardTheme.popupSubscriptionSize

                canvas.drawText(ellipsis,
                    width.toFloat()-ellipsisWidth,
                    keyHeight.toFloat(),
                    paint)
            }
        }
    }
}