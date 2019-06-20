package pm.gnosis.heimdall.ui.tokens.receive

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.model.Solidity

abstract class ReceiveTokenContract : ViewModel() {
    abstract fun observeSafeInfo(safeAddress: Solidity.Address): Observable<ViewUpdate>

    sealed class ViewUpdate {
        data class Address(val checksumAddress: String) : ViewUpdate()
        data class Info(val name: String) : ViewUpdate()
        data class QrCode(val qrCode: Bitmap) : ViewUpdate()
    }
}
