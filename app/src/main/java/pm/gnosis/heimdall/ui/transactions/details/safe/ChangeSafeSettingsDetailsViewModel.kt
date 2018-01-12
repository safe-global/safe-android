package pm.gnosis.heimdall.ui.transactions.details.safe

import android.content.Context
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.helpers.SingleValueStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.utils.GnosisSafeUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsEthereumAddress
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigInteger
import javax.inject.Inject


class ChangeSafeSettingsDetailsViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val detailsRepository: TransactionDetailsRepository,
        private val safeRepository: GnosisSafeRepository
) : ChangeSafeSettingsDetailsContract() {

    private var cachedAddOwnerInfo: Pair<String, Int>? = null

    override fun loadFormData(preset: Transaction?): Single<Pair<String, Int>> =
            // Check if we have a cached value
            cachedAddOwnerInfo?.let { Single.just(it) } ?:
                    // Else parse preset to extract address
                    preset?.let {
                        detailsRepository.loadTransactionDetails(preset)
                                .map {
                                    when (it.data) {
                                        is AddSafeOwnerData -> it.data.let {
                                            val address =
                                                    if (it.newOwner == BigInteger.ZERO) ""
                                                    else it.newOwner.asEthereumAddressString()
                                            address to it.newThreshold
                                        }
                                        else -> "" to -1
                                    }
                                }
                                .doOnSuccess {
                                    cachedAddOwnerInfo = it
                                }
                    } ?: Single.just("" to -1)

    override fun inputTransformer(safeAddress: BigInteger?) = ObservableTransformer<CharSequence, Result<Transaction>> {
        Observable.combineLatest(
                loadSafeInfo(safeAddress),
                it,
                BiFunction { info: Optional<SafeInfo>, input: CharSequence -> info.toNullable() to input.toString() }
        )
                .map { (safeInfo, input) ->
                    try {
                        DataResult(buildTransaction(input, safeAddress, safeInfo))
                    } catch (t: Throwable) {
                        ErrorResult<Transaction>(t)
                    }
                }
    }

    private fun loadSafeInfo(safeAddress: BigInteger?) =
            safeAddress?.let {
                safeRepository.loadInfo(safeAddress)
                        .map { it.toOptional() }
                        .onErrorReturnItem(None)
            } ?: Observable.just(None)

    private fun buildTransaction(input: String, safe: BigInteger?, safeInfo: SafeInfo?): Transaction {
        if (input.isBlank()) {
            throw TransactionInputException(context.getString(R.string.invalid_ethereum_address), TransactionInputException.TARGET_FIELD, false)
        }
        val newOwner = input.hexAsEthereumAddressOrNull()
        if (newOwner == null || newOwner == BigInteger.ZERO) {
            throw TransactionInputException(context.getString(R.string.invalid_ethereum_address), TransactionInputException.TARGET_FIELD, true)
        }
        // If we have safe info we should check that the owner does not exist yet
        if (safeInfo?.owners?.contains(newOwner) == true) {
            throw TransactionInputException(context.getString(R.string.error_owner_already_added), TransactionInputException.TARGET_FIELD, true)
        }
        // TODO: add proper error message
        SimpleLocalizedException.assert(safe != null, context, R.string.unknown_error)
        val newThreshold = cachedAddOwnerInfo?.second ?: safeInfo?.owners?.size?.let { GnosisSafeUtils.calculateThreshold(it + 1) }
        SimpleLocalizedException.assert(newThreshold != null, context, R.string.unknown_error)
        val addOwnerData = GnosisSafe.AddOwner.encode(Solidity.Address(newOwner), Solidity.UInt8(BigInteger.valueOf(newThreshold!!.toLong())))
        // Update cached values
        cachedAddOwnerInfo = input to newThreshold
        return Transaction(safe!!, data = addOwnerData)
    }

    override fun loadAction(transaction: Transaction?): Single<Action> =
            transaction?.let {
                detailsRepository.loadTransactionDetails(transaction)
                        .map {
                            when (it.data) {
                                is RemoveSafeOwnerData -> Action.RemoveOwner(it.data.ownerIndex.toString(16))
                                is AddSafeOwnerData -> Action.AddOwner(it.data.newOwner.asEthereumAddressString())
                                is ReplaceSafeOwnerData -> Action.ReplaceOwner(it.data.newOwner.asEthereumAddressString(), it.data.oldOwnerIndex.toString(16))
                                else -> throw IllegalStateException()
                            }
                        }
            } ?: Single.error<Action>(IllegalStateException())

    fun transactionTransformer(): ObservableTransformer<Optional<Transaction>, Result<Transaction>> =
            ObservableTransformer {
                it.flatMapSingle {
                    val transaction = it.toNullable()
                    transaction?.let {
                        detailsRepository.loadTransactionDetails(it)
                                .map(::checkDetails)
                                .mapToResult()
                    } ?: Single.just(ErrorResult(IllegalStateException()))
                }
            }

    private fun checkDetails(details: TransactionDetails): Transaction {
        return when (details.type) {
            TransactionType.TOKEN_TRANSFER -> {
                val data = details.data as? TokenTransferData
                if (data == null || data.tokens == BigInteger.ZERO) {
                    throw TransactionInputException(context, TransactionInputException.AMOUNT_FIELD, true)
                }
                details.transaction
            }
            TransactionType.ETHER_TRANSFER -> {
                if (details.transaction.value == null || details.transaction.value?.value == BigInteger.ZERO) {
                    throw TransactionInputException(context, TransactionInputException.AMOUNT_FIELD, true)
                }
                details.transaction
            }
            else ->
                throw IllegalStateException()
        }
    }

}
