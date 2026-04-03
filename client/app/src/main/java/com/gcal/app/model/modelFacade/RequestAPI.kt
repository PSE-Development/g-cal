package com.gcal.app.model.modelFacade

import com.gcal.app.model.modelData.data.system.LeaderboardEntry
import com.gcal.app.model.modelData.data.system.PersonalUser
import com.gcal.app.model.modelData.data.system.RegularUser
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelFacade.general.Appointment
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelFacade.general.SharedEvent
import com.gcal.app.model.modelFacade.general.ToDo
import com.gcal.app.model.modelFacade.general.User

interface RequestAPI {
    // User
    suspend fun getActiveUser(idToken: String): Response<PersonalUser>
    suspend fun getActiveUser(): Response<PersonalUser>
    suspend fun login(idToken: String, name: String)
    suspend fun createNewUser(user: PersonalUser): Response<Unit>
    suspend fun updateActiveUser(user: PersonalUser): Response<Unit>
    suspend fun getUserGroups(): Response<List<Group>>
    suspend fun getUserAppointments(): Response<List<Appointment>>
    suspend fun addUserAppointments(newAppointment: UserAppointment): Response<Unit>
    suspend fun updateUserAppointment(updatedAppointment: UserAppointment): Response<Unit>
    suspend fun getUserToDo(): Response<List<ToDo>>
    suspend fun addUserToDo(newToDo: UserToDo): Response<Unit>
    suspend fun updateUserToDo(updatedToDo: UserToDo): Response<Unit>
    suspend fun getUserSharedEvents(): Response<List<SharedEvent>>
    suspend fun getUserFriends(): Response<List<User>>

    // changed to only Username:
    suspend fun addUserFriend(newFriendUsername: String): Response<RegularUser>

    suspend fun removeUserFriend(removeFriendUsername: String): Response<Unit>

    //Ergänzung zum Entwurf:
    suspend fun getLeaderboard(): Response<List<LeaderboardEntry>>

    suspend fun addUserGroup(newGroup: Group): Response<Unit>

    suspend fun updateGroup(updatedGroup: Group): Response<Unit>

    suspend fun deleteUserGroup(group: Group): Response<Unit>

    suspend fun deleteUserToDo(toDo: UserToDo): Response<Unit>

    suspend fun deleteUserAppointment(event: UserAppointment): Response<Unit>

    suspend fun addSharedEvent(
        appointment: UserSharedEvent,
        users: List<RegularUser>
    ): Response<Unit>

    suspend fun updateSharedEvent(sharedEvent: UserSharedEvent): Response<Unit>

    suspend fun deleteSharedEvent(sharedEvent: UserSharedEvent): Response<Unit>
}