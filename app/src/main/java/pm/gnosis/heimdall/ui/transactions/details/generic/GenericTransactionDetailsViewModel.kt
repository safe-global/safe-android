package pm.gnosis.heimdall.ui.transactions.details.generic

import android.content.Context
import io.reactivex.ObservableTransformer
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.decimalAsBigIntegerOrNull
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import pm.gnosis.utils.hexStringToByteArrayOrNull
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import javax.inject.Inject


class GenericTransactionDetailsViewModel @Inject constructor(): GenericTransactionDetailsContract() {
    override fun inputTransformer(context: Context, originalTransaction: Transaction?) =
            ObservableTransformer<InputEvent, Result<Transaction>> {
                it.scan { old, new -> old.diff(new) }
                    .map {
                        val to = it.to.first.hexAsEthereumAddressOrNull()
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
                            ErrorResult<Transaction>(TransactionInputException(context, errorFields, showToast))
                        } else {
                            val nonce = originalTransaction?.nonce
                            DataResult(Transaction(to!!, value = Wei(value!!), data = data?.toHexString(), nonce = nonce))
                        }
                    }
    }
}