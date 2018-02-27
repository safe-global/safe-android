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
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.utils.GnosisSafeUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.asEthereumAddressString
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
            detailsRepository.loadTransactionData(preset)
                .map {
                    val data = it.toNullable()
                    when (data) {
                        is AddSafeOwnerData -> data.let {
                            val address =
                                if (it.newOwner == BigInteger.ZERO) ""
                                else it.newOwner.asEthereumAddressString()
                            address to it.newThreshold
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                .doOnSuccess {
                    cachedAddOwnerInfo = it
                }
                .onErrorReturnItem(EMPTY_FORM_DATA)
        } ?: Single.just(EMPTY_FORM_DATA)

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

    override fun loadAction(safeAddress: BigInteger?, transaction: Transaction?): Single<Action> =
        transaction?.let {
            detailsRepository.loadTransactionData(transaction)
                .flatMap {
                    val data = it.toNullable()
                    when (data) {
                        is RemoveSafeOwnerData -> Single.just(Action.RemoveOwner(data.owner))
                        is AddSafeOwnerData -> Single.just(Action.AddOwner(data.newOwner))
                        is ReplaceSafeOwnerData -> Single.just(Action.ReplaceOwner(data.newOwner, data.owner))
                        else -> throw IllegalStateException()
                    }
                }
        } ?: Single.error<Action>(IllegalStateException())

    companion object {
        val EMPTY_FORM_DATA = "" to -1
    }
}
