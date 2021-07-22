package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerNameEditBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import javax.inject.Inject

class OwnerEditNameFragment : BaseViewBindingFragment<FragmentOwnerNameEditBinding>() {

    override fun screenId() = ScreenId.OWNER_EDIT_NAME

    override suspend fun chainId(): BigInteger? = null

    @Inject
    lateinit var viewModel: OwnerEditNameViewModel

    private val navArgs by navArgs<OwnerEditNameFragmentArgs>()
    private val ownerAddress by lazy { navArgs.ownerAddress.asEthereumAddress()!! }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerNameEditBinding =
        FragmentOwnerNameEditBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            saveButton.setOnClickListener {
                viewModel.saveOwnerName(ownerAddress, ownerName.text.trim().toString())
            }

            ownerName.showKeyboardForView()
            ownerName.doOnTextChanged { text, _, _, _ -> binding.saveButton.isEnabled = !text.isNullOrBlank() }
            ownerName.setOnEditorActionListener listener@{ v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE && !ownerName.text.isNullOrBlank()) {
                    viewModel.saveOwnerName(ownerAddress, ownerName.text.trim().toString())
                    return@listener true
                }
                return@listener false
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {

            when (it.viewAction) {
                is CloseScreen -> close()
                else -> {
                    binding.ownerName.setText(it.name)
                    binding.ownerName.setSelection(it.name?.length ?: 0)
                }
            }
        })

        viewModel.loadOwnerName(ownerAddress)
    }

    private fun close() {
        activity?.hideSoftKeyboard()
        findNavController().navigateUp()
    }
}
