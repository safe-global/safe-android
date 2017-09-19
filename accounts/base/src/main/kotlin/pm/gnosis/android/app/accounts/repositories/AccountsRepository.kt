package pm.gnosis.android.app.accounts.repositories

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction

interface AccountsRepository {
    fun loadActiveAccount(): Single<Account>

    fun signTransaction(transaction: Transaction): Single<String>

    fun saveAccount(privateKey: ByteArray): Completable

    fun saveAccountFromMnemonic(mnemonic: String, accountIndex: Long = 0): Completable

    fun saveMnemonic(mnemonic: String): Completable

    companion object {
        const val CHAIN_ID_ANY = 0
    }
}
