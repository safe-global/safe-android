package pm.gnosis.heimdall.ui.keycard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import javax.inject.Inject

@ExperimentalCoroutinesApi
abstract class KeycardPairingContract(
    context: Context,
    appDispatchers: ApplicationModule.AppCoroutineDispatchers
) : BaseStateViewModel<KeycardPairingContract.State>(context, appDispatchers) {

    abstract fun startPairing(pin: String, pairingKey: String)

    abstract fun stopPairing()

    sealed class State : BaseStateViewModel.State {
        data class WaitingForInput(val pinError: String?, val pairingKeyError: String?, override var viewAction: ViewAction?) : State()
        data class ReadingCard(val reading: Boolean, val error: String?, override var viewAction: ViewAction?) : State()
        data class CardBlocked(override var viewAction: ViewAction?) : State()
        data class NoSlotsAvailable(override var viewAction: ViewAction?) : State()
    }
}

@ExperimentalCoroutinesApi
abstract class KeycardPairingBaseFragment : Fragment() {

    @Inject
    lateinit var viewModel: KeycardPairingContract

    abstract val layout: Int

    abstract fun inject(component: ViewComponent)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(layout, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(
            DaggerViewComponent.builder()
                .viewModule(ViewModule(context!!, parentFragment))
                .applicationComponent(HeimdallApplication[context!!])
                .build()
        )
        viewModel.state.observe(this, Observer { })
    }
}

interface ReadingCardScreen {
    val screen: View

    fun setupView(cancelListener: (View) -> Unit) {
        screen.setOnClickListener(cancelListener)
    }

    fun updateView(reading: Boolean, error: String?) {
    }
}

@ExperimentalCoroutinesApi
class KeycardPairingReadingCardFragment() : KeycardPairingBaseFragment(), ReadingCardScreen {
    override val layout = R.layout.dialog_fingerprint_scan

    override val screen: View
        get() = view!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(cancelListener = { viewModel.stopPairing() })
    }

    override fun inject(component: ViewComponent) = component.inject(this)
}

@ExperimentalCoroutinesApi
class KeycardPairingInputFragment : KeycardPairingBaseFragment() {

    override val layout = R.layout.dialog_fingerprint_scan

    override fun inject(component: ViewComponent) = component.inject(this)
}

class KeycardPairingDialog : DialogFragment() {

    @Inject
    lateinit var viewModel: KeycardPairingContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_fingerprint_scan, container, false)

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!, this))
            .applicationComponent(HeimdallApplication[context!!])
            .build()
            .inject(this)
    }
}