package io.gnosis.safe.ui.dialogs

import android.content.DialogInterface
import android.graphics.Color
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class EnsInputDialog : BaseViewBindingDialogFragment<DialogEnsInputBinding>() {

    lateinit var selectedChain: Chain

    @Inject
    lateinit var viewModel: EnsInputViewModel

    @Inject
    lateinit var addressHelper: AddressHelper

    var callback: ((Solidity.Address) -> Unit)? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, R.style.DayNightFullscreenDialog)
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            val chainId = requireArguments().getInt(CHAIN_ID)
            val chainName = requireArguments().getString(CHAIN_NAME)!!
            val textColor = requireArguments().getString(CHAIN_TEXT_COLOR)!!
            val bgColor = requireArguments().getString(CHAIN_BACKGROUND_COLOR)!!
            selectedChain = Chain(chainId, chainName, textColor, bgColor)
        }
    }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): DialogEnsInputBinding =
        DialogEnsInputBinding.inflate(inflater, container, false)

    override fun screenId(): ScreenId? = ScreenId.SAFE_ADD_ENS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { dismiss() }
            confirmButton.setOnClickListener { onClick.offer(Unit) }
            dialogEnsInputUrl.showKeyboardForView()
            chainRibbon.text = selectedChain.name
            try {
                chainRibbon.setTextColor(Color.parseColor(selectedChain.textColor))
                chainRibbon.setBackgroundColor(Color.parseColor(selectedChain.backgroundColor))
            } catch (e: Exception) {
                tracker.logException(e)
                chainRibbon.setTextColor(requireContext().getColorCompat(R.color.white))
                chainRibbon.setBackgroundColor(requireContext().getColorCompat(R.color.primary))
            }
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
            runCatching { viewModel.processEnsInput(string) }
                .onSuccess { address ->
                    binding.dialogEnsInputProgress.visible(false)
                    binding.confirmButton.isEnabled = true
                    binding.successViews.visible(true)
                    binding.dialogEnsInputUrlLayout.isErrorEnabled = false
                    onNewAddress.offer(address)
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
                    binding.dialogEnsInputUrlLayout.error = error.message(requireContext(), R.string.error_description_ens_name)

                    binding.dialogEnsInputUrlLayout.isErrorEnabled = true

                    onNewAddress.offer(null)
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
                binding.dialogEnsInputProgress.visible(true)
                job = onUrlChanged(text.toString())
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
            dialog.arguments = bundleOf(
                CHAIN_NAME to chain.name,
                CHAIN_ID to chain.chainId,
                CHAIN_BACKGROUND_COLOR to chain.backgroundColor,
                CHAIN_TEXT_COLOR to chain.textColor
            )
            return dialog
        }

        private const val CHAIN_NAME = "chain_name"
        private const val CHAIN_ID = "chain_id"
        private const val CHAIN_BACKGROUND_COLOR = "background_color"
        private const val CHAIN_TEXT_COLOR = "text_color"
    }
}
