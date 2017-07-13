package pm.gnosis.android.app.wallet.util

import android.content.SharedPreferences

inline fun SharedPreferences.edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.func()
    editor.apply()
}

//Map functions that throw exceptions into optional types
fun <T> nullOnThrow(func: () -> T): T? = try { func.invoke() } catch (e: Exception) { null }
