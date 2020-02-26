package com.camelcc.keyboard

import android.content.res.Resources
import android.util.TypedValue

val Int.dp2px: Int get() = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)).toInt()
val Int.sp2px: Int get() = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics)).toInt()


