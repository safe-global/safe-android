package io.gnosis.safe.ui.settings.owner.ledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogLedgerSignBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseBottomSheetDialogFragment
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class LedgerSignDialog : BaseBottomSheetDialogFragment<DialogLedgerSignBinding>() {

    private val navArgs by navArgs<LedgerSignDialogArgs>()
    private val confirmation by lazy { navArgs.confirmation }
    private val owner by lazy { navArgs.owner.asEthereumAddress()!! }
    private val safeTxHash by lazy { navArgs.safeTxHash }

    override fun screenId() = ScreenId.LEDGER_SIGN

    @Inject
    lateinit var viewModel: LedgerSignViewModel

    override fun inject(viewComponent: ViewComponent) {
        viewComponent.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DialogLedgerSignBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            actionLabel.text = getString(if (confirmation) R.string.ledger_sign_confirm else R.string.ledger_sign_reject)
            hash.text = viewModel.getPreviewHash(safeTxHash)
            cancel.setOnClickListener {
                navigateBack()
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            state.viewAction?.let { action ->
                when (action) {
                    is Signature -> {
                        navigateBack(action.signature)
                    }
                    is ShowError -> {
                        findNavController().navigateUp()
                    }
                    else -> {
                    }
                }
            }
        })

        viewModel.getSignature(owner, safeTxHash)
    }

    override fun onStop() {
        super.onStop()
        viewModel.disconnectFromDevice()
    }

    private fun navigateBack(signedSafeTxHash: String? = null) {
        if (confirmation) {
            findNavController().popBackStack(R.id.signingOwnerSelectionFragment, true)
            signedSafeTxHash?.let {
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    SafeOverviewBaseFragment.OWNER_SELECTED_RESULT,
                    owner.asEthereumAddressString()
                )
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    SafeOverviewBaseFragment.OWNER_SIGNED_RESULT,
                    it
                )
            }
        } else {
            findNavController().popBackStack(R.id.signingOwnerSelectionFragment, true)
            signedSafeTxHash?.let {
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    SafeOverviewBaseFragment.OWNER_SELECTED_RESULT,
                    owner.asEthereumAddressString()
                )
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    SafeOverviewBaseFragment.OWNER_SIGNED_RESULT,
                    it
                )
            }
        }
    }
}
