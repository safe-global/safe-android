package io.gnosis.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.gnosis.data.models.Erc20Token
import pm.gnosis.model.Solidity

@Dao
interface Erc20TokenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(erC20Token: Erc20Token)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(erC20Tokens: List<Erc20Token>)

    @Query("SELECT * FROM ${Erc20Token.TABLE_NAME}")
    suspend fun loadTokens(): List<Erc20Token>

    @Query("SELECT * FROM ${Erc20Token.TABLE_NAME} WHERE ${Erc20Token.COL_ADDRESS} = :address")
    suspend fun loadToken(address: Solidity.Address): Erc20Token?

    @Query("DELETE FROM ${Erc20Token.TABLE_NAME} WHERE ${Erc20Token.COL_ADDRESS} = :address")
    suspend fun deleteToken(address: Solidity.Address)
}
