package pm.gnosis.heimdall.ui.splash

import io.reactivex.Single
import pm.gnosis.svalinn.security.EncryptionManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(
    private val encryptionManager: EncryptionManager
) : SplashContract() {
    companion object {
        const val LAUNCH_DELAY = 500L
    }

    override fun initialSetup(): Single<ViewAction> {
        return encryptionManager.initialized()
            .flatMap { isEncryptionInitialized ->
                if (isEncryptionInitialized)  checkUnlocked()
                else Single.just(StartPasswordSetup)
            }
            // We need a short delay to avoid weird flickering
            .delay(LAUNCH_DELAY, TimeUnit.MILLISECONDS)
    }

    private fun checkUnlocked(): Single<ViewAction> =
            encryptionManager.unlocked()
                .map {
                    if (it) StartMain else StartUnlock
                }
                .onErrorReturnItem(StartUnlock)
}
