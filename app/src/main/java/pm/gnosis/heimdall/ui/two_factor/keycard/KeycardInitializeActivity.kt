package pm.gnosis.heimdall.ui.two_factor.keycard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import kotlinx.android.synthetic.main.screen_keycard_initialize.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.helpers.NfcViewModelActivity
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.handleViewAction
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.security.SecureRandom
import javax.inject.Inject


abstract class KeycardCredentialsContract(
    context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers
) : BaseStateViewModel<KeycardCredentialsContract.State>(context, appDispatchers) {
    data class State(val pin: String?, val puk: String?, val pairingKey: String?, override var viewAction: ViewAction?) : BaseStateViewModel.State
}

class KeycardCredentialsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers
) : KeycardCredentialsContract(context, appDispatchers) {
    private val random = SecureRandom()

    override val state = liveData {
        generateCredentials()
        for (event in stateChannel.openSubscription()) emit(event)
    }

    private fun generateCredentials() {
        safeLaunch {
            val pin = randomString(6, numerics)
            val puk = randomString(12, numerics)
            val pairingKey = randomString(15, alphaNumerics)
            updateState { copy(pin = pin, puk = puk, pairingKey = pairingKey) }
        }
    }

    private fun randomString(length: Int, charset: CharArray): String {
        val sb = StringBuilder()
        for (i in 0 until length) {
            sb.append(charset[random.nextInt(charset.size)])
        }
        return sb.toString()
    }

    override fun initialState() = State(null, null, null, null)

    companion object {
        private val numerics = "0123456789".toCharArray()
        private val alphaNumerics = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
    }
}

class KeycardInitializeActivity : NfcViewModelActivity<KeycardCredentialsContract>(), KeycardInitializeDialog.PairingCallback {

    override fun screenId() = ScreenId.KEYCARD_INITIALIZE

    override fun layout() = R.layout.screen_keycard_initialize

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_keycard_initialize)

        keycard_initialize_back_button.setOnClickListener { onBackPressed() }
        viewModel.state.observe(this, Observer { state ->
            keycard_initialize_pin_value.text = state.pin
            keycard_initialize_puk_value.text = state.puk
            keycard_initialize_pairing_key_value.text = state.pairingKey
            if (state.pin != null && state.puk != null && state.pairingKey != null)
                keycard_initialize_continue.setOnClickListener {
                    KeycardInitializeDialog.create(
                        intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress(), state.pin, state.puk, state.pairingKey
                    ).show(supportFragmentManager, null)
                }
            else
                keycard_initialize_continue.setOnLongClickListener(null)

            keycard_initialize_scroll.handleViewAction(state.viewAction) { finish() }
        })
    }

    override fun onPaired(authenticatorInfo: AuthenticatorSetupInfo) {
        setResult(Activity.RESULT_OK, authenticatorInfo.put(Intent()))
        finish()
    }

    companion object {
        private const val EXTRA_SAFE = "extra.string.safe"
        fun createIntent(context: Context, safe: Solidity.Address?) =
            Intent(context, KeycardInitializeActivity::class.java).apply {
                putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
            }
    }

}
