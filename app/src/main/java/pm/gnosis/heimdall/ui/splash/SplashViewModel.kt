package pm.gnosis.heimdall.ui.splash

import android.arch.persistence.room.EmptyResultSetException
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(
        private val accountsRepository: AccountsRepository,
        private val tokenRepository: TokenRepository
) : SplashContract() {
    companion object {
        const val LAUNCH_DELAY = 500L
    }

    override fun initialSetup(): Single<ViewAction> {
        return tokenRepository.setup()
                .onErrorComplete()
                .andThen(accountsRepository.loadActiveAccount())
                .map { StartMain() as ViewAction }
                .onErrorReturn {
                    when (it) {
                        is EmptyResultSetException, is NoSuchElementException ->
                            StartSetup()
                        else ->
                            StartMain()
                    }
                }
                // We need a short delay to avoid weird flickering
                .delay(LAUNCH_DELAY, TimeUnit.MILLISECONDS)
    }
}
