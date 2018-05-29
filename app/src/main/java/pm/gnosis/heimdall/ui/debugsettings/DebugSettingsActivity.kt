package pm.gnosis.heimdall.ui.debugsettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_debug_settings.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.safe.create.PairingActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.svalinn.common.utils.toast
import timber.log.Timber
import javax.inject.Inject

class DebugSettingsActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: DebugSettingsContract

    override fun screenId() = ScreenId.DEBUG_SETTINGS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_debug_settings)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_debug_settings_auth.clicks()
            .doOnNext { toast("Syncing Authentication...") }
            .subscribeBy(onNext = { viewModel.forceSyncAuthentication() }, onError = Timber::e)

        disposables += layout_debug_settings_pair.clicks()
            .subscribeBy(onNext = {
                //startActivity(PairingActivity.createIntent(this))
                QRCodeScanActivity.startForResult(this)
            }, onError = Timber::e)

        disposables += layout_debug_settings_safe_creation.clicks()
            .flatMapSingle {
                viewModel.sendTestSafeCreationPush(layout_debug_settings_address.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        layout_debug_settings_pair.isEnabled = false
                        layout_debug_settings_progress_bar.visibility = View.VISIBLE
                    }
                    .doFinally {
                        layout_debug_settings_pair.isEnabled = true
                        layout_debug_settings_progress_bar.visibility = View.GONE
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onComplete = {
                toast("Notification sent")
            }, onError = { Timber.e(it); toast("Error sending notification") })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, onQrCodeResult = {
            disposables += viewModel.pair(it)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    layout_debug_settings_pair.isEnabled = false
                    layout_debug_settings_progress_bar.visibility = View.VISIBLE
                }
                .doOnTerminate {
                    layout_debug_settings_pair.isEnabled = true
                    layout_debug_settings_progress_bar.visibility = View.GONE
                }
                .subscribeBy(onComplete = {
                    toast("Devices paired successfully")
                    finish()
                }, onError = {
                    toast("Error pairing devices")
                    Timber.e(it)
                })
        })
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, DebugSettingsActivity::class.java)
    }
}
