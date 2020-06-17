package com.camelcc.keyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.pinyin.PinyinIME
import com.camelcc.keyboard.pinyin.PinyinExpandedListView
//import com.camelcc.keyboard.pinyin.PinyinCandidateView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job


class InputService : InputMethodService(),
    KeyboardActionListener,
    CandidateView.CandidateViewListener,
    PinyinIME.PinyinIMEListener {
    val tag = "[SK]"

    enum class IME {
        ENGLISH,
        PINYIN
    }

    enum class InputState {
        TYPING, FINISHED, SUGGESTED
    }

    private val ioJob = Job()
    private val serviceIOScope = CoroutineScope(Dispatchers.IO + ioJob)

    private lateinit var inputManager: InputMethodManager


    private var imeType = IME.ENGLISH
    private lateinit var keyboard: Keyboard
    private lateinit var keyboardView: KeyboardView

    private lateinit var candidateView: CandidateView
//    private lateinit var pinyinCandidateView: PinyinCandidateView

    private var mCompletionOn = false
    private var mPredictionOn = false
    private var mDeferSelectionUpdate = false

    // english only
    private lateinit var suggestion: TypeSuggestion
    private val mComposing = StringBuilder()
    private var mCompletions: Array<CompletionInfo> = arrayOf()
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
    private var mInputState = InputState.FINISHED
    private var mSentenceBreak = true

    // pinyinonly
    private lateinit var pinyin: PinyinIME
    private lateinit var pinyinListView: PinyinExpandedListView

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "onCreate")

        inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        suggestion = TypeSuggestion(serviceIOScope)
        // TODO: try catch
        suggestion.initializeDictionary(this)
        pinyin = PinyinIME(this)
        pinyin.setListener(this)
        pinyin.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        pinyin.onDestroy()
        ioJob.cancel()
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    override fun onInitializeInterface() {
        val displayContext = getDisplayContext()
        keyboard = if (imeType == IME.ENGLISH) EnglishKeyboard(displayContext) else PinyinKeyboard(displayContext)
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

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.clear()

        mPredictionOn = false
        mCompletionOn = false

        when (attribute.inputType.and(InputType.TYPE_MASK_CLASS)) {
            InputType.TYPE_CLASS_TEXT -> {
                mPredictionOn = true

                // We now look for a few special variations of text that will
                // modify our behavior.
                val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false
                }
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    variation == InputType.TYPE_TEXT_VARIATION_URI ||
                    variation == InputType.TYPE_TEXT_VARIATION_FILTER ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false
                }

                if (attribute.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false
                    mCompletionOn = isFullscreenMode
                }
            }
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        // TODO:
//        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions)
        keyboard.buildLayout()

        if (mPredictionOn && !mCompletionOn) {
            updateComposingRegion(attribute.initialSelStart, attribute.initialSelEnd)
        }
    }

    override fun onCreateInputView(): View {
        Log.i(tag, "onCreateInputView")
        val inputView = KeyboardView(this)
        inputView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        inputView.setKeyboard(keyboard)
        inputView.setKeyboardListener(this)
        keyboardView = inputView
        return inputView
    }

    override fun onCreateCandidatesView(): View {
        Log.i(tag, "onCreateCandidatesView")
        candidateView = CandidateView(this)
        candidateView.listener = this
//        if (imeType == IME.ENGLISH) {
//            return candidateView
//        } else if (imeType == IME.PINYIN) {
//            pinyinCandidateView =
//                PinyinCandidateView(this)
//            pinyinCandidateView.listener = this
//            return pinyinCandidateView
//        }
//        return View(this)
        return candidateView
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.i(tag, "onStartInputView")

        keyboardView.setKeyboard(keyboard)
        keyboardView.closing()
        setCandidatesViewShown(true)

        pinyinListView = PinyinExpandedListView(this)
        keyboardView.setCoverContentView(pinyinListView)

        if (imeType == IME.ENGLISH) {
            if (info.initialCapsMode == TextUtils.CAP_MODE_CHARACTERS) {
                (keyboard as EnglishKeyboard).showStickyUpper()
            } else if (info.initialCapsMode != 0) {
                (keyboard as EnglishKeyboard).showUpper()
            }
            candidateView.resetDisplayStyle(false, false)
        } else {
            candidateView.resetDisplayStyle(true, true)
        }
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    override fun onFinishInput() {
        super.onFinishInput()
        Log.i(tag, "onFinishInput")

        // Clear current composing text and candidates.
        mComposing.clear()
//        updateCandidates()

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false)
//        keyboardView.closing()
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
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (newSelStart != candidatesEnd || newSelEnd != candidatesEnd) {
            if (mDeferSelectionUpdate) {
                mDeferSelectionUpdate = false
                return
            }

            if (mPredictionOn && !mCompletionOn) {
                updateComposingRegion(newSelStart, newSelEnd)
                updateCandidates()
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
        if (!mCompletionOn) {
            return
        }

        mCompletions = completions ?: arrayOf()
        if (completions == null || completions.isEmpty()) {
            candidateView.setSuggestions(listOf(), true, false)
            return
        }

        val sug = completions.map { it.text.toString() }
        candidateView.setSuggestions(sug, true, true)
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        Log.i(tag, "onCurrentInputMethodSubtypeChanged")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            // Special handling of the delete key: if we currently are
            // composing text for the user, we want to modify that instead
            // of let the application to the delete itself.
            if (mComposing.isNotEmpty()) {
                onKey(KeyEvent.KEYCODE_DEL)
                return true
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Let the underlying text editor always handle these.
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    // no space, must be letter or punctuation or symbols
    override fun onChar(c: Char, fromPopup: Boolean) {
        val ic = currentInputConnection ?: return

        if (imeType == IME.ENGLISH) {
            if (!Character.isLetter(c)) {
                mSentenceBreak = c == '.' || c == '?' || c == '!'
                mComposing.append(c)
                ic.commitText(mComposing, 1)
                mComposing.clear()
                mDeferSelectionUpdate = true
                updateCandidates()
                mInputState = InputState.FINISHED
            } else {
                mSentenceBreak = false
                ic.beginBatchEdit()
                if (mInputState == InputState.SUGGESTED) {
                    ic.commitText(" ", 1)
                }
                mComposing.append(c)
                ic.setComposingText(mComposing, 1)
                ic.endBatchEdit()
                mDeferSelectionUpdate = true
                updateCandidates()
                mInputState = InputState.TYPING
            }
        } else if (imeType == IME.PINYIN) {
            pinyin.processText(c)
            updateCandidates()
        }
    }

    override fun onKey(keyCode: Int) {
        val ic = currentInputConnection ?: return

        if (imeType == IME.ENGLISH) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (mComposing.isNotEmpty()) {
                    mComposing.delete(mComposing.length-1, mComposing.length)
                    ic.setComposingText(mComposing, 1)
                    mDeferSelectionUpdate = true
                    updateCandidates()
                } else {
                    mDeferSelectionUpdate = false
                    sendDownUpKeyEvents(keyCode)
                }
            } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
                if (keyCode == KeyEvent.KEYCODE_SPACE) {
                    if (mSentenceBreak) {
                        (keyboard as EnglishKeyboard).showUpper()
                    }
                }

                ic.commitText(mComposing, 1)
                mDeferSelectionUpdate = true
                mComposing.clear()
                sendDownUpKeyEvents(keyCode)

                mInputState = InputState.FINISHED
            }
        } else if (imeType == IME.PINYIN) {
            if (!pinyin.processKeycode(keyCode)) {
                sendDownUpKeyEvents(keyCode)
            }
            updateCandidates()
        }
    }

    override fun onLangSwitch() {
        if (imeType == IME.ENGLISH) {
            imeType = IME.PINYIN
            keyboard = PinyinKeyboard(getDisplayContext())
            keyboard.buildLayout()
            keyboardView.setKeyboard(keyboard)
            pinyin.reset()
            candidateView.resetDisplayStyle(true, true)
//            pinyinCandidateView =
//                PinyinCandidateView(getDisplayContext())
//            pinyinCandidateView.listener = this
//            setCandidatesView(pinyinCandidateView)
            mComposing.clear()
            updateCandidates()
        } else if (imeType == IME.PINYIN) {
            imeType = IME.ENGLISH
            keyboard = EnglishKeyboard(getDisplayContext())
            keyboard.buildLayout()
            keyboardView.setKeyboard(keyboard)
            candidateView.resetDisplayStyle(false, false)
//            candidateView = CandidateView(getDisplayContext())
//            candidateView.listener = this
//            setCandidatesView(candidateView)
            mComposing.clear()
            updateCandidates()
        }
    }

    override fun onSuggestion(text: String, index: Int, fromCompletion: Boolean) {
        keyboardView.dismissCoverPopup()
        if (fromCompletion) {
            currentInputConnection.commitCompletion(mCompletions[index])
            return
        }
        if (imeType == IME.ENGLISH) {
            currentInputConnection.commitText(text, 1)
            mInputState = InputState.SUGGESTED
            mComposing.clear()
        } else if (imeType == IME.PINYIN) {
            pinyin.onChoiceTouched(index)
        }

        mDeferSelectionUpdate = true
        updateCandidates()
    }

    override fun onMoreExpand() {
        if (imeType == IME.ENGLISH) {
            return
        }
        keyboardView.showCoverPopup()
    }

    override fun onMoreDismiss() {
        if (imeType == IME.ENGLISH) {
            return
        }
        keyboardView.dismissCoverPopup()
    }

    override fun commitText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        updateCandidates()
    }

    override fun commitCompletion(ci: CompletionInfo) {
        val ic = currentInputConnection ?: return
        ic.commitCompletion(ci)
        updateCandidates()
    }

    override fun getTextBeforeCursor(length: Int): CharSequence {
        val ic = currentInputConnection ?: return ""
        return ic.getTextBeforeCursor(length, 0)
    }

    private fun updateComposingRegion(selStart: Int, selEnd: Int) {
        if (imeType == IME.ENGLISH) {
            mComposing.clear()
            if (currentInputConnection == null) {
                return
            }
            val before = currentInputConnection.getTextBeforeCursor(48, 0)
            val current = currentInputConnection.getSelectedText(0)
            val after = currentInputConnection.getTextAfterCursor(48, 0)
            if (!current.isNullOrEmpty()) {
                mComposing.append(current)
            }
            if (!mPredictionOn || mCompletionOn) {
                return
            }
            mInputState =
                if (!before.isNullOrBlank() && before[before.length - 1].isLetter() && (current.isNullOrEmpty() || current[0].isLetter())) {
                    InputState.TYPING
                } else {
                    InputState.FINISHED
                }
            var start = selStart
            if (!before.isNullOrBlank()) {
                for (i in before.indices.reversed()) {
                    if (!Character.isSpaceChar(before[i])) {
                        mComposing.insert(0, before[i])
                        start--
                        mSentenceBreak =
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
                        mComposing.append(after[i])
                        end++
                    } else {
                        break
                    }
                }
            }
            if (start == selStart && end == selEnd) {
                return
            }
            currentInputConnection.setComposingRegion(start, end)
        }
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private fun updateCandidates() {
        if (imeType == IME.ENGLISH) {
            if (mComposing.isBlank()) {
                candidateView.setSuggestions(listOf(), false, false)
            }
            if (!mPredictionOn || mCompletionOn) {
                return
            }
            val searchWord = mComposing.toString()
            val suggestions = suggestion.dictionary?.fuseQuery(searchWord)
            val words = mutableListOf<String>()
            for (s in suggestions?.suggestions ?: listOf()) {
                if (s.mWord == searchWord) {
                    continue
                }
                words.add(s.mWord)
            }
            words.add(0, searchWord)
            candidateView.setSuggestions(words, false, suggestions?.valid ?: false)
        } else if (imeType == IME.PINYIN) {
            candidateView.setSuggestions(pinyin.candidates, false, false, pinyin.displayComposing ?: "")
//            pinyinCandidateView.setSuggestions(pinyin.candidates, false)
        }
    }
}
