package pm.gnosis.heimdall.ui.account

import pm.gnosis.heimdall.accounts.repositories.AccountsRepository
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.common.di.ForView
import javax.inject.Inject

@ForView
class AccountPresenter @Inject constructor(private val accountsRepository: AccountsRepository,
                                           private val ethereumJsonRpcRepository: EthereumJsonRpcRepository) {
    fun getAccountAddress() = accountsRepository.loadActiveAccount()
    fun getAccountBalance() = ethereumJsonRpcRepository.getBalance()
}
