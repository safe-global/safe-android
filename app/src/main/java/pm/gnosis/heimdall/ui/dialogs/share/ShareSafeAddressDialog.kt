package pm.gnosis.heimdall.ui.dialogs.share

import android.os.Bundle
import io.reactivex.Observable
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import java.math.BigInteger
import javax.inject.Inject

class ShareSafeAddressDialog : BaseShareAddressDialog() {
    @Inject
    lateinit var safeRepository: GnosisSafeRepository

    override fun addressSourceObservable(): Observable<Pair<String?, BigInteger>> =
            safeRepository.observeSafe(address)
                    .toObservable()
                    .map { it.name to it.address }

    override fun inject() {
        DaggerViewComponent.builder()
                .viewModule(ViewModule(context!!))
                .applicationComponent(HeimdallApplication[context!!].component)
                .build()
                .inject(this)
    }

    companion object {
        fun create(address: String): ShareSafeAddressDialog {
            val bundle = Bundle()
            bundle.putString(BaseShareAddressDialog.ADDRESS_EXTRA, address)
            return ShareSafeAddressDialog().apply { arguments = bundle }
        }
    }
}
