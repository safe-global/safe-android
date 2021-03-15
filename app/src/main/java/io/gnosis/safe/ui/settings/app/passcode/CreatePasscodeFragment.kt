package io.gnosis.safe.ui.settings.app.passcode

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.R

class CreatePasscodeFragment : Fragment() {

    companion object {
        fun newInstance() = CreatePasscodeFragment()
    }

    private lateinit var viewModel: CreatePasscodeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_passcode, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreatePasscodeViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
