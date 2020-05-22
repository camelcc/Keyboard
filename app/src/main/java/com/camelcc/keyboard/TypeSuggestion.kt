package com.camelcc.keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TypeSuggestion(private val scope: CoroutineScope) {
    companion object {
        const val DICT_FILE_NAME = "wordlist.dict"
    }

    var dictionary: BinaryDictionary? = null

    fun initializeDictionary(context: Context) {
        try {
            scope.launch {
                val target = File(context.filesDir, DICT_FILE_NAME)
                // TODO: validation check
                if (target.exists()) {
                    dictionary = BinaryDictionary(target)
                    return@launch
                }

                context.assets.open(DICT_FILE_NAME).use { ios ->
                    FileOutputStream(target).use { oos ->
                        ios.copyTo(oos)
                    }
                }

                dictionary = BinaryDictionary(target)
            }
        } catch (e: IOException) {
            Log.e("[Keyboard]", "can not load word dictionary")
            throw e
        }
    }
}