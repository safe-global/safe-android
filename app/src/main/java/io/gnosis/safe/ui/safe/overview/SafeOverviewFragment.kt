package io.gnosis.safe.ui.safe.overview

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSafeOverviewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import kotlinx.android.synthetic.main.bottom_sheet_terms_and_conditions.*
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit

class SafeOverviewFragment : BaseFragment<FragmentSafeOverviewBinding>() {

    private lateinit var termsBottomSheetDialog: TermsBottomSheetDialog
//    private val preferencesManager =

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeOverviewBinding =
        FragmentSafeOverviewBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addSafeButton.setOnClickListener {
            if (termsAgreed()) {
                findNavController().navigate(SafeOverviewFragmentDirections.actionSafeOverviewFragmentToAddSafeNav())
            } else {
                termsBottomSheetDialog = TermsBottomSheetDialog(this.requireContext()).apply {
                    setContentView(layoutInflater.inflate(R.layout.bottom_sheet_terms_and_conditions, null))
                }
                termsBottomSheetDialog.bottom_sheet_terms_and_conditions_third_bullet_text.apply {
                    text = Html.fromHtml(getString(R.string.terms_third_bullet))
                    movementMethod = LinkMovementMethod.getInstance()
                }
                termsBottomSheetDialog.bottom_sheet_terms_and_conditions_agree.setOnClickListener {
                    setTermsAgreed(true)
                    termsBottomSheetDialog.dismiss()
                }
                termsBottomSheetDialog.show()
            }
        }
    }

    private fun termsAgreed(): Boolean {
        return getTermsAgreed()
    }

    fun setTermsAgreed(value: Boolean) {
        PreferencesManager(this.requireContext()).prefs.edit {
            putBoolean(TERMS_AGREED, value)
        }
    }

    fun getTermsAgreed(): Boolean = PreferencesManager(this.requireContext()).prefs.getBoolean(TERMS_AGREED, false)


    class TermsBottomSheetDialog(context: Context) : BottomSheetDialog(context)

    companion object {
        private const val TERMS_AGREED = "prefs.boolean.terms_agreed"
    }
}
