package com.gcal.app.model.modelData.repo

import android.util.Log
import com.gcal.app.model.modelData.data.dto.GroupDTO
import com.gcal.app.model.modelData.data.dto.LeaderboardEntryDTO
import com.gcal.app.model.modelData.data.dto.PersonalUserDTO
import com.gcal.app.model.modelData.data.dto.RegularUserDTO
import com.gcal.app.model.modelData.data.dto.SharedEventRequestDTO
import com.gcal.app.model.modelData.data.dto.UserAppointmentDTO
import com.gcal.app.model.modelData.data.dto.UserSharedEventDTO
import com.gcal.app.model.modelData.data.dto.UserToDoDTO
import com.gcal.app.model.modelData.data.dto.toDomain
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.LeaderboardEntry
import com.gcal.app.model.modelData.data.system.NoGroup
import com.gcal.app.model.modelData.data.system.PersonalUser
import com.gcal.app.model.modelData.data.system.RegularUser
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserGroup
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.toDTO
import com.gcal.app.model.modelFacade.RequestAPI
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.model.modelFacade.general.SharedEvent
import com.gcal.app.model.modelFacade.general.User
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class ClientAPI(val domain: String, val client: HttpClient) : RequestAPI {
    companion object {
        var username: String = "Guest"
            private set
        fun setUserName(name: String) {
            username = name
        }
    }

    private val loginRoute = "/login"
    private val userRoute = "/user"
    private val groupRoute = "/groups"
    private val appointmentRoute = "/appointments"
    private val todoRoute = "/toDos"
    private val sharedEventRoute = "/shared-events"
    private val friendsRoute = "/friends"
    private val leaderboardRoute = "/leaderboard"

    object AuthState {
        sealed class State {
            object Idle : State()
            object LoggedIn : State()
            object Failed : State()
        }

        private val _state = MutableStateFlow<State>(State.Idle)
        val state: StateFlow<State> = _state

        fun setLoggedIn() {
            _state.value = State.LoggedIn
        }

        fun setFailed() {
            _state.value = State.Failed
        }
    }
    // ActiveUser
    override suspend fun getActiveUser(idToken: String): Response<PersonalUser> {
        return try {
            val user: PersonalUserDTO = client.get(domain + userRoute) {
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
            }.body()
            Response.Success(user.toDomain())
        } catch (e: Exception) {
            Log.e("ClientAPI", e.message ?: "")
            Response.Error(e)
        }
    }

    override suspend fun getActiveUser(): Response<PersonalUser> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val user: PersonalUserDTO = client.get(domain + userRoute) {
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
            }.body()
            Response.Success(user.toDomain())
        } catch (e: Exception) {
            Log.e("ClientAPI", e.message ?: "")
            Response.Error(e)
        }
    }

    override suspend fun login(idToken: String, name: String) {
        client.post(domain + loginRoute) {
            url {
                parameters.append("idToken", idToken)
                parameters.append("name", name)
            }
        }
        AuthState.setLoggedIn()
    }

    override suspend fun createNewUser(user: PersonalUser): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.post(domain + userRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", user.username())
                    parameters.append("idToken", idToken)
                }
                setBody(user.toDTO())
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun updateActiveUser(user: PersonalUser): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.put(domain + userRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", user.username())
                    parameters.append("idToken", idToken)
                }
                setBody(user.toDTO())
            }
            Log.i(
                "ClientAPI.updateActiveUser()",
                "Send put request to server with url: $domain$userRoute?userName=${user.username()}?idToken=$idToken , and body: " + Json.encodeToString(
                    user.toDTO()
                )
            )
            if (response.status.value in 200..299) {
                Log.i("ClientAPI.updateActiveUser()", "Success")
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    // Appointments

    override suspend fun getUserAppointments(): Response<List<UserAppointment>> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val result: List<UserAppointmentDTO> =
                client.get(domain + userRoute + appointmentRoute) {
                    accept(ContentType.Application.Json)
                    url {
                        parameters.append("userName", username)
                        parameters.append("idToken", idToken)
                    }
                }.body()
            Response.Success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun addUserAppointments(newAppointment: UserAppointment): Response<Unit> {
        return try {
            Log.e("APPT", newAppointment.eventID().toString())
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.post(domain + userRoute + appointmentRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(newAppointment.toDTO())
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun updateUserAppointment(updatedAppointment: UserAppointment): Response<Unit> {
        return try {
            Log.e("APPT", updatedAppointment.eventID().toString())
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.put(domain + userRoute + appointmentRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(updatedAppointment.toDTO())
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }

    }

    override suspend fun deleteUserAppointment(event: UserAppointment): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.delete(domain + userRoute + appointmentRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("eventID", event.eventID().toString())
                    parameters.append("idToken", idToken)
                }
            }

            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    // UserTo Do

    override suspend fun getUserToDo(): Response<List<UserToDo>> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val result: List<UserToDoDTO> = client.get(domain + userRoute + todoRoute) {
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                accept(ContentType.Application.Json)
            }.body()
            Response.Success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun addUserToDo(newToDo: UserToDo): Response<Unit> {
        return try {
            Log.e("TODO", newToDo.eventID().toString())
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.post(domain + userRoute + todoRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(newToDo.toDTO())
            }
            if (response.status.value in 200..299) {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Log.e("ClientAPI", e.message ?: "leere Exception")
            Response.Error(e)
        }
    }

    override suspend fun updateUserToDo(updatedToDo: UserToDo): Response<Unit> {
        return try {
            Log.e("TODO", updatedToDo.eventID().toString())
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.put(domain + userRoute + todoRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(updatedToDo.toDTO())
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun deleteUserToDo(toDo: UserToDo): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.delete(domain + userRoute + todoRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("eventID", toDo.eventID().toString())
                    parameters.append("idToken", idToken)
                }
            }

            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    // SharedEvents

    override suspend fun getUserSharedEvents(): Response<List<SharedEvent>> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val result: List<UserSharedEventDTO> =
                client.get(domain + userRoute + sharedEventRoute) {
                    url {
                        parameters.append("userName", username)
                        parameters.append("idToken", idToken)
                    }
                    accept(ContentType.Application.Json)
                }.body()
            Log.i("ClientAPI.getUserSharedEvents()", "got Shared Events: "+ Json.encodeToString(result))
            Response.Success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun addSharedEvent(
        appointment: UserSharedEvent,
        users: List<RegularUser>
    ): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.post(domain + userRoute + sharedEventRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(SharedEventRequestDTO(appointment.toDTO(), users.map { it.toDTO() }))
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun updateSharedEvent(sharedEvent: UserSharedEvent): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.put(domain + userRoute + sharedEventRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(sharedEvent.toDTO())
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun deleteSharedEvent(sharedEvent: UserSharedEvent): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.delete(domain + userRoute + sharedEventRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("eventID", sharedEvent.eventID().toString())
                    parameters.append("idToken", idToken)
                }
            }

            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    // Friends + Other users

    override suspend fun getUserFriends(): Response<List<User>> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val result: List<RegularUserDTO> = client.get(domain + userRoute + friendsRoute) {
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                accept(ContentType.Application.Json)
            }.body()

            Response.Success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun addUserFriend(newFriendUsername: String): Response<RegularUser> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.post(domain + userRoute + friendsRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("friendName", newFriendUsername)
                    parameters.append("idToken", idToken)
                }
            }

            if (response.status.value in 200..299) {
                Response.Success(
                    (response.body() as RegularUserDTO).toDomain(),
                    response.status.value
                )
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun removeUserFriend(removeFriendUsername: String): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.delete(domain + userRoute + friendsRoute) {
                url {
                    parameters.append("userName", username)
                    parameters.append("friendName", removeFriendUsername)
                    parameters.append("idToken", idToken)
                }
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun getLeaderboard(): Response<List<LeaderboardEntry>> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val result: List<LeaderboardEntryDTO> = client.get(domain + leaderboardRoute) {
                accept(ContentType.Application.Json)
                url {
                    parameters.append("idToken", idToken)
                }
            }.body()

            Response.Success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    // Groups

    override suspend fun getUserGroups(): Response<List<Group>> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val groupMapDTO: List<GroupDTO> = client.get(domain + userRoute + groupRoute) {
                accept(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
            }.body()
            Response.Success(groupMapDTO.map { it.toDomain() })
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun addUserGroup(newGroup: Group): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val newGroupDTO = newGroup.toDTO()
            val response = client.post(domain + userRoute + groupRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(newGroupDTO)
            }

            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun updateGroup(updatedGroup: Group): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val updatedGroupDTO = when (updatedGroup) {
                is UserGroup -> GroupDTO(
                    groupID = updatedGroup.groupId(),
                    name = updatedGroup.groupName(),
                    colour = updatedGroup.groupColour()
                )

                is NoGroup -> GroupDTO(
                    groupID = 0,
                    name = "None",
                    colour = 0
                )
            }
            val response = client.put(domain + userRoute + groupRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("idToken", idToken)
                }
                setBody(updatedGroupDTO)
            }
            if (response.status.value in 200..299) {
                Response.Success(Unit)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    override suspend fun deleteUserGroup(group: Group): Response<Unit> {
        return try {
            val idToken: String = LoginViewModel.getFreshToken() ?: ""
            val response = client.delete(domain + userRoute + groupRoute) {
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("userName", username)
                    parameters.append("groupID", group.groupId().toString())
                    parameters.append("idToken", idToken)
                }
            }

            if (response.status.value in 200..299) {
                Response.Success(Unit, response.status.value)
            } else {
                Log.e("ClientAPI", response.bodyAsText() + response.status.value)
                Response.Error(null, response.status.value)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }
}