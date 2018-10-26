package pm.gnosis.tests.utils

import android.content.Intent
import android.os.Bundle
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import pm.gnosis.utils.toHex

fun matchesIntentExactly(intent: Intent): Matcher<Intent> = ExactIntentMatcher(intent)

private class ExactIntentMatcher internal constructor(private val expectedIntent: Intent) : TypeSafeMatcher<Intent>(Intent::class.java) {
    public override fun matchesSafely(intent: Intent): Boolean {
        if (intent.component != expectedIntent.component) return false
        if (intent.flags != expectedIntent.flags) return false
        if (intent.action != expectedIntent.action) return false
        if (intent.data != expectedIntent.data) return false
        // TODO add all attributes
        val bundle = intent.extras
        val expectedBundle = expectedIntent.extras
        if (bundle != null && expectedBundle != null) {
            return bundle.isSupersetBundle(expectedBundle) && expectedBundle.isSupersetBundle(bundle)
        }
        return bundle == expectedBundle
    }

    override fun describeTo(description: Description) {
        description.appendText("has bundle that matches exactly")
    }
}

private fun Bundle.isSupersetBundle(subSet: Bundle): Boolean {
    for (key in subSet.keySet()) {
        if (!containsKey(key)) return false
        val otherExtra = subSet.get(key)
        val thisExtra = get(key)
        if (thisExtra is ByteArray && otherExtra is ByteArray && thisExtra.toHex() == otherExtra.toHex()) continue // This is valid
        if (thisExtra != otherExtra) return false
    }
    return true
}
