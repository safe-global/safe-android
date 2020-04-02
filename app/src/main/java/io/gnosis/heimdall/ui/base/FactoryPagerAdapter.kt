package io.gnosis.heimdall.ui.base

import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * All fragments are lazily instantiated and cached until the adapter is gc'd.
 * Should not be used with a large amount of items
 */
class FactoryPagerAdapter(fragmentManager: FragmentManager, val factory: Factory) : FragmentPagerAdapter(fragmentManager) {

    private val fragments: SparseArray<Fragment> = SparseArray()

    override fun getItem(position: Int): Fragment {
        var fragment = fragments.get(position)
        if (fragment == null) {
            fragment = createFragment(position)
            fragments.put(position, fragment)
        }
        return fragment
    }

    private fun createFragment(position: Int) = factory.provider(position)

    override fun getCount() = factory.itemCount

    override fun getPageTitle(position: Int): CharSequence? = factory.title?.invoke(position)

    class Factory(val itemCount: Int, val provider: ((Int) -> Fragment), val title: ((Int) -> CharSequence)? = null)
}
