package pm.gnosis.heimdall.common.utils

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.View

inline fun SharedPreferences.edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.func()
    editor.apply()
}

inline fun FragmentManager.transaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun View.visible(visible: Boolean, hiddenVisibility: Int = View.GONE) {
    this.visibility = if (visible) View.VISIBLE else hiddenVisibility
}

inline fun Bundle.build(func: Bundle.() -> Unit): Bundle {
    this.func()
    return this
}

fun Fragment.withArgs(args: Bundle): Fragment {
    this.arguments = args
    return this
}

/**
 * Appends the character sequence `text` and spans `what` over the appended part.
 * See [Spanned] for an explanation of what the flags mean.
 * @param text the character sequence to append.
 * @param what the object to be spanned over the appended text.
 * @param flags see [Spanned].
 * @return this `SpannableStringBuilder`.
 *
 * Note: This is copied from the Android source to have support on older platforms
 */
fun SpannableStringBuilder.appendText(text: CharSequence, what: Any, flags: Int = 0): SpannableStringBuilder {
    val start = length
    append(text)
    setSpan(what, start, length, flags)
    return this
}
