package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_pairing.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.model.Solidity
import timber.log.Timber

class CreateSafePairingActivity : PairingActivity() {
    override fun titleRes(): Int = R.string.connect

    override fun onStart() {
        super.onStart()
        disposables += layout_pairing_later.clicks()
            .subscribeBy(
                onNext = {
                    startActivity(
                        CreateSafeRecoveryPhraseIntroActivity.createIntent(this, browserExtensionAddress = null, safeOwner = null)
                    )
                },
                onError = Timber::e
            )
    }

    override fun onSuccess(signingOwner: AccountsRepository.SafeOwner, extension: Solidity.Address) {
        startActivity(CreateSafeRecoveryPhraseIntroActivity.createIntent(this, extension, signingOwner))
    }

    override fun shouldShowLaterOption() = true

    override fun signingSafe(): Solidity.Address? = null

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafePairingActivity::class.java)
    }
}
