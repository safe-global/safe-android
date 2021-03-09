package io.gnosis.safe.ui.settings.app

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.R

class PasscodeSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = PasscodeSettingsFragment()
    }

    private lateinit var viewModel: PasscodeSettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_app_passcode, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PasscodeSettingsViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
