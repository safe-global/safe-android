package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.tokens.VerifiedTokenJson
import retrofit2.http.GET

interface VerifiedTokensServiceApi {
    companion object {
        const val BASE_URL = "https://gist.githubusercontent.com/rmeissner/98911fcf74b0ea9731e2dae2441c97a4/raw/"
    }

    @GET("verified_rinkeby_tokens.json")
    fun loadVerifiedTokenList(): Single<List<VerifiedTokenJson>>
}
