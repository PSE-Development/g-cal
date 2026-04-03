package com.gcal.app.model

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.localData.entity.AchievementEntity
import com.gcal.app.model.localData.entity.AppointmentEntity
import com.gcal.app.model.localData.entity.EventCompletionEntity
import com.gcal.app.model.localData.entity.FriendEntity
import com.gcal.app.model.localData.entity.GroupEntity
import com.gcal.app.model.localData.entity.LeaderboardEntity
import com.gcal.app.model.localData.entity.ProfileEntity
import com.gcal.app.model.localData.entity.SharedEventEntity
import com.gcal.app.model.localData.entity.ToDoEntity
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class LocalDataTest {
    lateinit var localData: LocalData
    lateinit var today: LocalDate
    lateinit var tomorrow: LocalDate
    lateinit var nextWeek: LocalDate

    @Before
    fun createDb() {
        today = LocalDateTime.now().toLocalDate()
        tomorrow = today.plusDays(1)
        nextWeek = today.plusDays(7)
        val context = ApplicationProvider.getApplicationContext<Context>()
        localData = Room.inMemoryDatabaseBuilder(context, LocalData::class.java)
            .allowMainThreadQueries()
            .addCallback(LocalData.ACHIEVEMENT_CALLBACK)
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        localData.close()
    }

    @Test
    @Throws(Exception::class)
    fun testAchievementInit() = runBlocking {
        var achievements = localData.achievementDao().observeAllAchievements().first()
        Truth.assertThat(achievements).hasSize(2)
        Truth.assertThat(achievements).contains(AchievementEntity("Früher Vogel", false, null))
        Truth.assertThat(achievements).contains(AchievementEntity("Nachteule", false, null))
        val completedAchievement = AchievementEntity("Früher Vogel", true, LocalDateTime.now())
        localData.achievementDao().updateAchievement(completedAchievement)
        achievements = localData.achievementDao().observeAllAchievements().first()
        Truth.assertThat(achievements).contains(completedAchievement)
        val completedAchievements = localData.achievementDao().getCompletedAchievements()
        Truth.assertThat(completedAchievements).hasSize(1)
        Truth.assertThat(completedAchievements).contains(completedAchievement)
    }

    @Test
    @Throws(Exception::class)
    fun testUser() {
        runBlocking {
            exampleUser()
            val updateUser = ProfileEntity("max@gnail.com", "max", 420, 70)
            localData.userDao().updateProfile(updateUser)
            val updateFriend = FriendEntity("jayk@gnail.com", "jayk", 430)
            localData.userDao().upsertFriends(listOf(updateFriend))
            val outUser = localData.userDao().observeProfile().first()
            Truth.assertThat(outUser).isEqualTo(updateUser)
            var outFriends = localData.userDao().observeFriends().first()
            Truth.assertThat(outFriends).contains(updateFriend)
            Truth.assertThat(outFriends).hasSize(2)
            localData.userDao().removeFriend(updateFriend.username)
            outFriends = localData.userDao().observeFriends().first()
            Truth.assertThat(outFriends).doesNotContain(updateFriend)
            val newFriend = FriendEntity("arda@gnail.com", "Arda", 200)
            localData.userDao().upsertFriends(listOf(newFriend))
            outFriends = localData.userDao().observeFriends().first()
            Truth.assertThat(outFriends).contains(newFriend)
            val leaderboard = localData.userDao().observeLeaderboard().first()
            val updateLeaderboard = leaderboard.toMutableList()
            updateLeaderboard.add(LeaderboardEntity(4, "jayk@gnail.com", "jayk", 430))
            localData.userDao().refreshLeaderboard(updateLeaderboard)
            val outLeaderboard = localData.userDao().observeLeaderboard().first()
            Truth.assertThat(outLeaderboard).hasSize(4)
            Truth.assertThat(outLeaderboard)
                .contains(LeaderboardEntity(4, "jayk@gnail.com", "jayk", 430))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEvents() {
        runBlocking {
            exampleEvents()
            val outAppointments = localData.eventDao()
                .observeAppointments(today.atTime(1, 0), nextWeek.atTime(23, 0)).first()
            Truth.assertThat(outAppointments).hasSize(3)
            val outToDos =
                localData.eventDao().observeToDos(today.atTime(1, 0), nextWeek.atTime(23, 0))
                    .first()
            Truth.assertThat(outToDos).hasSize(2)
            val outSharedEvents = localData.eventDao()
                .observeSharedEvents(today.atTime(1, 0), nextWeek.atTime(23, 0)).first()
            Truth.assertThat(outSharedEvents).hasSize(1)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testAppointments() {
        runBlocking {
            exampleEvents()
            var outAppointments = localData.eventDao()
                .observeCompletedAppointments(today.atTime(1, 0), nextWeek.atTime(23, 0))
                .first()
            Truth.assertThat(outAppointments).hasSize(0)
            outAppointments = localData.eventDao()
                .observeAppointments(today.atTime(1, 0), nextWeek.atTime(1, 0))
                .first()
            Truth.assertThat(outAppointments).hasSize(2)

            val appointment = outAppointments.first().appointment
            localData.eventDao()
                .updateCompletionDetail(
                    EventCompletionEntity(
                        appointment.eventId,
                        today.atTime(13, 45),
                        true
                    )
                )
            outAppointments = localData.eventDao()
                .observeCompletedAppointments(today.atTime(1, 0), nextWeek.atTime(23, 0))
                .first()
            Truth.assertThat(outAppointments).hasSize(1)
            val updatedAppointment = AppointmentEntity(
                appointment.eventId, "UpdatedAppointment",
                "test", appointment.groupId, today.atTime(13, 0),
                today.atTime(14, 0), 30
            )
            localData.eventDao().updateAppointment(updatedAppointment)
            outAppointments = localData.eventDao()
                .observeAppointments(today.atTime(1, 0), nextWeek.atTime(23, 0)).first()
            Truth.assertThat(outAppointments.map { it.appointment }).contains(updatedAppointment)
            Truth.assertThat(outAppointments).hasSize(3)
            localData.eventDao().deleteAppointment(updatedAppointment)
            outAppointments = localData.eventDao()
                .observeAppointments(today.atTime(1, 0), nextWeek.atTime(23, 0)).first()
            Truth.assertThat(outAppointments).doesNotContain(updatedAppointment)
            Truth.assertThat(outAppointments).hasSize(2)
            outAppointments = localData.eventDao()
                .observeCompletedAppointments(today.atTime(1, 0), nextWeek.atTime(23, 0))
                .first()
            Truth.assertThat(outAppointments).hasSize(0)
        }
    }

    private suspend fun exampleUser() {
        val user = ProfileEntity("max@gnail.com", "max", 400, 50)
        localData.userDao().insertProfile(user)

        val leaderboard = listOf(
            LeaderboardEntity(1, "tob@gnail.com", "xXTobiXx", 4200),
            LeaderboardEntity(2, "eier@gnail.com", "oleck", 1870),
            LeaderboardEntity(3, "amaa@gnail.com", "Herr Maier", 1691)
        )
        localData.userDao().refreshLeaderboard(leaderboard)
        val friends = listOf(
            FriendEntity("jayk@gnail.com", "jayk", 250),
            FriendEntity("niklas@gnail.com", "niklas", 500)
        )
        localData.userDao().upsertFriends(friends)
    }

    private suspend fun exampleEvents() {
        localData.eventDao().insertGroup(GroupEntity(1, "Wichtige Termine", 1))
        localData.eventDao().insertNewAppointment(
            AppointmentEntity(
                0, "appointmentToday",
                "test", 1, today.atTime(13, 0),
                today.atTime(14, 0), 30
            )
        )
        localData.eventDao().insertNewAppointment(
            AppointmentEntity(
                0, "appointmentTomorrow",
                "test", 1, tomorrow.atTime(10, 0),
                tomorrow.atTime(11, 0), 0
            )
        )
        localData.eventDao().insertNewAppointment(
            AppointmentEntity(
                0, "appointmentNextWeek",
                "test", null, nextWeek.atTime(16, 0),
                nextWeek.atTime(18, 0), 0
            )
        )
        localData.eventDao().insertNewToDo(
            ToDoEntity(
                0, "todoToday",
                "test", today.atTime(13, 0), 0
            )
        )
        localData.eventDao().insertNewToDo(
            ToDoEntity(
                0, "todoTomorrow",
                "test", tomorrow.atTime(10, 0), 0
            )
        )
        localData.eventDao().upsertSharedEvent(
            SharedEventEntity(
                123456, "sharedEventNextWeek", "test",
                null, nextWeek.atTime(13, 0),
                nextWeek.atTime(14, 0), 0
            )
        )
    }
}