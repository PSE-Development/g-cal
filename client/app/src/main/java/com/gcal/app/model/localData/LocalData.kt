package com.gcal.app.model.localData

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gcal.app.model.localData.entity.*

/**
 * Main database entry point for the G-Cal application.
 * Manages persistence for user profiles, social data, and all event types.
 */
@Database(
    entities = [
        ProfileEntity::class,
        FriendEntity::class,
        LeaderboardEntity::class,
        AppointmentEntity::class,
        ToDoEntity::class,
        SharedEventEntity::class,
        AchievementEntity::class,
        EventCompletionEntity::class,
        GroupEntity::class,
        EventEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class LocalData : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: LocalData? = null

        val ACHIEVEMENT_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO achievements (name, isCompleted) VALUES ('Früher Vogel', 0)")
                db.execSQL("INSERT INTO achievements (name, isCompleted) VALUES ('Nachteule', 0)")
            }
        }

        /**
         * Returns the singleton instance of [LocalData].
         */
        fun getDatabase(context: Context): LocalData {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalData::class.java,
                    "gcal_localDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(ACHIEVEMENT_CALLBACK)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
