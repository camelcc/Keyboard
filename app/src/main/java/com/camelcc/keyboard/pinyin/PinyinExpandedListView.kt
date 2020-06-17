package com.camelcc.keyboard.pinyin

import android.content.Context
import android.util.AttributeSet
import android.view.View

class PinyinExpandedListView : View {
    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int): super(context, attrs, defStyleAttr) {}
}