package pm.gnosis.heimdall.ui.settings.general

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.svalinn.common.utils.Result

abstract class GeneralSettingsContract : ViewModel() {
    abstract fun isFingerprintAvailable(): Boolean
    abstract fun clearFingerprintData(): Single<Result<Unit>>
    abstract fun loadPaymentToken(): Single<ERC20Token>
    abstract fun loadSafeAdresses(): Single<List<String>>
}
