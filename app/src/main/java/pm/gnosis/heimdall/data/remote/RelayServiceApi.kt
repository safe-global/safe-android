package pm.gnosis.heimdall.data.remote

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.*
import retrofit2.http.*


interface RelayServiceApi {
    companion object {
        const val BASE_URL = "https://safe-relay.dev.gnosisdev.com/api/"
    }

    @POST("v1/transactions/")
    fun execute(@Body params: ExecuteParams): Single<RelayExecution>

    @POST("v1/transactions/estimate/")
    fun estimate(@Body params: EstimateParams): Single<RelayEstimate>

    @POST("v1/safes/")
    fun safeCreation(@Body params: RelaySafeCreationParams): Single<RelaySafeCreation>

    @PUT("v1/safes/{address}/funded/")
    fun notifySafeFunded(@Path("address") address: String): Completable

    @GET("v1/safes/{address}/funded/")
    fun safeFundStatus(@Path("address") address: String): Single<RelaySafeFundStatus>
}
