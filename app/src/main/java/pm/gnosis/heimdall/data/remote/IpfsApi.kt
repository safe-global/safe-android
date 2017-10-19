package pm.gnosis.heimdall.data.remote

import io.reactivex.Observable
import pm.gnosis.heimdall.app.core.BuildConfig
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import retrofit2.http.GET
import retrofit2.http.Path

interface IpfsApi {
    companion object {
        const val BASE_URL: String = BuildConfig.IPFS_GATEWAY_URL
    }

    @GET("/ipfs/{descriptionHash}")
    fun transactionDescription(@Path("descriptionHash") descriptionHash: String): Observable<GnosisSafeTransactionDescription>
}