package pm.gnosis.heimdall.ui.safe.create

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class ConfirmSafeRecoveryPhraseContract : ViewModel() {
    abstract fun setup(encryptedMnemonic: String, chromeExtensionAddress: Solidity.Address): Single<List<String>>
    abstract fun isCorrectSequence(words: List<String>): Single<Result<Boolean>>
    abstract fun createSafe(): Single<Result<BigInteger>>
}
