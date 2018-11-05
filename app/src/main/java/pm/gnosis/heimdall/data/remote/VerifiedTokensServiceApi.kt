package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.tokens.VerifiedTokenResult
import retrofit2.http.GET

interface VerifiedTokensServiceApi {
    @GET("verified_rinkeby_tokens_v2.json")
    fun loadVerifiedTokenList(): Single<VerifiedTokenResult>
}
