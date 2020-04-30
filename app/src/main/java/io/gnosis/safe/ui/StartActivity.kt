package io.gnosis.safe.ui

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.BaseActivity
import kotlinx.android.synthetic.main.bottom_sheet_terms_and_conditions.*

class StartActivity : BaseActivity() {

    private lateinit var termsBottomSheetDialog: TermsBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        termsBottomSheetDialog = TermsBottomSheetDialog(this).apply {
            setContentView(layoutInflater.inflate(R.layout.bottom_sheet_terms_and_conditions, null))
        }

        termsBottomSheetDialog.bottom_sheet_terms_and_conditions_third_bullet_text.apply {
            text = Html.fromHtml(getString(R.string.terms_third_bullet))
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onStart() {
        super.onStart()
        termsBottomSheetDialog.show()
    }

    class TermsBottomSheetDialog(context: Context) : BottomSheetDialog(context)
}
