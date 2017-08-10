package pm.gnosis.android.app.wallet.data.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy

@Dao
interface ERC20TokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertERC20Token(erC20Token: ERC20Token)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertERC20Tokens(erC20Tokens: List<ERC20Token>)
}
