package io.gnosis.safe.ui.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.BottomSheetTermsAndConditionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseBottomSheetDialogFragment
import io.gnosis.safe.utils.appendLink

class TermsBottomSheetDialog : BaseBottomSheetDialogFragment<BottomSheetTermsAndConditionsBinding>() {

    lateinit var onAgreeClickListener: () -> Unit
    lateinit var onDismissClickListener: () -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            bottomSheetTermsAndConditionsPrivacyPolicyLink.appendLink(
                getString(R.string.link_privacy_policy),
                getString(R.string.terms_privacy_policy)
            )
            bottomSheetTermsAndConditionsTermsOfUseLink.appendLink(
                getString(R.string.link_terms_of_use),
                getString(R.string.terms_terms_of_use)
            )

            bottomSheetTermsAndConditionsAgree.setOnClickListener {
                onAgreeClickListener()
            }

            bottomSheetTermsAndConditionsReject.setOnClickListener {
                onDismissClickListener()
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) = BottomSheetTermsAndConditionsBinding.inflate(layoutInflater)

    override fun screenId() = ScreenId.LAUNCH_TERMS

    override fun inject(viewComponent: ViewComponent) {
        viewComponent.inject(this)
    }
}
