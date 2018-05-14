package pm.gnosis.heimdall.data.repositories

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import java.math.BigInteger


interface GnosisSafeExtensionRepository {

    fun buildAddRecoverExtensionTransaction(recoverOwner: Solidity.Address): Single<SafeTransaction>
    fun loadExtensionsInfo(extensions: List<Solidity.Address>): Single<List<Pair<Extension, Solidity.Address>>>
    fun loadRecoveryExtensionInfo(extension: Solidity.Address): Single<RecoveryExtensionInfo>
    fun buildAddDailyLimitExtensionTransaction(limits: List<Pair<Solidity.Address, BigInteger>>): Single<SafeTransaction>
    fun buildRemoveExtensionTransaction(safe: Solidity.Address, index: BigInteger, extension: Solidity.Address): Single<SafeTransaction>

    enum class Extension {
        SOCIAL_RECOVERY,
        SINGLE_ACCOUNT_RECOVERY,
        DAILY_LIMIT,
        UNKNOWN
    }

    class UnknownExtensionException : IllegalArgumentException("Unknown extension")

    data class RecoveryExtensionInfo(
        val recoverOwner: Solidity.Address,
        val triggerTime: Long,
        val replaceSafeOwnerData: ReplaceSafeOwnerData?
    )
}
