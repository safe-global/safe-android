package pm.gnosis.heimdall.data.repositories

import io.reactivex.Single
import pm.gnosis.model.Solidity

interface EnsRepository {
    fun resolve(url: String): Single<Solidity.Address>
}
