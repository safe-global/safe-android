package io.gnosis.data.db.daos

import androidx.room.*
import io.gnosis.data.models.Safe

@Dao
interface SafeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(safe: Safe)

    @Delete
    suspend fun delete(safe: Safe)

    @Query("SELECT * FROM ${Safe.TABLE_NAME}")
    suspend fun loadAll(): Array<Safe>
}
