package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Single
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.ethereum.MappedRequest
import pm.gnosis.ethereum.MappingBulkRequest
import pm.gnosis.heimdall.GnosisSafePersonalEdition
import pm.gnosis.heimdall.data.repositories.GnosisSafeModulesRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeModulesRepository.Module
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.nullOnThrow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGnosisSafeModulesRepository @Inject constructor(
    private val ethereumRepository: EthereumRepository,
    private val settingsRepository: SettingsRepository
) : GnosisSafeModulesRepository {

    override fun loadModulesInfo(modules: List<Solidity.Address>): Single<List<Pair<Module, Solidity.Address>>> {
        val requests = modules.mapIndexed { index, module ->
            MappedRequest(
                EthCall(
                    transaction = Transaction(
                        module,
                        data = GnosisSafePersonalEdition.NAME.encode() // We can use the NAME method from any contract
                    ),
                    id = index
                ), {
                    (nullOnThrow {
                        loadModuleType(GnosisSafePersonalEdition.NAME.decode(it!!).param0.value)
                    } ?: Module.UNKNOWN) to module
                })
        }.toList()

        return ethereumRepository.request(MappingBulkRequest(requests)).map { it.mapped() }.firstOrError()
    }

    private fun loadModuleType(name: String): Module =
        when (name) {
            "Social Recovery Module" -> Module.SOCIAL_RECOVERY
            "Single Account Recovery Module" -> Module.SINGLE_ACCOUNT_RECOVERY
            "Daily Limit Module" -> Module.DAILY_LIMIT
            else -> Module.UNKNOWN
        }

    private fun getModuleMasterCopyAddress(module: Module): Solidity.Address =
        when (module) {
            Module.SINGLE_ACCOUNT_RECOVERY -> settingsRepository.getRecoveryModuleMasterCopyAddress()
            Module.DAILY_LIMIT -> settingsRepository.getDailyLimitModuleMasterCopyAddress()
            Module.SOCIAL_RECOVERY, Module.UNKNOWN -> throw GnosisSafeModulesRepository.UnknownModuleException()
        }
}
