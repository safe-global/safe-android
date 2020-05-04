package io.gnosis.safe.ui.safe.overview

import android.content.Context
import android.text.Html
import android.text.method.LinkMovementMethod
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.R
import kotlinx.android.synthetic.main.bottom_sheet_terms_and_conditions.*

class TermsBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    lateinit var onUserClicksAgree: () -> Unit
    val agree = bottom_sheet_terms_and_conditions_agree

    init {
        setContentView(layoutInflater.inflate(R.layout.bottom_sheet_terms_and_conditions, null))

        bottom_sheet_terms_and_conditions_privacy_policy_link.text = Html.fromHtml(context.getString(R.string.terms_privacy_link))
        bottom_sheet_terms_and_conditions_privacy_policy_link.movementMethod = LinkMovementMethod.getInstance()

        bottom_sheet_terms_and_conditions_terms_of_use_link.text = Html.fromHtml(context.getString(R.string.terms_terms_of_use_link))
        bottom_sheet_terms_and_conditions_terms_of_use_link.movementMethod = LinkMovementMethod.getInstance()

        bottom_sheet_terms_and_conditions_agree.setOnClickListener {
            println("bottom_sheet_terms_and_conditions_agree")
            dismiss()
            onUserClicksAgree()
        }

        bottom_sheet_terms_and_conditions_reject.setOnClickListener {
            dismiss()
        }
    }

}
