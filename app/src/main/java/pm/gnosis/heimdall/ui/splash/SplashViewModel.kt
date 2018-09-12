package pm.gnosis.heimdall.ui.splash

import android.arch.persistence.room.EmptyResultSetException
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.security.EncryptionManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val encryptionManager: EncryptionManager
) : SplashContract() {
    companion object {
        const val LAUNCH_DELAY = 500L
    }

    override fun initialSetup(): Single<ViewAction> {
        return encryptionManager.initialized()
            .flatMap { isEncryptionInitialized ->
                if (isEncryptionInitialized) checkAccount()
                else Single.just(StartPasswordSetup)
            }
            // We need a short delay to avoid weird flickering
            .delay(LAUNCH_DELAY, TimeUnit.MILLISECONDS)
    }

    private fun checkAccount(): Single<ViewAction> =
        accountsRepository.loadActiveAccount()
            .flatMap { checkUnlocked() }
            .onErrorResumeNext {
                when (it) {
                    is EmptyResultSetException, is NoSuchElementException ->
                        Single.just(StartPasswordSetup)
                    else ->
                        checkUnlocked()
                }
            }

    private fun checkUnlocked(): Single<ViewAction> =
            encryptionManager.unlocked()
                .map {
                    if (it) StartMain else StartUnlock
                }
                .onErrorReturnItem(StartUnlock)
}
