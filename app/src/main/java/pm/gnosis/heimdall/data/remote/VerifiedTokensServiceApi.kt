package pm.gnosis.heimdall.data.remote

import io.reactivex.Single
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.remote.models.PaginatedResults
import pm.gnosis.heimdall.data.remote.models.tokens.VerifiedToken
import retrofit2.http.GET

interface VerifiedTokensServiceApi {
    @GET(BuildConfig.VERIFIED_TOKEN_SERVICE_ENDPOINT)
    fun loadVerifiedTokenList(): Single<PaginatedResults<VerifiedToken>>
}
