package pm.gnosis.heimdall.data.remote

import io.reactivex.Observable
import pm.gnosis.heimdall.data.remote.models.TxExecutionData
import pm.gnosis.heimdall.data.remote.models.TxExecutionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TxExecutorApi {

    companion object {
        const val BASE_URL = "https://gnosis-tx-executor.herokuapp.com"
        const val HEADER_SUBSCRIPTION_TOKEN = "SUBSCRIPTION_TOKEN"
    }

    @POST("api/1/execute_tx")
    fun executeTx(@Header(HEADER_SUBSCRIPTION_TOKEN) token: String, @Body data: TxExecutionData): Observable<TxExecutionResponse>

}
