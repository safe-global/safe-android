package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import android.arch.lifecycle.ViewModel
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger


abstract class AddRecoveryExtensionContract : ViewModel() {
    abstract fun loadRecoveryOwners(transaction: Transaction?): Single<Pair<Solidity.Address?, Solidity.Address?>>
    abstract fun inputTransformer(safeAddress: Solidity.Address?): ObservableTransformer<Pair<CharSequence, CharSequence>, Result<SafeTransaction>>
}
