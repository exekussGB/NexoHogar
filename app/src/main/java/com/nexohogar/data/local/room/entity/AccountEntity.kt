package com.nexohogar.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val type: String,
    val balance: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
