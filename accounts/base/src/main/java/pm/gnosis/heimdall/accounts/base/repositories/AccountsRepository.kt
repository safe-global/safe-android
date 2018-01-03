package pm.gnosis.heimdall.accounts.base.repositories

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.models.Transaction
import java.math.BigInteger

interface AccountsRepository {
    fun loadActiveAccount(): Single<Account>

    fun signTransaction(transaction: Transaction): Single<String>

    fun sign(data: ByteArray): Single<Signature>

    fun recover(data: ByteArray, signature: Signature): Single<BigInteger>

    fun saveAccount(privateKey: ByteArray): Completable

    fun saveAccountFromMnemonic(mnemonic: String, accountIndex: Long = 0): Completable

    fun saveMnemonic(mnemonic: String): Completable

    fun generateMnemonic(): Single<String>

    fun validateMnemonic(mnemonic: String): Single<String>
}
