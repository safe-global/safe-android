package pm.gnosis.heimdall.ui.walletconnect.intro

import androidx.lifecycle.ViewModel
import io.reactivex.Completable

abstract class WalletConnectIntroContract: ViewModel() {
    abstract fun markIntroDone(): Completable
}