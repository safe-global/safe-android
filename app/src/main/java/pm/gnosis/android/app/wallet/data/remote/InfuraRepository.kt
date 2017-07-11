package pm.gnosis.android.app.wallet.data.remote

import io.reactivex.Observable
import pm.gnosis.android.app.wallet.data.AccountManager
import pm.gnosis.android.app.wallet.data.model.Balance
import pm.gnosis.android.app.wallet.data.model.JsonRpcRequest
import pm.gnosis.android.app.wallet.data.model.Wei
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InfuraRepository @Inject constructor(private val infuraApi: InfuraApi,
                                           private val accountManager: AccountManager) {
    companion object {
        const val DEFAULT_BLOCK_EARLIEST = "earliest"
        const val DEFAULT_BLOCK_LATEST = "latest"
        const val DEFAULT_BLOCK_PENDING = "pending"
    }

    fun getBalance(): Observable<Balance> =
            infuraApi.post(
                    JsonRpcRequest(
                            method = "eth_getBalance",
                            params = arrayListOf(accountManager.getAccount().address.hex, DEFAULT_BLOCK_LATEST)))
                    .map { Balance(Wei(BigInteger(it.result.removePrefix("0x"), 16))) }
}