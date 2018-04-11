package pm.gnosis.heimdall.ui.transactions.details.generic

import android.content.Context
import io.reactivex.ObservableTransformer
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.decimalAsBigIntegerOrNull
import pm.gnosis.utils.hexStringToByteArrayOrNull
import pm.gnosis.utils.toHexString
import javax.inject.Inject

class GenericTransactionDetailsViewModel @Inject constructor() : GenericTransactionDetailsContract() {
    override fun inputTransformer(context: Context, originalTransaction: SafeTransaction?) =
        ObservableTransformer<InputEvent, Result<SafeTransaction>> {
            it.scan { old, new -> old.diff(new) }
                .map {
                    val to = it.to.first.asEthereumAddress()
                    val data = it.data.first.hexStringToByteArrayOrNull()
                    val value = it.value.first.decimalAsBigIntegerOrNull()
                    var errorFields = 0
                    var showToast = false
                    if (to == null) {
                        errorFields = errorFields or TransactionInputException.TO_FIELD
                        showToast = showToast or it.to.second
                    }
                    if (it.data.first.isNotBlank() && data == null) {
                        errorFields = errorFields or TransactionInputException.DATA_FIELD
                        showToast = showToast or it.data.second
                    }
                    if (value == null) {
                        errorFields = errorFields or TransactionInputException.VALUE_FIELD
                        showToast = showToast or it.value.second
                    }
                    if (errorFields > 0) {
                        ErrorResult<SafeTransaction>(TransactionInputException(context, errorFields, showToast))
                    } else {
                        val nonce = originalTransaction?.wrapped?.nonce
                        DataResult(
                            SafeTransaction(
                                Transaction(to!!, value = Wei(value!!), data = data?.toHexString(), nonce = nonce),
                                originalTransaction?.operation ?: TransactionRepository.Operation.CALL
                            )
                        )
                    }
                }
        }
}
