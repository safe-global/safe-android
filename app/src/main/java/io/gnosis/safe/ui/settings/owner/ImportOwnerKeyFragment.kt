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
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.snackbar
import javax.inject.Inject

class ImportOwnerKeyFragment : BaseViewBindingFragment<FragmentImportOwnerKeyBinding>() {

    @Inject
    lateinit var viewModel: ImportOwnerKeyViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentImportOwnerKeyBinding =
        FragmentImportOwnerKeyBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { findNavController().navigateUp() }
            nextButton.setOnClickListener { viewModel.validate(seedPhraseText.text.toString()) }
            seedPhraseText.doOnTextChanged { _, _, _, _ -> binding.seedPhraseLayout.isErrorEnabled = false }
            seedPhraseText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.validate(seedPhraseText.text.toString())
                }
                true
            }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is ImportOwnerKeyState.Error -> {
                    with(binding) {
                        seedPhraseLayout.error = getString(R.string.enter_seed_phrase_error)
                        seedPhraseLayout.isErrorEnabled = true
                    }
                }
                is ImportOwnerKeyState.ValidSeedPhraseSubmitted -> {
                    // TODO navigate to what goes next
                    // The value `state.validSeedPhrase` contains what to pass to the generator next
                    binding.seedPhraseLayout.isErrorEnabled = false
                    snackbar(requireView(), "Valid seed phrase submitted")
                }
            }
        })
    }

    override fun screenId(): ScreenId = ScreenId.OWNER_ENTER_SEED

    override fun viewModelProvider() = this

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }
}
