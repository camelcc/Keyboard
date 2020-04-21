package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import kotlin.math.max

class KeyboardView: View {
    companion object {
        const val MSG_SHOW_PREVIEW = 1
        const val MSG_REMOVE_PREVIEW = 2
        const val MSG_REPEAT = 3
        const val MSG_LONG_PRESS = 4
        const val REPEAT_INTERVAL = 50L // 20 keys per second
        const val REPEAT_START_DELAY = 400L
        const val DELAY_BEFORE_PREVIEW = 0L
        const val DELAY_AFTER_PREVIEW = 70L
    }
    private val LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()

    private var mKeyboardActionListener: KeyboardActionListener? = null
    private var mKeyboard: Keyboard? = null
    private var mPreviewingKey: Key = NOT_A_KEY

    // preview popup
    private val mShowPreview = true
    private var mPreviewPopup: PopupWindow
    private var mPreviewPopupView: PopupPreviewTextView

    // mini keyboard
    private var mMiniKeyboardShowing = false
    private var mMiniKeyboardPopup: PopupWindow
    private var mMiniKeyboardPopupBounds = Rect()
    private var mMiniKeyboard: PopupMiniKeyboardView

    private var mCurrentKey: Key = NOT_A_KEY
    private lateinit var mGestureDetector: GestureDetector
    private var mAbortKey = true
    private var mRepeatKey: Key = NOT_A_KEY
    private var mInvalidatedKey: Key = NOT_A_KEY

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
    private var mPaint = Paint()

    private lateinit var mHandler: Handler

    private var mOldPointerCount = 1
    private var mOldPointerX = .0f
    private var mOldPointerY = .0f

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        mPaint.isAntiAlias = true
        mPaint.textSize = .0f
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255

        mPreviewPopup = PopupWindow(context)
        mPreviewPopup.setBackgroundDrawable(null)
        mPreviewPopup.elevation = Keyboard.theme.popupElevation.toFloat()
        mPreviewPopup.isClippingEnabled = false
        mPreviewPopup.isTouchable = false

        mPreviewPopupView = PopupPreviewTextView(context)
        mPreviewPopup.contentView = mPreviewPopupView

        mMiniKeyboardPopup = PopupWindow(context)
        mMiniKeyboardPopup.setBackgroundDrawable(null)
        mMiniKeyboardPopup.elevation = Keyboard.theme.popupElevation.toFloat()
        mMiniKeyboardPopup.isClippingEnabled = false
        mMiniKeyboardPopup.isTouchable = false

        mMiniKeyboard = PopupMiniKeyboardView(context)
        mMiniKeyboard.clickListener = object : KeyboardActionListener {
            override fun onText(text: String) {
                mKeyboardActionListener?.onText(text)
                dismissPopupKeyboard()
            }
            override fun onKey(keyCode: Int) {}
        }
        mMiniKeyboardPopup.contentView = mMiniKeyboard
    }

    fun setKeyboard(keyboard: Keyboard) {
        if (mKeyboard != null) {
            showPreview(NOT_A_KEY)
        }
        // Remove any pending messages
        removeMessages()
        mKeyboard = keyboard
        keyboard.keyboardListener = object : Keyboard.KeyboardListener {
            override fun onLayoutChanged() {
                invalidateAllKeys()
            }

            override fun onText(text: String) {
                mKeyboardActionListener?.onText(text)
            }

            override fun onKey(keyCode: Int) {
                mKeyboardActionListener?.onKey(keyCode)
            }
        }
        background = ColorDrawable(Keyboard.theme.background)
        requestLayout()
        // Hint to reallocate the buffer if the size changed
        mKeyboardChanged = true
        invalidateAllKeys()
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        mAbortKey = true
    }

    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
        invalidate()
    }

    fun setKeyboardListener(listener: KeyboardActionListener?) {
        mKeyboardActionListener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mGestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
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
        mHandler = Handler(Handler.Callback { msg ->
            when (msg.what) {
                MSG_SHOW_PREVIEW -> showKey(msg.obj as Key)
                MSG_REMOVE_PREVIEW -> mPreviewPopupView.visibility = GONE
                MSG_REPEAT -> {
                    if (mRepeatKey != NOT_A_KEY && mCurrentKey == mRepeatKey) {
                        mKeyboard?.onClick(mRepeatKey)
                    }
                    val repeat = Message.obtain(mHandler, MSG_REPEAT)
                    mHandler.sendMessageDelayed(repeat, REPEAT_INTERVAL)
                }
                MSG_LONG_PRESS -> openPopupIfRequired()
                else -> {}
            }
            false
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
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
        closing()
        // Release the buffer if any and it will be reallocated on the next draw
        mBuffer = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, .0f, .0f, null)
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
        val keys = mKeyboard?.getKeys() ?: listOf()
        val invalidKey = mInvalidatedKey

        paint.color = Color.BLACK
        var drawSingleKey = false
        if (invalidKey != NOT_A_KEY && canvas.getClipBounds(clipRegion)) {
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

        mInvalidatedKey = NOT_A_KEY

        mCanvas?.restore()
        mDrawPending = false
        mDirtyRect.setEmpty()
    }

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
        val touchX = (ev.x - paddingLeft).toInt()
        val touchY = (ev.y - paddingTop).toInt()
        val action = ev.action
        val key = mKeyboard?.getKey(touchX, touchY) ?: return true // consume

        Log.i("[SK]", "[KeyboardView] onModifiedTouchEvent: ${action.action}, x = ${ev.rawX}, y = ${ev.rawY}")

        // Ignore all motion events until a DOWN
        if (mAbortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        if (mGestureDetector.onTouchEvent(ev)) {
            showPreview(NOT_A_KEY)
            mHandler.removeMessages(MSG_REPEAT)
            mHandler.removeMessages(MSG_LONG_PRESS)
            return true
        }

        // Needs to be called after the gesture detector gets a turn, as it may have
        // displayed the mini keyboard
        if (mMiniKeyboardShowing && mMiniKeyboardPopupBounds.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
            val now = ev.eventTime
            val miniEV = MotionEvent.obtain(now, now, ev.action,
                ev.rawX-mMiniKeyboardPopupBounds.left-Keyboard.theme.miniKeyboardPadding,
                ev.rawY-mMiniKeyboardPopupBounds.top-Keyboard.theme.miniKeyboardPadding, ev.metaState)
            if (mMiniKeyboard.onTouchEvent(miniEV)) {
                return true
            }
        }

        if (mMiniKeyboardShowing) {
            dismissPopupKeyboard()
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mAbortKey = false
                mCurrentKey = key
                if (key.repeatable) {
                    mRepeatKey = key
                    val msg = mHandler.obtainMessage(MSG_REPEAT)
                    mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY)
                    mKeyboard?.onClick(key)
                    // Delivering the key could have caused an abort
                    if (mAbortKey) {
                        mRepeatKey = NOT_A_KEY
                    }
                }
                val msg = mHandler.obtainMessage(MSG_LONG_PRESS)
                mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT)
                showPreview(key)
            }
            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (mCurrentKey == NOT_A_KEY) {
                    mCurrentKey = key
                } else {
                    if (key == mCurrentKey) {
                        continueLongPress = true
                    } else {
                        mCurrentKey = key
                    }
                }
                if (!continueLongPress) {
                    // Cancel old longpress
                    mHandler.removeMessages(MSG_LONG_PRESS)
                    // Start new longpress if key has changed
                    val msg = mHandler.obtainMessage(MSG_LONG_PRESS)
                    mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT)
                }
                showPreview(mCurrentKey)
            }
            MotionEvent.ACTION_UP -> {
                removeMessages()
                mCurrentKey = key
                showPreview(NOT_A_KEY)
                // If we're not on a repeating key (which sends on a DOWN event)
                if (mRepeatKey == NOT_A_KEY && !mAbortKey && mCurrentKey != NOT_A_KEY) {
                    mKeyboard?.onClick(mCurrentKey)
                }
                invalidateKey(key)
                mRepeatKey = NOT_A_KEY
            }
            MotionEvent.ACTION_CANCEL -> {
                removeMessages()
                dismissPopupKeyboard()
                mAbortKey = true
                showPreview(NOT_A_KEY)
                invalidateKey(mCurrentKey)
            }
        }
        return true
    }

    private fun showPreview(key: Key) {
        val oldKey = mPreviewingKey
        mPreviewingKey = key

        // Release the old key and press the new key
        if (oldKey != mPreviewingKey) {
            if (oldKey != NOT_A_KEY) {
                oldKey.onReleased(mPreviewingKey == NOT_A_KEY)
                invalidateKey(oldKey)
            }
            if (mPreviewingKey != NOT_A_KEY) {
                mPreviewingKey.onPressed()
                invalidateKey(mPreviewingKey)
            }
        }
        // If key changed and preview is on
        if (oldKey != mPreviewingKey && mShowPreview) {
            mHandler.removeMessages(MSG_SHOW_PREVIEW)
            if (mPreviewPopup.isShowing) {
                if (mPreviewingKey == NOT_A_KEY) {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_REMOVE_PREVIEW),
                        DELAY_AFTER_PREVIEW)
                }
            }
            if (key != NOT_A_KEY) {
                if (mPreviewPopup.isShowing && mPreviewPopupView.visibility == VISIBLE) {
                    // Show right away, if it's already visible and finger is moving around
                    showKey(key)
                } else {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SHOW_PREVIEW, key), DELAY_BEFORE_PREVIEW)
                }
            }
        }
    }

    private fun showKey(key: Key) {
        if (key !is PreviewTextKey) {
            mPreviewPopupView.visibility = GONE
            return
        }

        mPreviewPopupView.key = key
        val popupWidth = (key.width + Keyboard.theme.keyGap).toInt()
        val popupHeight = Keyboard.theme.popupMarginBottom + (mKeyboard?.keyHeight ?: 0)

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
        mPreviewPopupView.visibility = VISIBLE
    }

    private fun removeMessages() {
        if (::mHandler.isInitialized) {
            mHandler.removeMessages(MSG_REPEAT)
            mHandler.removeMessages(MSG_LONG_PRESS)
            mHandler.removeMessages(MSG_SHOW_PREVIEW)
        }
    }

    fun closing() {
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
        removeMessages()
        dismissPopupKeyboard()

        mBuffer = null
        mCanvas = null
    }

    private fun openPopupIfRequired(): Boolean {
        if (mCurrentKey == NOT_A_KEY) {
            return false
        }

        var result = false
        result = onLongPress(mCurrentKey)
        if (result) {
            showPreview(NOT_A_KEY)
        }
        return result
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
    private fun onLongPress(key: Key): Boolean {
        if (key !is PreviewTextKey || key.miniKeys.isNullOrEmpty()) {
            return false
        }
        Log.i("[SK]", "[KeyboardView] onLongPress $key")
        val popupKeyWidth = key.width + Keyboard.theme.keyGap

        val popupKeyHeight = mKeyboard?.keyHeight ?: 0
        var popupHeight = Keyboard.theme.popupMarginBottom + popupKeyHeight
        var popupWidth = (key.miniKeys.size*popupKeyWidth).toInt()
        val singleLine = key.miniKeys.size <= 5
        // multi-line
        if (!singleLine) {
            popupHeight += popupKeyHeight
            popupWidth = (popupKeyWidth*((key.miniKeys.size+1)/2)).toInt()
        }

        var popupY = key.y + key.height - popupHeight
        var popupX = key.x-Keyboard.theme.keyGap/2
        if (singleLine) {
            popupX -= ((key.miniKeys.size-1)/2)*popupKeyWidth
        } else {
            popupX -= (((key.miniKeys.size+1)/2-1)/2)*popupKeyWidth
        }
        while (popupX < 0) {
            popupX += popupKeyWidth
        }
        while (popupX + popupWidth > width) {
            popupX -= popupKeyWidth
        }

        val screenCoordinates = IntArray(2)
        getLocationOnScreen(screenCoordinates)
        val screenX = max(0, (popupX + screenCoordinates[0] - Keyboard.theme.miniKeyboardPadding).toInt())
        val screenY = max(0, (popupY + screenCoordinates[1] - Keyboard.theme.miniKeyboardPadding).toInt())
        mMiniKeyboardPopupBounds = Rect(screenX, screenY,
            screenX+popupWidth+2*Keyboard.theme.miniKeyboardPadding,
            screenY+popupHeight+2*Keyboard.theme.miniKeyboardPadding)
        Log.i("[SK]", "[KeyboardView] show mini keyboard at $mMiniKeyboardPopupBounds")

        val coordinates = IntArray(2)
        getLocationInWindow(coordinates)
        popupX += coordinates[0]
        popupY += coordinates[1]

        mMiniKeyboard.keys = key.miniKeys
        mMiniKeyboard.activeIndex = key.initMiniKeyIndex
        mMiniKeyboard.currentIndex = key.initMiniKeyIndex
        mMiniKeyboardPopup.width = popupWidth
        mMiniKeyboardPopup.height = popupHeight
        mMiniKeyboardPopup.showAtLocation(this, Gravity.NO_GRAVITY, popupX.toInt(), popupY.toInt())
        mMiniKeyboardShowing = true
        return true
    }

    private fun dismissPopupKeyboard() {
        if (mMiniKeyboardPopup.isShowing) {
            mMiniKeyboardPopup.dismiss()
            mMiniKeyboardShowing = false
        }
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
}
