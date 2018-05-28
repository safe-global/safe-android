package pm.gnosis.heimdall.data.repositories

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import java.math.BigInteger


interface GnosisSafeModulesRepository {

    fun loadModulesInfo(modules: List<Solidity.Address>): Single<List<Pair<Module, Solidity.Address>>>

    enum class Module {
        SOCIAL_RECOVERY,
        SINGLE_ACCOUNT_RECOVERY,
        DAILY_LIMIT,
        UNKNOWN
    }

    class UnknownModuleException : IllegalArgumentException("Unknown module")
}
