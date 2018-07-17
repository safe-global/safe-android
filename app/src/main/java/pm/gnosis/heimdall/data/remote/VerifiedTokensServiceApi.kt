package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.tokens.VerifiedTokenJson
import retrofit2.http.GET

interface VerifiedTokensServiceApi {
    @GET("verified_rinkeby_tokens.json")
    fun loadVerifiedTokenList(): Single<List<VerifiedTokenJson>>
}
