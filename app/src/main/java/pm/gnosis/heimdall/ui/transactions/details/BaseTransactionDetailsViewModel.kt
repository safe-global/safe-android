package pm.gnosis.heimdall.ui.transactions.details

import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import java.math.BigInteger
import javax.inject.Inject

class BaseTransactionDetailsViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : BaseTransactionDetailsContract() {
    private var currentSelectedSafe: BigInteger? = null

    override fun observeSafes(defaultSafe: BigInteger?): Flowable<State> =
            safeRepository.observeDeployedSafes().map {
                BaseTransactionDetailsContract.State(currentSelectedSafe ?: defaultSafe, it)
            }


    override fun updateSelectedSafe(selectedSafe: BigInteger?) {
        currentSelectedSafe = selectedSafe
    }
}
