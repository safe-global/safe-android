package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerSeedPhraseBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

class OwnerSeedPhraseFragment : BaseViewBindingFragment<FragmentOwnerSeedPhraseBinding>() {

    @Inject
    lateinit var viewModel: OwnerSeedPhraseViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerSeedPhraseBinding =
        FragmentOwnerSeedPhraseBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            enterSeedPhraseDescription.text = replaceDoubleNewlineWithParagraphLineSpacing(R.string.enter_seed_phrase_description)

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
                    val error = state.throwable.toError()
                    if (error.trackingRequired) {
                        tracker.logException(state.throwable)
                    }
                    with(binding) {
                        seedPhraseLayout.error = error.message(requireContext(), R.string.error_description_seed_phrase)
                        seedPhraseLayout.isErrorEnabled = true
                        nextButton.isEnabled = false
                    }
                }
                is ImportOwnerKeyState.ValidSeedPhraseSubmitted -> {
                    findNavController().navigate(
                        OwnerSeedPhraseFragmentDirections.actionOwnerSeedPhraseFragmentToOwnerSelectionFragment(
                            null,
                            state.validSeedPhrase
                        )
                    )
                }
                is ImportOwnerKeyState.ValidKeySubmitted -> {
                    findNavController().navigate(
                        OwnerSeedPhraseFragmentDirections.actionOwnerSeedPhraseFragmentToOwnerSelectionFragment(
                            state.key,
                            null
                        )
                    )
                }
            }
        })
    }

    private fun replaceDoubleNewlineWithParagraphLineSpacing(@StringRes stringResource: Int): SpannableString {
        val spannableString = SpannableString(resources.getString(stringResource));
        val matcher = Pattern.compile("\n\n").matcher(resources.getString(stringResource));
        while (matcher.find()) {
            spannableString.setSpan(
                AbsoluteSizeSpan(resources.getDimension(R.dimen.default_paragraph_line_spacing).toInt()),
                matcher.start() + 1,
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return spannableString
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
