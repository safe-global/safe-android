package pm.gnosis.heimdall.ui.safe.selection

import android.content.Context
import android.content.Intent
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class SelectSafeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository,
    private val detailRepository: TransactionDetailsRepository
) : SelectSafeContract() {

    override fun loadSafes(): Single<List<Safe>> = safeRepository.observeDeployedSafes().firstOrError()

    override fun reviewTransaction(safe: Solidity.Address?, transaction: SafeTransaction): Single<Result<Intent>> =
        detailRepository.loadTransactionType(transaction.wrapped)
            .map {
                safe ?: throw SimpleLocalizedException(context.getString(R.string.no_safe_selected_error))
                TODO("Can probably be removed")
                Intent()
            }
            .mapToResult()
}
