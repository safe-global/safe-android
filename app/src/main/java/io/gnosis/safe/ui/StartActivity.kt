package io.gnosis.safe.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ToolbarSafeOverviewBinding
import io.gnosis.safe.ui.base.SafeOverviewNavigationHandler
import io.gnosis.safe.ui.base.activity.BaseActivity
import io.gnosis.safe.utils.abbreviateEthAddress
import kotlinx.coroutines.launch
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class StartActivity : BaseActivity(), SafeOverviewNavigationHandler {

    @Inject
    lateinit var safeRepository: SafeRepository

    private val toolbarBinding by lazy {
        ToolbarSafeOverviewBinding.bind(findViewById(R.id.toolbar_container))
    }
    private val toolbar by lazy { findViewById<View>(R.id.toolbar) }
    private val navBar by lazy { findViewById<BottomNavigationView>(R.id.nav_bar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        viewComponent().inject(this)

        toolbarBinding.safeSelection.setOnClickListener {
            Navigation.findNavController(this, R.id.nav_host).navigate(R.id.safeSelectionDialog)
        }
        setupNav()

        handleNotifications(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotifications(intent)
    }

    // Workaround in order to change active safe when push notification for unselected safe is received
    private fun handleNotifications(intent: Intent?) {
        intent?.let {
            val safeAddress = it.getStringExtra(EXTRA_SAFE)?.asEthereumAddress()
            val txId = it.getStringExtra(EXTRA_TX_ID)

            safeAddress?.let {

                lifecycleScope.launch {
                    val safe = safeRepository.getSafeBy(safeAddress)
                    safe?.let {
                        safeRepository.setActiveSafe(it)
                    }
                    if (txId == null) {
                        Navigation.findNavController(this@StartActivity, R.id.nav_host).navigate(R.id.transactionListFragment)
                    } else {
                        Navigation.findNavController(this@StartActivity, R.id.nav_host).navigate(R.id.transactionDetailsFragment, Bundle().apply {
                            putString("txId", txId)
                        })
                    }
                }
            }
        }
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
        id != R.id.assetsFragment &&
                id != R.id.transactionListFragment &&
                id != R.id.settingsFragment &&
                id != R.id.safeSelectionDialog &&
                id != R.id.shareSafeDialog

    override fun setSafeData(safe: Safe?) {
        if (safe == null)
            setNoSafe()
        else
            setSafe(safe)
    }

    private fun setNoSafe() {
        with(toolbarBinding) {
            safeImage.setOnClickListener(null)
            safeImage.setAddress(null)
            safeImage.setImageResource(R.drawable.ic_no_safe_loaded_36dp)
            safeName.visible(false)
            safeAddress.text = getString(io.gnosis.safe.R.string.no_safes_loaded)
            safeSelection.visible(false)
        }
    }

    private fun setSafe(safe: Safe) {
        with(toolbarBinding) {
            safeImage.setOnClickListener {
                Navigation.findNavController(this@StartActivity, R.id.nav_host).navigate(R.id.shareSafeDialog)
            }
            safeImage.setAddress(safe.address)
            safeName.visible(true)
            safeName.text = safe.localName
            safeAddress.text = safe.address.asEthereumAddressChecksumString().abbreviateEthAddress()
            safeSelection.visible(true)
        }
    }

    override fun screenId() = null

    companion object {
        const val EXTRA_SAFE = "extra.string.safe"
        const val EXTRA_TX_ID = "extra.string.tx_id"

        fun createIntent(context: Context, safe: Safe, txId: String? = null) =
            Intent(context, StartActivity::class.java).apply {
                putExtra(EXTRA_SAFE, safe.address.asEthereumAddressString())
                putExtra(EXTRA_TX_ID, txId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }
}
