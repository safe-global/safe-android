package pm.gnosis.android.app.authenticator.ui.account

import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.authenticator.data.remote.EthereumJsonRpcRepository
import pm.gnosis.android.app.authenticator.di.ForView
import javax.inject.Inject

@ForView
class AccountPresenter @Inject constructor(private val accountsRepository: AccountsRepository,
                                           private val ethereumJsonRpcRepository: EthereumJsonRpcRepository) {
    fun getAccountAddress() = accountsRepository.loadActiveAccount()
    fun getAccountBalance() = ethereumJsonRpcRepository.getBalance()
}
