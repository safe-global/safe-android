package pm.gnosis.heimdall.ui.keycard

import android.content.Context
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import im.status.keycard.applet.KeycardCommandSet
import im.status.keycard.io.CardChannel
import im.status.keycard.io.CardListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.suspendCancellableCoroutine
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.CardRepository
import pm.gnosis.heimdall.data.repositories.impls.StatusKeyCardManager
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.toKeyIndex
import pm.gnosis.svalinn.common.utils.transaction
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.absoluteValue

@ExperimentalCoroutinesApi
abstract class KeycardInitializeContract(
    context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers
) : BaseStateViewModel<KeycardInitializeContract.State>(context, appDispatchers) {

    abstract fun callback(): NfcAdapter.ReaderCallback

    abstract fun startInitialization(pin: String, puk: String, pairingKey: String)

    abstract fun stopInitialization()

    abstract fun cancel()

    sealed class State : BaseStateViewModel.State {
        data class ReadingCard(val reading: Boolean, val error: String?, override var viewAction: ViewAction?) : State() {
            override fun withAction(viewAction: ViewAction?) = copy(viewAction = viewAction)
        }

        data class InitializationDone(val authenticatorInfo: AuthenticatorInfo, override var viewAction: ViewAction?) : State() {
            override fun withAction(viewAction: ViewAction?) = copy(viewAction = viewAction)
        }

        abstract fun withAction(viewAction: ViewAction?): State
    }
}

@ExperimentalCoroutinesApi
class KeycardInitializeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers,
    private val accountsRepository: AccountsRepository,
    private val cardRepository: CardRepository
) : KeycardInitializeContract(context, appDispatchers) {

    override val state: LiveData<State> = liveData {
        for (event in stateChannel.openSubscription()) emit(event)
    }

    private val manager = WrappedManager()

    override fun callback(): NfcAdapter.ReaderCallback = manager.callback()

    override fun startInitialization(pin: String, puk: String, pairingKey: String) {
        safeLaunch {
            updateState { State.ReadingCard(false, null, null) }
            manager.performOnChannel {
                println("performOnChannel")
                safeLaunch {
                    updateState { State.ReadingCard(true, null, null) }
                    try {
                        val safeOwner = accountsRepository.createOwner().await()
                        val keyIndex = safeOwner.address.toKeyIndex()
                        // TODO: add proper exceptions to handle different cases
                        cardRepository.initCard(
                            StatusKeyCardManager(KeycardCommandSet(it)),
                            StatusKeyCardManager.InitParams(pin, puk, pairingKey)
                        )
                        val cardAddress = cardRepository.pairCard(
                            StatusKeyCardManager(KeycardCommandSet(it)),
                            StatusKeyCardManager.PairingParams(pin, pairingKey),
                            "",
                            keyIndex
                        )
                        val authenticatorInfo = AuthenticatorInfo(
                            AuthenticatorInfo.Type.KEYCARD,
                            cardAddress,
                            safeOwner,
                            keyIndex
                        )
                        updateState { State.InitializationDone(authenticatorInfo, null) }
                    } catch (e: Exception) {
                        updateState { State.ReadingCard(true, e.message, null) }
                        throw e
                    }
                }
            }
        }
    }

    override fun stopInitialization() {
        safeLaunch {
            updateState { withAction(ViewAction.CloseScreen) }
        }
    }

    override fun cancel() {
        viewModelScope.cancel()
    }

    override fun initialState(): State = State.ReadingCard(false, null, null)
}

@ExperimentalCoroutinesApi
abstract class KeycardInitializeBaseFragment : KeycardBaseFragment<KeycardInitializeContract.State, KeycardInitializeContract>() {

    @Inject
    override lateinit var viewModel: KeycardInitializeContract
}

@ExperimentalCoroutinesApi
class KeycardInitializeReadingCardFragment : KeycardInitializeBaseFragment(), ReadingCardScreen {
    override val layout = R.layout.screen_keycard_reading

    override val screen: View
        get() = view!!

    override val appContext: Context
        get() = context!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(cancelListener = { viewModel.stopInitialization() })
    }

    override fun updateState(state: KeycardInitializeContract.State) {
        if (state !is KeycardInitializeContract.State.ReadingCard) return
        println("state $state")
        updateView(state.reading, state.error)
    }


    override fun inject(component: ViewComponent) = component.inject(this)
}

@ExperimentalCoroutinesApi
class KeycardInitializeDialog private constructor() : DialogFragment() {

    @Inject
    lateinit var viewModel: KeycardInitializeContract

    private var currentState: KeycardInitializeContract.State? = null

    private lateinit var adapter: NfcAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        adapter = context?.let { NfcAdapter.getDefaultAdapter(it) } ?: run {
            dismiss()
            return
        }
    }

    override fun onStart() {
        super.onStart()
        adapter.enableReaderMode(activity!!, viewModel.callback(), NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
        val pin = arguments?.getString(ARGUMENTS_PIN) ?: return
        val puk = arguments?.getString(ARGUMENTS_PUK) ?: return
        val pairingKey = arguments?.getString(ARGUMENTS_PAIRING_KEY) ?: return
        viewModel.startInitialization(pin, puk, pairingKey)
    }

    override fun onStop() {
        activity?.let { adapter.disableReaderMode(it) }
        viewModel.cancel()
        super.onStop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.screen_single_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(this, Observer { updateState(it) })
    }

    private fun updateState(state: KeycardInitializeContract.State) {
        if (state.viewAction == BaseStateViewModel.ViewAction.CloseScreen) {
            dismiss()
            return
        }
        if (state == currentState) return
        currentState = state
        when (state) {
            is KeycardInitializeContract.State.ReadingCard -> KeycardInitializeReadingCardFragment()
            is KeycardInitializeContract.State.InitializationDone -> {
                ((activity ?: context) as? PairingCallback)?.onPaired(state.authenticatorInfo)
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

    interface PairingCallback {
        fun onPaired(authenticatorInfo: AuthenticatorInfo)
    }

    companion object {
        private const val ARGUMENTS_PIN = "argument.string.pin"
        private const val ARGUMENTS_PUK = "argument.string.puk"
        private const val ARGUMENTS_PAIRING_KEY = "argument.string.pairing_key"

        fun create(pin: String, puk: String, pairingKey: String) =
            KeycardInitializeDialog().apply {
                arguments = Bundle().apply {
                    putString(ARGUMENTS_PIN, pin)
                    putString(ARGUMENTS_PUK, puk)
                    putString(ARGUMENTS_PAIRING_KEY, pairingKey)
                }
            }
    }
}