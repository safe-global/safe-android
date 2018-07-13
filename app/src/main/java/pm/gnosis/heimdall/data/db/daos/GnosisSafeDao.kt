package pm.gnosis.heimdall.data.db.daos

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.db.models.RecoveringGnosisSafeDb
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.model.Solidity
import java.math.BigInteger

@Dao
interface GnosisSafeDao {

    // Deployed Safes
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

    @Update
    fun updateSafe(safe: GnosisSafeDb)

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
    fun queryPendingSafe(address: Solidity.Address): PendingGnosisSafeDb

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
    fun pendingSafeToDeployedSafe(safeAddress: Solidity.Address) {
        val safe = queryPendingSafe(safeAddress)
        removePendingSafe(safe.address)
        insertSafe(GnosisSafeDb(safe.address, safe.name))
    }

    @Transaction
    fun recoveringSafeToDeployedSafe(safeAddress: Solidity.Address) {
        val safe = queryRecoveringSafe(safeAddress)
        removeRecoveringSafe(safe.address)
        insertSafe(GnosisSafeDb(safe.address, safe.name))
    }
}
