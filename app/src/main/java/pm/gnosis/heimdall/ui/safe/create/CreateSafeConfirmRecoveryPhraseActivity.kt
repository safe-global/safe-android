package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber

class CreateSafeConfirmRecoveryPhraseActivity : ConfirmRecoveryPhraseActivity<CreateSafeConfirmRecoveryPhraseContract>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(
            intent.getStringExtra(EXTRA_BROWSER_EXTENSION_ADDRESS)?.run { asEthereumAddress()!! },
            intent.getParcelableExtra(EXTRA_SAFE_OWNER)
        )
    }

    override fun isRecoveryPhraseConfirmed() {
        disposables += viewModel.loadOwnerData()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { bottomBarEnabled(false) }
            .subscribeBy(
                onSuccess = { (deviceOwner, additionalOwners) -> onSafeOwners(deviceOwner, additionalOwners) },
                onError = ::onSafeCreationError
            )
    }

    private fun onSafeOwners(deviceOwner: AccountsRepository.SafeOwner?, additionalOwners: List<Solidity.Address>) {
        startActivity(CreateSafePaymentTokenActivity.createIntent(this, deviceOwner, additionalOwners))
    }

    private fun onSafeCreationError(throwable: Throwable) {
        Timber.e(throwable)
        bottomBarEnabled(true)
        toast(R.string.unknown_error)
    }

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        private const val EXTRA_BROWSER_EXTENSION_ADDRESS = "extra.string.browser_extension_address"
        private const val EXTRA_SAFE_OWNER = "extra.parcelable.safe_owner"

        fun createIntent(
            context: Context,
            recoveryPhrase: String,
            browserExtensionAddress: Solidity.Address?,
            safeOwner: AccountsRepository.SafeOwner?
        ) =
            Intent(context, CreateSafeConfirmRecoveryPhraseActivity::class.java).apply {
                putExtra(EXTRA_RECOVERY_PHRASE, recoveryPhrase)
                putExtra(EXTRA_BROWSER_EXTENSION_ADDRESS, browserExtensionAddress?.asEthereumAddressString())
                putExtra(EXTRA_SAFE_OWNER, safeOwner)
            }
    }
}
