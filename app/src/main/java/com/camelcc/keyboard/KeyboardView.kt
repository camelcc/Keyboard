package com.camelcc.keyboard

import android.R.attr
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.max


class KeyboardView: View {
    companion object {
        const val NOT_A_KEY = -1
        const val DEBOUNCE_TIME = 70
        const val MSG_SHOW_PREVIEW = 1
        const val MSG_REMOVE_PREVIEW = 2
        const val MSG_REPEAT = 3
        const val REPEAT_INTERVAL = 50L // 20 keys per second
        const val REPEAT_START_DELAY = 400L
        const val DELAY_BEFORE_PREVIEW = 0L
        const val DELAY_AFTER_PREVIEW = 70L
    }
    private var mKeyboard: Keyboard? = null
    private var mCurrentKeyIndex: Key? = null

    private val mShowPreview = true
    private lateinit var mPreviewPopup:PopupWindow
    private lateinit var mPreviewContainer: View
    private lateinit var mPreviewText: TextView

    // 一个touch序列中(down)，起始的位置
    private var mStartX = 0
    private var mStartY = 0

    private var mPaint = Paint()

    private var mLastKey: Key? = null
    private var mCurrentKey: Key? = null
//    private var mDownKey = NOT_A_KEY
    private lateinit var mGestureDetector: GestureDetector

    // #
    private var mRepeatKey: Key? = null
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

    private lateinit var mHandler: Handler

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        mPaint.isAntiAlias = true
        mPaint.textSize = .0f
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        mPreviewPopup = PopupWindow(context)
        mPreviewPopup.setBackgroundDrawable(null)
        mPreviewPopup.isClippingEnabled = false

        mPreviewContainer = layoutInflater.inflate(R.layout.keyboard_preview, null)
        mPreviewText = mPreviewContainer.findViewById(R.id.preview_text)

        mPreviewPopup.contentView = mPreviewContainer
        mPreviewPopup.isTouchable = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mGestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onDoubleTapEvent(ev: MotionEvent): Boolean {
                if (ev.action != MotionEvent.ACTION_UP) {
                    return false
                }

                val touchX = (ev.x - paddingLeft).toInt()
                val touchY = (ev.y - paddingTop).toInt()
                val key = mKeyboard?.getKey(touchX, touchY) ?: return false
                return mKeyboard?.onDoubleClick(key) ?: return false
            }
        })
        mGestureDetector.setIsLongpressEnabled(false)
        mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_SHOW_PREVIEW -> {
                        showKey(msg.obj as Key)
                    }
                    MSG_REMOVE_PREVIEW -> {
                        mPreviewContainer.visibility = GONE
                    }
                    MSG_REPEAT -> {
                        if (mRepeatKey != null && mCurrentKey == mRepeatKey) {
                            mKeyboard?.onClick(mRepeatKey!!)
                        }
                        val repeat = Message.obtain(this, MSG_REPEAT)
                        sendMessageDelayed(repeat, REPEAT_INTERVAL)
                    }
                    else -> {}
                }
            }
        }
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
//        Log.i("[SK]", "[KeyboardView] onTouchEvent: ${ev.action.action}, oc: $mOldPointerCount, c: $pointerCount")
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

//        Log.i("[SK]", "[KeyboardView] onModifiedTouchEvent: ${action.action}")

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
            showPreview(null)
            mHandler.removeMessages(MSG_REPEAT)
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
                if (key.repeatable) {
                    mRepeatKey = key
                    val msg = mHandler.obtainMessage(MSG_REPEAT)
                    mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY)
                    mKeyboard?.onClick(key)
                    // Delivering the key could have caused an abort
                    if (mAbortKey) {
                        mRepeatKey = null
                    }
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
                removeMessages()
                if (key == mCurrentKey) {

                } else {
                    mLastKey = mCurrentKey
                    mCurrentKey = key
                }
                showPreview(null)
                // If we're not on a repeating key (which sends on a DOWN event)
                if (mRepeatKey == null && !mAbortKey) {
                    mCurrentKey?.let {
                        mKeyboard?.onClick(it)
                    }
                }
                invalidateKey(key)
                mRepeatKey = null
            }
            MotionEvent.ACTION_CANCEL -> {
                removeMessages()
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
        if (oldKey != mCurrentKeyIndex && mShowPreview) {
            mHandler.removeMessages(MSG_SHOW_PREVIEW)
            if (mPreviewPopup.isShowing) {
                if (key == null) {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_REMOVE_PREVIEW),
                        DELAY_AFTER_PREVIEW)
                }
            }
            key?.let {
                if (mPreviewPopup.isShowing && mPreviewText.visibility == VISIBLE) {
                    // Show right away, if it's already visible and finger is moving around
                    showKey(it)
                } else {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SHOW_PREVIEW, it), DELAY_BEFORE_PREVIEW)
                }
            }
        }
    }

    private fun showKey(key: Key) {
        if (key !is TextKey) {
            mPreviewContainer.visibility = INVISIBLE
            return
        }

        mPreviewText.text = key.text
//        mPreviewText.textSize = Keyboard.theme.keyTextSize.toFloat()
        mPreviewText.typeface = Typeface.DEFAULT
        val popupWidth = (key.width + Keyboard.theme.keyGap).toInt()
        val popupHeight = 115.dp2px
//        var previewLP = mPreviewText.layoutParams
//        if (previewLP != null) {
//            previewLP = ViewGroup.LayoutParams(popupWidth, popupHeight)
//        }
//        previewLP.width = popupWidth
//        previewLP.height = popupHeight
//        mPreviewContainer.layoutParams = previewLP

        var popupX = key.x - Keyboard.theme.keyGap/2
        var popupY = key.y + key.height - popupHeight

        mHandler.removeMessages(MSG_REMOVE_PREVIEW)
        val coordinates = IntArray(2)
        getLocationInWindow(coordinates)
        popupX += coordinates[0]
        popupY += coordinates[1]

        if (mPreviewPopup.isShowing) {
            mPreviewPopup.update(popupX.toInt(), popupY.toInt(), popupWidth, popupHeight)
        } else {
            mPreviewPopup.width = popupWidth
            mPreviewPopup.height = popupHeight
            mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY, popupX.toInt(), popupY.toInt())
        }
        mPreviewContainer.visibility = VISIBLE
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
        val keys = mKeyboard?.keys ?: listOf()
        val invalidKey = mInvalidatedKey

//        Log.i("[SK]", "[KeyboardView] onBufferDraw")

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

    private fun removeMessages() {
        if (::mHandler.isInitialized) {
            mHandler.removeMessages(MSG_REPEAT)
        }
    }

    private fun closing() {
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
        removeMessages()

        mBuffer = null
        mCanvas = null
    }

    fun invalidateAllKeys() {
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
        if (mKeyboard != null) {
            showPreview(null)
        }
        // Remove any pending messages
        removeMessages()
        mKeyboard = keyboard
        background = Keyboard.theme.background
        requestLayout()
        // Hint to reallocate the buffer if the size changed
        mKeyboardChanged = true
        invalidateAllKeys()
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        mAbortKey = true
    }
}
