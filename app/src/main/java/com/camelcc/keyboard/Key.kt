package com.camelcc.keyboard

import android.graphics.drawable.Drawable

data class Key(var x: Int, var y: Int,
               var width: Int, var height: Int,
               val text: CharSequence?, val icon: Drawable?)
