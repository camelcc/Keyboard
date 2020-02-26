package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.max

class KeyboardView: View {
    private var mKeyboard: Keyboard? = null

    private var mLabelTextSize: Int = 14.dp2px
    private var mKeyTextSize: Int = 18.dp2px
    private var mKeyTextColor: Int = Color.BLACK

    private var mKeys = listOf<Key>()

    private var mPaint = Paint()

    // #
    private var mInvalidatedKey: Key? = null

    // ## Keyboard drawing
    // Whether the keyboard bitmap to be redrawn before it's blitted.
    private var mDrawPending: Boolean = false
    // The keyboard bitmap for faster updates
    private var mBuffer: Bitmap? = null
    // Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.
    private var mKeyboardChanged: Boolean = false
    // The canvas for the above mutable keyboard bitmap
    private var mCanvas: Canvas? = null
    // The dirty region in the keyboard bitmap
    private var mDirtyRect = Rect()


    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        mPaint.isAntiAlias = true
        mPaint.textSize = .0f
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.i("[SK]", "[KeyboardView]: onMeasure")
        // Always full screen width
        val height = paddingTop+paddingBottom+(mKeyboard?.height ?: 0)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.i("[SK]", "[KeyboardView]: onMeasure, w = $w, h = $h")
        mKeyboard?.resize(w, h)
        // Release the buffer if any and it will be reallocated on the next draw
        mBuffer = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.i("[SK]", "[KeyboardView]: onDraw")
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, .0f, .0f, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i("[SK]", "[KeyboardView]: onDetachedFromWindow, closing")
        closing()
    }

    private fun onBufferDraw() {
        if (mBuffer == null || mKeyboardChanged) {
            if (mBuffer == null || (mKeyboardChanged &&
                   (mBuffer?.width != width || mBuffer?.height != height))) {
                // Make sure our bitmap is at least 1x1
                mBuffer = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
                mBuffer?.let { mCanvas = Canvas(it) }
            }
            invalidateAllKeys()
            mKeyboardChanged = false
        }

        if (mKeyboard == null) {
            return
        }

        mCanvas?.save()
        val canvas = mCanvas!!
        canvas.clipRect(mDirtyRect)

        val paint = mPaint
        val clipRegion = Rect(0, 0, 0, 0)
        //TODO: key padding
        val padding = Rect(0, 0, 0, 0)
        val kbdPaddingLeft = paddingLeft
        val kbdPaddingTop = paddingTop
        val keys = mKeys
        val invalidKey = mInvalidatedKey

        paint.color = mKeyTextColor
        var drawSingleKey = false
        if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
            if (invalidKey.x + kbdPaddingLeft - 1 <= clipRegion.left &&
                    invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top &&
                    invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right &&
                    invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
                drawSingleKey = true
            }
        }
        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
        for (key in keys) {
            if (drawSingleKey && key != invalidKey) {
                continue
            }

            canvas.translate((key.x+kbdPaddingLeft).toFloat(), (key.y+kbdPaddingTop).toFloat())


            val label = key.text
            if (label != null) {
                // For characters, use large font. For labels like "Done", use small font.
                paint.typeface = Typeface.DEFAULT
                paint.textSize = mKeyTextSize.toFloat()
                canvas.drawText(label.toString(),
                    ((key.width-padding.left-padding.right)/2 + padding.left).toFloat(),
                    ((key.height-padding.top-padding.bottom)/2 + (paint.textSize - paint.descent()) + padding.top),
                    paint)
            } else if (key.icon != null) {
                val drawableX = (key.width-padding.left-padding.right-key.icon.intrinsicWidth)/2+padding.left
                val drawableY = (key.height-padding.top-padding.bottom-key.icon.intrinsicHeight)/2+paddingTop
                canvas.translate(drawableX.toFloat(), drawableY.toFloat())
                key.icon.setBounds(0, 0, key.icon.intrinsicWidth, key.icon.intrinsicHeight)
                key.icon.draw(canvas)
                canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())
            }
            canvas.translate((-key.x-kbdPaddingLeft).toFloat(), (-key.y-kbdPaddingTop).toFloat())
        }

        mInvalidatedKey = null

        mCanvas?.restore()
        mDrawPending = false
        mDirtyRect.setEmpty()
    }

    private fun closing() {
        mBuffer = null
        mCanvas = null
    }

    private fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
        invalidate()
    }

    fun setKeyboard(keyboard: Keyboard) {
        mKeyboard = keyboard
        background = ColorDrawable(Color.WHITE)
        mKeys = keyboard.keys
        requestLayout()
        mKeyboardChanged = true
    }
}
