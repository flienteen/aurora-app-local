package com.persidius.eos.aurora.ui.util

import android.graphics.Typeface
import android.widget.TextView
import androidx.databinding.BindingAdapter

abstract class TypefaceBindingAdapter {
    companion object {
        @JvmStatic
        @BindingAdapter("android:typeface")
        fun setTypeface(v: TextView, style: String) {
            when (style) {
                "bold" -> v.setTypeface(null, Typeface.BOLD)
                else -> v.setTypeface(null, Typeface.NORMAL)
            }
        }
    }
}
