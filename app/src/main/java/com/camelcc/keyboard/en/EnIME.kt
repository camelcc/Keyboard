package com.camelcc.keyboard.en

import android.content.Context
import android.view.KeyEvent
import com.camelcc.keyboard.IMEListener

class EnIME {
    var listener: IMEListener? = null
    var candidates = mutableListOf<String>()

    enum class EnImeState {
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
    private var enImeState = EnImeState.FINISHED
    private var dictionary: BinaryDictionary
    private val enComposing = StringBuilder()
    private var enSentenceBreak = true

    constructor(context: Context) {
        dictionary = BinaryDictionary(context)
    }

    fun reset() {
        enImeState = EnImeState.FINISHED
        enComposing.clear()
        enSentenceBreak = true
    }

    fun processText(c: Char) {
        if (!Character.isLetter(c)) {
            enSentenceBreak = c == '.' || c == '?' || c == '!'
            enComposing.append(c)
            listener?.commitText(enComposing.toString(), true)
            enComposing.clear()
            enImeState = EnImeState.FINISHED
        } else {
            enSentenceBreak = false
            if (enImeState == EnImeState.SUGGESTED) {
                listener?.commitText(" ", true)
            }
            enComposing.append(c)
            listener?.composingText(enComposing.toString(), true)
            enImeState = EnImeState.TYPING
        }
    }

    fun processKeycode(keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (enComposing.isNotEmpty()) {
                enComposing.delete(enComposing.length-1, enComposing.length)
                listener?.composingText(enComposing.toString(), true)
                return true
            } else {
                return false
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (enSentenceBreak) {
                    listener?.showCapsKeyboard()
                }
            }

            listener?.commitText(enComposing.toString(), true)
            enComposing.clear()

            enImeState = EnImeState.FINISHED
            return false
        }
        return false
    }

    fun onCandidate(text: String) {
        listener?.commitText(text)
        enImeState = EnImeState.SUGGESTED
        enComposing.clear()
    }

    fun reselectComposing(selStart: Int, selEnd: Int, before: CharSequence?, current: CharSequence?, after: CharSequence?): IntArray {
        enComposing.clear()
        if (!current.isNullOrEmpty()) {
            enComposing.append(current)
        }
        enImeState =
            if (!before.isNullOrBlank() && before[before.length - 1].isLetter() && (current.isNullOrEmpty() || current[0].isLetter())) {
                EnImeState.TYPING
            } else {
                EnImeState.FINISHED
            }
        var start = selStart
        if (!before.isNullOrBlank()) {
            for (i in before.indices.reversed()) {
                if (!Character.isSpaceChar(before[i])) {
                    enComposing.insert(0, before[i])
                    start--
                    enSentenceBreak =
                        i > 0 && (before[i - 1] == '.' || before[i - 1] == '?' || before[i - 1] == '!')
                } else {
                    break
                }
            }
        }
        var end = selEnd
        if (!after.isNullOrBlank()) {
            for (i in after.indices) {
                if (!Character.isSpaceChar(after[i])) {
                    enComposing.append(after[i])
                    end++
                } else {
                    break
                }
            }
        }
        return intArrayOf(start, end)
    }

    fun updateCandidates() {
        if (enComposing.isBlank()) {
            candidates = mutableListOf()
            return
        }
        val searchWord = enComposing.toString()
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
