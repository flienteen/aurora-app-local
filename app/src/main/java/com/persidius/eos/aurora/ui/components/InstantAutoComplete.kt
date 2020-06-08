package com.persidius.eos.aurora.ui.components

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.AutoCompleteTextView
import com.persidius.eos.aurora.R

class InstantAutoComplete: androidx.appcompat.widget.AppCompatAutoCompleteTextView {
    private var filterIgnoreText: Boolean = false
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        processAttributes(context, attrs)
    }
    constructor(context: Context, attrs: AttributeSet, num: Int) : super(context, attrs, num) {
        processAttributes(context, attrs)
    }

    private fun processAttributes(context: Context, attrs: AttributeSet) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.InstantAutoComplete,
            0, 0)
            .apply {
                try {
                    filterIgnoreText = getBoolean(R.styleable.InstantAutoComplete_filterIgnoreText, false)
                } finally {
                    recycle()
                }
            }
    }

    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if(focused && adapter != null) {
            if(filterIgnoreText) {
                performFiltering("", 0)
            } else {
                performFiltering( text, 0)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        showDropDown()
        return super.onTouchEvent(event)
    }
}
