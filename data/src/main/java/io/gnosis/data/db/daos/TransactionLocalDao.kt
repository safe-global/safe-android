package io.gnosis.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.gnosis.data.models.TransactionLocal
import pm.gnosis.model.Solidity
import java.math.BigInteger

@Dao
interface TransactionLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(tx: TransactionLocal)

    @Delete
    suspend fun delete(tx: TransactionLocal)

    @Query("DELETE FROM ${TransactionLocal.TABLE_NAME} WHERE ${TransactionLocal.COL_CHAIN_ID} = :chainId AND ${TransactionLocal.COL_SAFE_ADDRESS} = :safeAddress")
    suspend fun clearOldRecords(chainId: BigInteger, safeAddress: Solidity.Address)

    @Query("SELECT * FROM ${TransactionLocal.TABLE_NAME} WHERE ${TransactionLocal.COL_CHAIN_ID} = :chainId AND ${TransactionLocal.COL_SAFE_ADDRESS} = :safeAddress AND ${TransactionLocal.COL_SAFE_TX_HASH} = :safeTxHash")
    suspend fun loadyBySafeTxHash(chainId: BigInteger, safeAddress: Solidity.Address, safeTxHash: String): TransactionLocal?

    @Query("SELECT * FROM ${TransactionLocal.TABLE_NAME} WHERE ${TransactionLocal.COL_CHAIN_ID} = :chainId AND ${TransactionLocal.COL_SAFE_ADDRESS} = :safeAddress ORDER BY ${TransactionLocal.COL_SAFE_TX_NONCE} DESC LIMIT 1")
    suspend fun loadLatest(chainId: BigInteger, safeAddress: Solidity.Address): TransactionLocal?
}
