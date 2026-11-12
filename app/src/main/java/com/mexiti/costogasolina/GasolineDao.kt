package com.mexiti.costogasolina

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GasolineDao {

    @Insert
    suspend fun insert(transaction: GasolineTransaction)

    @Query("SELECT * FROM gasoline_transactions ORDER BY id DESC")
    fun getAll(): Flow<List<GasolineTransaction>>

    @Update
    suspend fun update(transaction: GasolineTransaction)

    @Delete
    suspend fun delete(transaction: GasolineTransaction)
}