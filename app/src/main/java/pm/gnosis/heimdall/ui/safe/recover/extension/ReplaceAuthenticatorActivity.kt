package pm.gnosis.heimdall.ui.safe.recover.extension

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import kotlinx.android.synthetic.main.layout_select_authenticator.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.ui.authenticator.SelectAuthenticatorActivity
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.handleViewAction
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

@ExperimentalCoroutinesApi
class ReplaceAuthenticatorActivity : SelectAuthenticatorActivity() {

    override fun onAuthenticatorSetupInfo(info: AuthenticatorSetupInfo) {
        val safeAddress = getSelectAuthenticatorExtras()!!
        startActivity(ReplaceAuthenticatorRecoveryPhraseActivity.createIntent(this, safeAddress, info))
    }

    companion object {
        fun createIntent(context: Context, safe: Solidity.Address) =
            Intent(context, ReplaceAuthenticatorActivity::class.java).addSelectAuthenticatorExtras(safe)
    }
}