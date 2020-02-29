package com.camelcc.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class KeyboardTheme(context: Context) {
    var background = ColorDrawable(0xFFE8EAED.toInt())
    var paddingLeft: Int = 0.dp2px
    var paddingRight: Int = 0.dp2px
    var paddingTop: Int = 0.dp2px
    var paddingBottom: Int = 0.dp2px

    var keyPadding: Int = 6.dp2px
    var keyHeight: Int = 48.dp2px
    var keyGap: Int = 5.dp2px
    var keyColor = Color.WHITE
    var keyBorderColor = 0xFFA9ABAD.toInt()
    val keyBorderWidth = 2//px
    val keyBorderRadius = 8.dp2px
    val keyTextSize = 26.dp2px
    val keySymbolTextSize = 22.dp2px
    val keySpaceTextSize = 14.dp2px
    val keyUpperTextSize = 12.dp2px

    val keyControlBackground = 0xFFCCCED5.toInt()
    val keyEnterBackground = 0xFF1A73E8.toInt()
}