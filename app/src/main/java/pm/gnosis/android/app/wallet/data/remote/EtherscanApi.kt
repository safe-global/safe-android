package pm.gnosis.android.app.wallet.data.remote

import io.reactivex.Observable
import pm.gnosis.android.app.wallet.data.model.Balance
import retrofit2.http.GET
import retrofit2.http.Query

interface EtherscanApi {
    companion object {
        const val RINKEBY_BASE_URL = "https://rinkeby.etherscan.io/"
    }

    @GET("api?module=account&action=balance&tag=latest")
    fun etherBalance(@Query("address") address: String): Observable<Balance>
}