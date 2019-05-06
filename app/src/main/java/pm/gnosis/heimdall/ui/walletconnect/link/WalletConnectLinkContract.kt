package pm.gnosis.heimdall.ui.walletconnect.link

import androidx.lifecycle.ViewModel
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

abstract class WalletConnectLinkContract : ViewModel() {
    abstract fun observeSafes(): Flowable<Result<Adapter.Data<Safe>>>
}

class WalletConnectLinkViewModel @Inject constructor(
    private val safeRepository: GnosisSafeRepository
): WalletConnectLinkContract() {
    override fun observeSafes(): Flowable<Result<Adapter.Data<Safe>>> =
        safeRepository.observeSafes()
            .scanToAdapterData()
            .mapToResult()
}