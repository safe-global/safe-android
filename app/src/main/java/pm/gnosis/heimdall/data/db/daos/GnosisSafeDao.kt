package pm.gnosis.heimdall.data.db.daos

import androidx.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeInfoDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.db.models.RecoveringGnosisSafeDb
import pm.gnosis.model.Solidity

@Dao
interface GnosisSafeDao {

    @Query("SELECT SUM(c) FROM (SELECT COUNT(*) as c FROM ${GnosisSafeDb.TABLE_NAME} UNION ALL SELECT COUNT(*) as c FROM ${PendingGnosisSafeDb.TABLE_NAME} UNION ALL SELECT COUNT(*) as c FROM ${RecoveringGnosisSafeDb.TABLE_NAME})")
    fun loadTotalSafeCount(): Single<Long>

    // Deployed Safes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSafe(safe: GnosisSafeDb)

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME}")
    fun observeSafes(): Flowable<List<GnosisSafeDb>>

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun observeSafe(address: Solidity.Address): Flowable<GnosisSafeDb>

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun querySafe(address: Solidity.Address): GnosisSafeDb?

    @Query("SELECT * FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun loadSafe(address: Solidity.Address): Single<GnosisSafeDb>

    @Query("DELETE FROM ${GnosisSafeDb.TABLE_NAME} WHERE ${GnosisSafeDb.COL_ADDRESS} = :address")
    fun removeSafe(address: Solidity.Address)

    @Update
    fun updateSafe(safe: GnosisSafeDb)

    // Safe owner
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSafeInfo(safeInfo: GnosisSafeInfoDb)

    @Query("DELETE FROM ${GnosisSafeInfoDb.TABLE_NAME} WHERE ${GnosisSafeInfoDb.COL_SAFE_ADDRESS} = :safeAddress")
    fun removeSafeInfo(safeAddress: Solidity.Address)

    @Query("SELECT * FROM ${GnosisSafeInfoDb.TABLE_NAME} WHERE ${GnosisSafeInfoDb.COL_SAFE_ADDRESS} = :safeAddress")
    fun loadSafeInfo(safeAddress: Solidity.Address): Single<GnosisSafeInfoDb>

    @Query("SELECT * FROM ${GnosisSafeInfoDb.TABLE_NAME}")
    fun loadSafeInfos(): Single<List<GnosisSafeInfoDb>>

    // Pending Safes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPendingSafe(safe: PendingGnosisSafeDb)

    @Query("SELECT * FROM ${PendingGnosisSafeDb.TABLE_NAME}")
    fun observePendingSafes(): Flowable<List<PendingGnosisSafeDb>>

    @Query("SELECT * FROM ${PendingGnosisSafeDb.TABLE_NAME} WHERE ${PendingGnosisSafeDb.COL_ADDRESS} = :address")
    fun observePendingSafe(address: Solidity.Address): Flowable<PendingGnosisSafeDb>

    @Query("SELECT * FROM ${PendingGnosisSafeDb.TABLE_NAME} WHERE ${PendingGnosisSafeDb.COL_ADDRESS} = :address")
    fun loadPendingSafe(address: Solidity.Address): Single<PendingGnosisSafeDb>

    @Query("SELECT * FROM ${PendingGnosisSafeDb.TABLE_NAME} WHERE ${PendingGnosisSafeDb.COL_ADDRESS} = :address")
    fun queryPendingSafe(address: Solidity.Address): PendingGnosisSafeDb?

    @Query("DELETE FROM ${PendingGnosisSafeDb.TABLE_NAME} WHERE ${PendingGnosisSafeDb.COL_ADDRESS} = :address")
    fun removePendingSafe(address: Solidity.Address)

    @Update
    fun updatePendingSafe(pendingSafe: PendingGnosisSafeDb)

    // Recovering Safes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecoveringSafe(safe: RecoveringGnosisSafeDb)

    @Query("SELECT * FROM ${RecoveringGnosisSafeDb.TABLE_NAME} WHERE ${RecoveringGnosisSafeDb.COL_ADDRESS} = :address")
    fun queryRecoveringSafe(address: Solidity.Address): RecoveringGnosisSafeDb

    @Query("SELECT * FROM ${RecoveringGnosisSafeDb.TABLE_NAME}")
    fun observeRecoveringSafes(): Flowable<List<RecoveringGnosisSafeDb>>

    @Query("SELECT * FROM ${RecoveringGnosisSafeDb.TABLE_NAME} WHERE ${RecoveringGnosisSafeDb.COL_ADDRESS} = :address")
    fun observeRecoveringSafe(address: Solidity.Address): Flowable<RecoveringGnosisSafeDb>

    @Query("SELECT * FROM ${RecoveringGnosisSafeDb.TABLE_NAME} WHERE ${RecoveringGnosisSafeDb.COL_ADDRESS} = :address")
    fun loadRecoveringSafe(address: Solidity.Address): Single<RecoveringGnosisSafeDb>

    @Query("DELETE FROM ${RecoveringGnosisSafeDb.TABLE_NAME} WHERE ${RecoveringGnosisSafeDb.COL_ADDRESS} = :address")
    fun removeRecoveringSafe(address: Solidity.Address)

    @Update
    fun updateRecoveringSafe(recoveringSafe: RecoveringGnosisSafeDb)

    // Db Transactions
    @Transaction
    fun pendingSafeToDeployedSafe(safeAddress: Solidity.Address): Boolean {
        queryPendingSafe(safeAddress)?.let { safe ->
            removePendingSafe(safe.address)
            insertSafe(GnosisSafeDb(safe.address))
            return true
        }
        return false
    }

    @Transaction
    fun recoveringSafeToDeployedSafe(safeAddress: Solidity.Address) {
        val safe = queryRecoveringSafe(safeAddress)
        removeRecoveringSafe(safe.address)
        insertSafe(GnosisSafeDb(safe.address))
    }
}
