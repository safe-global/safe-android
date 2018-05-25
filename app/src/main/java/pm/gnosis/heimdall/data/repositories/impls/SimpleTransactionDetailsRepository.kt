package pm.gnosis.heimdall.data.repositories.impls

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.*
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.removeSolidityMethodPrefix
import pm.gnosis.utils.toHexString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleTransactionDetailsRepository @Inject constructor(
    appDb: ApplicationDb,
    private val settingsRepository: SettingsRepository
) : TransactionDetailsRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    override fun loadTransactionData(transaction: Transaction): Single<Optional<TransactionTypeData>> =
        Single.fromCallable {
            val type = parseTransactionType(transaction.data)
            decodeTransactionData(type, transaction).toOptional()
        }.subscribeOn(Schedulers.computation())

    override fun loadTransactionType(transaction: Transaction): Single<TransactionType> =
        Single.fromCallable {
            parseTransactionType(transaction.data)
        }.subscribeOn(Schedulers.computation())

    override fun loadTransactionDetails(id: String): Single<TransactionDetails> =
        descriptionsDao.loadDescription(id)
            .subscribeOn(Schedulers.io())
            .map { decodeDescription(id, it.fromDb()) }

    private fun parseTransactionType(data: String?): TransactionType = when {
        data.isNullOrBlank() -> TransactionType.ETHER_TRANSFER // If we have no data we default to ether transfer
        data?.isSolidityMethod(StandardToken.Transfer.METHOD_ID) == true -> TransactionType.TOKEN_TRANSFER
        data?.isSolidityMethod(GnosisSafe.AddOwner.METHOD_ID) == true -> TransactionType.ADD_SAFE_OWNER
        data?.isSolidityMethod(GnosisSafe.RemoveOwner.METHOD_ID) == true -> TransactionType.REMOVE_SAFE_OWNER
        data?.isSolidityMethod(GnosisSafe.ReplaceOwner.METHOD_ID) == true -> TransactionType.REPLACE_SAFE_OWNER
        data?.isSolidityMethod(GnosisSafe.RemoveExtension.METHOD_ID) == true -> TransactionType.REMOVE_EXTENSION
        data?.isSolidityMethod(CreateAndAddExtension.CreateAndAddExtension.METHOD_ID) == true ->
            when (nullOnThrow { parseCreateAndAddExtensionData(data) }) {
                is AddRecoveryExtensionData -> TransactionType.ADD_RECOVERY_EXTENSION
                else -> TransactionType.GENERIC
            }
        else -> TransactionType.GENERIC
    }

    private fun parseCreateAndAddExtensionData(data: String): TransactionTypeData? {
        val argumentString = data.removeSolidityMethodPrefix(CreateAndAddExtension.CreateAndAddExtension.METHOD_ID)
        val args = CreateAndAddExtension.CreateAndAddExtension.decodeArguments(argumentString)
        val factoryDataString = args.data.items.toHexString()
        // Check if this wrapped creates a proxy (We will only handle this for now)
        if (!factoryDataString.isSolidityMethod(ProxyFactory.CreateProxy.METHOD_ID)) return null

        val factoryArgs = ProxyFactory.CreateProxy.decodeArguments(factoryDataString.removeSolidityMethodPrefix(ProxyFactory.CreateProxy.METHOD_ID))
        // Check what the master copy is that has been used with the proxy factory
        if (factoryArgs.mastercopy == settingsRepository.getRecoveryExtensionMasterCopyAddress()) {
            val extensionSetupDataString =
                factoryArgs.data.items.toHexString().removeSolidityMethodPrefix(SingleAccountRecoveryExtension.Setup.METHOD_ID)
            val setupArgs = SingleAccountRecoveryExtension.Setup.decodeArguments(extensionSetupDataString)
            return AddRecoveryExtensionData(setupArgs._recoverer, setupArgs._timeout.value)
        }
        return null
    }

    private fun decodeDescription(transactionId: String, description: GnosisSafeTransactionDescription) =
        decodeTransactionResult(transactionId, description.safeAddress, description.toTransaction(), description.submittedAt, description.subject)

    private fun decodeTransactionResult(
        transactionId: String,
        safe: Solidity.Address,
        transaction: SafeTransaction,
        submittedAt: Long,
        subject: String? = null
    ): TransactionDetails {
        val type = parseTransactionType(transaction.wrapped.data)
        return TransactionDetails(transactionId, type, decodeTransactionData(type, transaction.wrapped), transaction, safe, submittedAt, subject)
    }

    private fun decodeTransactionData(type: TransactionType, transaction: Transaction): TransactionTypeData? = when (type) {
        TransactionType.TOKEN_TRANSFER -> {
            val arguments = transaction.data!!.removeSolidityMethodPrefix(StandardToken.Transfer.METHOD_ID)
            StandardToken.Transfer.decodeArguments(arguments).let { TokenTransferData(it.to, it.value.value) }
        }
        TransactionType.ADD_SAFE_OWNER -> {
            val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.AddOwner.METHOD_ID)
            GnosisSafe.AddOwner.decodeArguments(arguments).let { AddSafeOwnerData(it.owner, it._threshold.value.toInt()) }
        }
        TransactionType.REMOVE_SAFE_OWNER -> {
            val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.RemoveOwner.METHOD_ID)
            GnosisSafe.RemoveOwner.decodeArguments(arguments)
                .let { RemoveSafeOwnerData(it.ownerindex.value, it.owner, it._threshold.value.toInt()) }
        }
        TransactionType.REPLACE_SAFE_OWNER -> {
            val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.ReplaceOwner.METHOD_ID)
            GnosisSafe.ReplaceOwner.decodeArguments(arguments)
                .let { ReplaceSafeOwnerData(it.oldownerindex.value, it.oldowner, it.newowner) }
        }
        TransactionType.REMOVE_EXTENSION -> {
            val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.RemoveExtension.METHOD_ID)
            GnosisSafe.RemoveExtension.decodeArguments(arguments).let { RemoveExtensionData(it.extensionindex.value, it.extension) }
        }
        TransactionType.ADD_RECOVERY_EXTENSION -> {
            parseCreateAndAddExtensionData(transaction.data!!)
        }
        else -> null
    }

    private fun GnosisSafeTransactionDescription.toTransaction(): SafeTransaction =
        SafeTransaction(Transaction(to, value = value, data = data, nonce = nonce), TransactionExecutionRepository.Operation.values()[operation.toInt()])
}
