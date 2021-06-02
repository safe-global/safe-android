package io.gnosis.safe.ui.settings.owner.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentOwnerExportBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class OwnerExportFragment : BaseViewBindingFragment<FragmentOwnerExportBinding>() {

    private val navArgs by navArgs<OwnerExportFragmentArgs>()
    private val ownerKey by lazy { navArgs.ownerKey }
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
            pager = OwnerExportPageAdapter(this@OwnerExportFragment)
            pager.hasSeedPhrase = ownerSeed != null
            content.adapter = pager
            TabLayoutMediator(tabBar, content, true) { tab, position ->
                when (OwnerExportPageAdapter.Tabs.values()[position]) {
                    OwnerExportPageAdapter.Tabs.SEED -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_notepad_24dp)
                        tab.text = getString(R.string.signing_owner_export_tab_seed)
                    }
                    OwnerExportPageAdapter.Tabs.KEY -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_locked_green_24dp)
                        tab.text = getString(R.string.signing_owner_export_tab_key)
                    }
                }
            }.attach()
        }
    }

}

class OwnerExportPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { SEED, KEY }

    var hasSeedPhrase: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = if(hasSeedPhrase) 2 else 1

    override fun createFragment(position: Int): Fragment =
        if (!hasSeedPhrase) {
            OwnerExportKeyFragment.newInstance()
        } else {
            when (Tabs.values()[position]) {
                Tabs.SEED -> OwnerExportSeedFragment.newInstance()
                Tabs.KEY -> OwnerExportKeyFragment.newInstance()
            }
        }
}
