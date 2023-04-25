package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentKeystoneRequestSignatureBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor

class KeystoneRequestSignatureFragment: BaseViewBindingFragment<FragmentKeystoneRequestSignatureBinding>() {
    private val navArgs by navArgs<KeystoneRequestSignatureFragmentArgs>()
    private val owner by lazy { navArgs.owner }
    private val chain by lazy { navArgs.chain }
    private val safeTxHash by lazy { navArgs.safeTxHash }

    override fun screenId() = ScreenId.KEYSTONE_REQUEST_SIGNATURE

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeystoneRequestSignatureBinding =
        FragmentKeystoneRequestSignatureBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }

            chainRibbon.text = chain.name
            chainRibbon.setTextColor(
                chain.textColor.toColor(
                    requireContext(),
                    R.color.white
                )
            )
            chainRibbon.setBackgroundColor(
                chain.backgroundColor.toColor(
                    requireContext(),
                    R.color.primary
                )
            )
        }
    }
}