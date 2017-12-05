package pm.gnosis.heimdall.ui.splash

import android.arch.persistence.room.EmptyResultSetException
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.security.EncryptionManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(
        private val accountsRepository: AccountsRepository,
        private val encryptionManager: EncryptionManager,
        private val tokenRepository: TokenRepository
) : SplashContract() {
    companion object {
        const val LAUNCH_DELAY = 500L
    }

    override fun initialSetup(): Single<ViewAction> {
        return tokenRepository.setup()
                .onErrorComplete()
                .andThen(encryptionManager.initialized())
                .flatMap { isEncryptionInitialized ->
                    if (isEncryptionInitialized) {
                        checkAccount()
                    } else {
                        Single.just(StartPasswordSetup())
                    }
                }
                // We need a short delay to avoid weird flickering
                .delay(LAUNCH_DELAY, TimeUnit.MILLISECONDS)
    }

    private fun checkAccount(): Single<ViewAction> =
            accountsRepository.loadActiveAccount()
                    .flatMap { Single.just(StartMain() as ViewAction) }
                    .onErrorReturn {
                        when (it) {
                            is EmptyResultSetException, is NoSuchElementException ->
                                StartAccountSetup()
                            else ->
                                StartMain()
                        }
                    }
}
