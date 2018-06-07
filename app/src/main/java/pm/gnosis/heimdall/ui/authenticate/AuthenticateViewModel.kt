package pm.gnosis.heimdall.ui.authenticate

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeActivity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.utils.ethereum.ERC67Parser
import javax.inject.Inject

class AuthenticateViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthenticateContract() {
    override fun checkResult(input: String): Observable<Result<Intent>> {
        return Observable.fromCallable {
            validateQrCode(input)
        }.subscribeOn(Schedulers.computation()).mapToResult()
    }

    private fun validateQrCode(qrCodeData: String): Intent {
        val parsedTransaction = ERC67Parser.parse(qrCodeData) ?: throw SimpleLocalizedException(context.getString(R.string.invalid_erc67))
        return SelectSafeActivity.createIntent(context, SafeTransaction(parsedTransaction, TransactionExecutionRepository.Operation.CALL))
    }
}
