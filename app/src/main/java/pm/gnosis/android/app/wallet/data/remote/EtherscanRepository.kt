package pm.gnosis.android.app.wallet.data.remote

import io.reactivex.Observable
import pm.gnosis.android.app.wallet.data.AccountManager
import pm.gnosis.android.app.wallet.data.model.Balance
import pm.gnosis.android.app.wallet.data.model.BlockNumber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EtherscanRepository @Inject constructor(private val etherscanApi: EtherscanApi,
                                              private val accountManager: AccountManager) {
    fun getEtherBalance(): Observable<Balance> =
            etherscanApi.etherBalance(accountManager.getAccount().address.hex)

    fun getMostRecentBlockNumber(): Observable<BlockNumber> =
            etherscanApi.mostRecentBlockNumber()
}
