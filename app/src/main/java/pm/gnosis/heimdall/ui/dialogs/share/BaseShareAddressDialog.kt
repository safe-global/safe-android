package pm.gnosis.heimdall.ui.dialogs.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_address_share.*
import pm.gnosis.heimdall.R
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.shareExternalText
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow

abstract class BaseShareAddressDialog : BaseShareQrCodeDialog() {

    protected var address: Solidity.Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        address = arguments?.getString(ADDRESS_EXTRA)?.asEthereumAddress() ?: run {
            dismiss()
            null
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_address_share, container, false)

    override fun data(): String? = address?.asEthereumAddressString()

    override fun shareTitle(): String = getString(R.string.address)

    override fun root(): View = dialog_address_share_root

    override fun onStart() {
        super.onStart()
        disposables += dialog_address_share_share.clicks()
            .subscribeBy(onNext = {
                nullOnThrow { data() }?.let {
                    context?.shareExternalText(it, shareTitle())
                }
            })
    }

    override fun onCopiedToClipboard() {
        context?.toast(R.string.address_clipboard_success)
    }

    override fun dataSourceObservable(): Observable<Pair<String?, String?>> =
        addressSourceObservable().map { it.first to it.second?.asEthereumAddressString() }

    abstract fun addressSourceObservable(): Observable<Pair<String?, Solidity.Address?>>

    companion object {
        const val ADDRESS_EXTRA = "extra.string.address"
    }
}
