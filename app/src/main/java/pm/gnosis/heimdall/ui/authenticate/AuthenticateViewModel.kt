package pm.gnosis.heimdall.ui.authenticate

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeActivity
import pm.gnosis.heimdall.ui.transactions.SignTransactionActivity
import pm.gnosis.heimdall.ui.transactions.SubmitTransactionActivity
import pm.gnosis.heimdall.utils.ERC67Parser
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import javax.inject.Inject


class AuthenticateViewModel @Inject constructor(
        private @ApplicationContext val context: Context
) : AuthenticateContract() {
    override fun checkResult(input: String): Observable<Result<Intent>> {
        return Observable.fromCallable {
            validateQrCode(input)
        }.subscribeOn(Schedulers.computation()).mapToResult()
    }

    private fun validateQrCode(qrCodeData: String): Intent {
        GnoSafeUrlParser.parse(qrCodeData)?.let {
            when (it) {
                is GnoSafeUrlParser.Parsed.SignRequest ->
                    return SignTransactionActivity.createIntent(context, it.safe, it.transaction)
                else -> null
            }
        }
        val parsedData = ERC67Parser.parse(qrCodeData) ?:
                throw SimpleLocalizedException(context.getString(R.string.invalid_erc67))
        return SelectSafeActivity.createIntent(context, parsedData.transaction)
    }
}
