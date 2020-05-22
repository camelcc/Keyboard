package com.camelcc.keyboard

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.view.textservice.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.*
import java.util.*
import java.util.stream.Stream
import kotlin.collections.ArrayList


class InputService : InputMethodService(),
    KeyboardActionListener,
    CandidateView.CandidateViewListener,
    SpellCheckerSession.SpellCheckerSessionListener {
    val tag = "[SK]"

    private val ioJob = Job()
    private val serviceIOScope = CoroutineScope(Dispatchers.IO + ioJob)

    private lateinit var inputManager: InputMethodManager
//    private lateinit var textService: TextServicesManager
    private lateinit var suggestion: TypeSuggestion

    private lateinit var keyboard: Keyboard
    private lateinit var keyboardView: KeyboardView
    private lateinit var candidateView: CandidateView

    private val mComposing = StringBuilder()
    private var mPredictionOn = false
    private var mCompletionOn = false
    private var mCompletions: Array<CompletionInfo> = arrayOf()

//    private lateinit var spellCheckSession: SpellCheckerSession

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "onCreate")

        inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        textService = getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        suggestion = TypeSuggestion(serviceIOScope)
        // TODO: try catch
        suggestion.initializeDictionary(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ioJob.cancel()
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    override fun onInitializeInterface() {
        val displayContext = getDisplayContext()
        keyboard = Keyboard(displayContext)
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
//        spellCheckSession = textService.newSpellCheckerSession(null, Locale.US, this, false)

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.clear()

        mPredictionOn = false
        mCompletionOn = false
        mCompletions = arrayOf()

        when (attribute.inputType.and(InputType.TYPE_MASK_CLASS)) {
            InputType.TYPE_CLASS_TEXT -> {
                mPredictionOn = true

                // We now look for a few special variations of text that will
                // modify our behavior.
                val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false
                }
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    variation == InputType.TYPE_TEXT_VARIATION_URI ||
                    variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
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
        return candidateView
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.i(tag, "onStartInputView")

        keyboardView.setKeyboard(keyboard)
        keyboardView.closing()

        setCandidatesViewShown(true)
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
        updateCandidates()

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false)
        keyboardView.closing()

//        spellCheckSession.close()
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
        if (mComposing.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            mComposing.clear()
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

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        if (text.isBlank() || !(text[0] in 'a'..'z' || text[0] in 'A'..'Z')) {
            ic.beginBatchEdit()
            ic.commitText(mComposing, 1)
            mComposing.clear()
            ic.commitText(text, 0)
            ic.endBatchEdit()
            return
        }

        mComposing.append(text)
        ic.setComposingText(mComposing, 1)
        updateCandidates()
//        spellCheckSession.getSentenceSuggestions(arrayOf(TextInfo(mComposing.toString())), 10)
//        candidateView.setSuggestions(listOf(mComposing.toString()), false, false)
    }

    override fun onKey(keyCode: Int) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (mComposing.length > 1) {
                mComposing.delete(mComposing.length-1, mComposing.length)
                currentInputConnection.setComposingText(mComposing, 1)
                updateCandidates()
            } else if (mComposing.isNotEmpty()) {
                mComposing.clear()
                updateCandidates()
            } else {
                sendDownUpKeyEvents(keyCode)
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            currentInputConnection.commitText(mComposing, 1)
            mComposing.clear()
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            updateCandidates()
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // Handle separator
            if (mComposing.isNotEmpty()) {
                currentInputConnection.commitText(mComposing, 1)
                mComposing.clear()
                updateCandidates()
            }
            sendDownUpKeyEvents(keyCode)
        }
    }

    override fun onSuggestion(text: String, index: Int, fromCompletion: Boolean) {
        if (fromCompletion) {
            currentInputConnection.commitCompletion(mCompletions[index])
            return
        }
        currentInputConnection.commitText(text, 1)
        mComposing.clear()
        updateCandidates()
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) {
        val suggesions = results.map { sentenceSuggestions ->
            (0 until sentenceSuggestions.suggestionsCount).map { sentenceSuggestions.getSuggestionsInfoAt(it) }
        }.flatten().map { suggestionsInfo ->
            (0 until suggestionsInfo.suggestionsCount).map { suggestionsInfo.getSuggestionAt(it) }
        }.flatten()
        candidateView.setSuggestions(suggesions, false, true)
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        val suggesions = results.map {  suggestionsInfo ->
            (0 until suggestionsInfo.suggestionsCount).map { suggestionsInfo.getSuggestionAt(it) }
        }.flatten()
        candidateView.setSuggestions(suggesions, false, true)
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private fun updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.isNotEmpty()) {
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
            } else {
                candidateView?.setSuggestions(listOf(), false, false)
            }
        }
    }
}
