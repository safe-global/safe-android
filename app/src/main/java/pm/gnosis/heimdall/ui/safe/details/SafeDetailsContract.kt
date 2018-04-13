package pm.gnosis.heimdall.ui.safe.details

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class SafeDetailsContract : ViewModel() {
    abstract fun setup(address: Solidity.Address, name: String?)
    abstract fun loadQrCode(contents: String): Single<Result<Bitmap>>
    abstract fun observeSafe(): Flowable<Safe>
}
