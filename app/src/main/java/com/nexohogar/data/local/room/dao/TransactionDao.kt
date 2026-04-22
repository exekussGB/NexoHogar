package com.nexohogar.data.local.room.dao

import androidx.room.*
import com.nexohogar.data.local.room.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE householdId = :householdId AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun getTransactionsByHousehold(householdId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE pendingSync = 1")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET pendingSync = 0 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
