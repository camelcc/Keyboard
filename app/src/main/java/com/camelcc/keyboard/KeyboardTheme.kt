package com.camelcc.keyboard

import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class KeyboardTheme {
    var background = 0xFFE8EAED.toInt()
    var paddingLeft: Int = 0.dp2px
    var paddingRight: Int = 0.dp2px
    var paddingTop: Int = 0.dp2px
    var paddingBottom: Int = 0.dp2px

    var keyPadding: Int = 6.dp2px
    var keyPaddingHorizontal: Int = 4.dp2px
    var keyHeight: Int = 48.dp2px
    var keyHeightHorizontal = 38.dp2px
    var keyGap: Int = 5.dp2px
    var keyColor = Color.WHITE
    var keyBorderColor = 0xFFA9ABAD.toInt()
    val keyBorderWidth = 2//px
    val keyBorderRadius = 8.dp2px.toFloat()
    val keyTextSize = 26.dp2px
    val keySymbolTextSize = 22.dp2px
    val keySpaceTextSize = 14.dp2px
    val keyUpperTextSize = 12.dp2px

    val keyControlBackground = 0xFFCCCED5.toInt()
    val keyControlPressedBackground = 0xFFBDC1C6.toInt()
    val keyEnterBackground = 0xFF1A73E8.toInt()
    val keyEnterPressedBackground = 0xFF185ABC.toInt()

    val popupBackground = 0xFFCCCED5.toInt()
    val popupRadius = 8.dp2px
    val popupTextSize = 28.dp2px.toFloat()
    val popupElevation = 4.dp2px
    val popupMarginBottom = 70.dp2px
    val popupSubscriptionSize = 16.dp2px.toFloat()

    val miniKeyboardPadding = 48.dp2px
    val miniKeyboardTextSize = 24.dp2px
    val miniKeyboardHighlight = 0xFF5E97F6.toInt()
    val miniKeyboardHighlightRadius = 4.dp2px

    val candidateTextSize = 24.dp2px.toFloat()
    val candidateMinTextSize = 16.dp2px.toFloat()
    val candidateHeight = 45.dp2px
    val candidateTextPadding = 2.dp2px
}
