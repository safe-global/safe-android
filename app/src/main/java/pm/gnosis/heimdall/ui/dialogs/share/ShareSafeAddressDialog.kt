package pm.gnosis.heimdall.ui.dialogs.share

import android.os.Bundle
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_address_share.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.utils.asEthereumAddressStringOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class ShareSafeAddressDialog : BaseShareAddressDialog() {
    @Inject
    lateinit var safeRepository: GnosisSafeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
    }

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
