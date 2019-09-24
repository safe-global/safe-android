package pm.gnosis.heimdall.ui.safe.mnemonic


import android.content.Intent
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_input_recovery_phrase.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

abstract class InputRecoveryPhraseActivity<VM : InputRecoveryPhraseContract> : ViewModelActivity<VM>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun layout() = R.layout.layout_input_recovery_phrase

    override fun onStart() {
        super.onStart()

        layout_input_recovery_phrase_input_info.text = null
        layout_input_recovery_phrase_progress.visible(true)
        layout_input_recovery_phrase_input_group.visible(false)
        layout_input_recovery_phrase_retry.visible(false)
        layout_input_recovery_phrase_next.disabled = true

        disposables += toolbarHelper.setupShadow(layout_input_recovery_phrase_toolbar_shadow, layout_input_recovery_phrase_content_scroll)

        disposables += viewModel.process(
            InputRecoveryPhraseContract.Input(
                layout_input_recovery_phrase_input.textChanges().debounce(500, TimeUnit.MILLISECONDS),
                layout_input_recovery_phrase_retry.clicks()
                    .doOnNext {
                        layout_input_recovery_phrase_retry.visible(false)
                        layout_input_recovery_phrase_progress.visible(true)
                    },
                layout_input_recovery_phrase_next.forwardClicks
                    .doOnNext {
                        layout_input_recovery_phrase_next.disabled = true
                        layout_input_recovery_phrase_input_group.visible(false)
                        layout_input_recovery_phrase_progress.visible(true)
                    }
            ),
            intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()!!,
            intent.getAuthenticatorInfo()
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::updateView, onError = Timber::e)


        disposables += layout_input_recovery_phrase_back_arrow.clicks().subscribeBy { onBackPressed() }
    }

    private fun updateView(update: InputRecoveryPhraseContract.ViewUpdate) {
        when (update) {
            is InputRecoveryPhraseContract.ViewUpdate.SafeInfoError -> {
                layout_input_recovery_phrase_retry.visible(true)
                layout_input_recovery_phrase_input_group.visible(false)
                layout_input_recovery_phrase_progress.visible(false)
                layout_input_recovery_phrase_next.disabled = true
            }
            InputRecoveryPhraseContract.ViewUpdate.InputMnemonic -> {
                layout_input_recovery_phrase_input_info.text = null
                layout_input_recovery_phrase_input_group.visible(true)
                layout_input_recovery_phrase_retry.visible(false)
                layout_input_recovery_phrase_progress.visible(false)
                layout_input_recovery_phrase_next.disabled = true
            }
            InputRecoveryPhraseContract.ViewUpdate.InvalidMnemonic -> {
                layout_input_recovery_phrase_input_info.text = getString(R.string.mnemonic_error_invalid)
                layout_input_recovery_phrase_next.disabled = true
            }
            InputRecoveryPhraseContract.ViewUpdate.WrongMnemonic -> {
                layout_input_recovery_phrase_input_info.text = getString(R.string.incorrect_recovery_phrase)
                layout_input_recovery_phrase_next.disabled = true
            }
            InputRecoveryPhraseContract.ViewUpdate.ValidMnemonic -> {
                layout_input_recovery_phrase_input_info.text = null
                layout_input_recovery_phrase_next.disabled = false
            }
            is InputRecoveryPhraseContract.ViewUpdate.RecoverDataError -> {
                layout_input_recovery_phrase_next.disabled = false
                layout_input_recovery_phrase_input_group.visible(true)
                layout_input_recovery_phrase_progress.visible(false)
                errorSnackbar(layout_input_recovery_phrase_input_group, update.error)
            }
            is InputRecoveryPhraseContract.ViewUpdate.NoRecoveryNecessary -> {
                noRecoveryNecessary(update.safeAddress)
            }
            is InputRecoveryPhraseContract.ViewUpdate.RecoverData -> {
                onSuccess(update)
            }
        }
    }

    abstract fun noRecoveryNecessary(safe: Solidity.Address)

    abstract fun onSuccess(recoverData: InputRecoveryPhraseContract.ViewUpdate.RecoverData)

    companion object {
        const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        fun addExtras(intent: Intent, safeAddress: Solidity.Address, authenticatorInfo: AuthenticatorSetupInfo?) = intent.apply {
            putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
            authenticatorInfo.put(this)
        }
    }
}
