package pm.gnosis.heimdall.helpers

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import javax.inject.Inject

class SimpleSignatureStore @Inject constructor(
    @ApplicationContext private val context: Context
) : ValueStore<Map<Solidity.Address, Signature>>(), SignatureStore {

    private val signatures = HashMap<Solidity.Address, Signature>()

    private var safeAddress: Solidity.Address? = null
    private var info: TransactionExecutionRepository.ExecuteInformation? = null
        private set(value) {
            // Transaction changed, clear signatures
            if (field?.transactionHash != value?.transactionHash) {
                signatures.clear()
            }
            field = value
        }

    override fun dataSet(): Map<Solidity.Address, Signature> = HashMap(signatures)

    override fun flatMapInfo(
        safeAddress: Solidity.Address,
        info: TransactionExecutionRepository.ExecuteInformation
    ): Observable<Map<Solidity.Address, Signature>> {
        transaction {
            this.safeAddress = safeAddress
            this.info = info
            // Check if any signatures are from owners that are not present anymore
            val validSignatures = signatures.filter { info.owners.contains(it.key) }
            signatures.clear()
            signatures.putAll(validSignatures)
        }
        // We only emit a copy of the signatures
        publish()
        return observe()
    }

    override fun loadSingingInfo(): Single<Pair<Solidity.Address, SafeTransaction>> =
        info?.let { Single.just(safeAddress!! to info!!.transaction) } ?: Single.error(IllegalStateException())

    override fun add(signature: Pair<Solidity.Address, Signature>) {
        transaction {
            SimpleLocalizedException.assert(info?.owners?.contains(signature.first) == true, context, R.string.error_signature_not_owner)
            SimpleLocalizedException.assert(info?.sender != signature.first, context, R.string.error_signature_already_exists)
            SimpleLocalizedException.assert(!signatures.containsKey(signature.first), context, R.string.error_signature_already_exists)
            SimpleLocalizedException.assert(info?.requiredConfirmation?.let { signatures.size < it } == true,
                context,
                R.string.error_signature_already_exists)
            signatures[signature.first] = signature.second
        }
        // We only emit a copy of the signatures
        publish()
    }
}

interface SignatureStore {
    fun flatMapInfo(safeAddress: Solidity.Address, info: TransactionExecutionRepository.ExecuteInformation): Observable<Map<Solidity.Address, Signature>>
    fun loadSingingInfo(): Single<Pair<Solidity.Address, SafeTransaction>>
    fun load(): Single<Map<Solidity.Address, Signature>>
    fun observe(): Observable<Map<Solidity.Address, Signature>>
    fun add(signature: Pair<Solidity.Address, Signature>)
}
