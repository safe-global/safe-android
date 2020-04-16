package io.gnosis.data.db.daos

import androidx.room.*
import io.gnosis.data.models.Safe
import pm.gnosis.model.Solidity

@Dao
interface SafeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(safe: Safe)

    @Delete
    suspend fun delete(safe: Safe)

    @Query("SELECT * FROM ${Safe.TABLE_NAME}")
    suspend fun loadAll(): Array<Safe>

    @Query("SELECT * FROM ${Safe.TABLE_NAME} WHERE ${Safe.COL_ADDRESS} = :address")
    suspend fun loadByAddress(address: Solidity.Address): Safe?
}
