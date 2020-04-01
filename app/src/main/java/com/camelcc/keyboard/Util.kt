package com.camelcc.keyboard

import android.content.res.Resources
import android.util.TypedValue

const val APP_TAG = "[SK]"

val Int.dp2px: Int get() = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)).toInt()
val Int.sp2px: Int get() = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics)).toInt()
val Int.action: String get() {
    return when (this) {
        0 -> "DOWN"
        1 -> "UP"
        2 -> "MOVE"
        3 -> "CANCEL"
        else -> "OTHER"
    }
}


