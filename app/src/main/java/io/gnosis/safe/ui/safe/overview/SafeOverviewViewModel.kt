package io.gnosis.safe.ui.safe.overview

import android.content.Context
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.R
import kotlinx.android.synthetic.main.bottom_sheet_terms_and_conditions.*
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject

class SafeOverviewViewModel
@Inject
constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private lateinit var termsBottomSheetDialog: TermsBottomSheetDialog

    private fun setTermsAgreed(value: Boolean) {
        preferencesManager.prefs.edit {
            putBoolean(TERMS_AGREED, value)
        }
    }

    private fun getTermsAgreed(): Boolean = preferencesManager.prefs.getBoolean(TERMS_AGREED, false)

    fun checkTerms(context: Context, agreed: () -> Unit) {
        if (getTermsAgreed()) {
            agreed()
        } else {
            termsBottomSheetDialog = TermsBottomSheetDialog(context).apply {
                setContentView(layoutInflater.inflate(R.layout.bottom_sheet_terms_and_conditions, null))
            }
            termsBottomSheetDialog.bottom_sheet_terms_and_conditions_third_bullet_text.apply {
                text = Html.fromHtml(context.getString(R.string.terms_third_bullet))
                movementMethod = LinkMovementMethod.getInstance()
            }
            termsBottomSheetDialog.bottom_sheet_terms_and_conditions_agree.setOnClickListener {
                setTermsAgreed(true)
                termsBottomSheetDialog.dismiss()
                agreed()
            }
            termsBottomSheetDialog.bottom_sheet_terms_and_conditions_reject.setOnClickListener {
                setTermsAgreed(false)
                termsBottomSheetDialog.dismiss()
            }
            termsBottomSheetDialog.show()
        }
    }

    companion object {
        private const val TERMS_AGREED = "prefs.boolean.terms_agreed"
    }

    class TermsBottomSheetDialog(context: Context) : BottomSheetDialog(context)
}
