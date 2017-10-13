package pm.gnosis.heimdall.ui.multisig.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_multisig_details.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.base.FactoryPagerAdapter
import pm.gnosis.heimdall.ui.multisig.details.info.MultisigInfoFragment
import pm.gnosis.heimdall.ui.multisig.details.info.MultisigInfoViewModel
import pm.gnosis.heimdall.ui.tokens.TokensFragment
import javax.inject.Inject


class MultisigDetailsActivity : BaseActivity() {

    private lateinit var multisigAddress: String

    private val items = listOf(R.string.tab_title_info, R.string.tab_title_transactions, R.string.tab_title_tokens)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_multisig_details)

        registerToolbar(layout_multisig_details_toolbar)
        layout_multisig_details_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        multisigAddress = intent.getStringExtra(EXTRA_MULTISIG_ADDRESS)!!
        val multisigName = intent.getStringExtra(EXTRA_MULTISIG_NAME)
        if (!multisigName.isNullOrBlank()) {
            layout_multisig_details_toolbar.title = multisigName
            layout_multisig_details_toolbar.subtitle = multisigAddress
        } else {
            layout_multisig_details_toolbar.title = multisigAddress
        }
        layout_multisig_details_viewpager.adapter = pagerAdapter()
        layout_multisig_details_tabbar.setupWithViewPager(layout_multisig_details_viewpager)
    }

    private fun positionToId(position: Int) = items.getOrElse(position, { -1 })

    private fun pagerAdapter() = FactoryPagerAdapter(supportFragmentManager, FactoryPagerAdapter.Factory(items.size, {
        when (positionToId(it)) {
            R.string.tab_title_info -> {
                MultisigInfoFragment.createInstance(multisigAddress)
            }
            R.string.tab_title_transactions -> {
                AccountFragment()
            }
            R.string.tab_title_tokens -> {
                TokensFragment()
            }
            else -> throw IllegalStateException("Unhandled tab position")
        }
    }, {
        getString(items[it])
    }))

    companion object {
        private const val EXTRA_MULTISIG_NAME = "extra.string.multisig_name"
        private const val EXTRA_MULTISIG_ADDRESS = "extra.string.multisig_address"
        fun createIntent(context: Context, multisig: MultisigWallet): Intent {
            val intent = Intent(context, MultisigDetailsActivity::class.java)
            intent.putExtra(EXTRA_MULTISIG_NAME, multisig.name)
            intent.putExtra(EXTRA_MULTISIG_ADDRESS, multisig.address)
            return intent
        }
    }
}