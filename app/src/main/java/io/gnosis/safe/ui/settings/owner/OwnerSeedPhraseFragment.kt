package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerSeedPhraseBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import timber.log.Timber
import javax.inject.Inject

class OwnerSeedPhraseFragment : BaseViewBindingFragment<FragmentOwnerSeedPhraseBinding>() {

    @Inject
    lateinit var viewModel: OwnerSeedPhraseViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerSeedPhraseBinding =
        FragmentOwnerSeedPhraseBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            backButton.setOnClickListener { findNavController().navigateUp() }
            nextButton.setOnClickListener { submit() }

            seedPhraseLayout.isErrorEnabled = false

            seedPhraseText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            seedPhraseText.setRawInputType(InputType.TYPE_CLASS_TEXT);
            seedPhraseText.doOnTextChanged { text, _, _, _ ->
                binding.seedPhraseLayout.isErrorEnabled = false
                binding.nextButton.isEnabled = !text.isNullOrBlank()
            }
            seedPhraseText.setOnEditorActionListener listener@{ _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit()
                    return@listener true
                }
                return@listener false
            }
            seedPhraseText.showKeyboardForView()
        }

        viewModel.state().observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is ImportOwnerKeyState.Error -> {
                    Timber.e(state.throwable)
                    with(binding) {
                        seedPhraseLayout.isErrorEnabled = true
                        seedPhraseLayout.error = getString(R.string.enter_seed_phrase_error)
                        nextButton.isEnabled = false
                    }
                }
                is ImportOwnerKeyState.ValidSeedPhraseSubmitted -> {
                    findNavController().navigate(OwnerSeedPhraseFragmentDirections.actionOwnerSeedPhraseFragmentToOwnerSelectionFragment(state.validSeedPhrase))
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
