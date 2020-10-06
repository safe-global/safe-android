package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentImportOwnerKeyBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.signing.owners.OwnerSelectionFragmentDirections
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class ImportOwnerKeyFragment : BaseViewBindingFragment<FragmentImportOwnerKeyBinding>() {

    @Inject
    lateinit var viewModel: ImportOwnerKeyViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentImportOwnerKeyBinding =
        FragmentImportOwnerKeyBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            seedPhraseLayout.isErrorEnabled = false
            backButton.setOnClickListener { findNavController().navigateUp() }
            nextButton.setOnClickListener { submit() }
            seedPhraseText.doOnTextChanged { _, _, _, _ -> binding.seedPhraseLayout.isErrorEnabled = false }
            seedPhraseText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit()
                }
                true
            }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is ImportOwnerKeyState.AwaitingInput -> {
                    binding.seedPhraseLayout.isErrorEnabled = false
                }
                is ImportOwnerKeyState.Error -> {
                    (state.viewAction as? BaseStateViewModel.ViewAction.ShowError)?.let { Timber.e(it.error) }
                    with(binding) {
                        seedPhraseLayout.isErrorEnabled = true
                        seedPhraseLayout.error = getString(R.string.enter_seed_phrase_error)
                    }
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        requireActivity().hideSoftKeyboard()
    }

    override fun screenId(): ScreenId = ScreenId.OWNER_ENTER_SEED

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    private fun submit() {
        binding.seedPhraseLayout.isErrorEnabled = false
        val seedPhrase = viewModel.cleanupSeedPhrase(binding.seedPhraseText.text.toString())
        viewModel.validate(seedPhrase).takeIf { it }?.run {
            findNavController().navigate(ImportOwnerKeyFragmentDirections.actionImportOwnerKeyFragmentToOwnerSelectionFragment(seedPhrase))
        }
    }
}
