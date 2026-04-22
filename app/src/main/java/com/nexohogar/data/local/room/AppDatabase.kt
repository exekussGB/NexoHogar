package com.nexohogar.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nexohogar.data.local.room.dao.AccountDao
import com.nexohogar.data.local.room.dao.TransactionDao
import com.nexohogar.data.local.room.entity.AccountEntity
import com.nexohogar.data.local.room.entity.TransactionEntity

@Database(entities = [TransactionEntity::class, AccountEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexohogar_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
