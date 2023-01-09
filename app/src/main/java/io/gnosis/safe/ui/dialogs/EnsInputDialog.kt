package io.gnosis.safe.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogEnsInputBinding
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

class EnsInputDialog : BaseViewBindingDialogFragment<DialogEnsInputBinding>() {

    private val selectedChain by lazy { requireArguments()[ARGS_CHAIN] as Chain }

    override fun screenId() = ScreenId.SAFE_ADD_ENS

    override suspend fun chainId() = selectedChain.chainId

    @Inject
    lateinit var viewModel: EnsInputViewModel

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

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogEnsInputBinding =
        DialogEnsInputBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { dismiss() }
            confirmButton.setOnClickListener { onClick.trySend(Unit) }
            dialogEnsInputUrl.showKeyboardForView()
            chainRibbon.text = selectedChain.name
            chainRibbon.setTextColor(
                selectedChain.textColor.toColor(
                    requireContext(),
                    R.color.white
                )
            )
            chainRibbon.setBackgroundColor(
                selectedChain.backgroundColor.toColor(
                    requireContext(),
                    R.color.primary
                )
            )
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
            runCatching { viewModel.processEnsInput(string, selectedChain) }
                .onSuccess { address ->
                    binding.dialogEnsInputProgress.visible(false)
                    binding.confirmButton.isEnabled = true
                    binding.successViews.visible(true)
                    binding.dialogEnsInputUrlLayout.isErrorEnabled = false
                    onNewAddress.trySend(address)
                    addressHelper.populateAddressInfo(
                        binding.dialogEnsInputAddress,
                        binding.dialogEnsInputAddressImage,
                        address
                    )
                }
                .onFailure {
                    binding.dialogEnsInputProgress.visible(false)
                    binding.confirmButton.isEnabled = false
                    binding.successViews.visible(false)

                    val error = it.toError()
                    if (error.trackingRequired) {
                        tracker.logException(it)
                    }
                    binding.dialogEnsInputUrlLayout.error =
                        error.message(requireContext(), R.string.error_description_ens_name)

                    binding.dialogEnsInputUrlLayout.isErrorEnabled = true

                    onNewAddress.trySend(null)
                }
        }
    }

    val onUrlChanged: (String) -> Job? = debounce(1000, lifecycleScope, this::onUrlAvailable)

    private fun processInput() {
        var job: Job? = null
        binding.dialogEnsInputUrl.doOnTextChanged { text, _, _, _ ->
            binding.successViews.visible(false)
            binding.dialogEnsInputUrlLayout.isErrorEnabled = false
            if (text.toString().isNotEmpty()) {
                if (text.toString().startsWith(" ") || text.toString().endsWith(" ")) {
                    binding.dialogEnsInputUrl.setText(text.toString().trim())
                } else {
                    binding.dialogEnsInputProgress.visible(true)
                    job = onUrlChanged(text.toString())
                }
            } else {
                binding.dialogEnsInputProgress.visible(false)
                job?.cancel("Empty ENS name")
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
        fun create(chain: Chain): EnsInputDialog {
            val dialog = EnsInputDialog()
            dialog.arguments = bundleOf(ARGS_CHAIN to chain)
            return dialog
        }

        private const val ARGS_CHAIN = "args.serializable.chain"
    }
}
