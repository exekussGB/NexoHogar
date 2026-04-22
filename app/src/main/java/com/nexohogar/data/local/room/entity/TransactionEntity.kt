package com.nexohogar.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String, // UUID de Supabase o local temporal
    val householdId: String,
    val type: String, // expense, income, transfer
    val accountId: String,
    val categoryId: String?,
    val amountClp: Long,
    val description: String?,
    val transactionDate: String,
    val createdAt: String,
    val pendingSync: Boolean = false, // TRUE si se creó offline y no se ha subido
    val isDeleted: Boolean = false   // Soft delete para sincronización
)
