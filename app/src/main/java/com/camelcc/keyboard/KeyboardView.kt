package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class KeyboardView: View {
    private var mKeyboard: Keyboard? = null

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
        // Always full screen width
        val height = paddingTop+paddingBottom+(mKeyboard?.height ?: 0)
        Log.i("[SK]", "[KeyboardView]: onMeasure, h = $height, keyboard = $mKeyboard")
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.i("[SK]", "[KeyboardView]: onSizeChanged, w = $w, h = $h")
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

    var mOldPointerCount = 1
    var mOldPointerX = .0f
    var mOldPointerY = .0f

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Convert multi-pointer up/down events to single up/down events to
        // deal with the typical multi-pointer behavior of two thumb typing
        val pointerCount = ev.pointerCount
        val action = ev.action
        var result = false
        val now = ev.eventTime
        if (pointerCount != mOldPointerCount) {
            if (pointerCount == 1) {
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, ev.x, ev.y, ev.metaState)
                result = onModifiedTouchEvent(down, false)
                down.recycle()
                // If it's an up action, then deliver the up as well
                if (action == MotionEvent.ACTION_UP) {
                    result = onModifiedTouchEvent(ev, true)
                }
            } else {
                // Send an up event for the last pointer
                val up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, mOldPointerX, mOldPointerY, ev.metaState)
                result = onModifiedTouchEvent(up, true)
                up.recycle()
            }
        } else {
            if (pointerCount == 1) {
                result = onModifiedTouchEvent(ev, false)
                mOldPointerX = ev.x
                mOldPointerY = ev.y
            } else {
                // Don't do anything when 2 pointers are down and moving
                result = true
            }
        }
        mOldPointerCount = pointerCount
        return result
    }

    private fun onModifiedTouchEvent(ev: MotionEvent, possiblePoly: Boolean): Boolean {
        return true
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
        val keys = mKeys
        val invalidKey = mInvalidatedKey

        paint.color = Color.BLACK
        var drawSingleKey = false
        if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
            if (invalidKey.x - 1 <= clipRegion.left &&
                    invalidKey.y - 1 <= clipRegion.top &&
                    invalidKey.x + invalidKey.width + 1 >= clipRegion.right &&
                    invalidKey.y + invalidKey.height + 1 >= clipRegion.bottom) {
                drawSingleKey = true
            }
        }
        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
        for (key in keys) {
            if (drawSingleKey && key != invalidKey) {
                continue
            }
            key.paint(canvas, paint)
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
        background = Keyboard.theme.background
        mKeys = keyboard.keys
        requestLayout()
        mKeyboardChanged = true
    }

    // TODO: testing purpose
    fun test() {
        mKeyboard?.let {
            it.test()
            mKeys = it.keys
            invalidateAllKeys()
        }
    }
}
