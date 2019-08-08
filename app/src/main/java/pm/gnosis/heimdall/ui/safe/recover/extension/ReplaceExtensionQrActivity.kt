package pm.gnosis.heimdall.ui.safe.recover.extension

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import androidx.lifecycle.*
import com.squareup.moshi.Moshi
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.shareExternalText
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject
import kotlinx.android.synthetic.main.screen_replace_extension_qr.replace_extension_back_arrow as backArrow
import kotlinx.android.synthetic.main.screen_replace_extension_qr.replace_extension_bottom_panel as bottomPanel
import kotlinx.android.synthetic.main.screen_replace_extension_qr.replace_extension_coordinator as coordinator
import kotlinx.android.synthetic.main.screen_replace_extension_qr.replace_extension_extension_link as extensionLink
import kotlinx.android.synthetic.main.screen_replace_extension_qr.replace_extension_progress_bar as progressBar

class ReplaceExtensionQrActivity : ViewModelActivity<ReplaceExtensionQrContract>() {

    override fun layout() = R.layout.screen_replace_extension_qr

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    override fun screenId() = ScreenId.REPLACE_BROWSER_EXTENSION_QR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeAddress = (intent.extras[EXTRA_SAFE_ADDRESS] as String)?.asEthereumAddress()
        safeAddress?.let {
            viewModel.setup(it)
        } ?: finish()


        viewModel.observableState.observe(this, Observer {

            onPairingLoading(it.isLoading)

            it.pairingResult?.let {

                when(it) {

                    is ReplaceExtensionQrContract.PairingResult.PairingSuccess -> {
                        toast(R.string.devices_paired_successfuly)
                        startActivity(ReplaceExtensionRecoveryPhraseActivity.createIntent(this, safeAddress!!, it.extension))
                    }

                    is ReplaceExtensionQrContract.PairingResult.PairingError -> {
                        snackbar(coordinator, R.string.error_pairing_devices)
                        Timber.e(it.error)
                    }
                }
            }
        })

        extensionLink.apply {
            this.text = SpannableStringBuilder(Html.fromHtml(getString(R.string.pairing_first_step)))
            setOnClickListener { shareExternalText(BuildConfig.CHROME_EXTENSION_URL, "Browser Extension URL") }
        }
    }

    override fun onStart() {
        super.onStart()


        backArrow.setOnClickListener {
            finish()
        }

        disposables += bottomPanel.forwardClicks.subscribe {
            QRCodeScanActivity.startForResult(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, onQrCodeResult = { viewModel.pair(it) })
    }

    private fun onPairingLoading(isLoading: Boolean) {
        bottomPanel.disabled = isLoading
        progressBar.visible(isLoading)
    }

    companion object {

        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) = Intent(context, ReplaceExtensionQrActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
        }
    }
}

abstract class ReplaceExtensionQrContract : ViewModel() {

    abstract val observableState: LiveData<ViewUpdate>

    abstract fun setup(safeAddress: Solidity.Address)

    /**
     * @param signingSafe to indicate which key should be used for pairing. @null if a new key should be generated
     */
    abstract fun pair(payload: String)


    data class ViewUpdate(
        val isLoading: Boolean,
        val pairingResult: PairingResult? = null
    )

    sealed class PairingResult {

        data class PairingError(val error: Exception) : PairingResult()

        data class PairingSuccess(val safe: Solidity.Address, val extension: Solidity.Address) : PairingResult()
    }
}

class ReplaceExtensionQrViewModel @Inject constructor(
    private val pushServiceRepository: PushServiceRepository,
    private val moshi: Moshi
) : ReplaceExtensionQrContract() {

    override val observableState: LiveData<ViewUpdate>
        get() = _state
    private val _state = MutableLiveData<ViewUpdate>()

    private lateinit var safeAddress: Solidity.Address

    override fun setup(safeAddress: Solidity.Address) {

        this.safeAddress = safeAddress
    }

    override fun pair(payload: String) {

        viewModelScope.launch(Dispatchers.IO) {

            _state.postValue(
                ViewUpdate(true)
            )

            try {

                val authorization = parseChromeExtensionPayload(payload)
                val pairResult = pushServiceRepository.pair(authorization, safeAddress).await()

                _state.postValue(
                    ViewUpdate(false, PairingResult.PairingSuccess(pairResult.first.address, pairResult.second))
                )

            } catch (e: Exception) {
                _state.postValue(
                    ViewUpdate(false, PairingResult.PairingError(e))
                )
            }
        }
    }

    private suspend fun parseChromeExtensionPayload(payload: String): PushServiceTemporaryAuthorization = coroutineScope {
        moshi.adapter(PushServiceTemporaryAuthorization::class.java).fromJson(payload)!!
    }
}
