package pm.gnosis.heimdall.ui.safe.selection

import android.content.Context
import android.content.Intent
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.heimdall.ui.transactions.SubmitTransactionActivity
import pm.gnosis.models.Transaction
import java.math.BigInteger
import javax.inject.Inject


class SelectSafeViewModel @Inject constructor(
        private @ApplicationContext val context: Context,
        private val safeRepository: GnosisSafeRepository,
        private val detailRepository: TransactionDetailsRepository
) : SelectSafeContract() {

    override fun loadSafes(): Single<List<Safe>> =
            safeRepository.observeDeployedSafes()
                    .firstOrError()

    override fun reviewTransaction(safe: BigInteger?, transaction: Transaction): Single<Result<Intent>> =
            detailRepository.loadTransactionType(transaction)
                    .map {
                        safe
                                ?: throw SimpleLocalizedException(context.getString(R.string.no_safe_selected_error))
                        when (it) {
                            TransactionType.REMOVE_SAFE_OWNER ->
                                SubmitTransactionActivity.createIntent(context, safe, transaction)
                            else ->
                                CreateTransactionActivity.createIntent(context, safe, it, transaction)
                        }
                    }
                    .mapToResult()

}
