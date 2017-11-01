package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.*
import io.reactivex.Flowable
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import java.math.BigInteger

@Dao
interface GnosisSafeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSafe(safe: GnosisSafeDb)

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME}")
    fun observeSafes(): Flowable<List<GnosisSafeDb>>

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun observeSafe(address: BigInteger): Flowable<GnosisSafeDb>

    @Query("DELETE FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun removeSafe(address: BigInteger)

    @Update
    fun updateSafe(safe: GnosisSafeDb)
}
