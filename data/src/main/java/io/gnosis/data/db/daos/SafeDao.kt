package io.gnosis.data.db.daos

import androidx.room.*
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeMetaData
import pm.gnosis.model.Solidity

@Dao
interface SafeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(safe: Safe)

    @Delete
    suspend fun delete(safe: Safe)

    @Query("SELECT COUNT(*) FROM ${Safe.TABLE_NAME}")
    suspend fun safeCount(): Int

    @Query("SELECT * FROM ${Safe.TABLE_NAME}")
    suspend fun loadAll(): List<Safe>

    @Query("SELECT * FROM ${Safe.TABLE_NAME} WHERE ${Safe.COL_ADDRESS} = :address")
    suspend fun loadByAddress(address: Solidity.Address): Safe?

    @Query("SELECT * FROM ${SafeMetaData.TABLE_NAME}")
    suspend fun getMetas(): List<SafeMetaData>

    @Query("SELECT * FROM ${SafeMetaData.TABLE_NAME} WHERE ${SafeMetaData.COL_ADDRESS} = :address")
    suspend fun getMeta(address: Solidity.Address): SafeMetaData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMeta(safeMeta: SafeMetaData)
}
