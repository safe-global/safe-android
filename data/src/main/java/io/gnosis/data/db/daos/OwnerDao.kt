package io.gnosis.data.db.daos

import androidx.room.*
import io.gnosis.data.models.Owner
import pm.gnosis.model.Solidity

@Dao
interface OwnerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(owner: Owner)

    @Delete
    suspend fun delete(owner: Owner)

    @Query("DELETE FROM ${Owner.TABLE_NAME} WHERE ${Owner.COL_ADDRESS} = :address")
    suspend fun deleteByAddress(address: Solidity.Address)

    @Query("SELECT COUNT(*) FROM ${Owner.TABLE_NAME}")
    suspend fun ownerCount(): Int

    @Query("SELECT * FROM ${Owner.TABLE_NAME}")
    suspend fun loadAll(): List<Owner>

    @Query("SELECT * FROM ${Owner.TABLE_NAME} WHERE ${Owner.COL_ADDRESS} = :address")
    suspend fun loadByAddress(address: Solidity.Address): Owner?
}
