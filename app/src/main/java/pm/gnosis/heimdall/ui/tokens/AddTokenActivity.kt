package pm.gnosis.heimdall.ui.tokens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_add_token.*
import kotlinx.android.synthetic.main.layout_token_info.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.hideSoftKeyboard
import pm.gnosis.heimdall.common.util.subscribeForResult
import pm.gnosis.heimdall.common.util.toast
import pm.gnosis.heimdall.data.exceptions.InvalidAddressException
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class AddTokenActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: AddTokenContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_add_token)
        inject()
        registerToolbar(layout_add_token_toolbar)
        layout_add_token_toolbar.title = getString(R.string.add_token)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_add_token_address.textChanges()
                .map { it.toString() }
                .subscribeBy(onNext = {
                    layout_add_token_load_info_button.isEnabled = true
                    layout_add_token_add_token_button.isEnabled = false
                    layout_add_token_address_input_layout.error = null
                }, onError = Timber::e)

        disposables += layout_add_token_load_info_button.clicks()
                .flatMap {
                    viewModel.loadTokenInfo(layout_add_token_address.text.toString())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe { onTokenInfoLoading(true) }
                            .doOnTerminate { onTokenInfoLoading(false) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onTokenInfo, onError = this::onTokenInfoError)


        disposables += layout_add_token_add_token_button.clicks()
                .flatMapSingle { viewModel.addToken() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = { onTokenAdded() }, onError = this::onTokenAddError)

        intent?.getStringExtra(ADDRESS_EXTRA)?.let { address ->
            if (address.isValidEthereumAddress()) {
                layout_add_token_address.setText(address)
                layout_add_token_address.isEnabled = false
                layout_add_token_address.inputType = InputType.TYPE_NULL
                layout_add_token_load_info_button.callOnClick()
            }
        }
    }

    private fun onTokenInfo(erC20Token: ERC20Token) {
        hideSoftKeyboard()
        layout_token_info_symbol.text = erC20Token.symbol ?: getString(R.string.no_token_symbol)
        layout_token_info_decimals.text = erC20Token.decimals.toString()
        layout_token_info_name.text = erC20Token.name ?: getString(R.string.no_token_name)
        layout_add_token_add_token_button.isEnabled = true
        layout_add_token_info.visibility = View.VISIBLE
    }

    private fun onTokenInfoError(throwable: Throwable) {
        Timber.e(throwable)
        if (throwable is InvalidAddressException) {
            layout_add_token_address_input_layout.error = getString(R.string.invalid_ethereum_address)
        } else {
            errorSnackbar(layout_add_token_coordinator, throwable)
        }
        layout_add_token_add_token_button.isEnabled = false
        layout_add_token_info.visibility = View.GONE
    }

    private fun onTokenInfoLoading(isLoading: Boolean) {
        layout_add_token_load_info_button.isEnabled = !isLoading
        layout_add_token_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun onTokenAdded() {
        toast(getString(R.string.token_added_message))
        finish()
    }

    private fun onTokenAddError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_add_token_coordinator, throwable)
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

        fun createIntent(context: Context, address: String? = null): Intent {
            val intent = Intent(context, AddTokenActivity::class.java)
            address?.let { intent.putExtra(ADDRESS_EXTRA, it) }
            return intent
        }
    }
}
