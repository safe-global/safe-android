package io.gnosis.safe.utils

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast

enum class DrawablePosition(val index: Int) {
    DRAWABLE_LEFT(0),
    DRAWABLE_TOP(1),
    DRAWABLE_RIGHT(2),
    DRAWABLE_BOTTOM(3)
}

@SuppressLint("ClickableViewAccessibility")
inline fun TextView.setOnCompoundDrawableClicked(
    position: DrawablePosition = DrawablePosition.DRAWABLE_RIGHT,
    crossinline onClick: () -> Unit
): TextView = apply {
    setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP &&
            event.rawX >= (this.right - this.compoundDrawables[position.index].bounds.width())
        ) {
            onClick()
            true
        }
        false
    }
}

