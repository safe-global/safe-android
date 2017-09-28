package pm.gnosis.heimdall.ui.splash

import io.reactivex.Completable
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.common.util.edit
import pm.gnosis.heimdall.data.db.ERC20Token
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.utils.asEthereumAddressString
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ForView
class SplashPresenter @Inject constructor(private val preferencesManager: PreferencesManager,
                                          private val gnosisAuthenticatorDb: GnosisAuthenticatorDb,
                                          private val accountsRepository: AccountsRepository) {
    companion object {
        const val DELAY_NOT_FIRST_LAUNCH = 1500L
        const val DELAY_FIRST_LAUNCH = 2000L
    }

    fun initialSetup(): Completable {
        val isFirstLaunch = preferencesManager.prefs.getBoolean(PreferencesManager.FIRST_LAUNCH_KEY, true)
        return Completable.fromCallable {
            if (isFirstLaunch) {
                val tokens = ERC20.verifiedTokens.entries.map {
                    val erc20Token = ERC20Token()
                    erc20Token.address = it.key.asEthereumAddressString()
                    erc20Token.name = it.value
                    erc20Token.verified = true
                    return@map erc20Token
                }.toList()
                gnosisAuthenticatorDb.erc20TokenDao().insertERC20Tokens(tokens)
                preferencesManager.prefs.edit { putBoolean(PreferencesManager.FIRST_LAUNCH_KEY, false) }
            }
        }.delay(if (isFirstLaunch) DELAY_FIRST_LAUNCH else DELAY_NOT_FIRST_LAUNCH, TimeUnit.MILLISECONDS)
    }

    fun loadActiveAccount() = accountsRepository.loadActiveAccount()
}
