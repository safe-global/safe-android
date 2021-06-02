package io.gnosis.safe.ui.settings.owner.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentOwnerExportBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.export.OwnerExportPageAdapter.Tabs
import pm.gnosis.svalinn.common.utils.shareExternalText
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.removeHexPrefix

class OwnerExportFragment : BaseViewBindingFragment<FragmentOwnerExportBinding>() {

    private val navArgs by navArgs<OwnerExportFragmentArgs>()
    private val ownerKey by lazy { navArgs.ownerKey.removeHexPrefix() }
    private val ownerSeed by lazy { navArgs.ownerSeed }

    override fun screenId() = null

    private lateinit var pager: OwnerExportPageAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerExportBinding =
        FragmentOwnerExportBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {

            backButton.setOnClickListener {
                findNavController().navigateUp()
            }

            pager = OwnerExportPageAdapter(this@OwnerExportFragment, ownerKey, ownerSeed)
            content.adapter = pager
            if (ownerSeed != null) {
                tabBar.visible(true)
                TabLayoutMediator(tabBar, content, true) { tab, position ->
                    when (Tabs.values()[position]) {
                        Tabs.SEED -> {
                            tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_notepad_24dp)
                            tab.text = getString(R.string.signing_owner_export_tab_seed)
                        }
                        Tabs.KEY -> {
                            tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_locked_green_24dp)
                            tab.text = getString(R.string.signing_owner_export_tab_key)
                        }
                    }
                }.attach()
            } else {
                tabBar.visible(false)
            }

            shareButton.setOnClickListener {
                when (pager.tabForPosition(content.currentItem)) {
                    Tabs.SEED -> {
                        requireContext().shareExternalText(ownerSeed!!)
                    }
                    Tabs.KEY -> {
                        requireContext().shareExternalText(ownerKey)
                    }
                }
            }
        }
    }
}

class OwnerExportPageAdapter(
    fragment: Fragment,
    private val key: String,
    private val seed: String?
) : FragmentStateAdapter(fragment) {

    enum class Tabs { SEED, KEY }

    override fun getItemCount(): Int = if (seed != null) 2 else 1

    override fun createFragment(position: Int): Fragment =
        if (seed != null) {
            when (Tabs.values()[position]) {
                Tabs.SEED -> OwnerExportSeedFragment.newInstance(seed)
                Tabs.KEY -> OwnerExportKeyFragment.newInstance(key, false)
            }
        } else {
            OwnerExportKeyFragment.newInstance(key, true)
        }

    fun tabForPosition(position: Int) = if (seed != null) Tabs.values()[position] else Tabs.KEY
}
