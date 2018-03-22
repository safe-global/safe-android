package pm.gnosis.heimdall.data.remote

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface TxExecutorApi {

    companion object {
        const val BASE_URL = "https://gnosis-tx-executor.herokuapp.com"
        // TODO this might be added via interceptor
        const val HEADER_AUTH_ACCOUNT = "AUTH_ACCOUNT"
        const val HEADER_AUTH_SIGNATURE = "AUTH_SIGNATURE"
    }

    @POST("api/2/execute_tx")
    fun executeTx(
        @Header(HEADER_AUTH_ACCOUNT) account: String,
        @Header(HEADER_AUTH_SIGNATURE) signature: String,
        @Body data: TxExecutionData
    ): Observable<TxExecutionResponse>

    @POST("api/1/estimate_tx")
    fun estimateTx(
        @Header(HEADER_AUTH_ACCOUNT) account: String,
        @Header(HEADER_AUTH_SIGNATURE) signature: String,
        @Body data: TxExecutionData
    ): Observable<TxExecutionEstimate>

    @GET("api/1/balance")
    fun balance(
        @Header(HEADER_AUTH_ACCOUNT) account: String,
        @Header(HEADER_AUTH_SIGNATURE) signature: String
    ): Observable<TxExecutionBalance>

    @POST("api/1/redeem")
    fun reedeemVoucher(
        @Header(HEADER_AUTH_ACCOUNT) account: String,
        @Header(HEADER_AUTH_SIGNATURE) signature: String,
        @Body data: TxExecutionVoucherData
    ): Single<TxExecutionBalance>

}
