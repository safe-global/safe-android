package io.gnosis.safe.ui

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.review.ReviewManagerFactory
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.OnActivityChanged
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ToolbarSafeOverviewBinding
import io.gnosis.safe.ui.base.SafeOverviewNavigationHandler
import io.gnosis.safe.ui.base.activity.BaseActivity
import io.gnosis.safe.ui.transactions.TransactionsFragmentDirections
import io.gnosis.safe.ui.transactions.TxPagerAdapter
import io.gnosis.safe.utils.abbreviateEthAddress
import io.gnosis.safe.utils.dpToPx
import kotlinx.coroutines.launch
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class StartActivity : BaseActivity(), SafeOverviewNavigationHandler, OnActivityChanged {

    @Inject
    lateinit var safeRepository: SafeRepository

    @Inject
    lateinit var credentialsRepository: CredentialsRepository

    private val toolbarBinding by lazy {
        ToolbarSafeOverviewBinding.bind(findViewById(R.id.toolbar_container))
    }
    private val toolbar by lazy { findViewById<View>(R.id.toolbar) }
    private val navBar by lazy { findViewById<BottomNavigationView>(R.id.nav_bar) }

    var comingFromBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        if (settingsHandler.requireToOpen && settingsHandler.usePasscode) {
            Navigation.findNavController(this@StartActivity, R.id.nav_host).navigate(R.id.enterPasscodeFragment, Bundle().apply {
                putString("selectedOwner", "Dummy")
                putBoolean("requirePasscodeToOpen", true)
            })
        }

        viewComponent().inject(this)

        toolbarBinding.safeSelection.setOnClickListener {
            Navigation.findNavController(this, R.id.nav_host).navigate(R.id.safeSelectionDialog)
        }
        setupNav()

        handleNotifications(intent)

        (application as? HeimdallApplication)?.registerForActivity(this)
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
                        setSafeData(it)
                    }
                    if (txId == null) {
                        Navigation.findNavController(this@StartActivity, R.id.nav_host).navigate(R.id.transactionsFragment, Bundle().apply {
                            putInt("activeTab", TxPagerAdapter.Tabs.HISTORY.ordinal) // open history tab
                            putBoolean("requirePasscode", settingsHandler.requireToOpen && settingsHandler.usePasscode && comingFromBackground)
                        })
                    } else {
                        with(Navigation.findNavController(this@StartActivity, R.id.nav_host)) {
                            navigate(R.id.transactionsFragment, Bundle().apply {
                                putInt("activeTab", TxPagerAdapter.Tabs.QUEUE.ordinal) // open queued tab
                            })

                            navigate(
                                TransactionsFragmentDirections.actionTransactionsFragmentToTransactionDetailsFragment(
                                    txId,
                                    settingsHandler.requireToOpen && settingsHandler.usePasscode && comingFromBackground
                                )
                            )
                        }
                    }
                    comingFromBackground = false
                }
            } ?: run {
                settingsHandler.appStartCount++
                if (settingsHandler.appStartCount >= 3) {
                    startRateFlow()
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
                id != R.id.transactionsFragment &&
                id != R.id.settingsFragment &&
                id != R.id.safeSelectionDialog &&
                id != R.id.shareSafeDialog

    override fun setSafeData(safe: Safe?) {
        if (safe == null) {
            setNoSafe()
        } else {
            setSafe(safe)
            checkReadOnly()
        }
    }

    private fun setNoSafe() {
        with(toolbarBinding) {
            safeImage.setOnClickListener(null)
            safeImage.setAddress(null)
            safeImage.setImageResource(R.drawable.ic_no_safe_loaded_36dp)
            safeName.visible(false)
            readOnly.visible(false, View.INVISIBLE)
            safeAddress.text = getString(io.gnosis.safe.R.string.no_safes_loaded)
            safeSelection.visible(false)
        }
    }

    private fun setSafe(safe: Safe) {
        with(toolbarBinding) {
            safeImage.setOnClickListener {
                navigateToShareSafeDialog()
            }
            safeImage.setAddress(safe.address)
            safeName.visible(true)
            safeName.text = safe.localName
            safeName.setOnClickListener {
                navigateToShareSafeDialog()
            }

            adjustSafeNameWidth()

            safeAddress.text = safe.address.asEthereumAddressChecksumString().abbreviateEthAddress()
            safeAddress.setOnClickListener {
                navigateToShareSafeDialog()
            }

            safeSelection.visible(true)
        }
    }

    private fun adjustSafeNameWidth() {
        with(toolbarBinding) {
            val bounds = Rect()
            safeName.paint.getTextBounds(safeName.text.toString(), 0, safeName.text.length, bounds)
            val safeNameLength = bounds.right - bounds.left

            // wait till views are measured
            safeName.post {
                val readOnlySpace = readOnly.measuredWidth + readOnly.marginLeft + readOnly.marginRight
                if (safeNameLength > safeAddress.measuredWidth - readOnlySpace) {
                    safeName.width = safeAddress.measuredWidth - readOnlySpace
                    safeName.ellipsize = TextUtils.TruncateAt.END
                } else {
                    safeName.ellipsize = null
                    safeName.width = safeNameLength + dpToPx(1)
                }
            }
        }
    }

    private fun checkReadOnly() {
        lifecycleScope.launch {
            // fail silently if safe info cannot be loaded
            kotlin.runCatching {
                val activeSafe = safeRepository.getActiveSafe()
                activeSafe?.let {
                    val safeOwners = safeRepository.getSafeInfo(it.address).owners.map { it.value }.toSet()
                    val localOwners = credentialsRepository.owners().map { it.address }.toSet()
                    toolbarBinding.readOnly.visible(safeOwners.intersect(localOwners).isEmpty(), View.INVISIBLE)
                }
            }.onFailure {
                tracker.logException(it)
                toolbarBinding.readOnly.visible(false, View.INVISIBLE)
            }
        }
    }

    private fun startRateFlow() {
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = request.result
                val flow = manager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow and reset the counter
                    settingsHandler.appStartCount = 0
                }

            }
        }
    }

    private fun navigateToShareSafeDialog() {
        Navigation.findNavController(this@StartActivity, R.id.nav_host).navigate(R.id.shareSafeDialog)
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

    override fun appInForeground() {
        Timber.i("---> appInForeground()")
    }

    override fun appInBackground() {
        Timber.i("---> appInBackground()")
        comingFromBackground = true
        if (settingsHandler.requireToOpen && settingsHandler.usePasscode && comingFromBackground) {
            Navigation.findNavController(this@StartActivity, R.id.nav_host).navigate(R.id.enterPasscodeFragment, Bundle().apply {
                putString("selectedOwner", "Dummy")
                putBoolean("requirePasscodeToOpen", true)
            })
            comingFromBackground = false
        }
    }
}
