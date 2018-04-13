package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import android.arch.lifecycle.ViewModel
import com.gojuno.koptional.Optional
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger


abstract class AddRecoveryExtensionContract : ViewModel() {
    abstract fun loadCreateRecoveryInfo(): Single<Pair<Solidity.Address, String>>
    abstract fun loadRecoveryOwner(transaction: Transaction?): Single<Pair<Solidity.Address, BigInteger>>
    abstract fun inputTransformer(safeAddress: Solidity.Address?): ObservableTransformer<Solidity.Address, Result<SafeTransaction>>
}
