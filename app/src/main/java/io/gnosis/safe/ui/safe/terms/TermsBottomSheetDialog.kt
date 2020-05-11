package io.gnosis.safe.ui.safe.terms

import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.R
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.safe.terms.TermsViewModel.ViewAction.ShowBottomSheet
import io.gnosis.safe.ui.safe.terms.TermsViewModel.ViewAction.TermsAgreed
import kotlinx.android.synthetic.main.bottom_sheet_terms_and_conditions.*
import javax.inject.Inject

class TermsBottomSheetDialog(val activity: AppCompatActivity) : BottomSheetDialog(activity) {

    private lateinit var advance: () -> Unit

    @Inject
    lateinit var viewModel: TermsViewModel

    init {
        inject()

        setContentView(layoutInflater.inflate(R.layout.bottom_sheet_terms_and_conditions, null))

        bottom_sheet_terms_and_conditions_privacy_policy_link.text = Html.fromHtml(activity.getString(R.string.terms_privacy_link))
        bottom_sheet_terms_and_conditions_privacy_policy_link.movementMethod = LinkMovementMethod.getInstance()

        bottom_sheet_terms_and_conditions_terms_of_use_link.text = Html.fromHtml(activity.getString(R.string.terms_terms_of_use_link))
        bottom_sheet_terms_and_conditions_terms_of_use_link.movementMethod = LinkMovementMethod.getInstance()

        bottom_sheet_terms_and_conditions_agree.setOnClickListener {
            onAgree()
        }

        bottom_sheet_terms_and_conditions_reject.setOnClickListener {
            onReject()
        }

        viewModel.state.observe(activity, Observer { viewAction ->
            when (viewAction) {
                ShowBottomSheet -> show()
                TermsAgreed -> dismissAndAdvance()
            }
        })
    }

    private fun dismissAndAdvance() {
        if (this.isShowing) {
            dismiss()
        }
        advance()
    }

    private fun onAgree() {
        viewModel.onAgreeClicked()
    }

    private fun onReject() {
        dismiss()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[context])
            .viewModule(ViewModule(context))
            .build().inject(this)
    }

    fun checkTerms(function: () -> Unit) {
        viewModel.checkTerms()
        advance = function
    }
}
