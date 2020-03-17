package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class KeyboardView: View {
    companion object {
        const val NOT_A_KEY = -1
        const val DEBOUNCE_TIME = 70
    }
    private var mKeyboard: Keyboard? = null
    private var mCurrentKeyIndex: Key? = null

    private var mKeys = listOf<Key>()

    // 一个touch序列中(down)，起始的位置
    private var mStartX = 0
    private var mStartY = 0

    private var mPaint = Paint()

    private var mLastKey: Key? = null
    private var mCurrentKey: Key? = null
//    private var mDownKey = NOT_A_KEY
    private val mGestureDetector: GestureDetector

    // #
    private var mRepeatKeyIndex: Key? = null
    private var mAbortKey = true
    private var mInvalidatedKey: Key? = null
    private var mPossiblePoly = false
    private val mSwipeTracker = SwipeTracker()

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

        mGestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })
        mGestureDetector.setIsLongpressEnabled(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
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
//        Log.i("[SK]", "[KeyboardView]: onDraw")
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

    private var mOldPointerCount = 1
    private var mOldPointerX = .0f
    private var mOldPointerY = .0f
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Convert multi-pointer up/down events to single up/down events to
        // deal with the typical multi-pointer behavior of two thumb typing
        val pointerCount = ev.pointerCount
        val action = ev.action
        var result = false
        val now = ev.eventTime
        Log.i("[SK]", "[KeyboardView] onTouchEvent: ${ev.action}, oc: $mOldPointerCount, c: $pointerCount")
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
        val touchX = (ev.x - paddingLeft).toInt()
        val touchY = (ev.y - paddingTop).toInt()
        val action = ev.action
        val evTime = ev.eventTime
        val key = mKeyboard?.getKey(touchX, touchY) ?: return true // consume
        mPossiblePoly = possiblePoly

        Log.i("[SK]", "[KeyboardView] onModifiedTouchEvent: $action")

        // Track the last few movements to look for spurious swipes
        if (action == MotionEvent.ACTION_DOWN) {
            mSwipeTracker.clear()
        }
        mSwipeTracker.addMovement(ev)

        // Ignore all motion events until a DOWN
        if (mAbortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        if (mGestureDetector.onTouchEvent(ev)) {
            // TODO: remove repeat and long press timer
            return true
        }

        // Needs to be called after the gesture detector gets a turn, as it may have
        // displayed the mini keyboard
        // TODO:

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mAbortKey = false
                mStartX = touchX
                mStartY = touchY
                mLastKey = null
                mCurrentKey = key
//                mDownKey = keyIndex
                // TODO: onPress key
                mCurrentKey?.let {
                    if (it.repeatable) {
                        // TODO: repeat key
                    }
                    // TODO: Long press
                }
                showPreview(key)
            }
            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (mCurrentKey == null) {
                    mCurrentKey = key
                } else {
                    if (key == mCurrentKey) {
                        continueLongPress = true
                    } else {
                        // TODO: multi tap
                        mLastKey = mCurrentKey
                        mCurrentKey = key
                    }
                }
                if (!continueLongPress) {
                    // Cancel old longpress
                    // TODO: cancel old longpress, and repeating
                }
                showPreview(mCurrentKey)
            }
            MotionEvent.ACTION_UP -> {
                // TODO: remove messages
                if (key == mCurrentKey) {

                } else {
                    mLastKey = mCurrentKey
                    mCurrentKey = key
                }
                showPreview(null)
                // If we're not on a repeating key (which sends on a DOWN event)
//                if (mRepeatKeyIndex == NOT_A_KEY && !mAbortKey) {
//                    detectAndSendKey(mCurrentKey, touchX, touchY, evTime)
//                }
                invalidateKey(key)
                mRepeatKeyIndex = null
            }
            MotionEvent.ACTION_CANCEL -> {
                // TODO: remove messages, dismiss popup
                mAbortKey = true
                showPreview(null)
                invalidateKey(mCurrentKey)
            }
        }
        return true
    }

    private fun showPreview(key: Key? = null) {
        val oldKey = mCurrentKeyIndex

        mCurrentKeyIndex = key
        // Release the old key and press the new key
        if (oldKey != mCurrentKeyIndex) {
            oldKey?.let {
                it.onReleased(mCurrentKeyIndex == null)
                invalidateKey(it)
                //TODO: accessibility
            }
            mCurrentKeyIndex?.let {
                it.onPressed()
                invalidateKey(it)
                //TODO: accessibility
            }
        }
        // If key changed and preview is on
    }

    private fun detectAndSendKey(index: Int, x: Int, y: Int, evTime: Long) {
        if (index != NOT_A_KEY && index < mKeys.size) {

        }
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

        Log.i("[SK]", "[KeyboardView] onBufferDraw")

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

    private fun invalidateKey(key: Key?) {
        if (key == null) {
            return
        }
        mInvalidatedKey = key
        mDirtyRect.union((key.x + paddingLeft).toInt(),
            (key.y + paddingTop).toInt(),
            (key.x + key.width + paddingLeft).toInt(),
            (key.y + key.height + paddingTop).toInt())
        onBufferDraw()
        invalidate()
    }

    fun setKeyboard(keyboard: Keyboard) {
        mKeyboard = keyboard
        background = Keyboard.theme.background
        mKeys = keyboard.keys
        requestLayout()
        // Hint to reallocate the buffer if the size changed
        mKeyboardChanged = true
        invalidateAllKeys()
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        mAbortKey = true
    }

    // TODO: testing purpose
    fun test() {
        mKeyboard?.let {
            it.test()
//            mKeys = it.keys
//            invalidateAllKeys()
        }
    }
}
