package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.PaginatedResults
import pm.gnosis.heimdall.data.remote.models.tokens.TokenInfo
import retrofit2.http.GET
import retrofit2.http.Query


interface TokenServiceApi {
    @GET("v1/tokens/?limit=1000&ordering=relevance,name&gas=true")
    fun paymentTokens(): Single<PaginatedResults<TokenInfo>>

    @GET("v1/tokens/?limit=1000&ordering=relevance,name")
    fun tokens(@Query("search") search: String): Single<PaginatedResults<TokenInfo>>
}
