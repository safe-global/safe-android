package pm.gnosis.heimdall.helpers

import android.content.Context
import io.reactivex.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Transaction
import java.math.BigInteger
import javax.inject.Inject

class SimpleSignatureStore @Inject constructor(
        @ApplicationContext val context: Context
) : SignatureStore, ObservableOnSubscribe<Map<BigInteger, Signature>>, SingleOnSubscribe<Map<BigInteger, Signature>> {

    private val signatureLock = Any()
    private val signatures = HashMap<BigInteger, Signature>()

    private var emitter: ObservableEmitter<Map<BigInteger, Signature>>? = null
    private var safeAddress: BigInteger? = null
    private var info: TransactionRepository.ExecuteInformation? = null
        private set(value) {
            // Transaction changed, clear signatures
            if (field?.transactionHash != value?.transactionHash) {
                signatures.clear()
            }
            field = value
        }

    override fun subscribe(e: ObservableEmitter<Map<BigInteger, Signature>>) {
        emitter = e
        // We only emit a copy of the signatures
        e.onNext(HashMap(signatures))
    }

    override fun subscribe(e: SingleEmitter<Map<BigInteger, Signature>>) {
        // We only emit a copy of the signatures
        e.onSuccess(HashMap(signatures))
    }

    override fun flatMapInfo(safeAddress: BigInteger, info: TransactionRepository.ExecuteInformation): Observable<Map<BigInteger, Signature>> {
        synchronized(signatureLock) {
            this.safeAddress = safeAddress
            this.info = info
            // Check if any signatures are from owners that are not present anymore
            val validSignatures = signatures.filter { info.owners.contains(it.key) }
            signatures.clear()
            signatures.putAll(validSignatures)
        }
        // We only emit a copy of the signatures
        emitter?.onNext(HashMap(signatures))
        return Observable.create(this)
    }

    override fun loadSignatures(): Single<Map<BigInteger, Signature>> = Single.create(this)

    override fun loadSingingInfo(): Single<Pair<BigInteger, Transaction>> =
            info?.let { Single.just(safeAddress!! to info!!.transaction) } ?: Single.error(IllegalStateException())

    override fun addSignature(signature: Pair<BigInteger, Signature>) {
        synchronized(signatureLock) {
            SimpleLocalizedException.assert(info?.owners?.contains(signature.first) == true, context, R.string.error_signature_not_owner)
            SimpleLocalizedException.assert(!signatures.containsKey(signature.first), context, R.string.error_signature_already_exists)
            signatures.put(signature.first, signature.second)
        }
        // We only emit a copy of the signatures
        emitter?.onNext(HashMap(signatures))
    }
}

interface SignatureStore {
    fun flatMapInfo(safeAddress: BigInteger, info: TransactionRepository.ExecuteInformation): Observable<Map<BigInteger, Signature>>
    fun loadSingingInfo(): Single<Pair<BigInteger, Transaction>>
    fun loadSignatures(): Single<Map<BigInteger, Signature>>
    fun addSignature(signature: Pair<BigInteger, Signature>)
}