package pm.gnosis.heimdall.ui.dialogs.ens

import androidx.lifecycle.ViewModel
import io.reactivex.ObservableTransformer
import pm.gnosis.heimdall.data.repositories.EnsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

abstract class EnsInputContract : ViewModel() {
    abstract fun processEnsInput(): ObservableTransformer<CharSequence, Result<Solidity.Address>>

}

class EnsInputViewModel @Inject constructor(
    private val ensRepository: EnsRepository
) : EnsInputContract() {
    override fun processEnsInput(): ObservableTransformer<CharSequence, Result<Solidity.Address>> = ObservableTransformer {
        it.switchMapSingle { input -> ensRepository.resolve(input.toString()).mapToResult() }
    }
}
