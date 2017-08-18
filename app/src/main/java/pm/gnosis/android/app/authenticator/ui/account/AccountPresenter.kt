package pm.gnosis.android.app.authenticator.ui.account

import pm.gnosis.android.app.authenticator.data.geth.GethRepository
import pm.gnosis.android.app.authenticator.data.remote.EthereumJsonRpcRepository
import pm.gnosis.android.app.authenticator.di.ForView
import javax.inject.Inject

@ForView
class AccountPresenter @Inject constructor(private val gethRepository: GethRepository,
                                           private val ethereumJsonRpcRepository: EthereumJsonRpcRepository) {
    fun getAccountAddress() = gethRepository.getAccount().address.hex
    fun getAccountBalance() = ethereumJsonRpcRepository.getBalance()
}
