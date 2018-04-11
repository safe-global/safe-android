package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import android.content.Context
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddRecoveryExtensionData
import pm.gnosis.heimdall.data.repositories.GnosisSafeExtensionRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import javax.inject.Inject


class AddRecoveryExtensionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extensionRepository: GnosisSafeExtensionRepository,
    private val detailsRepository: TransactionDetailsRepository
) : AddRecoveryExtensionContract() {

    override fun loadRecoveryOwners(transaction: Transaction?): Single<Pair<Solidity.Address?, Solidity.Address?>> =
        transaction?.let {
            detailsRepository.loadTransactionData(transaction)
                .map {
                    val data = it.toNullable()
                    when (data) {
                        is AddRecoveryExtensionData -> {
                            val acc1 = data.recoveryOwners.firstOrNull()?.nonZeroBigIntegerOrNull()
                            val acc2 = data.recoveryOwners.getOrNull(1)?.nonZeroBigIntegerOrNull()
                            acc1 to acc2
                        }
                        else -> throw IllegalStateException()
                    }
                }
        } ?: Single.error<Pair<Solidity.Address?, Solidity.Address?>>(IllegalStateException())

    private fun BigInteger.nonZeroBigIntegerOrNull(): Solidity.Address? =
        if (this == BigInteger.ZERO) null else Solidity.Address(this)

    override fun inputTransformer(safeAddress: Solidity.Address?) = ObservableTransformer<Pair<CharSequence, CharSequence>, Result<SafeTransaction>> {
        it.flatMapSingle { (recoveryOwner1Input, recoveryOwner2Input) ->
            Single.fromCallable {
                parseRecoveryOwners(recoveryOwner1Input.toString(), recoveryOwner2Input.toString())
            }.flatMap {
                extensionRepository.buildAddRecoverExtensionTransaction(it)
            }.mapToResult()
        }
    }

    private fun parseRecoveryOwners(recoveryOwner1: String, recoveryOwner2: String): List<Solidity.Address> {
        if (recoveryOwner1.isBlank() || recoveryOwner2.isBlank()) {
            throw TransactionInputException(context.getString(R.string.invalid_ethereum_address), TransactionInputException.TARGET_FIELD, false)
        }
        val recoveryOwner1Address = recoveryOwner1.asEthereumAddress()
        val recoveryOwner2Address = recoveryOwner2.asEthereumAddress()
        if (recoveryOwner1Address == null || recoveryOwner1Address.value == BigInteger.ZERO ||
            recoveryOwner2Address == null || recoveryOwner2Address.value == BigInteger.ZERO
        ) {
            throw TransactionInputException(context.getString(R.string.invalid_ethereum_address), TransactionInputException.TARGET_FIELD, true)
        }
        return listOf(recoveryOwner1Address, recoveryOwner2Address)
    }
}
