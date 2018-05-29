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
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.setupEtherscanAddressUrl
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class TokenInfoActivity : BaseActivity() {

    override fun screenId() = ScreenId.TOKEN_INFO

    @Inject
    lateinit var viewModel: TokenInfoContract

    private val removeClicksSubject = PublishSubject.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()

        try {
            viewModel.setup(intent.getStringExtra(ADDRESS_EXTRA))
        } catch (e: Exception) {
            toast(R.string.invalid_ethereum_address)
            finish()
        }

        setContentView(R.layout.layout_token_info)
        setupToolbar(layout_token_info_toolbar)
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
        disposables += viewModel.observeToken()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::onTokenInfo, ::onTokenInfoError)

        disposables += removeClicksSubject
            .flatMap { viewModel.removeToken() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult({ onTokenDeleted() }, ::onTokenDeleteError)
    }

    private fun onTokenInfo(token: ERC20Token) {
        layout_token_info_toolbar.title = token.name ?: getString(R.string.token_info)
        layout_token_info_name.text = token.name ?: "-"
        layout_token_info_symbol.text = token.symbol ?: "-"
        layout_token_info_decimals.text = token.decimals.toString()

        val tokenAddressString = token.address.asEthereumAddressString()
        layout_token_info_address.setupEtherscanAddressUrl(tokenAddressString, R.string.view_contract_on)

        val menuItem = layout_token_info_toolbar.menu.findItem(R.id.token_info_menu_delete)
        menuItem.isVisible = !token.verified
        menuItem.isEnabled = !token.verified
    }

    private fun onTokenInfoError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_token_info_coordinator, R.string.token_information_error)
    }

    private fun onTokenDeleted() {
        toast(R.string.token_removed_message)
        finish()
    }

    private fun onTokenDeleteError(throwable: Throwable) {
        snackbar(layout_token_info_coordinator, R.string.tokens_delete_error)
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

    companion object {
        private const val ADDRESS_EXTRA = "extra.string.address"

        fun createIntent(context: Context, tokenAddress: Solidity.Address) =
            Intent(context, TokenInfoActivity::class.java)
                .apply { putExtra(ADDRESS_EXTRA, tokenAddress.asEthereumAddressString()) }
    }
}
