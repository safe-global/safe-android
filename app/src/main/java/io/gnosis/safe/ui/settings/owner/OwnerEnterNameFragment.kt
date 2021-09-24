package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.Owner
import io.gnosis.data.models.OwnerTypeConverter
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerNameEnterBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.CloseScreen
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.NavigateTo
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.ledger.LedgerController.Companion.LEDGER_LIVE_PATH
import io.gnosis.safe.ui.settings.owner.list.imageRes24dp
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import javax.inject.Inject

@ExcludeClassFromJacocoGeneratedReport
class OwnerEnterNameFragment : BaseViewBindingFragment<FragmentOwnerNameEnterBinding>() {

    override fun screenId() = ScreenId.OWNER_ENTER_NAME

    override suspend fun chainId(): BigInteger? = null

    @Inject
    lateinit var viewModel: OwnerEnterNameViewModel

    private val navArgs by navArgs<OwnerEnterNameFragmentArgs>()
    private val ownerAddress by lazy { navArgs.ownerAddress.asEthereumAddress()!! }
    private val ownerKey by lazy { navArgs.ownerKey.hexAsBigInteger() }
    private val fromSeedPhrase by lazy { navArgs.fromSeedPhrase }
    private val ownerSeedPhrase by lazy { navArgs.ownerSeedPhrase }
    private val ownerType: Owner.Type by lazy { OwnerTypeConverter().toType(navArgs.ownerType) }
    private val derivationPathWithIndex: String? by lazy { navArgs.derivationPathWithIndex }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerNameEnterBinding =
        FragmentOwnerNameEnterBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            newAddressBlockies.setAddress(ownerAddress)
            newAddressHex.text = ownerAddress.formatEthAddress(requireContext(), addMiddleLinebreak = false)
            keyType.setImageResource(ownerType.imageRes24dp())
            backButton.setOnClickListener { findNavController().navigateUp() }

            when (ownerType) {
                Owner.Type.IMPORTED -> {
                    nextButton.text = getString(R.string.signing_owner_import)
                    nextButton.setOnClickListener {
                        viewModel.importOwner(ownerAddress, ownerNameEntry.text.toString(), ownerKey, fromSeedPhrase)
                    }
                }
                Owner.Type.GENERATED -> {
                    nextButton.text = getString(R.string.signing_owner_save)
                    nextButton.setOnClickListener {
                        viewModel.importGeneratedOwner(ownerAddress, ownerNameEntry.text.toString(), ownerKey, ownerSeedPhrase!!)
                    }
                }
                Owner.Type.LEDGER_NANO_X -> {
                    nextButton.text = getString(R.string.signing_owner_import)
                    nextButton.setOnClickListener {
                        viewModel.importLedgerOwner(ownerAddress, ownerNameEntry.text.toString(), derivationPathWithIndex!!)
                    }
                    derivationPathWithIndex?.let {
                        ownerNameEntry.setText(generateDefaultNameFromDerivationPath(it))
                    }
                }
            }
            nextButton.isEnabled = !ownerNameEntry.text.isNullOrBlank()
            ownerNameEntry.doOnTextChanged { text, _, _, _ -> binding.nextButton.isEnabled = !text.isNullOrBlank() }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (val viewAction = it.viewAction) {
                is CloseScreen -> {
                    findNavController().popBackStack(R.id.ownerAddOptionsFragment, true)
                    if (ownerSeedPhrase != null) {
                        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_CREATE_RESULT, true)
                    } else {
                        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_IMPORT_RESULT, true)
                    }
                }
                is NavigateTo -> {
                    findNavController().navigate(viewAction.navDirections)
                }
            }
        })
    }

    private fun generateDefaultNameFromDerivationPath(derivationPathWithIndex: String): String {
        val index = derivationPathWithIndex.split("/").last().toInt()
        val ledgerLive =
            derivationPathWithIndex.removeSuffix(index.toString()) == LEDGER_LIVE_PATH.removeSuffix("{index}")
        return if (ledgerLive) {
            "Ledger Live key #${index + 1}"
        } else {
            "Ledger key #${index + 1}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().hideSoftKeyboard()
    }
}
