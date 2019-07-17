package pm.gnosis.heimdall.utils

import android.text.SpannableString
import android.text.style.UnderlineSpan

fun String.asMiddleEllipsized(boundariesLength: Int): String {
    return if (this.length > boundariesLength * 2)
        "${this.subSequence(0, boundariesLength)}...${this.subSequence(this.length - boundariesLength, this.length)}"
    else this
}

fun String.underline(): CharSequence =
    SpannableString(this).also {
        it.setSpan(UnderlineSpan(), 0, length, 0)
    }
