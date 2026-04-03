package com.gcal.app.model.modelData

import android.content.Intent
import android.util.Log
import com.gcal.app.LoginActivity
import com.gcal.app.model.MyContext
import com.gcal.app.model.localData.entity.ProfileEntity
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.asCompletedDomainModel
import com.gcal.app.model.asDatabaseEntity
import com.gcal.app.model.asRegularUser
import com.gcal.app.model.asSharedEvent
import com.gcal.app.model.asUpdatedDomainModel
import com.gcal.app.model.localData.entity.AppointmentEntity
import com.gcal.app.model.localData.entity.SharedEventEntity
import com.gcal.app.model.localData.entity.ToDoEntity
import com.gcal.app.model.modelData.data.ReportUserData
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.UserGroup
import com.gcal.app.model.modelData.data.system.XP
import com.gcal.app.model.modelData.data.system.toDTO
import com.gcal.app.model.modelData.repo.AchievementHandler
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelData.repo.EventRepo
import com.gcal.app.model.modelData.repo.UserRepo
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.model.modelFacade.RequestAPI
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.model.modelFacade.general.Achievement
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelFacade.general.Event
import com.gcal.app.model.modelFacade.general.ReportData
import com.gcal.app.model.modelFacade.general.SharedEvent
import com.gcal.app.model.modelFacade.general.ToDo
import com.gcal.app.model.modelFacade.general.User
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.delete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

class ModelData(
    val api: RequestAPI,
    db: LocalData,
    val userRepo: UserRepo = UserRepo(api, db, CoroutineScope(Dispatchers.IO)),
    val eventRepo: EventRepo = EventRepo(api, db, CoroutineScope(Dispatchers.IO)),
    val achievementData: AchievementHandler = AchievementHandler(
        userRepo,
        eventRepo,
        db.achievementDao()
    )
) : ModelFacade {
    private val xpHandler: XpDistributor = XpDistributor(userRepo, eventRepo)

    companion object {
        lateinit var instance: ModelData
            private set
    }

    init {
        instance = this
    }

    override fun personalAccount(): Flow<User> =
        userRepo.getCurrentUser()

    override fun friends(): Flow<List<User>> = userRepo.getFriends()

    override fun getEventsIn(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Event>> {
        return combine(
            eventRepo.getAppointmentsIn(start, end),
            eventRepo.getToDoIn(start, end),
            eventRepo.getSharedEventsIn(start, end)
        ) { appointments, toDos, sharedEvents ->
            appointments + toDos + sharedEvents
        }.flowOn(Dispatchers.IO)
    }

    override fun globalLeaderboard(): Flow<List<User>> = userRepo.getLeaderboard()

    override suspend fun login(): Result<Unit> {
        val intent = Intent(MyContext.appContext, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        MyContext.appContext.startActivity(intent)
        val state = ClientAPI.AuthState.state.first { it != ClientAPI.AuthState.State.Idle }
        return when (state) {
            is ClientAPI.AuthState.State.LoggedIn -> {
                Result.success(Unit)
            }

            is ClientAPI.AuthState.State.Failed -> {
                Result.failure(Exception("Login fehlgeschlagen"))
            }

            else -> {
                Result.failure(Exception("Login fehlgeschlagen")) //Tritt nie ein
            }
        }
    }

    override suspend fun register(username: String): Result<Unit> {
        return login()
    }

    override suspend fun logout(): Result<Unit> {
        val data = userRepo.getCurrentUser().first()
        val profile = ProfileEntity(
            data.username(),
            data.name(), data.totalXp.value(), data.dailyXp.value()
        )
        LocalData.getDatabase(MyContext.appContext).userDao().updateProfile(profile)
        val loginFirebase = LoginViewModel(this)
        withContext(Dispatchers.IO) {
            eventRepo.database.clearAllTables()
            userRepo.database.clearAllTables()
        }
        loginFirebase.logout()
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val idToken: String =
            LoginViewModel.getFreshToken() ?: return Result.failure(Exception("Not Authorized"))
        val data = userRepo.getCurrentUser().first()
        val client = HttpClient(Android)
        client.delete("http://193.196.39.173:8080/delete") {
            url {
                parameters.append("userName", data.username())
                parameters.append("idToken", idToken)
            }
        }
        client.close()
        return logout()
    }

    override suspend fun createEvent(event: Event): Result<Unit> {

        val result = when (event) {
            is UserAppointment -> {
                asResult(eventRepo.createAppointment(event))
            }

            is UserToDo -> {
                asResult(eventRepo.createTodo(event))
            }

            else -> {
                Result.failure(Exception("Event not supported"))

            }
        }
        xpHandler.distributeXp()
        return result
    }

    override suspend fun createGroup(group: Group): Result<Unit> = if (group is UserGroup) {
        if (eventRepo.createGroup(group) is Response.Success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to create group"))
        }
    } else {
        Result.failure(Exception("group not supported"))
    }

    override suspend fun updateEvent(event: Event): Result<Unit> {
        if (event is UserAppointment) {
            if (eventRepo.updateAppointment(event) is Response.Success) {
                return Result.success(Unit)
            }
        }
        if (event is UserToDo) {
            if (eventRepo.updateToDo(event) is Response.Success) {
                return Result.success(Unit)
            }
        }
        if (event is UserSharedEvent) {
            if (eventRepo.updateSharedEvent(event) is Response.Success) {
                return Result.success(Unit)
            }
        }
        xpHandler.distributeXp()
        return Result.failure(Exception("Event not supported"))
    }

    override suspend fun deleteEvent(event: Event): Result<Unit> {
        if (event is UserAppointment) {
            if (eventRepo.deleteAppointment(event) is Response.Success) {
                return Result.success(Unit)
            }
        }
        if (event is UserToDo) {
            if (eventRepo.deleteToDo(event) is Response.Success) {
                return Result.success(Unit)
            }
        }
        if (event is UserSharedEvent) {
            if (eventRepo.deleteSharedEvent(event) is Response.Success) {
                return Result.success(Unit)
            }
        }
        xpHandler.distributeXp()
        return Result.failure(Exception("Event not supported"))
    }

    override suspend fun generateReport(
        start: LocalDateTime,
        end: LocalDateTime
    ): Result<ReportData> {
        val eventsFlow: Flow<List<Event>> = combine(
            eventRepo.getAppointmentsIn(start, end),
            eventRepo.getCompletedToDoIn(start, end)
        ) { appointments, todos ->
            val filteredAppointments = appointments.filter { it.checkCompletion().completed() }
            filteredAppointments + todos
        }
        return Result.success(
            ReportUserData(
                eventsFlow.first(),
                achievementData.getCompletedAchievementsIn(start, end)
            )
        )
    }

    override fun getAllGroups(): Flow<List<Group>> =
        eventRepo.getGroups()


    override suspend fun updateGroup(group: Group): Result<Unit> {
        if (group !is UserGroup) {
            return Result.failure(Exception("Group type not supported"))
        }
        return asResult(eventRepo.updateGroup(group))
    }

    override suspend fun postSharedEvent(
        event: SharedEvent,
        users: List<User>
    ): Result<Unit> =
        asResult(
            eventRepo.createSharedEvent(
                event.asSharedEvent(),
                users.map { it.asRegularUser() })
        )


    /**
     * Completes an event, updates its status in the repository, and awards experience points to the user.
     * @return A [Result] containing the earned [XP] or an error.
     */
    override suspend fun completeEvent(event: Event): Result<List<Achievement>> {
        if (event.checkCompletion().completed()) {
            return Result.failure(IllegalStateException("Event already completed"))
        }

        val updatedEvent = when (event) {
            is UserAppointment -> {
                if (LocalDateTime.now().isBefore(event.start())) {
                    return Result.failure(IllegalStateException("Event has not started yet"))
                }
                event.asCompletedDomainModel()
            }

            is UserToDo -> xpHandler.calculateTodo(event)
            is UserSharedEvent -> {
                if (LocalDateTime.now().isBefore(event.start())) {
                    return Result.failure(IllegalStateException("Shared Event has not started yet"))
                }
                event.asCompletedDomainModel()
            }

            else -> return Result.failure(IllegalArgumentException("Event type not supported"))
        }

        val response = when (updatedEvent) {
            is UserAppointment -> eventRepo.updateAppointment(updatedEvent)
            is UserToDo -> eventRepo.updateToDo(updatedEvent)
            is UserSharedEvent -> eventRepo.updateSharedEvent(updatedEvent)
            else -> Response.Error(Exception("Mapping failed"))
        }

        return if (response is Response.Success) {
            Log.i(
                "ModelData.completeEvent()",
                "Event successfully completed: (" + updatedEvent.eventID() + ", " + updatedEvent.eventName() + ", " + updatedEvent.checkCompletion()
                    .completed() + ")"
            )
            val updatedUser = userRepo.getCurrentUser().first()
                .asUpdatedDomainModel(updatedEvent.experiencePoints())
            userRepo.updateCurrentUser(updatedUser)
            Log.i(
                "ModelData.completeEvent()",
                "User successfully updated: "
                        + Json.encodeToString(
                    updatedUser.toDTO()
                )
            )
            Result.success(achievementData.checkAchievements())
        } else {
            Result.failure(Exception("Failed to synchronize event completion"))
        }
    }

    override suspend fun addFriend(username: String): Result<Unit> =
        asResult(userRepo.addFriend(username))


    override suspend fun removeFriend(username: String): Result<Unit> =
        asResult(userRepo.removeFriend(username))

    override suspend fun getAllAchievements(): Result<List<Achievement>> {
        return Result.success(achievementData.getAllAchievements())
    }

    override suspend fun syncExperiencePoints(): Result<Unit> {
        xpHandler.distributeXp()
        return Result.success(Unit)
    }

    override suspend fun getEvents() {
        Log.d("getEvents", "Events holen für ${ClientAPI.username}. Token verfügbar: ${LoginViewModel.getFreshToken() != null}")
        val responseGroup = api.getUserGroups()
        if (responseGroup is Response.Success) {
            val groups = responseGroup.data
            groups.map { eventRepo.database.eventDao().insertGroup(it.asDatabaseEntity()) }
            Log.d("getEvents", "Gruppen geladen ${groups.size}")
        }
        val responseToDo = api.getUserToDo()
        if (responseToDo is Response.Success) {
            val toDos = responseToDo.data
            toDos.map { eventRepo.database.eventDao().insertNewToDo(it.asDatabaseEntity()) }
            //toDos.map { eventRepo.database.eventDao().updateCompletionDetail(it.checkCompletion().asDatabaseEntity()) }
            Log.d("getEvents", "toDos geladen ${toDos.size}")
        }
        val responseAppt = api.getUserAppointments()
        if (responseAppt is Response.Success) {
            val appointments = responseAppt.data
            appointments.map { eventRepo.database.eventDao().insertNewAppointment(it.asDatabaseEntity()) }
            Log.d("getEvents", "appointments geladen ${appointments.size}")
        }
        val responseShared = api.getUserSharedEvents()
        if (responseShared is Response.Success) {
            val sharedEvents = responseShared.data
            sharedEvents.map { eventRepo.database.eventDao().upsertSharedEvent(it.asDatabaseEntity()) }
            Log.d("getEvents", "sharedEvents geladen ${sharedEvents.size}")
        }
        val responseFriend = api.getUserFriends()
        if (responseFriend is Response.Success) {
            val friends = responseFriend.data
            eventRepo.database.userDao().upsertFriends(friends.map { it.asDatabaseEntity() })
            Log.d("getEvents", "freunde geladen ${friends.size}")
        }
    }

    private fun <T> asResult(response: Response<T>): Result<T> {
        return if (response is Response.Success) {
            Result.success(response.data)
        } else {
            Result.failure((response as Response.Error).e ?: Exception("Unknown error"))
        }
    }
}



