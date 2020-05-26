package io.gnosis.safe.ui.safe.settings.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentGetInTouchBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import timber.log.Timber

class GetInTouchFragment : BaseFragment<FragmentGetInTouchBinding>() {

    override fun screenId() = ScreenId.SETTINGS_GET_IN_TOUCH

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGetInTouchBinding =
        FragmentGetInTouchBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).setSupportActionBar(binding.getInTouchToolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            email.setOnClickListener {
                sendEmail()
            }
            discord.setOnClickListener {
                openDiscord()
            }
            twitter.setOnClickListener {
                openTwitter()
            }
            helpCenter.setOnClickListener {
                openHelpCenter()
            }
            featureSuggestion.setOnClickListener {
                openFeatureSuggestionPage()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openDiscord() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/${getString(R.string.id_discord)}")))
    }

    private fun openTwitter() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${getString(R.string.id_twitter)}")))
    }

    private fun openHelpCenter() {
        requireContext().openUrl(getString(R.string.link_help_center))
    }

    private fun openFeatureSuggestionPage() {
        requireContext().openUrl(getString(R.string.link_feature_suggestion))
    }
    
    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            type = "text/html"
            data = Uri.parse("mailto:${getString(R.string.email_feedback)}")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject, getString(R.string.app_name)))
        }
        kotlin.runCatching {
            startActivity(intent)
        }
            .onFailure {
                Timber.e(it)
                snackbar(binding.root, getString(R.string.email_chooser_error), Snackbar.LENGTH_SHORT)
            }
    }
}
