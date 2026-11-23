package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.Di319Entity
import com.example.data.model.SyncMetadataEntity

@Database(
    entities = [CustomerEntity::class, Di319Entity::class, SyncMetadataEntity::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun di319Dao(): Di319Dao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `di319_rows` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cif` TEXT NOT NULL, `noRekening` TEXT NOT NULL, `namaNasabah` TEXT NOT NULL, `saldo` REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_metadata` (`cacheKey` TEXT NOT NULL, `lastSyncedAt` INTEGER NOT NULL, `rowCount` INTEGER NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`cacheKey`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "customer_database"
                )
                    .addMigrations(MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
