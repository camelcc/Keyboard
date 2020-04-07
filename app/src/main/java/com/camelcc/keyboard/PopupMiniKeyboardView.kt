package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider

class PopupMiniKeyboardView: View {
    var keys: List<String> = listOf()
    var activeIndex: Int = 0
    var currentIndex: Int = activeIndex

    private val singleLine: Boolean get() = keys.size <= 5
    private val keyWidth: Int get() = if (singleLine) width/keys.size else width/(keys.size/2)
    private val keyHeight: Int get() = Keyboard.theme.popupKeyHeight

    var clickListener: PopupMiniKeyboardViewListener? =null

    private val mPaint = Paint()
    private var mOutlinePath = Path()

    private val outline = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            mOutlinePath = buildOutlinePath()
            outline.setConvexPath(mOutlinePath)
        }
    }

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        outlineProvider = outline
        clipToOutline = true

        mPaint.isAntiAlias = true
        mPaint.textSize = .0f
        mPaint.color = Color.BLACK
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255
        mPaint.typeface = Typeface.DEFAULT
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) {
            return false
        }

        if (keyWidth == 0) {
            Log.e("[SK]", "[PopupMiniKeyboardView] keywidth is 0")
            return true
        }

        val x = ev.x.coerceAtLeast(.0f).coerceAtMost(keyWidth*(if (singleLine) keys.size else keys.size/2)-1.0f).toInt()
        val y = ev.y.coerceAtLeast(.0f).coerceAtMost(height-1.0f).toInt()
        var index = x/keyWidth
        if (!singleLine && y >= height/2) {
            index += keys.size/2
        }
        Log.i("[SK]", "[PopupMiniKeyboardView] onTouchEvent: ${ev.action.action}, ex: ${ev.x}, ey: ${ev.y}, x: $x, y: $y, kw: $keyWidth, i: $index")

        index = index.coerceAtMost(keys.size-1)
        if (keys[index].isBlank()) {
            index = if (index - 1 >= 0) index-1 else index+1
        }
        if (index != currentIndex) {
            currentIndex = index
            invalidate()
        }

        if (action == MotionEvent.ACTION_UP) {
            clickListener?.onText("a")
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mOutlinePath = buildOutlinePath()
    }

    private fun buildOutlinePath(): Path {
        val outlinePath = Path()
        outlinePath.addRoundRect(RectF(.0f, .0f, width.toFloat(), (height-Keyboard.theme.popupMarginBottom).toFloat()),
            Keyboard.theme.popupRadius.toFloat(), Keyboard.theme.popupRadius.toFloat(), Path.Direction.CCW)


        val dropPath = Path()
        val x = (if (singleLine) activeIndex else (if (activeIndex < keys.size/2) activeIndex else activeIndex-keys.size/2))*keyWidth
        val y = height-Keyboard.theme.popupMarginBottom
        dropPath.addRoundRect(RectF(x.toFloat(), (y-Keyboard.theme.popupRadius).toFloat(), (x+keyWidth).toFloat(), height.toFloat()), Keyboard.theme.popupRadius.toFloat(), Keyboard.theme.popupRadius.toFloat(), Path.Direction.CCW)

        outlinePath.op(dropPath, Path.Op.UNION)
        return outlinePath
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(mOutlinePath)

        val paint = mPaint
        // background
        paint.style = Paint.Style.FILL
        paint.color = Keyboard.theme.popupBackground
        canvas.drawRect(.0f, .0f, width.toFloat(), height.toFloat(), paint)

        var x = 0
        var y = 0
        for (i in keys.indices) {
            if (keys[i].isBlank()) {
                x += keyWidth
                continue
            }
            if (!singleLine && i == keys.size/2) {
                x = 0
                y = keyHeight
            }

            val active = i == currentIndex
            if (active) {
                val style = paint.style
                val color = paint.color

                paint.style = Paint.Style.FILL
                paint.color = Keyboard.theme.miniKeyboardHighlight
                paint.strokeWidth = .0f
                canvas.drawRoundRect(x.toFloat(), y.toFloat(),
                    x+keyWidth.toFloat(), y+keyHeight.toFloat(),
                    Keyboard.theme.miniKeyboardHighlightRadius.toFloat(), Keyboard.theme.miniKeyboardHighlightRadius.toFloat(), paint)
                paint.style = style
                paint.color = color

                paint.color = Color.WHITE
            } else {
                paint.color = Color.BLACK
            }
            paint.typeface = Typeface.DEFAULT
            paint.textSize = Keyboard.theme.miniKeyboardTextSize.toFloat()
            canvas.drawText(
                keys[i],
                (x + keyWidth/2).toFloat(),
                y + keyHeight/2-(paint.descent()+paint.ascent())/2, paint)

            x += keyWidth
        }

        canvas.restore()
    }
}

interface PopupMiniKeyboardViewListener {
    fun onText(text: String)
}
