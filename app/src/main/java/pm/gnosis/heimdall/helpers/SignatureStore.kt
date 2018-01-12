package pm.gnosis.heimdall.helpers

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Transaction
import java.math.BigInteger
import javax.inject.Inject

class SimpleSignatureStore @Inject constructor(
        @ApplicationContext private val context: Context
) : ValueStore<Map<BigInteger, Signature>>(), SignatureStore {

    private val signatures = HashMap<BigInteger, Signature>()

    private var safeAddress: BigInteger? = null
    private var info: TransactionRepository.ExecuteInformation? = null
        private set(value) {
            // Transaction changed, clear signatures
            if (field?.transactionHash != value?.transactionHash) {
                signatures.clear()
            }
            field = value
        }

    override fun dataSet(): Map<BigInteger, Signature> = HashMap(signatures)

    override fun flatMapInfo(safeAddress: BigInteger, info: TransactionRepository.ExecuteInformation): Observable<Map<BigInteger, Signature>> {
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

    override fun loadSingingInfo(): Single<Pair<BigInteger, Transaction>> =
            info?.let { Single.just(safeAddress!! to info!!.transaction) } ?: Single.error(IllegalStateException())

    override fun add(signature: Pair<BigInteger, Signature>) {
        transaction {
            SimpleLocalizedException.assert(info?.owners?.contains(signature.first) == true, context, R.string.error_signature_not_owner)
            SimpleLocalizedException.assert(info?.sender != signature.first, context, R.string.error_signature_already_exists)
            SimpleLocalizedException.assert(!signatures.containsKey(signature.first), context, R.string.error_signature_already_exists)
            signatures.put(signature.first, signature.second)
        }
        // We only emit a copy of the signatures
       publish()
    }
}

interface SignatureStore {
    fun flatMapInfo(safeAddress: BigInteger, info: TransactionRepository.ExecuteInformation): Observable<Map<BigInteger, Signature>>
    fun loadSingingInfo(): Single<Pair<BigInteger, Transaction>>
    fun load(): Single<Map<BigInteger, Signature>>
    fun add(signature: Pair<BigInteger, Signature>)
}