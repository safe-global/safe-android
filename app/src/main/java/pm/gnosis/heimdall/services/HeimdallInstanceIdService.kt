package pm.gnosis.heimdall.services

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.data.repositories.impls.GnosisSafePushServiceRepository
import timber.log.Timber
import javax.inject.Inject

class HeimdallInstanceIdService : FirebaseInstanceIdService() {
    @Inject
    lateinit var gnosisSafePushServiceRepository: GnosisSafePushServiceRepository

    override fun onCreate() {
        super.onCreate()
        HeimdallApplication[this].component.inject(this)
    }

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        FirebaseInstanceId.getInstance().token?.let {
            Timber.d("Refreshed Token: $it")
            gnosisSafePushServiceRepository.syncAuthentication()
        }
    }
}
