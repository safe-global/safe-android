package pm.gnosis.heimdall.reporting.impl

import android.content.Context
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import pm.gnosis.heimdall.data.preferences.PreferencesSafe
import pm.gnosis.heimdall.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FabricCore @Inject constructor(
    @ApplicationContext context: Context,
    prefs: PreferencesSafe
) {
    val instance = Fabric.with(context, Crashlytics())!!

    val installId: String = prefs.installId

    init {
        Crashlytics.setUserIdentifier(installId)
    }
}
