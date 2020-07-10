package com.camelcc.keyboard.en

import android.content.Context
import android.view.KeyEvent
import com.camelcc.keyboard.IMEListener

class IME(private val context: Context) {
    var listener: IMEListener? = null
    var candidates = mutableListOf<String>()

    enum class State {
        TYPING, FINISHED, SUGGESTED
    }
    /*
     * Simple state machine for input state
     *   <--------- SPACE + chars ------------------
     *   |                                         |
     * TYPING ---- (candidate UI Selection) ---> SUGGESTED
     *   |                                         |
     *   |<-------------                           |
     *   |           chars                    punctuation
     * punctuation     |                           |
     *   |----------> FINISH <----------------------
     */
    private var state = State.FINISHED
    private lateinit var dictionary: BinaryDictionary
    private val composing = StringBuilder()
    private var isSentenceBreak = true

    fun onCreate() {
        dictionary = BinaryDictionary(context)
    }

    fun reset() {
        state = State.FINISHED
        composing.clear()
        isSentenceBreak = true
    }

    fun processText(c: Char) {
        if (!Character.isLetter(c)) {
            isSentenceBreak = c == '.' || c == '?' || c == '!'
            composing.append(c)
            listener?.commitText(composing.toString())
            composing.clear()
            state = State.FINISHED
        } else {
            isSentenceBreak = false
            if (state == State.SUGGESTED) {
                listener?.commitText(" ")
            }
            composing.append(c)
            listener?.composingText(composing.toString())
            state = State.TYPING
        }
        updateCandidates()
    }

    fun processKeycode(keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            return if (composing.isNotEmpty()) {
                composing.delete(composing.length-1, composing.length)
                listener?.composingText(composing.toString())
                updateCandidates()
                true
            } else {
                false
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (isSentenceBreak) {
                    listener?.showCapsKeyboard()
                }
            }

            listener?.commitText(composing.toString())
            composing.clear()
            candidates.clear()

            state = State.FINISHED
            return false
        }
        return false
    }

    fun onCandidate(text: String) {
        listener?.commitText(text)
        state = State.SUGGESTED
        composing.clear()
        candidates.clear()
    }

    private fun updateCandidates() {
        if (composing.isBlank()) {
            candidates = mutableListOf()
            return
        }
        val searchWord = composing.toString()
        val suggestions = dictionary.fuseQuery(searchWord)
        val words = mutableListOf<String>()
        for (s in suggestions?.suggestions ?: listOf()) {
            if (s.mWord == searchWord) {
                continue
            }
            words.add(s.mWord)
        }
        words.add(0, searchWord)
        candidates = words
    }
}
