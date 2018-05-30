package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.EstimateParams
import pm.gnosis.heimdall.data.remote.models.ExecuteParams
import pm.gnosis.heimdall.data.remote.models.RelayEstimate
import pm.gnosis.heimdall.data.remote.models.RelayExecution
import retrofit2.http.Body
import retrofit2.http.POST


interface RelayServiceApi {
    companion object {
        const val BASE_URL = "https://safe-push.dev.gnosisdev.com/api/"
    }

    @POST("v1/transactions/")
    fun execute(@Body params: ExecuteParams): Single<RelayExecution>

    @POST("v1/transactions/estimate/")
    fun estimate(@Body params: EstimateParams): Single<RelayEstimate>
}
