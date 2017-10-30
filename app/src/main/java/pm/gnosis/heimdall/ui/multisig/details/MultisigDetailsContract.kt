package pm.gnosis.heimdall.ui.multisig.details

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import java.math.BigInteger

abstract class MultisigDetailsContract : ViewModel() {
    abstract fun setup(address: BigInteger, name: String?)
    abstract fun getMultisigWallet(): MultisigWallet
    abstract fun loadQrCode(contents: String): Single<Result<Bitmap>>
}
