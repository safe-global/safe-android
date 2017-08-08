package pm.gnosis.android.app.wallet.ui.account

import pm.gnosis.android.app.wallet.data.geth.GethRepository
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.di.ForView
import javax.inject.Inject

@ForView
class AccountPresenter @Inject constructor(private val gethRepository: GethRepository,
                                           private val infuraRepository: InfuraRepository) {
    fun getAccountAddress() = gethRepository.getAccount().address.hex

    fun getAccountBalance() = infuraRepository.getBalance()
}