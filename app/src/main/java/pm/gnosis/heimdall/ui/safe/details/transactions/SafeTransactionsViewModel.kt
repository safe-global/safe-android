package pm.gnosis.heimdall.ui.safe.details.transactions

import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import java.math.BigInteger
import javax.inject.Inject


class SafeTransactionsViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : SafeTransactionsContract() {

    private var address: BigInteger? = null

    override fun setup(address: BigInteger) {
        this.address = address
    }
    override fun observeTransactions(): Flowable<out Result<Adapter.Data<String>>> {
        return this.address?.let {
            safeRepository.observeTransactionDescriptions(it)
                    .scanToAdapterData()
                    .mapToResult()
        } ?: Flowable.empty<Result<Adapter.Data<String>>>()
    }

}