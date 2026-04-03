package com.gcal.app.model

import com.gcal.app.model.modelData.data.system.EventCD
import com.gcal.app.model.modelData.data.system.Group
import com.gcal.app.model.modelData.data.system.LeaderboardEntry
import com.gcal.app.model.modelData.data.system.NoGroup
import com.gcal.app.model.modelData.data.system.PersonalUser
import com.gcal.app.model.modelData.data.system.RegularUser
import com.gcal.app.model.modelData.data.system.SharedEventRequest
import com.gcal.app.model.modelData.data.system.UserAppointment
import com.gcal.app.model.modelData.data.system.UserSharedEvent
import com.gcal.app.model.modelData.data.system.UserToDo
import com.gcal.app.model.modelData.data.system.XP
import java.time.LocalDate
import java.time.LocalDateTime

object TestData {
    val profile = PersonalUser("max.user", "max", XP.from(1300), XP.from(70))
    val updatedProfile =
        PersonalUser("max.user", "max", XP.from(1330), XP.from(100))

    val friends = listOf(
        RegularUser("jayk.user", "jayk", XP.from(540)),
        RegularUser("niklas.user", "niklas", XP.from(430)),
        RegularUser("arda.user", "arda", XP.from(187))
    )
    val updatedFriends = listOf(
        RegularUser("jayk.user", "jayk", XP.from(540)),
        RegularUser("niklas.user", "niklas", XP.from(460)),
        RegularUser("arda.user", "arda", XP.from(240))
    )

    val newFriend = RegularUser("fritz.user", "fritz", XP.from(13))


    val leaderboard = listOf(
        LeaderboardEntry(1, "example@gmail.com", "jim", XP.from(20000)),
        LeaderboardEntry(2, "example1@gmail.com", "john", XP.from(19999)),
        LeaderboardEntry(3, "example2@gmail.com", "joachim123", XP.from(12345))
    )
    val updatedLeaderboard = listOf(
        LeaderboardEntry(1, "example@gmail.com", "jim", XP.from(20000)),
        LeaderboardEntry(2, "example1@gmail.com", "john", XP.from(19999)),
        LeaderboardEntry(3, "example2@gmail.com", "joachim123", XP.from(14541)),
        LeaderboardEntry(4, "example3@gmail.com", "kebab", XP.from(10000))
    )

    val groups = listOf(NoGroup, Group.asUserGroup(1, "testGroup", 1))
    val updatedGroup = Group.asUserGroup(1, "neuer Name", 2)

    val newGroup = Group.asUserGroup(2, "neue Gruppe", 3)


    fun appointments(): List<UserAppointment> = appointmentsToday() + appointmentsTomorrow()


    fun appointmentsToday(): List<UserAppointment> {
        val today = LocalDate.now()
        return listOf(
            UserAppointment(
                0,
                "test1",
                "ab",
                today.atTime(8, 0),
                today.atTime(10, 0),
                groups.get(0),
                XP.from(20),
                EventCD(0, false, null)
            ),
            UserAppointment(
                1,
                "test2",
                "xy",
                today.atTime(13, 0),
                today.atTime(14, 0),
                groups.get(0),
                XP.from(11),
                EventCD(1, false, null)
            )
        )
    }

    fun bareAppointmentsToday(): List<UserAppointment> {
        val today = LocalDate.now()
        return listOf(
            UserAppointment(
                0,
                "test1",
                "ab",
                today.atTime(8, 0),
                today.atTime(10, 0),
                groups.get(0),
                XP.from(0),
                EventCD(0, false, null)
            ),
            UserAppointment(
                1,
                "test2",
                "xy",
                today.atTime(13, 0),
                today.atTime(14, 0),
                groups.get(0),
                XP.from(0),
                EventCD(1, false, null)
            )
        )
    }

    fun appointmentsTomorrow(): List<UserAppointment> {
        val tomorrow = LocalDate.now().plusDays(1)
        return listOf(
            UserAppointment(
                2,
                "test3",
                "nk",
                tomorrow.atTime(17, 0),
                tomorrow.atTime(17, 45),
                groups.get(1),
                XP.from(0),
                EventCD(2, false, null)
            ),
            UserAppointment(
                4,
                "test5",
                "ml",
                tomorrow.atTime(13, 0),
                tomorrow.atTime(14, 45),
                groups.get(1),
                XP.from(0),
                EventCD(4, false, null)
            )
        )
    }

    val newAppointment = UserAppointment(
        3,
        "test4",
        "ij",
        LocalDate.now().atTime(19, 0),
        LocalDate.now().atTime(19, 30),
        groups.get(1),
        XP.from(6),
        EventCD(3, false, null)
    )

    fun updateAppointment(oldAppointment: UserAppointment) = UserAppointment(
        oldAppointment.eventID(),
        "Neuer Name",
        "Neue Beschreibung",
        oldAppointment.start(),
        oldAppointment.end(),
        oldAppointment.group(),
        oldAppointment.experiencePoints(),
        oldAppointment.checkCompletion().asEventCompletion()
    )

    fun completableAppointment(): UserAppointment {
        val now = LocalDateTime.now()
        return UserAppointment(
            5,
            "easy",
            "test",
            now.minusMinutes(30),
            now.plusMinutes(60),
            groups.get(0),
            XP.from(15),
            EventCD(5, false, null)
        )
    }

    fun todo(): List<UserToDo> {
        val today = LocalDate.now()
        return listOf(
            UserToDo(
                10, "testToDo1", "ab", today.atTime(20, 0), XP.from(0),
                EventCD(10, false, null)
            ),
            UserToDo(
                20, "testToDo2", "mn", null, XP.from(0),
                EventCD(20, false, null)
            )
        )
    }

    val newToDo = UserToDo(
        30, "NewToDo2", "pq", null, XP.from(0),
        EventCD(30, false, null)
    )
    fun completableTodo()= todo().get(1)

    fun updateToDo(oldToDo: UserToDo) = UserToDo(
        oldToDo.eventID(),
        "New ToDo Name",
        "New ToDo Description",
        oldToDo.end(),
        oldToDo.experiencePoints(),
        oldToDo.checkCompletion().asEventCompletion()
    )

    fun sharedEvents(): List<UserSharedEvent> {
        val today = LocalDate.now()
        return listOf(
            UserSharedEvent(
                100,
                "testSharedEvent1",
                "ab",
                today.atTime(4, 0),
                today.atTime(9, 0),
                groups.get(0),
                XP.from(32),
                EventCD(100, false, null)
            ),
            UserSharedEvent(
                200,
                "testSharedEvent1",
                "xy",
                today.atTime(15, 55),
                today.atTime(16, 17),
                groups.get(0),
                XP.from(5),
                EventCD(200, false, null)
            )
        )
    }

    fun newSharedEvent(): SharedEventRequest {
        val today = LocalDate.now()
        return SharedEventRequest(
            UserSharedEvent(
                300,
                "newSharedEvent",
                "xyz",
                today.atTime(11, 55),
                today.atTime(14, 44),
                groups.get(0),
                XP.from(35),
                EventCD(300, false, null)
            ), friends
        )
    }

    fun updateSharedEvent(oldSharedEvent: UserSharedEvent) = UserSharedEvent(
        oldSharedEvent.eventID(),
        oldSharedEvent.eventName(),
        oldSharedEvent.description(),
        oldSharedEvent.start(),
        oldSharedEvent.end(),
        oldSharedEvent.group(),
        oldSharedEvent.experiencePoints(),
        EventCD(
            oldSharedEvent.eventID(), true,
            LocalDateTime.now()
        )
    )

    fun completableSharedEvent(): UserSharedEvent {
        val now = LocalDateTime.now()
        return UserSharedEvent(
            400,
            "easy",
            "test",
            now.minusMinutes(30),
            now.plusMinutes(60),
            groups.get(0),
            XP.from(18),
            EventCD(400, false, null)
        )
    }
}