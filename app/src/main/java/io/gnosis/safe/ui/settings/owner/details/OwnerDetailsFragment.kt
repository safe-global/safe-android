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
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.OwnerEditNameFragmentArgs
import io.gnosis.safe.utils.formatEthAddress
import io.gnosis.safe.utils.showConfirmDialog
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class OwnerDetailsFragment : BaseViewBindingFragment<FragmentOwnerDetailsBinding>() {

    override fun screenId() = ScreenId.OWNER_DETAILS

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
                //TODO: navigate to export screen
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
                        link.setOnClickListener {
                            requireContext().openUrl(
                                getString(
                                    R.string.etherscan_address_url,
                                    owner.asEthereumAddressChecksumString()
                                )
                            )
                        }
                        removeButton.setOnClickListener {
                            showConfirmDialog(context = requireContext(), message = R.string.signing_owner_dialog_description) {
                                viewModel.removeOwner(owner)
                                findNavController().previousBackStackEntry?.savedStateHandle?.set(ARGS_RESULT_OWNER_REMOVED, true)
                            }
                        }

                        ownerQrCode.setImageBitmap(viewAction.ownerDetails.qrCode)
                        content.animate().alpha(1f).setDuration(1000)
                    }
                }
            }
        })
        viewModel.loadOwnerDetails(owner)
    }

    companion object {
        const val ARGS_RESULT_OWNER_REMOVED = "args.boolean.owner_removed_result"
    }
}
