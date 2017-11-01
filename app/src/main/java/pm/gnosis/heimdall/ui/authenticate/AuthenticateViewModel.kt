package pm.gnosis.heimdall.ui.authenticate

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.ZxingIntentIntegrator
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsActivity
import pm.gnosis.heimdall.utils.ERC67Parser
import pm.gnosis.utils.isSolidityMethod
import javax.inject.Inject


class AuthenticateViewModel @Inject constructor(
        private @ApplicationContext val context: Context
) : AuthenticateContract() {
    override fun checkResult(result: ActivityResults): Observable<Result<Intent>> {
        if (result.requestCode != ZxingIntentIntegrator.REQUEST_CODE ||
                result.resultCode != Activity.RESULT_OK ||
                result.data?.hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA) != true) {
            return Observable.empty()
        }
        return Observable.fromCallable {
            validateQrCode(result.data.getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))
        }.mapToResult()
    }

    private fun validateQrCode(qrCodeData: String): Intent {
        val parsedData = ERC67Parser.parse(qrCodeData) ?:
                throw LocalizedException(context.getString(R.string.invalid_erc67))
        val data = parsedData.transaction.data
        if (data != null && (data.isSolidityMethod(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID) ||
                data.isSolidityMethod(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID))) {
            return TransactionDetailsActivity.createIntent(context, parsedData.transaction, parsedData.descriptionHash)
        } else {
            throw LocalizedException(context.getString(R.string.unknown_wallet_action))
        }
    }
}
