package pm.gnosis.heimdall.data.repositories

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger


interface GnosisSafeExtensionRepository {

    fun buildAddRecoverExtensionTransaction(recoverOwners: List<Solidity.Address>): Single<SafeTransaction>
    fun loadExtensionsInfo(extensions: List<Solidity.Address>): Single<List<Pair<Extension, Solidity.Address>>>

    enum class Extension {
        SOCIAL_RECOVERY,
        DAILY_LIMIT,
        UNKNOWN
    }

    class UnknownExtensionException: IllegalArgumentException("Unknown extension")

    fun buildAddDailyLimitExtensionTransaction(limits: List<Pair<Solidity.Address, BigInteger>>): Single<SafeTransaction>
}
