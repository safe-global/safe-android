package io.gnosis.safe.ui.safe.terms

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.R
import io.gnosis.safe.databinding.BottomSheetTermsAndConditionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseBottomSheetDialogFragment

class TermsBottomSheetDialog() : BaseBottomSheetDialogFragment<BottomSheetTermsAndConditionsBinding>() {
    lateinit var onAgreeClickListener: () -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            bottomSheetTermsAndConditionsPrivacyPolicyLink.text = Html.fromHtml(context!!.getString(R.string.terms_privacy_link))
            bottomSheetTermsAndConditionsPrivacyPolicyLink.movementMethod = LinkMovementMethod.getInstance()

            bottomSheetTermsAndConditionsPrivacyPolicyLink.text = Html.fromHtml(context!!.getString(R.string.terms_terms_of_use_link))
            bottomSheetTermsAndConditionsPrivacyPolicyLink.movementMethod = LinkMovementMethod.getInstance()

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
