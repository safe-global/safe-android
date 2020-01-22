package pm.gnosis.heimdall.data.remote

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.*
import pm.gnosis.heimdall.data.remote.models.tokens.TokenInfo
import retrofit2.http.*


interface RelayServiceApi {
    @POST("v1/safes/{address}/transactions/")
    fun execute(@Path("address") address: String, @Body params: ExecuteParams): Single<RelayExecution>

    @POST("v1/safes/{address}/transactions/estimate/")
    fun estimate(@Path("address") address: String, @Body params: EstimateParams): Single<RelayEstimate>

    @GET("v1/tokens/?limit=3000&ordering=relevance,name&gas=true")
    fun paymentTokens(): Single<PaginatedResults<TokenInfo>>

    @GET("v1/tokens/?limit=1000&ordering=relevance,name")
    fun tokens(@Query("search") search: String): Single<PaginatedResults<TokenInfo>>

    @POST("v1/safes/{address}/transactions/estimates/")
    fun transactionEstimates(@Path("address") address: String, @Body params: EstimatesParams): Single<RelayEstimates>

    @PUT("v2/safes/{address}/funded/")
    fun notifySafeFunded(@Path("address") address: String): Completable

    @GET("v2/safes/{address}/funded/")
    fun safeFundStatus(@Path("address") address: String): Single<RelaySafeFundStatus>

    @POST("v3/safes/estimates/")
    fun creationEstimates(@Body params: CreationEstimatesParams): Single<List<CreationEstimate>>

    @POST("v3/safes/")
    fun safeCreation(@Body params: RelaySafeCreationParams): Single<RelaySafeCreation>
}
