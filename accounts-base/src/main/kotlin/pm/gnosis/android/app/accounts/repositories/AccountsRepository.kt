package pm.gnosis.android.app.accounts.repositories

import io.reactivex.Single
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction

interface AccountsRepository {
    fun loadActiveAccount(): Single<Account>

    fun signTransaction(transaction: Transaction): Single<String>

    companion object {
        const val CHAIN_ID_ANY = 0
    }
}
