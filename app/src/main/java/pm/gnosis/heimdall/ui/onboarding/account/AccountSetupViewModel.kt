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
import javax.inject.Inject

class AccountSetupViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39,
    private val googleSmartLockRepository: GoogleSmartLockHelper
) : AccountSetupContract() {
    private var mnemonic: String? = null

    override fun continueWithGoogle() =
        googleSmartLockRepository.retrieveCredentials()
            .map { credential ->
                if (credential.id != GoogleSmartLockHelper.GNOSIS_SAFE_CREDENTIAL_ID)
                    throw IllegalArgumentException("Credentials with invalid id. ID is ${credential.id}")
                credential.password!!
            }
            .onErrorResumeNext { t: Throwable ->
                //If we don't have credentials stored we should generate a new one and store it
                (t as? NoCredentialsStoredException)?.let { generateAndStoreMnemonic() } ?: Single.error(t)
            }
            .flatMapCompletable { setAccountFromMnemonic(it) }
            .andThen(Single.just(Unit))
            .mapToResult()

    private fun generateAndStoreMnemonic(): Single<String> =
        Single.fromCallable { bip39.generateMnemonic(languageId = R.id.english) }
            .doOnSuccess { mnemonic = it }
            .flatMap { storeMnemonic(it) }

    private fun storeMnemonic(mnemonic: String) =
        googleSmartLockRepository.storeCredentials(mnemonic)
            .andThen(Single.just(mnemonic))

    override fun setAccountFromCredential(credential: Credential): Completable =
        Single.fromCallable {
            if (credential.id != GoogleSmartLockHelper.GNOSIS_SAFE_CREDENTIAL_ID)
                throw IllegalArgumentException("Credentials with invalid id. ID is ${credential.id}")
            credential.password!!
        }
            .flatMapCompletable(::setAccountFromMnemonic)

    private fun setAccountFromMnemonic(mnemonic: String): Completable =
        accountsRepository.saveAccountFromMnemonicSeed(bip39.mnemonicToSeed(mnemonic))
            .andThen(accountsRepository.saveMnemonic(mnemonic))
            .doOnComplete { this.mnemonic = null }

    override fun continueStoreFlow(): Completable =
        mnemonic?.let {
            setAccountFromMnemonic(it)
        } ?: Completable.error(IllegalStateException())
}
