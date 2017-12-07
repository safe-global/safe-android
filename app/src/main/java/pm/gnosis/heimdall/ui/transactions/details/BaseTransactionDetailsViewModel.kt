package pm.gnosis.heimdall.ui.transactions.details

import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger
import javax.inject.Inject

class BaseTransactionDetailsViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : BaseTransactionDetailsContract() {
    private var currentSelectedSafe: BigInteger? = null

    override fun observeSafes(defaultSafe: BigInteger?): Flowable<State> =
            safeRepository.observeDeployedSafes().map {
                BaseTransactionDetailsContract.State(getCurrentSelectedSafeIndex(currentSelectedSafe ?: defaultSafe, it), it)
            }

    private fun getCurrentSelectedSafeIndex(selectedSafeAddress: BigInteger?, safes: List<Safe>): Int {
        selectedSafeAddress?.let {
            safes.forEachIndexed { index, safe -> if (safe.address == selectedSafeAddress) return index }
        }
        return 0
    }

    override fun updateSelectedSafe(selectedSafe: BigInteger?) {
        currentSelectedSafe = selectedSafe
    }
}
