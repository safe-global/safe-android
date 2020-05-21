package io.gnosis.safe.ui.safe.settings.app

import android.content.ActivityNotFoundException
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
import io.gnosis.safe.databinding.FragmentGetInTouchBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import pm.gnosis.svalinn.common.utils.snackbar

class GetInTouchFragment : BaseFragment<FragmentGetInTouchBinding>() {

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
                sendFeatureSuggestion()
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
        //TODO:
    }

    private fun openTwitter() {
        //TODO:
    }

    private fun openHelpCenter() {
        //TODO:
    }

    private fun sendFeatureSuggestion() {
        //TODO:
    }

    // Do we need telegram channel or is discord sufficient?
    private fun openTelegramChannel() {
        var intent = try {
            Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=${getString(R.string.id_telegram)}"))
        } catch (e: Exception) { //App not found open in browser
            Intent(Intent.ACTION_VIEW, Uri.parse("http://www.telegram.me/${getString(R.string.id_telegram)}"))
        }
        startActivity(intent)
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "text/html"
        intent.data = Uri.parse("mailto:${getString(R.string.email_feedback)}")
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject, getString(R.string.app_name)))
        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            snackbar(binding.root, getString(R.string.email_chooser_error), Snackbar.LENGTH_SHORT)
        }
    }
}
