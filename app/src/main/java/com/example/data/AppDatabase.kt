package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BankingItem::class,
        QuantityLog::class,
        AtmLoadingLog::class,
        DigitalForm::class,
        TodoTask::class,
        CustomerHunting::class,
        BranchUser::class,
        PasswordHistoryEntry::class,
        LetterIssued::class,
        RecycleBinItem::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankingDao(): BankingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recycle_bin` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`originalType` TEXT NOT NULL, " +
                        "`originalId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`subtitle` TEXT NOT NULL, " +
                        "`serializedData` TEXT NOT NULL, " +
                        "`deletedTimestamp` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `todo_tasks` ADD COLUMN `phoneNumber` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `todo_tasks` ADD COLUMN `mailerName` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "toufiq_smart_banking_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
