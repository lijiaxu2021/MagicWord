package com.magicword.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Word::class, Library::class, TestHistory::class, TestSession::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE libraries ADD COLUMN lastIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `test_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `totalQuestions` INTEGER NOT NULL, 
                        `correctCount` INTEGER NOT NULL, 
                        `testType` TEXT NOT NULL, 
                        `durationSeconds` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `test_session` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `currentIndex` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `isFinished` INTEGER NOT NULL,
                        `shuffledIndicesJson` TEXT NOT NULL,
                        `testType` TEXT NOT NULL,
                        `libraryId` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE test_history ADD COLUMN questionsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN nextReviewTime INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE words ADD COLUMN easinessFactor REAL NOT NULL DEFAULT 2.5")
                database.execSQL("ALTER TABLE words ADD COLUMN interval INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE words ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "word_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate default library
                        db.execSQL("INSERT INTO libraries (id, name, description, createdAt, lastIndex) VALUES (1, '默认词库', 'Default Library', ${System.currentTimeMillis()}, 0)")
                    }
                })
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
