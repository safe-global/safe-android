package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.CreateAndAddExtension
import pm.gnosis.heimdall.DailyLimitExtension
import pm.gnosis.heimdall.ProxyFactory
import pm.gnosis.heimdall.SocialRecoveryExtension
import pm.gnosis.heimdall.data.repositories.GnosisSafeExtensionRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeExtensionRepository.Extension
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.nullOnThrow
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGnosisSafeExtensionRepository @Inject constructor(
    private val ethereumRepository: EthereumRepository,
    private val settingsRepository: SettingsRepository
) : GnosisSafeExtensionRepository {

    override fun buildAddRecoverExtensionTransaction(recoverOwners: List<Solidity.Address>): Single<SafeTransaction> =
        Single.fromCallable {
            val data = SocialRecoveryExtension.Setup.encode(
                SolidityBase.Vector(recoverOwners),
                Solidity.UInt8(BigInteger.valueOf(2))
            )
            createExtensionTransaction(Extension.SOCIAL_RECOVERY, data)
        }.subscribeOn(Schedulers.computation())

    override fun buildAddDailyLimitExtensionTransaction(limits: List<Pair<Solidity.Address, BigInteger>>): Single<SafeTransaction> =
        Single.fromCallable {
            val data = DailyLimitExtension.Setup.encode(
                SolidityBase.Vector(limits.map { it.first }),
                SolidityBase.Vector(limits.map { Solidity.UInt256(it.second) })
            )
            createExtensionTransaction(Extension.DAILY_LIMIT, data)
        }.subscribeOn(Schedulers.computation())

    private fun createExtensionTransaction(type: Extension, setupExtensionData: String): SafeTransaction {
        /*
        We need to send the resulting wrapped as a delegatecall to the safe.
        The creation flow is as follows:
        GnosisSafe -(1)(delegatecall)-> CreateAndAddExtension -(2)(delegatecall)-> ProxyFactory -(3)(call)-> Proxy/Extension
        */

        val factoryAddress = settingsRepository.getProxyFactoryAddress()
        val masterCopyAddress = getExtensionMasterCopyAddress(type)
        val createAndAddExtensionAddress = settingsRepository.getCreateAndAddExtensionContractAddress()

        // param setupExtensionData is the data for call from ProxyFactory to Proxy/Extension (3)
        // data for delegatecall from CreateAndAddExtension to ProxyFactory (2)
        val createExtensionProxyData = ProxyFactory.CreateProxy.encode(
            masterCopyAddress,
            Solidity.Bytes(setupExtensionData.hexStringToByteArray())
        )

        // data for delegatecall from GnosisSafe to CreateAndAddExtension (1)
        val data = CreateAndAddExtension.CreateAndAddExtension.encode(
            factoryAddress,
            Solidity.Bytes(createExtensionProxyData.hexStringToByteArray())
        )

        // wrapped send to gnosis safe to trigger initial delegatecall to CreateAndAddExtension
        return SafeTransaction(Transaction(createAndAddExtensionAddress, data = data), TransactionRepository.Operation.DELEGATE_CALL)
    }

    override fun loadExtensionsInfo(extensions: List<Solidity.Address>): Single<List<Pair<Extension, Solidity.Address>>> {
        val requests = extensions.mapIndexed { index, extension ->
            MappedRequest(
                EthCall(
                    transaction = Transaction(
                        extension,
                        data = SocialRecoveryExtension.NAME.encode()
                    ),
                    id = index
                ), {
                    (nullOnThrow {
                        loadExtensionType(SocialRecoveryExtension.NAME.decode(it!!).param0.value)
                    } ?: Extension.UNKNOWN) to extension
                })
        }.toList()

        return ethereumRepository.request(MappingBulkRequest(requests)).map { it.mapped() }.firstOrError()
    }

    private fun loadExtensionType(name: String): Extension =
        when (name) {
            "Social Recovery Extension" -> Extension.SOCIAL_RECOVERY
            else -> throw GnosisSafeExtensionRepository.UnknownExtensionException()
        }

    private fun getExtensionMasterCopyAddress(extension: Extension): Solidity.Address =
        when (extension) {
            Extension.SOCIAL_RECOVERY -> settingsRepository.getRecoveryExtensionMasterCopyAddress()
            Extension.DAILY_LIMIT -> settingsRepository.getDailyLimitExtensionMasterCopyAddress()
            Extension.UNKNOWN -> throw GnosisSafeExtensionRepository.UnknownExtensionException()
        }

    private class ExtensionInfoRequest() : BulkRequest()
}
