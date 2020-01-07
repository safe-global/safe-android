package pm.gnosis.heimdall.ui.two_factor.authenticator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.moshi.Moshi
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_pairing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.shareExternalText
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class PairingAuthenticatorActivity : ViewModelActivity<PairingAuthenticatorContract>() {
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.CONNECT_BROWSER_EXTENSION

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun layout(): Int = R.layout.layout_pairing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safe = intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress()
        viewModel.setup(safe)

        pairing_extension_link.apply {
            val linkDrawable = ContextCompat.getDrawable(context, R.drawable.ic_external_link)!!
            linkDrawable.setBounds(0, 0, linkDrawable.intrinsicWidth, linkDrawable.intrinsicHeight)
            this.text = SpannableStringBuilder(Html.fromHtml(getString(R.string.pairing_first_step)))
                .append(" ")
                .appendText(" ", ImageSpan(linkDrawable, ImageSpan.ALIGN_BASELINE))
            setOnClickListener { shareExternalText(getString(R.string.authenticator_link), getString(R.string.authenticator_url)) }
        }

        viewModel.observableState.observe(this, Observer {

            onPairingLoading(it.isLoading)

            it.pairingResult?.let {
                when (it) {
                    is PairingAuthenticatorContract.PairingResult.PairingSuccess -> {
                        setResult(Activity.RESULT_OK, it.setpInfo.put(Intent()))
                        finish()
                    }

                    is PairingAuthenticatorContract.PairingResult.PairingError -> {
                        errorSnackbar(pairing_coordinator, it.error)
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()

        disposables += pairing_bottom_panel.forwardClicks
            .subscribeBy(
                onNext = { QRCodeScanActivity.startForResult(this) },
                onError = Timber::e
            )

        disposables += pairing_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(pairing_toolbar_shadow, pairing_content_scroll)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data,
            onQrCodeResult = {
                viewModel.pair(it)
            }
        )
    }

    private fun onPairingLoading(isLoading: Boolean) {
        pairing_bottom_panel.disabled = isLoading
        pairing_progress_bar.visible(isLoading)
    }

    fun onSuccess(signingOwner: AccountsRepository.SafeOwner, extension: Solidity.Address) {
        val authenticatorInfo = AuthenticatorSetupInfo(signingOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, extension))
        setResult(Activity.RESULT_OK, authenticatorInfo.put(Intent()))
        finish()
    }

    companion object {
        private const val EXTRA_SAFE = "extra.string.safe"
        private const val EXTRA_ONBOARDING = "extra.boolean.onboarding"
        fun createIntent(context: Context, safe: Solidity.Address?, onboarding: Boolean = false) =
            Intent(context, PairingAuthenticatorActivity::class.java).apply {
                putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
                putExtra(EXTRA_ONBOARDING, onboarding)
            }
    }
}

abstract class PairingAuthenticatorContract : ViewModel() {

    abstract val observableState: LiveData<ViewUpdate>

    abstract fun setup(safeAddress: Solidity.Address?)
    /**
     * @param signingSafe to indicate which key should be used for pairing. @null if a new key should be generated
     */
    abstract fun pair(payload: String)

    sealed class PairingResult {

        data class PairingError(val error: Exception) : PairingResult()

        data class PairingSuccess(val setpInfo: AuthenticatorSetupInfo) : PairingResult()
    }

    data class ViewUpdate(
        val isLoading: Boolean,
        val pairingResult: PairingResult? = null
    )
}

class PairingAuthenticatorViewModel @Inject constructor(
    private val pushServiceRepository: PushServiceRepository,
    private val moshi: Moshi,
    private val appDispatcher: ApplicationModule.AppCoroutineDispatchers
) : PairingAuthenticatorContract() {

    override val observableState: LiveData<ViewUpdate>
        get() = _state
    private val _state = MutableLiveData<ViewUpdate>()

    private var safeAddress: Solidity.Address? = null

    override fun setup(safeAddress: Solidity.Address?) {
        this.safeAddress = safeAddress
    }

    override fun pair(payload: String) {

        viewModelScope.launch(appDispatcher.background) {

            _state.postValue(
                ViewUpdate(true)
            )

            try {
                val authorization = parseChromeExtensionPayload(payload)
                val (safeOwner, extensionAddress) = pushServiceRepository.pair(authorization, safeAddress).await()
                val setupInfo = AuthenticatorSetupInfo(safeOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, extensionAddress))

                _state.postValue(
                    ViewUpdate(
                        false,
                        PairingResult.PairingSuccess(setupInfo)
                    )
                )
            } catch (e: Exception) {
                _state.postValue(
                    ViewUpdate(
                        false,
                        PairingResult.PairingError(e)
                    )
                )
                Timber.e(e)
            }
        }
    }

    private suspend fun parseChromeExtensionPayload(payload: String): PushServiceTemporaryAuthorization = coroutineScope {
        moshi.adapter(PushServiceTemporaryAuthorization::class.java).fromJson(payload)!!
    }
}


