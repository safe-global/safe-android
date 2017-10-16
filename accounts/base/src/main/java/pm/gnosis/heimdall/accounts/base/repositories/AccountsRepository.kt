package pm.gnosis.heimdall.accounts.base.repositories

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.models.Transaction

interface AccountsRepository {
    fun loadActiveAccount(): Single<Account>

    fun signTransaction(transaction: Transaction): Single<String>

    fun saveAccount(privateKey: ByteArray): Completable

    fun saveAccountFromMnemonic(mnemonic: String, accountIndex: Long = 0): Completable

    fun saveMnemonic(mnemonic: String): Completable

    fun generateMnemonic(): Single<String>

    fun validateMnemonic(mnemonic: String): Single<String>

    companion object {
        const val CHAIN_ID_ANY = 0
    }
}
