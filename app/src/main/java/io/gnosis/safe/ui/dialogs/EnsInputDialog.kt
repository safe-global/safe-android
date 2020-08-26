package io.gnosis.safe.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogEnsInputBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.AddressHelper
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.base.fragment.BaseViewBindingDialogFragment
import io.gnosis.safe.utils.debounce
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class EnsInputDialog : BaseViewBindingDialogFragment<DialogEnsInputBinding>() {

    @Inject
    lateinit var viewModel: EnsInputViewModel

    @Inject
    lateinit var addressHelper: AddressHelper

    var callback: ((Solidity.Address) -> Unit)? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialog)
        super.onCreate(savedInstanceState)
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

                    when(it) {
                        is Offline -> {
                            binding.dialogEnsInputUrlLayout.error = getString(R.string.error_no_internet)
                        }
                        is EnsResolutionError -> {
                            binding.dialogEnsInputUrlLayout.error = it.msg ?: getString(R.string.error_resolve_ens)
                        }
                        else -> {
                            binding.dialogEnsInputUrlLayout.error = getString(R.string.error_resolve_ens)
                        }
                    }
                    binding.dialogEnsInputUrlLayout.isErrorEnabled = true

                    onNewAddress.offer(null)
                }
        }
    }

    val onUrlChanged: (String) -> Unit = debounce(1000, lifecycleScope, this::onUrlAvailable)

    private fun processInput() {
        binding.dialogEnsInputUrl.doOnTextChanged { text, _, _, _ ->
            binding.successViews.visible(false)
            binding.dialogEnsInputUrlLayout.isErrorEnabled = false
            binding.dialogEnsInputProgress.visible(true)
            onUrlChanged(text.toString())
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
        fun create() = EnsInputDialog()
    }
}
