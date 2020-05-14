package io.gnosis.safe.ui.safe.terms

import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.BottomSheetTermsAndConditionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseBottomSheetDialogFragment
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.openUrl

class TermsBottomSheetDialog : BaseBottomSheetDialogFragment<BottomSheetTermsAndConditionsBinding>() {
    lateinit var onAgreeClickListener: () -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            bottomSheetTermsAndConditionsPrivacyPolicyLink.appendLink(getString(R.string.link_terms_privacy), getString(R.string.terms_privacy_policy))
            bottomSheetTermsAndConditionsPrivacyPolicyLink.appendLink(getString(R.string.link_terms_terms_of_use), getString(R.string.terms_terms_of_use))

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
    var colorSpans: Array<ForegroundColorSpan> =
        (text as SpannedString).getSpans(0, text.length, ForegroundColorSpan::class.java)
//    val linkDrawable = ContextCompat.getDrawable(this.context, R.drawable.ic_external_link)!!
//    linkDrawable.setBounds(0, 0, linkDrawable.intrinsicWidth, linkDrawable.intrinsicHeight)
    this.text = SpannableStringBuilder()
        .appendText(Html.fromHtml(text.toString()), colorSpans[0])
        .append(" ")
        .appendText(urlText, ForegroundColorSpan(context.getColorCompat(R.color.link)))
//        .append(" ")
//        .appendText(" ", ImageSpan(linkDrawable, ImageSpan.ALIGN_BASELINE))
    setOnClickListener { this.context.openUrl(url) }
}
