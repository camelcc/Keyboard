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
        set(value) {
            field = value
            invalidate()
        }

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mOutlinePath = buildOutlinePath()
    }

    private fun buildOutlinePath(): Path {
        val outlinePath = Path()
        outlinePath.addRoundRect(RectF(.0f, .0f, width.toFloat(), (height-Keyboard.theme.popupMarginBottom).toFloat()),
            Keyboard.theme.popupRadius.toFloat(), Keyboard.theme.popupRadius.toFloat(), Path.Direction.CCW)
        val singleLine = keys.size <= 5
        val keyWidth = if (singleLine) width/keys.size else width/(keys.size/2)

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

        val singleLine = keys.size <= 5
        val keyWidth = if (singleLine) width/keys.size else width/(keys.size/2)
        val keyHeight = Keyboard.theme.popupKeyHeight
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

            val active = i == activeIndex
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
