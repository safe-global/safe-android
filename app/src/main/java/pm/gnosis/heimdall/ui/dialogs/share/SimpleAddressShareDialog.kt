package pm.gnosis.heimdall.ui.dialogs.share

import android.os.Bundle
import io.reactivex.Observable
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.model.Solidity

class SimpleAddressShareDialog : BaseShareAddressDialog() {

    override fun screenId() = ScreenId.DIALOG_SHARE_ADDRESS

    override fun addressSourceObservable(): Observable<Pair<String?, Solidity.Address?>> =
        Observable.just(context?.getString(R.string.share_address) to address)

    override fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!].component)
            .build()
            .inject(this)
    }

    companion object {
        fun create(address: String): SimpleAddressShareDialog {
            val bundle = Bundle()
            bundle.putString(BaseShareAddressDialog.ADDRESS_EXTRA, address)
            return SimpleAddressShareDialog().apply { arguments = bundle }
        }
    }
}
