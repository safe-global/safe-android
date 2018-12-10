package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.PaginatedResults
import pm.gnosis.heimdall.data.remote.models.tokens.TokenInfo
import retrofit2.http.GET


interface TokenServiceApi {
    @GET("v1/tokens/?gas=true")
    fun paymentTokens(): Single<PaginatedResults<TokenInfo>>
}
