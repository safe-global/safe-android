package pm.gnosis.heimdall.ui.transactions.details.base

import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger
import javax.inject.Inject

class BaseEditableTransactionDetailsViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : BaseEditableTransactionDetailsContract() {
    private var currentSelectedSafe: BigInteger? = null

    override fun observeSafes(defaultSafe: BigInteger?): Flowable<State> =
            safeRepository.observeDeployedSafes().map {
                State(getCurrentSelectedSafeIndex(currentSelectedSafe ?: defaultSafe, it), it)
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
