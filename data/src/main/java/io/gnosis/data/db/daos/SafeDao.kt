package io.gnosis.data.db.daos

import androidx.room.*
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeWithChainData
import pm.gnosis.model.Solidity
import java.math.BigInteger

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

    @Query("SELECT * FROM ${Safe.TABLE_NAME} WHERE ${Safe.COL_ADDRESS} = :address AND ${Safe.COL_CHAIN_ID} = :chainId")
    suspend fun loadByAddressAndChainId(address: Solidity.Address, chainId: BigInteger): Safe?

    @Transaction
    @Query("SELECT * FROM ${Safe.TABLE_NAME}")
    suspend fun loadAllWithChainData(): List<SafeWithChainData>

    @Transaction
    @Query("SELECT * FROM ${Safe.TABLE_NAME} WHERE ${Safe.COL_ADDRESS} = :address")
    suspend fun loadByAddressWithChainData(address: Solidity.Address): SafeWithChainData?

    @Transaction
    @Query("SELECT * FROM ${Safe.TABLE_NAME} WHERE ${Safe.COL_ADDRESS} = :address AND ${Safe.COL_CHAIN_ID} = :chainId")
    suspend fun loadByAddressWithChainData(address: Solidity.Address, chainId: BigInteger): SafeWithChainData?

    @Transaction
    @Query("SELECT * FROM ${Safe.TABLE_NAME} WHERE ${Safe.COL_CHAIN_ID} = :chainId")
    suspend fun loadAllByChain(chainId: BigInteger): List<SafeWithChainData>
}
