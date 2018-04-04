package pm.gnosis.heimdall.ui.onboarding.account

import com.google.android.gms.auth.api.credentials.Credential
import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.GoogleSmartLockHelper
import pm.gnosis.heimdall.helpers.NoCredentialsStoredException
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.hexStringToByteArray
import javax.inject.Inject

class AccountSetupViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39,
    private val googleSmartLockRepository: GoogleSmartLockHelper
) : AccountSetupContract() {
    override fun continueWithGoogle() =
        googleSmartLockRepository.retrieveCredentials()
            .onErrorResumeNext { t: Throwable ->
                //If we don't have credentials stored we should generate a new one and store it
                (t as? NoCredentialsStoredException)?.let { storeCredentialsWithGoogle() } ?: Single.error(t)
            }
            .flatMapCompletable { setAccountFromCredential(it) }
            .andThen(Single.just(Unit))
            .mapToResult()

    private fun storeCredentialsWithGoogle(): Single<Credential> =
        Single.fromCallable { bip39.generateMnemonic(languageId = R.id.english) }
            .map { bip39.mnemonicToSeed(it) }
            .flatMap { storeWithGoogleSmartLock(it) }

    private fun storeWithGoogleSmartLock(mnemonicSeed: ByteArray) =
        googleSmartLockRepository.storeCredentials(mnemonicSeed)
            .andThen(googleSmartLockRepository.retrieveCredentials())

    override fun setAccountFromCredential(credential: Credential): Completable =
        Single.fromCallable {
            if (credential.id != GoogleSmartLockHelper.GNOSIS_SAFE_CREDENTIAL_ID) throw IllegalArgumentException("Credentials with invalid id. ID is ${credential.id}")
            credential.password!!.hexStringToByteArray()
        }
            .flatMapCompletable { accountsRepository.saveAccountFromMnemonicSeed(it) }
}
