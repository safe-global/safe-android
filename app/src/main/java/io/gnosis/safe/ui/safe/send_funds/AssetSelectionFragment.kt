package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAssetSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import javax.inject.Inject

class AssetSelectionFragment : BaseViewBindingFragment<FragmentAssetSelectionBinding>() {

    private val navArgs by navArgs<AssetSelectionFragmentArgs>()
    private val selectedChain by lazy { navArgs.chain }

    override fun screenId() = ScreenId.SAFE_ADD_ADDRESS

    override suspend fun chainId() = selectedChain.chainId

    @Inject
    lateinit var viewModel: AssetSelectionViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAssetSelectionBinding =
        FragmentAssetSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}