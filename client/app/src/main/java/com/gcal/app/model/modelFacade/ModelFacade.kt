package com.gcal.app.model.modelFacade

import com.gcal.app.model.modelFacade.general.Event
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelFacade.general.User
import com.gcal.app.model.modelFacade.general.Achievement
import com.gcal.app.model.modelFacade.general.ReportData
import com.gcal.app.model.modelFacade.general.SharedEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface ModelFacade {

    /**
     * Explanation why Flow<User> is used in the ModelFacade interface instead of StateFlow,
     * even though the ViewModel internally works with StateFlow.
     *
     * Why Flow<User> is better in the Model layer:
     * - Room compatibility:
     *   The real Model implementation will use Room as the local database.
     *   Room DAOs naturally return Flow (cold streams).
     *   Requiring StateFlow in the interface would force the implementation
     *   to convert a database Flow into a StateFlow inside the Model layer,
     *   which would require a CoroutineScope and introduce lifecycle concerns.
     *
     * - Separation of concerns:
     *   The Model layer should only expose a stream of data (Flow) and remain passive.
     *   The ViewModel acts as the state holder by converting the Flow into a StateFlow,
     *   ensuring the UI always has a current value (e.g., across configuration changes).
     */
    fun personalAccount(): Flow<User>
    fun friends(): Flow<List<User>>
    fun getEventsIn(start: LocalDateTime, end: LocalDateTime): Flow<List<Event>>
    fun globalLeaderboard(): Flow<List<User>>
    suspend fun login(): Result<Unit>
    suspend fun register(username: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun createEvent(event: Event): Result<Unit>
    suspend fun createGroup(group: Group): Result<Unit>
    suspend fun updateEvent(event: Event): Result<Unit>
    suspend fun completeEvent(event: Event): Result<List<Achievement>>
    suspend fun deleteEvent(event: Event): Result<Unit>
    suspend fun generateReport(
        start: LocalDateTime,
        end: LocalDateTime
    ): Result<ReportData>

    // Ergänzung zum Entwurf

    fun getAllGroups(): Flow<List<Group>>

    suspend fun updateGroup(group: Group): Result<Unit>

    suspend fun postSharedEvent(event: SharedEvent, users: List<User>): Result<Unit>

    suspend fun addFriend(username: String): Result<Unit>

    suspend fun removeFriend(username: String): Result<Unit>

    suspend fun getAllAchievements(): Result<List<Achievement>>

    suspend fun syncExperiencePoints(): Result<Unit>

    suspend fun getEvents()

}