package com.camelcc.keyboard.pinyin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.camelcc.keyboard.KeyboardListener
import com.camelcc.keyboard.KeyboardTheme
import com.camelcc.keyboard.dp2px

class PinyinDetailsAdapter :
    RecyclerView.Adapter<PinyinDetailsAdapter.ViewHolder>() {
    var listener: KeyboardListener? = null
    // maybe changed by other threads, use with null protected
    var candidates = listOf<String>()

    private val paint = Paint() // only for measure text width purpose

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    private class CandidateTextView(context: Context): AppCompatTextView(context) {
        private val paint = Paint()

        init {
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawLine(
                width - 1.dp2px.toFloat(),
                2 * KeyboardTheme.candidateVerticalPadding.toFloat(),
                width.toFloat(),
                height - 2 * KeyboardTheme.candidateVerticalPadding.toFloat(),
                paint
            )
        }
    }

    init { paint.textSize = KeyboardTheme.candidateTextSize }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = CandidateTextView(parent.context)
        textView.minWidth = parent.measuredWidth/8
        textView.minHeight = parent.measuredHeight/4
        textView.gravity = Gravity.CENTER
        textView.textSize = KeyboardTheme.candidateTextSizeSP
        textView.setTextColor(Color.BLACK)
        val vh = ViewHolder(textView)
        vh.itemView.setOnClickListener {
            val pos = vh.adapterPosition
            listener?.onCandidate(candidates.getOrNull(pos) ?: "", pos)
        }
        return vh
    }

    override fun getItemCount(): Int {
        return candidates.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = candidates.getOrNull(position) ?: ""
    }

    fun getCalculatedWidth(position: Int): Int {
        return (2*KeyboardTheme.candidateTextPadding + paint.measureText(candidates.getOrNull(position) ?: "")).toInt()
    }
}
