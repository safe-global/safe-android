package io.gnosis.safe.ui.safe.terms

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.BottomSheetTermsAndConditionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseBottomSheetDialogFragment
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.openUrl

class TermsBottomSheetDialog : BaseBottomSheetDialogFragment<BottomSheetTermsAndConditionsBinding>() {
    lateinit var onAgreeClickListener: () -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            bottomSheetTermsAndConditionsPrivacyPolicyLink.appendLink(
                getString(R.string.link_terms_privacy),
                getString(R.string.terms_privacy_policy)
            )
            bottomSheetTermsAndConditionsTermsOfUseLink.appendLink(
                getString(R.string.link_terms_terms_of_use),
                getString(R.string.terms_terms_of_use)
            )

            bottomSheetTermsAndConditionsAgree.setOnClickListener {
                onAgreeClickListener()
            }

            bottomSheetTermsAndConditionsReject.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) = BottomSheetTermsAndConditionsBinding.inflate(layoutInflater)

    override fun inject(viewComponent: ViewComponent) {
        viewComponent.inject(this)
    }
}

fun TextView.appendLink(url: String, urlText: String) {
    text = SpannableStringBuilder().appendText(urlText, UnderlineSpan())
    setOnClickListener { context.openUrl(url) }
}
