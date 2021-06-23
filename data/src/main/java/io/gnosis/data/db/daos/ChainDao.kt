package io.gnosis.data.db.daos

import androidx.room.*
import io.gnosis.data.models.Chain

@Dao
interface ChainDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chain: Chain)

    @Delete
    suspend fun delete(chain: Chain)

    @Query("SELECT COUNT(*) FROM ${Chain.TABLE_NAME}")
    suspend fun chainCount(): Int

    @Query("SELECT * FROM ${Chain.TABLE_NAME}")
    suspend fun loadAll(): List<Chain>

    @Query("SELECT * FROM ${Chain.TABLE_NAME} WHERE ${Chain.COL_NAME} = :name")
    suspend fun loadByName(name: String): Chain?

    @Query("SELECT * FROM ${Chain.TABLE_NAME} WHERE ${Chain.COL_CHAIN_ID} = :id")
    suspend fun loadByChainId(id: Int): Chain?

}
