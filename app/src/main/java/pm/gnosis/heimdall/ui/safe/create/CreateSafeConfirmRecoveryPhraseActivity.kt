package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import timber.log.Timber

class CreateSafeConfirmRecoveryPhraseActivity : ConfirmRecoveryPhraseActivity<CreateSafeConfirmRecoveryPhraseContract>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(intent.getAuthenticatorInfo())
    }

    override fun isRecoveryPhraseConfirmed() {
        disposables += viewModel.loadOwnerData()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { bottomBarEnabled(false) }
            .subscribeBy(
                onSuccess = { (authenticatorInfo, additionalOwners) -> onSafeOwners(authenticatorInfo, additionalOwners) },
                onError = ::onSafeCreationError
            )
    }

    private fun onSafeOwners(authenticatorInfo: AuthenticatorSetupInfo?, additionalOwners: List<Solidity.Address>) {
        startActivity(CreateSafePaymentTokenActivity.createIntent(this, authenticatorInfo, additionalOwners))
    }

    private fun onSafeCreationError(throwable: Throwable) {
        Timber.e(throwable)
        bottomBarEnabled(true)
        toast(R.string.unknown_error)
    }

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        fun createIntent(
            context: Context,
            recoveryPhrase: String,
            authenticatorInfo: AuthenticatorSetupInfo?
        ) =
            Intent(context, CreateSafeConfirmRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_RECOVERY_PHRASE, recoveryPhrase)
                authenticatorInfo.put(this)
            }
    }
}
