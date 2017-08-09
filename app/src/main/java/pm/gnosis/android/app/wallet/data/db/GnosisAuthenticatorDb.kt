package pm.gnosis.android.app.wallet.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = arrayOf(MultisigWallet::class), version = 1)
abstract class GnosisAuthenticatorDb : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-authenticator-db"
    }

    abstract fun multisigWalletDao(): MultisigWalletDao
}
