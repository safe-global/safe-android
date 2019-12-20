package pm.gnosis.heimdall.ui.two_factor.keycard

import android.content.Context
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import im.status.keycard.applet.KeycardCommandSet
import kotlinx.android.synthetic.main.screen_keycard_signing_input.view.*
import kotlinx.android.synthetic.main.screen_single_fragment.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.models.push.PushMessage
import pm.gnosis.heimdall.data.repositories.CardRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.impls.StatusKeyCardManager
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.NfcDialog
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.handleViewAction
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.transaction
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.*
import javax.inject.Inject

abstract class KeycardSigningContract(
    context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers
) : BaseStateViewModel<KeycardSigningContract.State>(context, appDispatchers) {

    abstract fun setup(address: Solidity.Address, hash: String)

    abstract fun callback(): NfcAdapter.ReaderCallback

    abstract fun startSigning(pin: String)

    abstract fun stopSigning()

    sealed class State : BaseStateViewModel.State {
        data class WaitingForInput(val pinError: String?, override var viewAction: ViewAction?) : State() {
            override fun withAction(viewAction: ViewAction?) = copy(viewAction = viewAction)
        }

        data class ReadingCard(val reading: Boolean, val error: String?, override var viewAction: ViewAction?) : State() {
            override fun withAction(viewAction: ViewAction?) = copy(viewAction = viewAction)
        }

        data class CardBlocked(override var viewAction: ViewAction?) : State() {
            override fun withAction(viewAction: ViewAction?) = copy(viewAction = viewAction)
        }

        data class SigningDone(val signature: Pair<Solidity.Address, ECDSASignature>, override var viewAction: ViewAction?) : State() {
            override fun withAction(viewAction: ViewAction?) = copy(viewAction = viewAction)
        }

        abstract fun withAction(viewAction: ViewAction?): State
    }
}

class KeycardSigningViewModel @Inject constructor(
    @ApplicationContext context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers,
    private val cardRepository: CardRepository,
    private val pushServiceRepository: PushServiceRepository,
    private val safeRepository: GnosisSafeRepository
) : KeycardSigningContract(context, appDispatchers) {

    override val state: LiveData<State> = liveData {
        for (event in stateChannel.openSubscription()) emit(event)
    }

    private val manager = WrappedManager()

    private lateinit var address: Solidity.Address
    private lateinit var hash: ByteArray

    override fun setup(address: Solidity.Address, hash: String) {
        this.address = address
        this.hash = hash.hexToByteArray()
    }

    override fun callback(): NfcAdapter.ReaderCallback = manager.callback()

    override fun startSigning(pin: String) {
        safeLaunch {
            updateState { State.ReadingCard(false, null, null) }
            manager.performOnChannel {
                safeLaunch {
                    updateState { State.ReadingCard(true, null, null) }
                    try {
                        val info = safeRepository.loadAuthenticatorInfo(address)
                        // TODO: add proper exceptions to handle different cases
                        val signature =
                            cardRepository.signWithCard(
                                StatusKeyCardManager(KeycardCommandSet(it)),
                                StatusKeyCardManager.UnlockParams(pin),
                                hash,
                                info.keyIndex ?: 0
                            )
                        // Small hack to propagate signature
                        pushServiceRepository.handlePushMessage(
                            PushMessage.ConfirmTransaction(
                                hash.toHexString().addHexPrefix(),
                                signature.second.r.asDecimalString(),
                                signature.second.s.asDecimalString(),
                                signature.second.v.toString(10)
                            )
                        )
                        updateState { State.SigningDone(signature, null) }
                    } catch (e: Exception) {
                        updateState { State.ReadingCard(true, e.message, null) }
                    }
                }
            }
        }
    }

    override fun stopSigning() {
        safeLaunch {
            updateState { withAction(ViewAction.CloseScreen) }
        }
    }

    override fun initialState(): State = State.WaitingForInput(null, null)

}

abstract class KeycardSigningBaseFragment : KeycardBaseFragment<KeycardSigningContract.State, KeycardSigningContract>() {

    @Inject
    override lateinit var viewModel: KeycardSigningContract
}

class KeycardSigningReadingCardFragment : KeycardSigningBaseFragment(), ReadingCardScreen {
    override val layout = R.layout.screen_keycard_reading

    override val screen: View
        get() = view!!

    override val appContext: Context
        get() = context!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(cancelListener = { viewModel.stopSigning() })
    }

    override fun updateState(state: KeycardSigningContract.State) {
        if (state !is KeycardSigningContract.State.ReadingCard) return
        updateView(state.reading, state.error)
    }

    override fun inject(component: ViewComponent) = component.inject(this)
}

class KeycardSigningInputFragment : KeycardSigningBaseFragment() {

    override val layout = R.layout.screen_keycard_signing_input

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.keycard_signing_input_cancel_button.setOnClickListener {
            viewModel.stopSigning()
        }

        view.keycard_signing_input_confirm_button.setOnClickListener {
            val pin = view.keycard_signing_input_pin.text.toString()
            viewModel.startSigning(pin)
        }

        view.keycard_signing_input_pin.showKeyboardForView()
    }

    override fun updateState(state: KeycardSigningContract.State) {
        if (state !is KeycardSigningContract.State.WaitingForInput) return
        view!!.keycard_signing_input_pin_error.apply {
            visible(state.pinError != null)
            text = state.pinError
        }
    }
}

class KeycardSigningDialog private constructor() : NfcDialog() {

    @Inject
    lateinit var viewModel: KeycardSigningContract

    private var currentState: KeycardSigningContract.State? = null

    override fun nfcCallback(): NfcAdapter.ReaderCallback = viewModel.callback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        val address = arguments?.getString(ARGUMENTS_ADDRESS)?.asEthereumAddress()
        val hash = arguments?.getString(ARGUMENTS_HASH)
        if (address == null || hash == null) {
            dismiss()
            return
        }
        viewModel.setup(address, hash)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.screen_single_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        viewModel.state.observe(this, Observer { updateState(it) })
    }

    private fun updateState(state: KeycardSigningContract.State) {
        single_fragment_content.handleViewAction(state.viewAction) {
            dismiss()
        }
        if (currentState?.let { state::class == it::class } == true) return
        currentState = state
        when (state) {
            is KeycardSigningContract.State.WaitingForInput -> KeycardSigningInputFragment()
            is KeycardSigningContract.State.ReadingCard -> KeycardSigningReadingCardFragment()
            is KeycardSigningContract.State.CardBlocked -> KeycardBlockedFragment()
            is KeycardSigningContract.State.SigningDone -> {
                ((activity ?: context) as? SigningCallback)?.onSigned(state.signature)
                dismiss()
                null
            }
        }?.let {
            childFragmentManager.transaction { replace(R.id.single_fragment_content, it) }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!, this))
            .applicationComponent(HeimdallApplication[context!!])
            .build()
            .inject(this)
    }

    interface SigningCallback {
        fun onSigned(signature: Pair<Solidity.Address, ECDSASignature>)
    }

    companion object {
        private const val ARGUMENTS_HASH = "argument.string.hash"
        private const val ARGUMENTS_ADDRESS = "argument.string.address"

        fun create(address: Solidity.Address, hash: String) =
            KeycardSigningDialog().apply {
                arguments = Bundle().apply {
                    putString(ARGUMENTS_HASH, hash)
                    putString(ARGUMENTS_ADDRESS, address.asEthereumAddressString())
                }
            }
    }
}
