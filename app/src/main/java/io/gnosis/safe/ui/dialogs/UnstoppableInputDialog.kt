package io.gnosis.safe.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogUnstoppableInputBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.AddressHelper
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingDialogFragment
import io.gnosis.safe.utils.debounce
import io.gnosis.safe.utils.toColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class UnstoppableInputDialog : BaseViewBindingDialogFragment<DialogUnstoppableInputBinding>() {

    private val selectedChain by lazy { requireArguments()[ARGS_CHAIN] as Chain }

    override fun screenId() = ScreenId.SAFE_ADD_UD

    override suspend fun chainId() = selectedChain.chainId

    @Inject
    lateinit var viewModel: UnstoppableInputViewModel

    @Inject
    lateinit var addressHelper: AddressHelper

    var callback: ((Solidity.Address) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, R.style.DayNightFullscreenDialog)
        super.onCreate(savedInstanceState)
    }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): DialogUnstoppableInputBinding =
        DialogUnstoppableInputBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { dismiss() }
            confirmButton.setOnClickListener { onClick.trySend(Unit).isSuccess }
            dialogUnstoppableInputDomain.showKeyboardForView()
            chainRibbon.text = selectedChain.name
            chainRibbon.setTextColor(selectedChain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(selectedChain.backgroundColor.toColor(requireContext(), R.color.primary))
        }
    }

    override fun onStart() {
        super.onStart()
        processInput()
        lifecycleScope.launch {
            onClick.asFlow().collect {
                onNewAddress.valueOrNull?.let { propagateResult(it) }
            }
        }
    }

    private val onNewAddress = ConflatedBroadcastChannel<Solidity.Address?>()
    private val onClick = ConflatedBroadcastChannel<Unit>()

    private fun onUrlAvailable(string: String) {
        lifecycleScope.launch {
            runCatching { viewModel.processInput(string, selectedChain) }
                .onSuccess { address ->
                    with(binding) {
                        dialogEnsInputProgress.visible(false)
                        confirmButton.isEnabled = true
                        successViews.visible(true)
                        dialogUnstoppableDomainLayout.isErrorEnabled = false
                        onNewAddress.trySend(address).isSuccess
                        addressHelper.populateAddressInfo(
                            dialogUnstoppableInputAddress,
                            dialogEnsInputAddressImage,
                            address
                        )
                    }
                }
                .onFailure {
                    with(binding) {
                        dialogEnsInputProgress.visible(false)
                        confirmButton.isEnabled = false
                        successViews.visible(false)

                        val error = when (it.cause) {
                            is NamingServiceException -> it.cause!!.toError()
                            else -> it.toError();
                        }

                        dialogUnstoppableDomainLayout.error =
                            error.message(
                                requireContext(),
                                R.string.error_description_ens_name
                            )

                        dialogUnstoppableDomainLayout.isErrorEnabled = true

                        onNewAddress.trySend(null).isSuccess
                    }
                }
        }
    }

    val onUrlChanged: (String) -> Job? = debounce(1000, lifecycleScope, this::onUrlAvailable)

    private fun processInput() {
        var job: Job? = null
        binding.dialogUnstoppableInputDomain.doOnTextChanged { text, _, _, _ ->
            binding.successViews.visible(false)
            binding.dialogUnstoppableDomainLayout.isErrorEnabled = false
            if (text.toString().isNotEmpty()) {
                binding.dialogEnsInputProgress.visible(true)
                job = onUrlChanged(text.toString())
            } else {
                binding.dialogEnsInputProgress.visible(false)
                job?.cancel("Empty Unstoppable domain")
                binding.confirmButton.isEnabled = false
            }
        }
    }

    private fun propagateResult(state: Solidity.Address) {
        callback?.invoke(state)
        onNewAddress.close()
        onClick.close()
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        callback = null
        super.onDismiss(dialog)
    }

    companion object {
        fun create(chain: Chain): UnstoppableInputDialog {
            val dialog = UnstoppableInputDialog()
            dialog.arguments = bundleOf(ARGS_CHAIN to chain)
            return dialog
        }

        private const val ARGS_CHAIN = "args.serializable.chain"
    }
}
