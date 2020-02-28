package com.camelcc.keyboard

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable

class KeyboardTheme(context: Context) {
    var background: Drawable = ColorDrawable(context.getColor(R.color.bg))
    var paddingLeft: Int = 0.dp2px
    var paddingRight: Int = 0.dp2px
    var paddingTop: Int = 0.dp2px
    var paddingBottom: Int = 4.dp2px

    var keyPadding: Int = 4.dp2px
    var keyHeight: Int = 52.dp2px

    val keyBorderRadius: Int = 4.dp2px
}
