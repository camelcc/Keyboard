package com.camelcc.keyboard

import android.content.Context
import android.content.res.Resources
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import com.android.inputmethod.pinyin.PinyinIME
import com.camelcc.keyboard.en.EnIME

interface KeyboardListener {
    fun onLangSwitch()

    fun onKeyboardChanged()

    fun onKeyboardChar(c: Char, fromPopup: Boolean = false)
    fun onKeyboardKeyCode(keyCode: Int)

    fun onCandidate(text: String, index: Int)
    fun showMoreCandidates()
    fun dismissMoreCandidates()
}

interface IMEListener {
    fun commitText(text: String, suppressUpdates: Boolean = false)
    fun commitCompletion(ci: CompletionInfo)
    fun composingText(text: String, suppressUpdates: Boolean = false)
    fun getTextBeforeCursor(length: Int): CharSequence?
    fun showCapsKeyboard()
}

val Int.dp2px: Int get() = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)).toInt()

class InputService : InputMethodService(), KeyboardListener, IMEListener {
    enum class IMEMode {
        ENGLISH,
        PINYIN
    }

    private var imeMode = IMEMode.ENGLISH

    private lateinit var keyboard: Keyboard
    private lateinit var keyboardView: KeyboardView
    private lateinit var candidateView: CandidateView

    private var completionOn = false
    private var predictionOn = false
    private var deferSelectionUpdate = false
    private var completions: Array<CompletionInfo> = arrayOf()

    private lateinit var en: EnIME
    private lateinit var pinyin: PinyinIME

    override fun onCreate() {
        super.onCreate()
        Log.d("[IME]", "onCreate")

        en = EnIME(this)
        en.listener = this

        pinyin = PinyinIME(this)
        pinyin.setListener(this)
        pinyin.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        pinyin.onDestroy()
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    override fun onInitializeInterface() {
        val displayContext = getDisplayContext()
        keyboard = if (imeMode == IMEMode.ENGLISH) EnglishKeyboard(displayContext) else PinyinKeyboard(displayContext)
    }

    /**
     * Create new context object whose resources are adjusted to match the metrics of the display
     * which is managed by WindowManager.
     *
     * @see {@link Context.createDisplayContext
     */
    private fun getDisplayContext(): Context {
        // TODO (b/133825283): Non-activity components Resources / DisplayMetrics update when
        // moving to external display.
        // An issue in Q that non-activity components Resources / DisplayMetrics in
        // Context doesn't well updated when the IME window moving to external display.
        // Currently we do a workaround is to create new display context directly and re-init
        // keyboard layout with this context.
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return createDisplayContext(wm.defaultDisplay)
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        Log.i("[SK]", "[IME] onStartInput $restarting")

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        en.reset()
        pinyin.reset()

        predictionOn = true
        completionOn = false

        when (attribute.inputType.and(InputType.TYPE_MASK_CLASS)) {
            InputType.TYPE_CLASS_TEXT -> {
                predictionOn = true

                // We now look for a few special variations of text that will
                // modify our behavior.
                val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    predictionOn = false
                }
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    predictionOn = false
                }

                if (attribute.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    predictionOn = false
                    completionOn = isFullscreenMode
                }
            }
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        // TODO:
//        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions)
        keyboard.buildLayout()

        if (predictionOn && !completionOn) {
            updateComposingRegion(attribute.initialSelStart, attribute.initialSelEnd)
        }
    }

    override fun onCreateInputView(): View {
        Log.d("[IME]", "onCreateInputView")
        val inputView = KeyboardView(this)
        inputView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        inputView.setKeyboard(keyboard)
        //TODO: better way to hook datasource
        inputView.getCandidatesAdapter().pinyinIME = pinyin
        inputView.getCandidatesAdapter().notifyDataSetChanged()

        inputView.listener = this
        keyboardView = inputView
        return inputView
    }

    override fun onCreateCandidatesView(): View {
        Log.d("[IME]", "onCreateCandidatesView")
        candidateView = CandidateView(this)
        candidateView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        candidateView.listener = this
        return candidateView
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d("[IME]", "onStartInputView")

        keyboardView.setKeyboard(keyboard)
        keyboardView.closing()
        setCandidatesViewShown(true)

        if (imeMode == IMEMode.ENGLISH) {
            if (info.initialCapsMode == TextUtils.CAP_MODE_CHARACTERS) {
                (keyboard as EnglishKeyboard).showStickyUpper()
            } else if (info.initialCapsMode != 0) {
                (keyboard as EnglishKeyboard).showUpper()
            }
            candidateView.resetDisplayStyle(false, false)
        } else {
            candidateView.resetDisplayStyle(true, true)
        }
        updateCandidates()
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    override fun onFinishInput() {
        super.onFinishInput()
        Log.d("[IME]", "onFinishInput")

        // Clear current composing text and candidates.
        en.reset()
        pinyin.reset()

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false)
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    override fun onUpdateSelection(oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        if (!predictionOn) {
            return
        }
        if (completionOn) {
            return
        }
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (newSelStart != candidatesEnd || newSelEnd != candidatesEnd) {
            if (deferSelectionUpdate) {
                deferSelectionUpdate = false
                return
            }

            updateComposingRegion(newSelStart, newSelEnd)
            updateCandidates()
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
        if (!completionOn) {
            return
        }

        this.completions = completions ?: arrayOf()
        if (completions == null || completions.isEmpty()) {
            candidateView.setSuggestions(listOf())
            return
        }

        val sug = completions.map { it.text.toString() }
        candidateView.setSuggestions(sug)
    }

    // no space, must be letter or punctuation or symbols
    override fun onKeyboardChar(c: Char, fromPopup: Boolean) {
        if (imeMode == IMEMode.ENGLISH) {
            en.processText(c)
        } else if (imeMode == IMEMode.PINYIN) {
            pinyin.processText(c)
        }
        updateCandidates()
    }

    override fun onKeyboardKeyCode(keyCode: Int) {
        if (imeMode == IMEMode.ENGLISH) {
            if (!en.processKeycode(keyCode)) {
                deferSelectionUpdate = false
                sendDownUpKeyEvents(keyCode)
            } else {
                updateCandidates()
            }
        } else if (imeMode == IMEMode.PINYIN) {
            if (!pinyin.processKeycode(keyCode)) {
                sendDownUpKeyEvents(keyCode)
            }
            updateCandidates()
        }
    }

    override fun onLangSwitch() {
        if (imeMode == IMEMode.ENGLISH) {
            imeMode = IMEMode.PINYIN
            keyboard = PinyinKeyboard(getDisplayContext())
            keyboard.buildLayout()
            keyboardView.setKeyboard(keyboard)
            candidateView.resetDisplayStyle(true, true)
        } else if (imeMode == IMEMode.PINYIN) {
            imeMode = IMEMode.ENGLISH
            keyboard = EnglishKeyboard(getDisplayContext())
            keyboard.buildLayout()
            keyboardView.setKeyboard(keyboard)
            candidateView.resetDisplayStyle(false, false)
        }

        pinyin.reset()
        en.reset()
        updateCandidates()
    }

    override fun onKeyboardChanged() {
        keyboardView.invalidateAllKeys()
    }

    override fun onCandidate(text: String, index: Int) {
        keyboardView.dismissCandidatesPopup()
        if (completionOn) {
            currentInputConnection.commitCompletion(completions[index])
            return
        }
        if (imeMode == IMEMode.ENGLISH) {
            en.onCandidate(text)
        } else if (imeMode == IMEMode.PINYIN) {
            pinyin.onChoiceTouched(index)
        }
        updateCandidates()
    }

    override fun showMoreCandidates() {
        if (imeMode == IMEMode.ENGLISH) {
            return
        }
        keyboardView.showMoreCandidatesPopup()
    }

    override fun dismissMoreCandidates() {
        if (imeMode == IMEMode.ENGLISH) {
            return
        }
        keyboardView.dismissCandidatesPopup()
    }

    override fun commitText(text: String, suppressUpdates: Boolean) {
        val ic = currentInputConnection ?: return
        if (suppressUpdates) {
            deferSelectionUpdate = true
        }
        ic.commitText(text, 1)
        updateCandidates()
    }

    override fun commitCompletion(ci: CompletionInfo) {
        val ic = currentInputConnection ?: return
        ic.commitCompletion(ci)
        updateCandidates()
    }

    override fun composingText(text: String, suppressUpdates: Boolean) {
        val ic = currentInputConnection ?: return
        if (suppressUpdates) {
            deferSelectionUpdate = true
        }
        ic.setComposingText(text, 1)
    }

    override fun getTextBeforeCursor(length: Int): CharSequence {
        val ic = currentInputConnection ?: return ""
        return ic.getTextBeforeCursor(length, 0)
    }

    override fun showCapsKeyboard() {
        if (imeMode == IMEMode.ENGLISH) {
            (keyboard as? EnglishKeyboard)?.showUpper()
        }
    }

    private fun updateComposingRegion(selStart: Int, selEnd: Int) {
        if (!predictionOn || completionOn || currentInputConnection == null) {
            return
        }

        if (imeMode == IMEMode.ENGLISH) {
            val sel = en.reselectComposing(selStart, selEnd,
                currentInputConnection.getTextBeforeCursor(48, 0),
                currentInputConnection.getSelectedText(0),
                currentInputConnection.getTextAfterCursor(48, 0))
            currentInputConnection.setComposingRegion(sel[0], sel[1])
        }
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private fun updateCandidates() {
        if (imeMode == IMEMode.ENGLISH) {
            if (!predictionOn || completionOn) {
                return
            }
            en.updateCandidates()
            candidateView.setSuggestions(en.candidates)
        } else if (imeMode == IMEMode.PINYIN) {
            candidateView.setSuggestions(pinyin.candidates, pinyin.displayComposing ?: "")
        }
    }
}
