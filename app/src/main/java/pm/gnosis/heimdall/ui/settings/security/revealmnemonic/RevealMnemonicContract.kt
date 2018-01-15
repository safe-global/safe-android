package pm.gnosis.heimdall.ui.settings.security.revealmnemonic

import android.arch.lifecycle.ViewModel
import io.reactivex.Single

abstract class RevealMnemonicContract : ViewModel() {
    abstract fun loadMnemonic(): Single<String>
}
