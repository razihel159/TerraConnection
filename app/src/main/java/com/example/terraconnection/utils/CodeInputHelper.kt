package com.example.terraconnection.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import com.example.terraconnection.R

class CodeInputHelper(private val digits: List<EditText>) {
    private var onCodeCompleteListener: ((String) -> Unit)? = null

    init {
        setupDigitInputs()
    }

    private fun setupDigitInputs() {
        digits.forEachIndexed { index, editText ->
            // Add text change listener
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < digits.size - 1) {
                        digits[index + 1].requestFocus()
                    }
                    checkCodeComplete()
                }
            })

            // Add key listener for backspace
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        digits[index - 1].apply {
                            requestFocus()
                            setText("")
                        }
                        return@setOnKeyListener true
                    }
                }
                false
            }

            // Add focus change listener
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editText.setSelection(editText.text.length)
                }
            }
        }
    }

    private fun checkCodeComplete() {
        val code = getCode()
        if (code.length == digits.size) {
            onCodeCompleteListener?.invoke(code)
        }
    }

    fun getCode(): String {
        return digits.joinToString("") { it.text.toString() }
    }

    fun setCode(code: String) {
        code.forEachIndexed { index, char ->
            if (index < digits.size) {
                digits[index].setText(char.toString())
            }
        }
    }

    fun clearCode() {
        digits.forEach { it.setText("") }
        digits.firstOrNull()?.requestFocus()
    }

    fun setError(isError: Boolean) {
        val backgroundResId = if (isError) {
            R.drawable.code_input_background_error
        } else {
            R.drawable.code_input_background
        }
        digits.forEach { it.setBackgroundResource(backgroundResId) }
    }

    fun setOnCodeCompleteListener(listener: (String) -> Unit) {
        onCodeCompleteListener = listener
    }
} 