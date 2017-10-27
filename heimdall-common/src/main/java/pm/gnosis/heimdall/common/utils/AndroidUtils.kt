package pm.gnosis.heimdall.common.utils

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment

inline fun SharedPreferences.edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.func()
    editor.apply()
}

inline fun Bundle.build(func: Bundle.() -> Unit): Bundle {
    this.func()
    return this
}


fun Fragment.withArgs(args: Bundle): Fragment {
    this.arguments = args
    return this
}
