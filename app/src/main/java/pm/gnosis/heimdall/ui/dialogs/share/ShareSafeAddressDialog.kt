package pm.gnosis.heimdall.ui.dialogs.share

import android.os.Bundle
import io.reactivex.Observable
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class ShareSafeAddressDialog : BaseShareAddressDialog() {

    @Inject
    lateinit var safeRepository: GnosisSafeRepository

    override fun screenId() = ScreenId.DIALOG_SHARE_SAFE

    override fun addressSourceObservable(): Observable<Pair<String?, Solidity.Address?>> =
        address?.let {
            safeRepository.observeSafe(it)
                .toObservable()
                .map { it.name to address }
        } ?: Observable.just<Pair<String?, Solidity.Address?>>("" to address)

    override fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!].component)
            .build()
            .inject(this)
    }

    companion object {
        fun create(address: Solidity.Address): ShareSafeAddressDialog {
            val bundle = Bundle()
            bundle.putString(BaseShareAddressDialog.ADDRESS_EXTRA, address.asEthereumAddressString())
            return ShareSafeAddressDialog().apply { arguments = bundle }
        }
    }
}
