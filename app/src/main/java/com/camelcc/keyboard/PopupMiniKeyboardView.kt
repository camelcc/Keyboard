package com.camelcc.keyboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider

class PopupMiniKeyboardView: View {
    var keys: List<Char> = listOf()
    var activeIndex: Int = 0
    var currentIndex: Int = activeIndex
    var listener: KeyboardListener? = null

    private val singleLine: Boolean get() = keys.size <= 5
    private val keyWidth: Int get() = if (singleLine) width/keys.size else width/(keys.size/2)
    private val keyHeight: Int get() = if (singleLine) height-KeyboardTheme.popupMarginBottom else (height-KeyboardTheme.popupMarginBottom)/2

    private val paint = Paint()
    private var outlinePath = Path()

    private val outline = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outlinePath = buildOutlinePath()
            outline.setConvexPath(outlinePath)
        }
    }

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        outlineProvider = outline
        clipToOutline = true

        paint.isAntiAlias = true
        paint.textSize = .0f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        paint.alpha = 255
        paint.typeface = Typeface.DEFAULT
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) {
            return false
        }

        if (keyWidth == 0) {
            return true
        }

        val x = ev.x.coerceAtLeast(.0f).coerceAtMost(keyWidth*(if (singleLine) keys.size else keys.size/2)-1.0f).toInt()
        val y = ev.y.coerceAtLeast(.0f).coerceAtMost(height-1.0f).toInt()
        var index = x/keyWidth
        if (!singleLine && y >= height/2) {
            index += keys.size/2
        }

        index = index.coerceAtMost(keys.size-1)
        if (keys[index] == '\u0000') {
            index = if (index - 1 >= 0) index-1 else index+1
        }
        if (index != currentIndex) {
            currentIndex = index
            invalidate()
        }

        if (action == MotionEvent.ACTION_UP) {
            listener?.onKeyboardChar(keys[index])
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outlinePath = buildOutlinePath()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(outlinePath)

        val paint = paint
        // background
        paint.style = Paint.Style.FILL
        paint.color = KeyboardTheme.popupBackground
        canvas.drawRect(.0f, .0f, width.toFloat(), height.toFloat(), paint)

        var x = .0f
        var y = .0f
        for (i in keys.indices) {
            if (keys[i] == '\u0000') {
                x += keyWidth
                continue
            }
            if (!singleLine && i == keys.size/2) {
                x = .0f
                y = keyHeight.toFloat()
            }

            val active = i == currentIndex
            if (active) {
                val style = paint.style
                val color = paint.color

                paint.style = Paint.Style.FILL
                paint.color = KeyboardTheme.miniKeyboardHighlight
                paint.strokeWidth = .0f
                canvas.drawRoundRect(x, y,
                    x+keyWidth, y+keyHeight,
                    KeyboardTheme.miniKeyboardHighlightRadius.toFloat(), KeyboardTheme.miniKeyboardHighlightRadius.toFloat(), paint)
                paint.style = style
                paint.color = color

                paint.color = Color.WHITE
            } else {
                paint.color = Color.BLACK
            }
            paint.typeface = Typeface.DEFAULT
            paint.textSize = KeyboardTheme.miniKeyboardTextSize.toFloat()
            canvas.drawText(
                keys[i].toString(),
                x + keyWidth/2,
                y + keyHeight/2-(paint.descent()+paint.ascent())/2, paint)

            x += keyWidth
        }

        canvas.restore()
    }

    private fun buildOutlinePath(): Path {
        val outlinePath = Path()
        outlinePath.addRoundRect(RectF(.0f, .0f, width.toFloat(), (height-KeyboardTheme.popupMarginBottom).toFloat()),
            KeyboardTheme.popupRadius.toFloat(), KeyboardTheme.popupRadius.toFloat(), Path.Direction.CCW)

        val dropPath = Path()
        val x = (if (singleLine) activeIndex else (if (activeIndex < keys.size/2) activeIndex else activeIndex-keys.size/2))*keyWidth*1.0f
        val y = (height-KeyboardTheme.popupMarginBottom).toFloat()

        dropPath.addRect(x, y-KeyboardTheme.popupRadius, x+keyWidth, (height-KeyboardTheme.popupRadius).toFloat(), Path.Direction.CCW)

        val dropRect = Path()
        dropRect.addRoundRect(RectF(x, y+KeyboardTheme.popupRadius, x+keyWidth, height.toFloat()), KeyboardTheme.popupRadius.toFloat(), KeyboardTheme.popupRadius.toFloat(), Path.Direction.CCW)
        dropPath.op(dropRect, Path.Op.UNION)

        outlinePath.op(dropPath, Path.Op.UNION)
        return outlinePath
    }
}
