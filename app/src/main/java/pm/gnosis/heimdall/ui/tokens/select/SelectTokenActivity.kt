package pm.gnosis.heimdall.ui.tokens.select


import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_select_token.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesFragment
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.transaction
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class SelectTokenActivity : BaseActivity() {

    override fun screenId() = ScreenId.SELECT_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewComponent().inject(this)
        setContentView(R.layout.layout_select_token)

        val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }
        supportFragmentManager.transaction {
            replace(R.id.layout_select_token_fragment, TokenBalancesFragment.createInstance(safeAddress))
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_select_token_back_button.clicks()
            .subscribeBy { onBackPressed() }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        fun createIntent(context: Context, safeAddress: Solidity.Address) =
            Intent(context, SelectTokenActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
            }
    }
}
