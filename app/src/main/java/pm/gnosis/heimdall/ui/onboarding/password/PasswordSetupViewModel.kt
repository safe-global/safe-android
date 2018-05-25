package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupActivity
import pm.gnosis.heimdall.ui.safe.overview.SafesOverviewActivity
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class PasswordSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager,
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39
) : PasswordSetupContract() {
    override fun validatePassword(password: String): Single<Result<List<Pair<PasswordValidationCondition, Boolean>>>> =
        Single.fromCallable {
            listOf(
                PasswordValidationCondition.NON_IDENTICAL_CHARACTERS to (!password.hasConsecutiveChars(CONSECUTIVE_CHARS) && password.isNotEmpty()),
                PasswordValidationCondition.MINIMUM_CHARACTERS to (password.length >= MIN_CHARS),
                PasswordValidationCondition.ONE_NUMBER_ONE_LETTER to (password.matches(CONTAINS_DIGIT_REGEX) && password.matches(CONTAINS_LETTER_REGEX))
            )
        }
            .mapToResult()
            .subscribeOn(Schedulers.computation())

    override fun passwordToHash(password: String): Single<Result<ByteArray>> =
        Single.fromCallable { Sha3Utils.keccak(password.toByteArray()) }.subscribeOn(Schedulers.computation()).mapToResult()

    override fun isSamePassword(passwordHash: ByteArray, repeat: String): Single<Result<Boolean>> =
        Single.fromCallable { isEqualPassword(passwordHash, repeat) }
            .subscribeOn(Schedulers.computation())
            .mapToResult()

    override fun createAccount(passwordHash: ByteArray, repeat: String) =
        Single.fromCallable {
            if (!isEqualPassword(passwordHash, repeat)) throw PasswordInvalidException(PasswordsNotEqual())
            repeat.toByteArray()
        }.flatMap {
            encryptionManager.setupPassword(it)
                .map { if (it) Unit else throw Exception() }
                .onErrorResumeNext { _: Throwable -> Single.error(SimpleLocalizedException(context.getString(R.string.password_error_saving))) }
        }.flatMapCompletable {
            createAccount()
        }.andThen(Single.fromCallable {
            if (encryptionManager.canSetupFingerprint()) FingerprintSetupActivity.createIntent(context)
            else SafesOverviewActivity.createIntent(context)
        }).mapToResult()

    private fun isEqualPassword(passwordHash: ByteArray, repeat: String) = Sha3Utils.keccak(repeat.toByteArray()).contentEquals(passwordHash)

    private fun createAccount() =
        Single.fromCallable { bip39.mnemonicToSeed(bip39.generateMnemonic(languageId = R.id.english)) }
            .flatMapCompletable { accountsRepository.saveAccountFromMnemonicSeed(it) }

    private fun String.hasConsecutiveChars(consecutiveChars: Int): Boolean {
        var currentStart = 0
        this.forEachIndexed { index, c ->
            if (c != this[currentStart]) currentStart = index
            if ((index - currentStart + 1) >= consecutiveChars) return true
        }
        return false
    }

    companion object {
        private const val MIN_CHARS = 8
        private const val CONSECUTIVE_CHARS = 3
        private val CONTAINS_DIGIT_REGEX = ".*\\d+.*".toRegex()
        private val CONTAINS_LETTER_REGEX = ".*[[:alpha:]].*".toRegex()
    }
}
