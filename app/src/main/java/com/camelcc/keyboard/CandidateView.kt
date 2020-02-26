package com.camelcc.keyboard

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View

class CandidateView: View {
    constructor(context: Context): super(context) {
        setBackgroundColor(Color.RED)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.e("[SK]", "CandidateView onMeasure")
        setMeasuredDimension(resolveSize(50, widthMeasureSpec),
            resolveSize(70, heightMeasureSpec))
    }
}
