package pm.gnosis.android.app.accounts.repositories

import io.reactivex.Observable
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction


interface AccountsRepository {
    fun loadActiveAccount(): Observable<Account>

    fun signTransaction(transaction: Transaction): Observable<String>

    companion object {
        const val CHAIN_ID_ANY = 0
    }
}