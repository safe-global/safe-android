package io.gnosis.safe.ui.settings.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentGetInTouchBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.launch
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import timber.log.Timber
import javax.inject.Inject

class GetInTouchFragment : BaseViewBindingFragment<FragmentGetInTouchBinding>() {

    @Inject
    lateinit var helper: GetInTouchViewModel

    override fun screenId() = ScreenId.SETTINGS_GET_IN_TOUCH

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGetInTouchBinding =
        FragmentGetInTouchBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
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
        lifecycleScope.launch {
            val feedback = helper.createFeedbackText(requireContext())
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                type = "text/html"
                // gmail is ignoring intent extras
                data = Uri.parse(
                    "mailto:${getString(R.string.email_feedback)}" +
                            "?subject=${getString(R.string.feedback_subject)}" +
                            "&body=${feedback}"
                )
                putExtra(Intent.EXTRA_EMAIL, getString(R.string.email_feedback))
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                putExtra(Intent.EXTRA_TEXT, feedback)
            }
            Intent.FLAG_ACTIVITY_NEW_TASK
            kotlin.runCatching {
                startActivity(intent)
            }
                .onFailure {
                    Timber.e(it)
                    snackbar(binding.root, getString(R.string.email_chooser_error), Snackbar.LENGTH_SHORT)
                }
        }
    }
}

class GetInTouchViewModel
@Inject constructor(
    private val safeRepository: SafeRepository
) : ViewModel() {

    suspend fun createFeedbackText(context: Context): String {
        val activeSafe = safeRepository.getActiveSafe()
        val text = StringBuilder()
        text.appendln("${context.getString(R.string.app_name)} v${BuildConfig.VERSION_NAME}")
        text.appendln(context.getString(R.string.feedback_safe, activeSafe?.address?.asEthereumAddressChecksumString() ?: ""))
        text.appendln(context.getString(R.string.feedback_chain, activeSafe?.chain?.name ?: "", activeSafe?.chain?.chainId))
        text.appendln(context.getString(R.string.feedback_feedback))
        return text.toString()
    }
}
