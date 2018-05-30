package pm.gnosis.heimdall.data.db.daos

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.model.Solidity
import java.math.BigInteger

@Dao
interface GnosisSafeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSafe(safe: GnosisSafeDb)

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME}")
    fun observeSafes(): Flowable<List<GnosisSafeDb>>

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun observeSafe(address: Solidity.Address): Flowable<GnosisSafeDb>

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun loadSafe(address: Solidity.Address): Single<GnosisSafeDb>

    @Query("DELETE FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun removeSafe(address: Solidity.Address)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPendingSafe(safe: PendingGnosisSafeDb)

    @Query("SELECT * FROM ${PendingGnosisSafeDb.TABLE_NAME}")
    fun observePendingSafes(): Flowable<List<PendingGnosisSafeDb>>

    @Query("SELECT * FROM ${PendingGnosisSafeDb.TABLE_NAME} WHERE ${PendingGnosisSafeDb.COL_TX_HASH} = :hash")
    fun observePendingSafe(hash: BigInteger): Flowable<PendingGnosisSafeDb>

    @Query("SELECT * FROM ${PendingGnosisSafeDb.TABLE_NAME} WHERE ${PendingGnosisSafeDb.COL_TX_HASH} = :hash")
    fun loadPendingSafe(hash: BigInteger): Single<PendingGnosisSafeDb>

    @Query("DELETE FROM ${PendingGnosisSafeDb.TABLE_NAME} WHERE ${PendingGnosisSafeDb.COL_TX_HASH} = :hash")
    fun removePendingSafe(hash: BigInteger)

    @Update
    fun updateSafe(safe: GnosisSafeDb)

    @Update
    fun updatePendingSafe(pendingSafe: PendingGnosisSafeDb)

    @Transaction
    fun pendingSafeToDeployedSafe(pendingSafe: PendingSafe) {
        removePendingSafe(pendingSafe.hash)
        insertSafe(GnosisSafeDb(pendingSafe.address, pendingSafe.name))
    }
}
