package io.gnosis.safe.ui.settings.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppChainPrefixBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import java.math.BigInteger

class ChainPrefixAppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppChainPrefixBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_CHAIN_PREFIX

    override suspend fun chainId(): BigInteger? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppChainPrefixBinding =
        FragmentSettingsAppChainPrefixBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
