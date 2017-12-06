package pm.gnosis.heimdall.ui.settings.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_network_settings.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NetworkSettingsActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: NetworkSettingsContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_network_settings)

        registerToolbar(layout_network_settings_toolbar)
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.loadIpfsUrl().subscribe(::setupIpfsInput, ::handleSetupError)
        disposables += viewModel.loadRpcUrl().subscribe(::setupRpcInput, ::handleSetupError)
        disposables += viewModel.loadSafeFactoryAddress().subscribe(::setupSafeFactoryInput, ::handleSetupError)
    }

    private fun setupSafeFactoryInput(address: String) {
        layout_settings_safe_factory_input.setText(address)
        disposables += layout_settings_safe_factory_input.textChanges()
                .skipInitialValue()
                .debounce(TIMEOUT, TIMEOUT_UNIT)
                .flatMapSingle {
                    viewModel.updateSafeFactoryAddress(it.toString())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(this::handleSuccess, this::handleError)
    }

    private fun setupRpcInput(url: String) {
        layout_network_settings_rpc_input.setText(url)
        disposables += layout_network_settings_rpc_input.textChanges()
                .skipInitialValue()
                .debounce(TIMEOUT, TIMEOUT_UNIT)
                .flatMapSingle {
                    viewModel.updateRpcUrl(it.toString())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(::handleSuccess, ::handleError)
    }

    private fun setupIpfsInput(url: String) {
        layout_network_settings_ipfs_input.setText(url)
        disposables += layout_network_settings_ipfs_input.textChanges()
                .skipInitialValue()
                .debounce(TIMEOUT, TIMEOUT_UNIT)
                .flatMapSingle {
                    viewModel.updateIpfsUrl(it.toString())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(::handleSuccess, ::handleError)
    }

    private fun handleSetupError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_settings_coordinator, R.string.error_try_again)
    }

    private fun handleSuccess(url: String) {
        snackbar(layout_settings_coordinator, R.string.success_update_settings, Snackbar.LENGTH_SHORT)
    }

    private fun handleError(throwable: Throwable) {
        errorSnackbar(layout_settings_coordinator, throwable)
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    companion object {
        private const val TIMEOUT = 750L
        private val TIMEOUT_UNIT = TimeUnit.MILLISECONDS

        fun createIntent(context: Context) = Intent(context, NetworkSettingsActivity::class.java)
    }
}
