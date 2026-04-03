package com.gcal.app.model

import android.util.Log
import com.gcal.app.model.modelFacade.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import app.cash.turbine.test
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.localData.UserDao
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelData.repo.UserRepo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before


class UserRepoTest {
    private val apiMock = mockk<ClientAPI>()
    private val userDaoMock = mockk<UserDao>()

    private val dbMock = mockk<LocalData> {
        every { userDao() } returns userDaoMock
    }

    @Before
    fun mockLogging(){
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } answers {
            println("LOG E: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        every { Log.i(any(), any()) } answers {
            println("LOG I: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
    }

    @After
    fun unmockLogging(){
        unmockkStatic(Log::class)
    }

    @Test
    fun getCurrentUserTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = UserRepo(apiMock, dbMock, this, testDispatcher)

        val domainUser = TestData.profile
        val domainUpdatedUser = TestData.updatedProfile
        val mockUserEntity = domainUser.asDatabaseEntity()
        val mockUpdatedEntity = domainUpdatedUser.asDatabaseEntity()
        val databaseFlow = MutableStateFlow(mockUserEntity)

        every { dbMock.userDao().observeProfile() } returns databaseFlow
        coEvery { apiMock.getActiveUser() } returns Response.Success(domainUpdatedUser)
        coEvery { dbMock.userDao().updateProfile(any()) } coAnswers {
            delay(50)
            databaseFlow.value = mockUpdatedEntity
        }

        repository.getCurrentUser().test {
            val result = awaitItem()
            assertEquals(domainUser, result)
            val updatedResult = awaitItem()
            assertEquals(domainUpdatedUser, updatedResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getFriendsTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = UserRepo(apiMock, dbMock, this, testDispatcher)

        val friends = TestData.friends
        val updatedFriends = TestData.updatedFriends
        val friendEntities = friends.map { it.asDatabaseEntity() }
        val updatedFriendEntities = updatedFriends.map { it.asDatabaseEntity() }
        val apiResponse = Response.Success(updatedFriends)
        val databaseFlow = MutableStateFlow(friendEntities)

        coEvery { apiMock.getUserFriends() } returns apiResponse
        every { dbMock.userDao().observeFriends() } returns databaseFlow
        coEvery { dbMock.userDao().upsertFriends(any()) } coAnswers {
            delay(1000)
            databaseFlow.value = updatedFriendEntities
        }

        repository.getFriends().test {
            val firstEmission = awaitItem()
            assertEquals(friends, firstEmission)
            advanceTimeBy(1001)
            val secondEmission = awaitItem()
            assertEquals(updatedFriends, secondEmission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLeaderboardTest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = UserRepo(apiMock, dbMock, this, testDispatcher)

        val rankEntities=
            TestData.leaderboard.map { it.asDatabaseEntity() }
        val newRankEntities = TestData.updatedLeaderboard.map { it.asDatabaseEntity() }
        val apiResponse = Response.Success(TestData.updatedLeaderboard)
        val databaseFlow = MutableStateFlow(rankEntities)

        coEvery { apiMock.getLeaderboard() } returns apiResponse
        every { dbMock.userDao().observeLeaderboard() } returns databaseFlow
        coEvery { dbMock.userDao().refreshLeaderboard(any()) } coAnswers {
            delay(50)
            databaseFlow.value = newRankEntities
        }

        repository.getLeaderboard().test {
            val firstEmission = awaitItem()
            assertEquals(TestData.leaderboard, firstEmission)
            val secondEmission = awaitItem()
            assertEquals(TestData.updatedLeaderboard, secondEmission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateCurrentUserTest() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val repository = UserRepo(apiMock, dbMock, this, testDispatcher)
            val user = TestData.profile
            val dbEntity = user.asDatabaseEntity()
            coEvery { apiMock.updateActiveUser(user) } returns Response.Success(Unit, 200)
            coEvery { dbMock.userDao().updateProfile(dbEntity) } returns Unit

            val result = repository.updateCurrentUser(user)
            assertTrue(result is Response.Success)
            coVerify(exactly = 1) { dbMock.userDao().updateProfile(dbEntity) }

            coEvery { apiMock.updateActiveUser(user) } returns Response.Error(Exception("Server Error"))

            val secondResult = repository.updateCurrentUser(user)
            assertTrue(secondResult is Response.Error)
            coVerify(exactly = 1) { dbMock.userDao().updateProfile(dbEntity) }
        }
}
