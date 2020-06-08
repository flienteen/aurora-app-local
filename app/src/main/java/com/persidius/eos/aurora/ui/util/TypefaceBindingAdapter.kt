package com.persidius.eos.aurora.ui.util

import android.graphics.Typeface
import android.widget.TextView
import androidx.databinding.BindingAdapter

object TypefaceBindingAdapter {
    @JvmStatic
    @BindingAdapter("android:isBold")
    fun setTypeface(v: TextView, bold: Boolean) {
        v.setTypeface(null, if(bold) Typeface.BOLD else Typeface.NORMAL)
    }
}
