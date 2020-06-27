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
import com.android.inputmethod.pinyin.PinyinIME
import com.camelcc.keyboard.CandidateView
import com.camelcc.keyboard.KeyboardTheme
import com.camelcc.keyboard.dp2px

class PinyinDetailsAdapter :
    RecyclerView.Adapter<PinyinDetailsAdapter.ViewHolder>() {

    private lateinit var decodingInfo: PinyinIME.DecodingInfo

    private val paint = Paint()

    var listener: CandidateView.CandidateViewListener? = null

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    class CandidateTextView(context: Context): AppCompatTextView(context) {
        private val paint = Paint()

        init {
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawLine(width-1.dp2px.toFloat(),
                2*KeyboardTheme.candidateVerticalPadding.toFloat(),
                width.toFloat(),
                height-2*KeyboardTheme.candidateVerticalPadding.toFloat(),
                paint)
        }
    }

    fun setDecodingInfo(di: PinyinIME.DecodingInfo) {
        paint.textSize = KeyboardTheme.candidateTextSize
        decodingInfo = di
    }

    fun loadMore() {
        decodingInfo.getCandiagtesForCache()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = CandidateTextView(parent.context)
        textView.minWidth = parent.measuredWidth/8
        textView.minHeight = parent.measuredHeight/4
        textView.gravity = Gravity.CENTER
        textView.textSize = 18.0f
        textView.setTextColor(Color.BLACK)
        val vh = ViewHolder(textView)
        vh.itemView.setOnClickListener {
            val pos = vh.adapterPosition
            listener?.onSuggestion(decodingInfo.mCandidatesList[pos], pos, false)
        }
        return vh
    }

    override fun getItemCount(): Int {
        return decodingInfo.mCandidatesList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = decodingInfo.mCandidatesList[position]
    }

    fun getCalculatedWidth(position: Int): Int {
        return (2*KeyboardTheme.candidateTextPadding + paint.measureText(decodingInfo.mCandidatesList[position])).toInt()
    }
}
