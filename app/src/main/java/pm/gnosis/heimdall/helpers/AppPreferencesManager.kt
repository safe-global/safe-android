package pm.gnosis.heimdall.helpers

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.svalinn.common.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

interface AppPreferencesManager {
    fun get(key: String? = null): SharedPreferences
}

@Singleton
class WrappedAppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
): AppPreferencesManager {
    override fun get(key: String?): SharedPreferences =
            key?.let { context.getSharedPreferences(key, Context.MODE_PRIVATE) } ?: preferencesManager.prefs
}
