package pm.gnosis.heimdall.reporting.impl

import android.content.Context
import android.content.SharedPreferences
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FabricCore @Inject constructor(
    @ApplicationContext context: Context,
    preferencesManager: PreferencesManager
) {
    val instance = Fabric.with(context, Crashlytics())!!

    val installId: String

    init {
        installId = preferencesManager.prefs.let {
            it.getString(INSTALL_ID_KEY, null) ?: it.generateAndStoreInstallId()
        }
        Crashlytics.setUserIdentifier(installId)
    }

    private fun SharedPreferences.generateAndStoreInstallId(): String {
        val installId = UUID.randomUUID().toString()
        edit {
            putString(INSTALL_ID_KEY, installId)
        }
        return installId
    }

    companion object {
        private const val INSTALL_ID_KEY = "fabric_core.string.install_id"
    }
}
