package pm.gnosis.heimdall.ui.tokens.info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_token_info.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class TokenInfoActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: TokenInfoContract

    private lateinit var tokenAddress: BigInteger
    private val removeClicksSubject = PublishSubject.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()

        val addressExtra = intent.extras.getString(ADDRESS_EXTRA, "").hexAsEthereumAddressOrNull()
        if (addressExtra != null) {
            tokenAddress = addressExtra
        } else {
            toast(R.string.invalid_ethereum_address)
            finish()
        }

        setContentView(R.layout.layout_token_info)
        registerToolbar(layout_token_info_toolbar)
        layout_token_info_toolbar.inflateMenu(R.menu.token_info_menu)
        layout_token_info_toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.token_info_menu_delete -> showDeleteDialog()
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeToken(tokenAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokenInfo, this::onTokenInfoError)

        disposables += removeClicksSubject
                .flatMap { viewModel.removeToken() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(this::onTokenDeleted, this::onTokenDeleteError)
    }

    private fun onTokenInfo(token: ERC20Token) {
        layout_token_info_toolbar.title = token.name ?: getString(R.string.token_info)
        layout_token_info_name.text = token.name ?: "-"
        layout_token_info_symbol.text = token.symbol ?: "-"
        layout_token_info_decimals.text = token.decimals.toString()
        layout_token_info_address.text = token.address.asEthereumAddressStringOrNull() ?: "-"

        val menuItem = layout_token_info_toolbar.menu.findItem(R.id.token_info_menu_delete)
        menuItem.isVisible = !token.verified
        menuItem.isEnabled = !token.verified
    }

    private fun onTokenInfoError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_token_info_coordinator, R.string.token_information_error)
    }

    private fun onTokenDeleted(token: ERC20Token) {
        toast(getString(R.string.token_removed_message, tokenDescription(token)))
        finish()
    }

    private fun onTokenDeleteError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
                .setMessage(R.string.dialog_remove_token_message)
                .setPositiveButton(R.string.remove, { _, _ -> removeClicksSubject.onNext(Unit) })
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .show()
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    private fun tokenDescription(token: ERC20Token) =
            token.name ?: token.address.asEthereumAddressStringOrNull() ?: getString(R.string.token)

    companion object {
        private const val ADDRESS_EXTRA = "extra.string.address"

        fun createIntent(context: Context, tokenAddress: BigInteger) =
                Intent(context, TokenInfoActivity::class.java)
                        .apply { putExtra(ADDRESS_EXTRA, tokenAddress.asEthereumAddressStringOrNull()) }
    }
}
