package io.gnosis.safe.ui.settings.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.databinding.FragmentAboutSafeBinding
import io.gnosis.safe.databinding.FragmentGetInTouchBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.settings.SettingsFragmentDirections
import pm.gnosis.svalinn.common.utils.openUrl

class AboutSafeFragment : BaseViewBindingFragment<FragmentAboutSafeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_ABOUT_SAFE

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAboutSafeBinding =
        FragmentAboutSafeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            terms.setOnClickListener {
                requireContext().openUrl(getString(io.gnosis.safe.R.string.link_terms_of_use))
            }
            privacy.setOnClickListener {
                requireContext().openUrl(getString(io.gnosis.safe.R.string.link_privacy_policy))
            }
            licenses.setOnClickListener {
                requireContext().openUrl(getString(io.gnosis.safe.R.string.link_licenses))
            }
            rateApp.setOnClickListener {
                openPlayStore()
            }
        }
    }

    private fun openPlayStore() {
        kotlin.runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireActivity().packageName}")))
        }
            .onFailure {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store.apps/details?id=${requireActivity().packageName}")))
            }
    }
}
