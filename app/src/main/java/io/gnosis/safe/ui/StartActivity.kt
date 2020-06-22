package io.gnosis.safe.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ToolbarSafeOverviewBinding
import io.gnosis.safe.ui.base.BaseActivity
import io.gnosis.safe.ui.safe.SafeOverviewNavigationHandler
import io.gnosis.safe.utils.asMiddleEllipsized
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString

class StartActivity : BaseActivity(), SafeOverviewNavigationHandler {

    private val toolbarBinding by lazy {
        ToolbarSafeOverviewBinding.bind(findViewById(R.id.toolbar_container))
    }
    private val toolbar by lazy { findViewById<View>(R.id.toolbar) }
    private val navBar by lazy { findViewById<BottomNavigationView>(R.id.nav_bar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        toolbarBinding.safeSelection.setOnClickListener {
            Navigation.findNavController(this, R.id.nav_host).navigate(R.id.safeSelectionDialog)
        }
        setupNav()
    }

    private fun setupNav() {
        val navController = Navigation.findNavController(this, R.id.nav_host)
        navBar.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (isFullscreen(destination.id)) {
                toolbar.visibility = View.GONE
                navBar.visibility = View.GONE
            } else {
                toolbar.visibility = View.VISIBLE
                navBar.visibility = View.VISIBLE
            }
        }
    }

    private fun isFullscreen(id: Int?): Boolean =
        id != R.id.safeBalancesFragment &&
                id != R.id.transactionsFragment &&
                id != R.id.settingsFragment &&
                id != R.id.safeSelectionDialog

    override fun setSafeData(safe: Safe?) {
        if (safe == null)
            setNoSafe()
        else
            setSafe(safe)
    }

    private fun setNoSafe() {
        with(toolbarBinding) {
            safeImage.setAddress(null)
            safeImage.setImageResource(io.gnosis.safe.R.drawable.ic_no_safe_loaded_36dp)
            safeName.visible(false)
            safeAddress.text = getString(io.gnosis.safe.R.string.no_safes_loaded)
            safeSelection.visible(false)
        }
    }

    private fun setSafe(safe: Safe) {
        with(toolbarBinding) {
            safeImage.setAddress(safe.address)
            safeName.visible(true)
            safeName.text = safe.localName
            safeAddress.text = safe.address.asEthereumAddressString().asMiddleEllipsized(4)
            safeSelection.visible(true)
        }
    }

    override fun screenId() = null
}
