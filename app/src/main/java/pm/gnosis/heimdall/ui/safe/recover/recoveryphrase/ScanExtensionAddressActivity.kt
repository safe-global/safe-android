package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_extension_address.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import javax.inject.Inject

class ScanExtensionAddressActivity : ViewModelActivity<ScanExtensionAddressContract>() {
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.SCAN_EXTENSION_ADDRESS

    private val qrCodeResultSubject = PublishSubject.create<String>()
    private var qrCodeResultSubscription: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val safeAddress = nullOnThrow { intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() } ?: run { finish(); return }
        viewModel.setup(safeAddress)

        qrCodeResultSubscription = qrCodeResultSubject
            .flatMapSingle { payload ->
                viewModel.validatePayload(payload)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { onBrowserExtensionValidationProgress(true) }
                    .doAfterTerminate { onBrowserExtensionValidationProgress(false) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onValidBrowserExtension, onError = ::onBrowserExtensionValidationError)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_extension_address_scan.clicks()
            .subscribeBy(onNext = { QRCodeScanActivity.startForResult(this) }, onError = Timber::e)

        disposables += layout_create_safe_intro_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_extension_address_toolbar_shadow, layout_extension_address_content_scroll)
    }

    private fun onValidBrowserExtension(browserExtensionAddress: Solidity.Address) {
        startActivity(SetupNewRecoveryPhraseActivity.createIntent(this, viewModel.getSafeAddress(), browserExtensionAddress))
    }

    private fun onBrowserExtensionValidationError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_extension_address_coordinator, throwable)
    }

    private fun onBrowserExtensionValidationProgress(inProgress: Boolean) {
        layout_extension_address_progress_bar.visible(inProgress)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, onQrCodeResult = { qrCodeData -> qrCodeResultSubject.onNext(qrCodeData) })
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodeResultSubscription?.dispose()
    }

    override fun layout() = R.layout.layout_extension_address

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) = Intent(context, ScanExtensionAddressActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
        }
    }
}
