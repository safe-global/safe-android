package pm.gnosis.android.app.wallet.data.remote

import retrofit2.http.POST
import retrofit2.http.Query

interface EtherscanService {
    companion object {
        const val RINKEBY_BASE_URL = "https://rinkeby.etherscan.io/"
        const val SEND_RAW_TRANSACTION = "eth_sendRawTransaction"
    }

    @POST("api")
    fun post(@Query("module") module: String = "proxy",
             @Query("action") action: String,
             @Query("hex") signedTransactionData: String)
}