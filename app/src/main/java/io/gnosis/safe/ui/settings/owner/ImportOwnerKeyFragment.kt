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
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
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
        viewModel.state().observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is ImportOwnerKeyState.Error -> {
                    Timber.e(state.throwable)
                    with(binding) {
                        seedPhraseLayout.isErrorEnabled = true
                        seedPhraseLayout.error = getString(R.string.enter_seed_phrase_error)
                    }
                }
                is ImportOwnerKeyState.ValidSeedPhraseSubmitted -> {
                    findNavController().navigate(ImportOwnerKeyFragmentDirections.actionImportOwnerKeyFragmentToOwnerSelectionFragment(state.validSeedPhrase))
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
        viewModel.validate(binding.seedPhraseText.text.toString())
    }
}
