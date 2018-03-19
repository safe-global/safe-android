package pm.gnosis.heimdall.data.repositories

import com.google.android.gms.auth.api.credentials.Credential
import io.reactivex.Completable
import io.reactivex.Single

interface GoogleSmartLockRepository {
    fun retrieveCredentials(): Single<Credential>
    fun storeCredentials(mnemonicSeed: ByteArray): Completable

    companion object {
        const val GNOSIS_SAFE_CREDENTIAL_ID = "Gnosis Safe Account"
    }
}
