package com.gcal.app.model.localData

import androidx.room.*
import com.gcal.app.model.localData.entity.FriendEntity
import com.gcal.app.model.localData.entity.LeaderboardEntity
import com.gcal.app.model.localData.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Optimized Data Access Object for all User related Information.
 */
@Dao
abstract class UserDao {

    // --- Profile (Me) ---
    @Query("SELECT * FROM user_profile LIMIT 1")
    abstract fun observeProfile(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertProfile(profile: ProfileEntity)

    @Update
    abstract suspend fun updateProfile(profile: ProfileEntity)

    // --- Friends ---
    @Query("SELECT * FROM friends ORDER BY username ASC")
    abstract fun observeFriends(): Flow<List<FriendEntity>>

    @Upsert
    abstract suspend fun upsertFriends(friends: List<FriendEntity>)

    @Query("DELETE FROM friends WHERE username = :name")
    abstract suspend fun removeFriend(name: String)

    // --- Leaderboard ---
    @Query("SELECT * FROM leaderboard ORDER BY rank ASC")
    abstract fun observeLeaderboard(): Flow<List<LeaderboardEntity>>

    @Transaction
    open suspend fun refreshLeaderboard(entries: List<LeaderboardEntity>) {
        clearLeaderboard()
        insertLeaderboard(entries)
    }
    // Private Methods
    @Query("DELETE FROM leaderboard")
    protected abstract suspend fun clearLeaderboard()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLeaderboard(entries: List<LeaderboardEntity>)
}