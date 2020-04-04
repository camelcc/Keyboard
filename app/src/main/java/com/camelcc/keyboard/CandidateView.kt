package com.camelcc.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View

class CandidateView: View {
    constructor(context: Context): super(context) {
        background = ColorDrawable(Color.RED)
//        setBackgroundColor(Color.RED)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.e("[SK]", "CandidateView onMeasure")

        val height = Keyboard.theme.keyHeight + Keyboard.theme.popupKeyHeight
        setMeasuredDimension(resolveSize(50, widthMeasureSpec),
            height)
    }
}
