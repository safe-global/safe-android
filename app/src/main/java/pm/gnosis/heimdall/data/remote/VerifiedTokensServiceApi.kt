package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.tokens.VerifiedTokenJson
import retrofit2.http.GET

interface VerifiedTokensServiceApi {
    companion object {
        const val BASE_URL = "https://gist.githubusercontent.com/fmrsabino/9d42308e79daf1c014e7ac504cb449f0/raw/1c9e93a4f00cf82bb5039e0e3b344dc29ee44579/"
    }

    @GET("verified_tokens_rinkeby.json")
    fun loadVerifiedTokenList(): Single<List<VerifiedTokenJson>>
}
