package com.gcal.app.model.modelData.repo

import android.util.Log
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.localData.entity.FriendEntity
import com.gcal.app.model.localData.entity.LeaderboardEntity
import com.gcal.app.model.asDatabaseEntity
import com.gcal.app.model.asDomainModel
import com.gcal.app.model.modelData.data.system.LeaderboardEntry
import com.gcal.app.model.modelData.data.system.PersonalUser
import com.gcal.app.model.modelData.data.system.toDTO
import com.gcal.app.model.modelFacade.RequestAPI
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.model.modelFacade.general.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class UserRepo(
    val api: RequestAPI,
    val database: LocalData,
    val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getCurrentUser(): Flow<PersonalUser> {
        refreshCurrentUser()
        return database.userDao().observeProfile()
            .map {
                it.asDomainModel()
            }.flowOn(ioDispatcher)
    }

    fun getFriends(): Flow<List<User>> {
        refreshFriends()
        return database.userDao().observeFriends()
            .map { entityList: List<FriendEntity> ->
                entityList.map { entity: FriendEntity ->
                    entity.asDomainModel()
                }
            }.flowOn(ioDispatcher)
    }

    fun getLeaderboard(): Flow<List<LeaderboardEntry>> {
        refreshLeaderboard()
        return database.userDao().observeLeaderboard()
            .map { entityList: List<LeaderboardEntity> ->
                entityList.map { entity: LeaderboardEntity -> entity.asDomainModel() }
            }
            .flowOn(ioDispatcher)
    }

    suspend fun addFriend(username: String): Response<Unit> {
        return try {
            val response = api.addUserFriend(username)
            if (response is Response.Success) {
                database.userDao().upsertFriends(listOf(response.data.asDatabaseEntity()))
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(Exception("Remote server failed to add your friend"))
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    suspend fun removeFriend(username: String): Response<Unit> {
        return try {
            val response = api.removeUserFriend(username)
            if (response is Response.Success) {
                database.userDao().removeFriend(username)
                Response.Success(Unit, response.statusCode)
            } else {
                Response.Error(Exception("Remote server failed to remove your friend"))
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    suspend fun updateCurrentUser(user: PersonalUser): Response<Unit> {
        return try {
            Log.i(
                "UserRepo.updateCurrentUser()",
                "user: " + Json.encodeToString(user)
            )
            val response = api.updateActiveUser(user)
            if (response is Response.Success) {
                database.userDao().updateProfile(user.asDatabaseEntity())
                Log.i(
                    "UserRepo.updateCurrentUser()",
                    "Local database updated: " + Json.encodeToString(user.toDTO())
                )
                Response.Success(Unit, response.statusCode)
            } else {
                Log.e("UserRepo.updateCurrentUser()", "Server update failed")
                Response.Error(Exception("Server update for User failed"))
            }
        } catch (e: Exception) {
            Log.e("UserRepo.updateCurrentUser()", "Exception: " + e.message)
            Response.Error(e)
        }
    }

    //  Async Update of database with API

    private fun refreshCurrentUser() {
        coroutineScope.launch(ioDispatcher) {
            try {
                val response = api.getActiveUser()
                if (response is Response.Success) {
                    database.userDao().updateProfile(response.data.asDatabaseEntity())
                } else {
                    Log.e("UserRepo.refreshCurrentUser()", "Error connecting to Server")
                }
            } catch (e: Exception) {
                Log.e("UserRepo.refreshCurrentUser()", "Error mapping User: ${e.message}")
            }
        }
    }

    private fun refreshFriends() {
        coroutineScope.launch(ioDispatcher) {
            try {
                val response = api.getUserFriends()
                if (response is Response.Success) {
                    database.userDao().upsertFriends(response.data.map {
                        it.asDatabaseEntity()
                    })
                } else {
                    Log.e("UserRepo.refreshFriends()", "Error connecting to Server")
                }
            } catch (e: Exception) {
                Log.e("UserRepo.refreshFriends()", "Error mapping Friends: ${e.message}")
            }
        }
    }

    private fun refreshLeaderboard() {
        coroutineScope.launch(ioDispatcher) {
            try {
                val response = api.getLeaderboard()
                if (response is Response.Success) {
                    database.userDao()
                        .refreshLeaderboard(response.data.map { it.asDatabaseEntity() })
                } else {
                    Log.e("UserRepo.refreshLeaderboard()", "Error connecting to Server")
                }
            } catch (e: Exception) {
                Log.e("UserRepo.refreshLeaderboard()", "Error mapping Leaderboard: ${e.message}")
            }
        }
    }
}