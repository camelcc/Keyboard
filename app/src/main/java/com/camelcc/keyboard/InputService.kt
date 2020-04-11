package com.camelcc.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype

class InputService : InputMethodService() {
    val tag = "[SK]"

    private var candidateView: CandidateView? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "onCreate")
    }

    override fun onCreateInputView(): View {
        Log.i(tag, "onCreateInputView")
        val inputView = KeyboardView(this)
        inputView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        inputView.setKeyboard(Keyboard(this, inputView))
        return inputView
    }

    override fun onCreateCandidatesView(): View {
        Log.i(tag, "onCreateCandidatesView")
        candidateView = CandidateView(this)
        return candidateView!!
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.i(tag, "onStartInputView")
        setCandidatesViewShown(true)
        candidateView?.setSuggestions(listOf("hell", "hello", "haha"), false)
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        Log.i(tag, "onCurrentInputMethodSubtypeChanged")
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.i(tag, "onFinishInput")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "onDestroy")
    }
}
