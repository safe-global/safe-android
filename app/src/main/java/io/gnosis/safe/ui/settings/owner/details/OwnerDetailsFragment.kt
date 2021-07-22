package io.gnosis.safe.ui.settings.owner.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerDetailsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.CloseScreen
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.NavigateTo
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.OwnerEditNameFragmentArgs
import io.gnosis.safe.utils.formatEthAddress
import io.gnosis.safe.utils.showConfirmDialog
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

class OwnerDetailsFragment : BaseViewBindingFragment<FragmentOwnerDetailsBinding>() {

    override fun screenId() = ScreenId.OWNER_DETAILS

    override suspend fun chainId(): BigInteger? = null

    @Inject
    lateinit var viewModel: OwnerDetailsViewModel

    private val navArgs by navArgs<OwnerEditNameFragmentArgs>()
    private val owner by lazy { navArgs.ownerAddress.asEthereumAddress()!! }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerDetailsBinding =
        FragmentOwnerDetailsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            exportButton.setOnClickListener {
                viewModel.startExportFlow()
            }
            ownerName.setOnClickListener {
                findNavController().navigate(OwnerDetailsFragmentDirections.actionOwnerDetailsFragmentToOwnerEditNameFragment(owner.asEthereumAddressString()))
            }
            content.alpha = 0f
            ownerBlockie.setAddress(owner)
            ownerAddress.text = owner.formatEthAddress(requireContext(), addMiddleLinebreak = false)
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {

            when (val viewAction = it.viewAction) {
                is CloseScreen -> findNavController().navigateUp()
                is ShowOwnerDetails -> {
                    with(binding) {
                        ownerName.text = viewAction.ownerDetails.name
                        ownerAddress.setOnClickListener {
                            requireContext().copyToClipboard(getString(R.string.address_copied), owner.asEthereumAddressChecksumString()) {
                                snackbar(requireView(), getString(R.string.copied_success))
                            }
                        }
                        //FIXME: owners are chain independent; there is not enough context to decide on which blockexplorer to show owner's address
                        link.setOnClickListener {
                            // BlockExplorer.forChain().showAddress(requireContext(), owner)
                        }
                        removeButton.setOnClickListener {
                            showConfirmDialog(context = requireContext(), message = R.string.signing_owner_dialog_description) {
                                viewModel.removeOwner(owner)
                                findNavController().previousBackStackEntry?.savedStateHandle?.set(ARGS_RESULT_OWNER_REMOVED, true)
                            }
                        }

                        ownerQrCode.setImageBitmap(viewAction.ownerDetails.qrCode)
                        content.animate().alpha(1f).setDuration(1000)
                        exportButton.isEnabled = viewAction.ownerDetails.exportable
                    }
                }
                is NavigateTo -> {
                    findNavController().navigate(viewAction.navDirections)
                }
            }
        })
        viewModel.loadOwnerDetails(owner)
    }

    override fun onResume() {
        super.onResume()
        if (passcodeUnlocked()) {
            resetPasscodeUnlocked()
            resumeExportFlow()
        }
    }

    private fun resumeExportFlow() {
        viewModel.showExportData()
    }

    private fun passcodeUnlocked(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(RESULT_PASSCODE_UNLOCKED) == true
    }

    private fun resetPasscodeUnlocked() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(RESULT_PASSCODE_UNLOCKED, null)
    }

    companion object {
        const val ARGS_RESULT_OWNER_REMOVED = "args.boolean.owner_removed_result"
        const val RESULT_PASSCODE_UNLOCKED = "result.boolean.passcode_unlocked"
    }
}
