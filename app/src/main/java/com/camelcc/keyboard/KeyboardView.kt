package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.*
import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.camelcc.keyboard.pinyin.PinyinDetailsAdapter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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

    var listener: KeyboardListener? = null
        set(value) {
            field = value
            candidateDetailViewAdapter.listener = value
            keyboard?.keyboardListener = value
        }

    private val LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()

    private val displayWidth: Int

    private var keyboard: Keyboard? = null
    private var previewingKey: Key = NOT_A_KEY

    // candidates popup
    private val candidateDetailPopupWindow: PopupWindow
    private val candidateDetailView: RecyclerView
    private val candidateDetailViewAdapter: PinyinDetailsAdapter

    // preview popup
    private var previewPopup: PopupWindow
    private var previewPopupView: PopupPreviewTextView

    // mini keyboard popup
    private var miniKeyboardShowing = false
    private var miniKeyboardPopup: PopupWindow
    private var miniKeyboardPopupBounds = Rect()
    private var miniKeyboard: PopupMiniKeyboardView

    private lateinit var gestureDetector: GestureDetector
    private var messageHandler: Handler
    private var currentKey: Key = NOT_A_KEY
    private var abortKey = true
    private var repeatKey: Key = NOT_A_KEY
    private var invalidatedKey: Key = NOT_A_KEY

    // ## Keyboard drawing
    // Whether the keyboard bitmap to be redrawn before it's blitted.
    private var drawPending: Boolean = false
    // The keyboard bitmap for faster updates
    private var buffer: Bitmap? = null
    // Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.
    private var keyboardChanged: Boolean = false
    // The canvas for the above mutable keyboard bitmap
    private var canvas: Canvas? = null
    // The dirty region in the keyboard bitmap
    private var dirtyRect = Rect()
    private var paint = Paint()

    private var oldPointerCount = 1
    private var oldPointerX = .0f
    private var oldPointerY = .0f

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        val dm = context.resources.displayMetrics
        displayWidth = min(dm.widthPixels, dm.heightPixels)

        paint.isAntiAlias = true
        paint.textSize = .0f
        paint.textAlign = Paint.Align.CENTER
        paint.alpha = 255

        val gridLayoutManager = GridLayoutManager(context, 128)
        candidateDetailViewAdapter = PinyinDetailsAdapter()
        candidateDetailView = RecyclerView(context)
        candidateDetailView.setBackgroundColor(KeyboardTheme.background)
        candidateDetailView.layoutManager = gridLayoutManager
        candidateDetailView.setHasFixedSize(true)
        candidateDetailView.adapter = candidateDetailViewAdapter
        candidateDetailPopupWindow = PopupWindow(context)
        candidateDetailPopupWindow.isClippingEnabled = false
        candidateDetailPopupWindow.setBackgroundDrawable(null)
        candidateDetailPopupWindow.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        candidateDetailPopupWindow.contentView = candidateDetailView
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return calculateSpan(candidateDetailViewAdapter.getCalculatedWidth(position))
            }
        }
        candidateDetailView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val visible = gridLayoutManager.childCount
                    val total = gridLayoutManager.itemCount
                    val first = gridLayoutManager.findFirstVisibleItemPosition()
                    if (first + visible >= total - 10) { // some buffer
                        candidateDetailView.post {
                            // TODO: do this in IO thread
                            candidateDetailViewAdapter.loadMoreCandidates()
                            candidateDetailViewAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        })

        previewPopup = PopupWindow(context)
        previewPopup.setBackgroundDrawable(null)
        previewPopup.elevation = KeyboardTheme.popupElevation.toFloat()
        previewPopup.isClippingEnabled = false
        previewPopup.isTouchable = false
        previewPopupView = PopupPreviewTextView(context)
        previewPopup.contentView = previewPopupView

        miniKeyboardPopup = PopupWindow(context)
        miniKeyboardPopup.setBackgroundDrawable(null)
        miniKeyboardPopup.elevation = KeyboardTheme.popupElevation.toFloat()
        miniKeyboardPopup.isClippingEnabled = false
        miniKeyboardPopup.isTouchable = false
        miniKeyboard = PopupMiniKeyboardView(context)
        miniKeyboard.listener = object : KeyboardListener {
            override fun onKeyboardChar(c: Char, fromPopup: Boolean) {
                listener?.onKeyboardChar(c, true)
                dismissPopupKeyboard()
            }

            override fun onLangSwitch() {}
            override fun onKeyboardChanged() {}
            override fun onKeyboardKeyCode(keyCode: Int) {}
            override fun onCandidate(text: String, index: Int) {}
            override fun showMoreCandidates() {}
            override fun dismissMoreCandidates() {}
        }
        miniKeyboardPopup.contentView = miniKeyboard

        messageHandler = Handler { msg -> return@Handler handleMessage(msg) }
    }

    private fun calculateSpan(textWidth: Int): Int {
        return if (width <= displayWidth) { // portrait
            (ceil(textWidth*1.0/(width/8)).toInt()*16).coerceAtMost(128)
        } else {
            (ceil(textWidth*1.0/(width/16)).toInt()*8).coerceAtMost(128)
        }
    }

    //TODO: refactor this api
    fun getCandidatesAdapter(): PinyinDetailsAdapter {
        return candidateDetailViewAdapter
    }

    fun setKeyboard(kb: Keyboard) {
        if (keyboard != null) {
            showPreview(NOT_A_KEY)
        }
        // Remove any pending messages
        removeMessages()
        keyboard = kb
        kb.keyboardListener = listener
        background = ColorDrawable(KeyboardTheme.background)
        requestLayout()
        // Hint to reallocate the buffer if the size changed
        keyboardChanged = true
        invalidateAllKeys()
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        abortKey = true
    }

    fun invalidateAllKeys() {
        dirtyRect.union(0, 0, width, height)
        drawPending = true
        invalidate()
    }

    fun showMoreCandidatesPopup() {
        candidateDetailViewAdapter.notifyDataSetChanged()

        val coordinates = IntArray(2)
        getLocationInWindow(coordinates)
        if (candidateDetailPopupWindow.isShowing) {
            candidateDetailPopupWindow.update(coordinates[0], coordinates[1], width, height)
        } else {
            candidateDetailPopupWindow.width = width
            candidateDetailPopupWindow.height = height
            candidateDetailPopupWindow.showAtLocation(this, Gravity.NO_GRAVITY, coordinates[0], coordinates[1])
        }
    }

    fun dismissCandidatesPopup() {
        if (candidateDetailPopupWindow.isShowing) {
            candidateDetailPopupWindow.dismiss()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTapEvent(ev: MotionEvent): Boolean {
                if (ev.action != MotionEvent.ACTION_UP) {
                    return false
                }

                val touchX = (ev.x - paddingLeft).toInt()
                val touchY = (ev.y - paddingTop).toInt()
                val key = keyboard?.getKey(touchX, touchY) ?: return false
                return keyboard?.onDoubleClick(key) ?: return false
            }
        })
        gestureDetector.setIsLongpressEnabled(false)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    private fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_SHOW_PREVIEW -> showKey(msg.obj as Key)
            MSG_REMOVE_PREVIEW -> previewPopupView.visibility = GONE
            MSG_REPEAT -> {
                if (repeatKey != NOT_A_KEY && currentKey == repeatKey) {
                    keyboard?.onClick(repeatKey)
                }
                val repeat = Message.obtain(messageHandler, MSG_REPEAT)
                messageHandler.sendMessageDelayed(repeat, REPEAT_INTERVAL)
            }
            MSG_LONG_PRESS -> openPopupIfRequired()
            else -> {}
        }
        return false // continue receiving messages
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Always full screen width
        val height = paddingTop+paddingBottom+(keyboard?.height ?: 0)
//        Log.d("[KeyboardView]", "[KeyboardView]: onMeasure, h = $height, keyboard = $keyboard")
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
//        Log.d("[KeyboardView]", "[KeyboardView]: onSizeChanged, w = $w, h = $h")
        keyboard?.resize(w, h)
        closing()
        // Release the buffer if any and it will be reallocated on the next draw
        buffer = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawPending || buffer == null || keyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(buffer!!, .0f, .0f, null)
    }

    private fun onBufferDraw() {
        if (buffer == null || keyboardChanged) {
            if (buffer == null || (keyboardChanged &&
                        (buffer?.width != width || buffer?.height != height))) {
                // Make sure our bitmap is at least 1x1
                buffer = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
                buffer?.let { canvas = Canvas(it) }
            }
            invalidateAllKeys()
            keyboardChanged = false
        }

        if (keyboard == null) {
            return
        }

        canvas?.save()
        val canvas = canvas!!
        canvas.clipRect(dirtyRect)

        val clipRegion = Rect(0, 0, 0, 0)
        val keys = keyboard?.keys ?: listOf()
        val invalidKey = invalidatedKey

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

        invalidatedKey = NOT_A_KEY

        this.canvas?.restore()
        drawPending = false
        dirtyRect.setEmpty()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Convert multi-pointer up/down events to single up/down events to
        // deal with the typical multi-pointer behavior of two thumb typing
        val pointerCount = ev.pointerCount
        val action = ev.action
        var result = false
        val now = ev.eventTime

        if (pointerCount != oldPointerCount) {
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
                val up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, oldPointerX, oldPointerY, ev.metaState)
                result = onModifiedTouchEvent(up, true)
                up.recycle()
            }
        } else {
            if (pointerCount == 1) {
                result = onModifiedTouchEvent(ev, false)
                oldPointerX = ev.x
                oldPointerY = ev.y
            } else {
                // Don't do anything when 2 pointers are down and moving
                result = true
            }
        }
        oldPointerCount = pointerCount
        return result
    }

    private val Int.action: String get() {
        return when (this) {
            0 -> "DOWN"
            1 -> "UP"
            2 -> "MOVE"
            3 -> "CANCEL"
            else -> "OTHER"
        }
    }

    private fun onModifiedTouchEvent(ev: MotionEvent, possiblePoly: Boolean): Boolean {
        val touchX = (ev.x - paddingLeft).toInt()
        val touchY = (ev.y - paddingTop).toInt()
        val action = ev.action
        val key = keyboard?.getKey(touchX, touchY) ?: return true // consume

//        Log.i(TAG, "[KeyboardView] onModifiedTouchEvent: ${action.action}, x = ${ev.rawX}, y = ${ev.rawY}")

        // Ignore all motion events until a DOWN
        if (abortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        if (gestureDetector.onTouchEvent(ev)) {
            showPreview(NOT_A_KEY)
            messageHandler.removeMessages(MSG_REPEAT)
            messageHandler.removeMessages(MSG_LONG_PRESS)
            return true
        }

        // Needs to be called after the gesture detector gets a turn, as it may have
        // displayed the mini keyboard
        if (miniKeyboardShowing && miniKeyboardPopupBounds.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
            val now = ev.eventTime
            val miniEV = MotionEvent.obtain(now, now, ev.action,
                ev.rawX-miniKeyboardPopupBounds.left-KeyboardTheme.miniKeyboardPadding,
                ev.rawY-miniKeyboardPopupBounds.top-KeyboardTheme.miniKeyboardPadding, ev.metaState)
            if (miniKeyboard.onTouchEvent(miniEV)) {
                return true
            }
        }

        if (miniKeyboardShowing) {
            dismissPopupKeyboard()
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                abortKey = false
                currentKey = key
                if (key.repeatable) {
                    repeatKey = key
                    val msg = messageHandler.obtainMessage(MSG_REPEAT)
                    messageHandler.sendMessageDelayed(msg, REPEAT_START_DELAY)
                    keyboard?.onClick(key)
                    // Delivering the key could have caused an abort
                    if (abortKey) {
                        repeatKey = NOT_A_KEY
                    }
                }
                val msg = messageHandler.obtainMessage(MSG_LONG_PRESS)
                messageHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT)
                showPreview(key)
            }
            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (currentKey == NOT_A_KEY) {
                    currentKey = key
                } else {
                    if (key == currentKey) {
                        continueLongPress = true
                    } else {
                        currentKey = key
                    }
                }
                if (!continueLongPress) {
                    // Cancel old longpress
                    messageHandler.removeMessages(MSG_LONG_PRESS)
                    // Start new longpress if key has changed
                    val msg = messageHandler.obtainMessage(MSG_LONG_PRESS)
                    messageHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT)
                }
                showPreview(currentKey)
            }
            MotionEvent.ACTION_UP -> {
                removeMessages()
                currentKey = key
                showPreview(NOT_A_KEY)
                // If we're not on a repeating key (which sends on a DOWN event)
                if (repeatKey == NOT_A_KEY && !abortKey && currentKey != NOT_A_KEY) {
                    keyboard?.onClick(currentKey)
                }
                invalidateKey(key)
                repeatKey = NOT_A_KEY
            }
            MotionEvent.ACTION_CANCEL -> {
                removeMessages()
                dismissPopupKeyboard()
                abortKey = true
                showPreview(NOT_A_KEY)
                invalidateKey(currentKey)
            }
        }
        return true
    }

    private fun showPreview(key: Key) {
        val oldKey = previewingKey
        previewingKey = key

        // Release the old key and press the new key
        if (oldKey != previewingKey) {
            if (oldKey != NOT_A_KEY) {
                oldKey.onReleased(previewingKey == NOT_A_KEY)
                invalidateKey(oldKey)
            }
            if (previewingKey != NOT_A_KEY) {
                previewingKey.onPressed()
                invalidateKey(previewingKey)
            }
        }
        // If key changed and preview is on
        if (oldKey != previewingKey) {
            messageHandler.removeMessages(MSG_SHOW_PREVIEW)
            if (previewPopup.isShowing) {
                if (previewingKey == NOT_A_KEY) {
                    messageHandler.sendMessageDelayed(messageHandler.obtainMessage(MSG_REMOVE_PREVIEW),
                        DELAY_AFTER_PREVIEW)
                }
            }
            if (key != NOT_A_KEY) {
                if (previewPopup.isShowing && previewPopupView.visibility == VISIBLE) {
                    // Show right away, if it's already visible and finger is moving around
                    showKey(key)
                } else {
                    messageHandler.sendMessageDelayed(messageHandler.obtainMessage(MSG_SHOW_PREVIEW, key), DELAY_BEFORE_PREVIEW)
                }
            }
        }
    }

    private fun showKey(key: Key) {
        if (key !is PreviewTextKey) {
            previewPopupView.visibility = GONE
            return
        }

        previewPopupView.key = key
        val popupWidth = (key.width + KeyboardTheme.keyGap).toInt()
        val popupHeight = KeyboardTheme.popupMarginBottom + (keyboard?.keyHeight ?: 0)

        var popupX = key.x - KeyboardTheme.keyGap/2
        var popupY = key.y + key.height - popupHeight

        messageHandler.removeMessages(MSG_REMOVE_PREVIEW)
        val coordinates = IntArray(2)
        getLocationInWindow(coordinates)
        popupX += coordinates[0]
        popupY += coordinates[1]

        if (previewPopup.isShowing) {
            previewPopup.update(popupX.toInt(), popupY.toInt(), popupWidth, popupHeight)
        } else {
            previewPopup.width = popupWidth
            previewPopup.height = popupHeight
            previewPopup.showAtLocation(this, Gravity.NO_GRAVITY, popupX.toInt(), popupY.toInt())
        }
        previewPopupView.visibility = VISIBLE
    }

    private fun removeMessages() {
        messageHandler.removeMessages(MSG_REPEAT)
        messageHandler.removeMessages(MSG_LONG_PRESS)
        messageHandler.removeMessages(MSG_SHOW_PREVIEW)
    }

    fun closing() {
        if (candidateDetailPopupWindow.isShowing) {
            candidateDetailPopupWindow.dismiss()
        }

        if (previewPopup.isShowing) {
            previewPopup.dismiss()
        }
        removeMessages()
        dismissPopupKeyboard()

        buffer = null
        canvas = null
    }

    private fun openPopupIfRequired(): Boolean {
        if (currentKey == NOT_A_KEY) {
            return false
        }

        var result = onLongPress(currentKey)
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
//        Log.i(TAG, "[KeyboardView] onLongPress $key")
        val popupKeyWidth = key.width + KeyboardTheme.keyGap

        val popupKeyHeight = keyboard?.keyHeight ?: 0
        var popupHeight = KeyboardTheme.popupMarginBottom + popupKeyHeight
        var popupWidth = (key.miniKeys.size*popupKeyWidth).toInt()
        val singleLine = key.miniKeys.size <= 5
        // multi-line
        if (!singleLine) {
            popupHeight += popupKeyHeight
            popupWidth = (popupKeyWidth*((key.miniKeys.size+1)/2)).toInt()
        }

        var popupY = key.y + key.height - popupHeight
        var popupX = key.x-KeyboardTheme.keyGap/2
        popupX -= if (singleLine) {
            ((key.miniKeys.size-1)/2)*popupKeyWidth
        } else {
            (((key.miniKeys.size+1)/2-1)/2)*popupKeyWidth
        }
        while (popupX < 0) {
            popupX += popupKeyWidth
        }
        while (popupX + popupWidth > width) {
            popupX -= popupKeyWidth
        }

        val screenCoordinates = IntArray(2)
        getLocationOnScreen(screenCoordinates)
        val screenX = max(0, (popupX + screenCoordinates[0] - KeyboardTheme.miniKeyboardPadding).toInt())
        val screenY = max(0, (popupY + screenCoordinates[1] - KeyboardTheme.miniKeyboardPadding).toInt())
        miniKeyboardPopupBounds = Rect(screenX, screenY,
            screenX+popupWidth+2*KeyboardTheme.miniKeyboardPadding,
            screenY+popupHeight+2*KeyboardTheme.miniKeyboardPadding)
//        Log.i(TAG, "[KeyboardView] show mini keyboard at $mMiniKeyboardPopupBounds")

        val coordinates = IntArray(2)
        getLocationInWindow(coordinates)
        popupX += coordinates[0]
        popupY += coordinates[1]

        miniKeyboard.keys = key.miniKeys
        miniKeyboard.activeIndex = key.initMiniKeyIndex
        miniKeyboard.currentIndex = key.initMiniKeyIndex
        miniKeyboardPopup.width = popupWidth
        miniKeyboardPopup.height = popupHeight
        miniKeyboardPopup.showAtLocation(this, Gravity.NO_GRAVITY, popupX.toInt(), popupY.toInt())
        miniKeyboardShowing = true
        return true
    }

    private fun dismissPopupKeyboard() {
        if (miniKeyboardPopup.isShowing) {
            miniKeyboardPopup.dismiss()
            miniKeyboardShowing = false
        }
    }

    private fun invalidateKey(key: Key?) {
        if (key == null) {
            return
        }
        invalidatedKey = key
        dirtyRect.union((key.x + paddingLeft).toInt(),
            (key.y + paddingTop).toInt(),
            (key.x + key.width + paddingLeft).toInt(),
            (key.y + key.height + paddingTop).toInt())
        onBufferDraw()
        invalidate()
    }
}
