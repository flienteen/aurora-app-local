package com.persidius.eos.aurora.ui.components

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.util.AttributeSet
import android.view.View
import android.widget.*
import java.util.*


class MultiSelectionSpinner : Spinner, OnMultiChoiceClickListener {

    interface OnSpinnerEventsListener {
        fun onSpinnerOpened()
        fun onSpinnerClosed()
    }

    private var listener: OnSpinnerEventsListener? = null
    private var _items: Array<String>? = null
    private var mSelection: BooleanArray? = null
    private var simpleAdapter: ArrayAdapter<String>

    constructor(context: Context?) : super(context, MODE_DROPDOWN) {
        simpleAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item)
        super.setAdapter(simpleAdapter)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        simpleAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item)
        super.setAdapter(simpleAdapter)
    }

    fun addSpinnerListener(listener: OnSpinnerEventsListener) {
        this.listener = listener
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            listener?.onSpinnerClosed()
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int, isChecked: Boolean) {
        if (mSelection != null && which < mSelection!!.size) {
            mSelection!![which] = isChecked
            simpleAdapter.clear()
            simpleAdapter.add(buildSelectedItemString())
        } else {
            throw IllegalArgumentException("Argument 'which' is out of bounds.")
        }
    }

    override fun performClick(): Boolean {
        val builder = AlertDialog.Builder(context)
        builder.setMultiChoiceItems(_items, mSelection, this)
        val popup = builder.show()
        listener?.onSpinnerOpened()
//        post { dropDownVerticalOffset += -200 }
//        val layoutParams = popup?.window?.attributes
//        if (layoutParams != null) {
//            layoutParams.y += -100
//            popup.window?.attributes = layoutParams
//        }
        return true
    }

    override fun setAdapter(adapter: SpinnerAdapter) {
        throw RuntimeException("setAdapter is not supported by MultiSelectSpinner.")
    }

    fun setItems(items: Array<String>?) {
        _items = items
        mSelection = BooleanArray(_items!!.size)
        simpleAdapter.clear()
//        simpleAdapter.add(_items!![0])
        simpleAdapter.add(buildSelectedItemString())
        Arrays.fill(mSelection, false)
    }

    fun setItems(items: List<String>) {
        _items = items.toTypedArray()
        mSelection = BooleanArray(_items!!.size)
        simpleAdapter.clear()
//        simpleAdapter.add(_items!![0])
        simpleAdapter.add(buildSelectedItemString())
        Arrays.fill(mSelection, false)
    }

    fun setSelection(selection: Array<String>) {
        for (cell in selection) {
            for (j in _items!!.indices) {
                if (_items!![j] == cell) {
                    mSelection!![j] = true
                }
            }
        }
    }

    fun setSelection(selection: List<String>) {
        for (i in mSelection!!.indices) {
            mSelection!![i] = false
        }
        for (sel in selection) {
            for (j in _items!!.indices) {
                if (_items!![j] == sel) {
                    mSelection!![j] = true
                }
            }
        }
        simpleAdapter.clear()
        simpleAdapter.add(buildSelectedItemString())
    }

    override fun setSelection(index: Int) {
        for (i in mSelection!!.indices) {
            mSelection!![i] = false
        }
        if (index >= 0 && index < mSelection!!.size) {
            mSelection!![index] = true
        } else {
            throw IllegalArgumentException("Index " + index + " is out of bounds.")
        }
        simpleAdapter.clear()
        simpleAdapter.add(buildSelectedItemString())
    }

    fun setSelection(selectedIndicies: IntArray) {
        for (i in mSelection!!.indices) {
            mSelection!![i] = false
        }
        for (index in selectedIndicies) {
            if (index >= 0 && index < mSelection!!.size) {
                mSelection!![index] = true
            } else {
                throw IllegalArgumentException("Index " + index + " is out of bounds.")
            }
        }
        simpleAdapter.clear()
        simpleAdapter.add(buildSelectedItemString())
    }

    val selectedStrings: List<String>
        get() {
            val selection: MutableList<String> = LinkedList()
            for (i in _items!!.indices) {
                if (mSelection!![i]) {
                    selection.add(_items!![i])
                }
            }
            return selection
        }

    val selectedIndicies: List<Int>
        get() {
            val selection: MutableList<Int> = LinkedList()
            for (i in _items!!.indices) {
                if (mSelection!![i]) {
                    selection.add(i)
                }
            }
            return selection
        }

    private fun buildSelectedItemString(): String {
//        val sb = StringBuilder()
//        var foundOne = false
//        for (i in _items!!.indices) {
//            if (mSelection!![i]) {
//                if (foundOne) {
//                    sb.append(", ")
//                }
//                foundOne = true
//                sb.append(_items!![i])
//            }
//        }
//        return sb.toString()
        return "Status"
    }

    val selectedItemsAsString: String
        get() {
            val sb = StringBuilder()
            var foundOne = false
            for (i in _items!!.indices) {
                if (mSelection!![i]) {
                    if (foundOne) {
                        sb.append(", ")
                    }
                    foundOne = true
                    sb.append(_items!![i])
                }
            }
            return sb.toString()
        }
}