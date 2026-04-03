package com.gcal.app.viewmodel.loginViewModel

import android.content.Context
import app.cash.turbine.test
import com.gcal.app.model.MyContext
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.localData.UserDao
import com.gcal.app.model.modelData.ModelData
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.ui.view_model.loginViewModel.LoginState
import com.gcal.app.ui.view_model.loginViewModel.LoginUiEvent
import com.gcal.app.ui.view_model.loginViewModel.LoginUiStep
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel
import com.gcal.app.viewmodel.MainDispatcherRule
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var viewModel: LoginViewModel
    private lateinit var modelData: ModelData
    private lateinit var mockAuth: FirebaseAuth

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0

        mockkStatic(LocalData::class)
        val mockUserDao = mockk<UserDao>(relaxed = true)
        val mockDb = mockk<LocalData>(relaxed = true)
        every { mockDb.userDao() } returns mockUserDao
        coEvery { mockUserDao.observeProfile() } returns flowOf(null)
        val instanceField = LocalData::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, mockDb)

        modelData = mockk(relaxed = true)
        mockkObject(ModelData)
        every { ModelData.instance } returns modelData

        coEvery { modelData.login() } returns Result.success(Unit)
        coEvery { modelData.register(any()) } returns Result.success(Unit)
        coEvery { modelData.getEvents() } just runs

        mockkStatic(FirebaseAuth::class)
        mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockAuth

        val mockContext = mockk<Context>(relaxed = true)
        val appContextField = MyContext::class.java.getDeclaredField("appContext")
        appContextField.isAccessible = true
        appContextField.set(null, mockContext)

        viewModel = LoginViewModel(modelData)
    }

    @Test
    fun `onLoginSuccess sets loginState to Success`() = runTest {
        viewModel.onLoginSuccess("token", "test@test.com", "Test")
        testScheduler.advanceUntilIdle()

        assertEquals(LoginState.Success, viewModel.loginState.value)
    }

    @Test
    fun `onLoginSuccess calls getEvents`() = runTest {
        viewModel.onLoginSuccess("token", "test@test.com", "Test")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { modelData.getEvents() }
    }

    @Test
    fun `onLoginSuccess does not set Success if getEvents throws`() = runTest {
        coEvery { modelData.getEvents() } throws Exception("Netzwerkfehler")

        try {
            viewModel.onLoginSuccess("token", "test@test.com", "Test")
            testScheduler.advanceUntilIdle()
        } catch (_: Exception) { }

        assertNotEquals(LoginState.Success, viewModel.loginState.value)
    }

    @Test
    fun `setClientUserName sets username in ClientAPI`() {
        mockkObject(ClientAPI)
        every { ClientAPI.setUserName(any()) } just runs

        viewModel.setClientUserName("test@test.com")

        verify { ClientAPI.setUserName("test@test.com") }
    }

    @Test
    fun `signInWithFirebase success calls onLoginSuccess`() = runTest {
        val mockCredential = mockk<AuthCredential>()
        val mockTask = mockk<Task<AuthResult>>(relaxed = true)
        every { mockAuth.signInWithCredential(any()) } returns mockTask
        every { mockTask.addOnCompleteListener(any()) } answers {
            val listener = arg<OnCompleteListener<AuthResult>>(0)
            val mockResult = mockk<Task<AuthResult>>()
            every { mockResult.isSuccessful } returns true
            listener.onComplete(mockResult)
            mockTask
        }

        viewModel.signInWithFirebase(mockCredential, "token", "test@test.com", "Test")
        testScheduler.advanceUntilIdle()

        assertEquals(LoginState.Success, viewModel.loginState.value)
    }

    @Test
    fun `signInWithFirebase failure sets loginState to Error`() = runTest {
        val mockCredential = mockk<AuthCredential>()
        val mockTask = mockk<Task<AuthResult>>(relaxed = true)
        every { mockAuth.signInWithCredential(any()) } returns mockTask
        every { mockTask.addOnCompleteListener(any()) } answers {
            val listener = arg<OnCompleteListener<AuthResult>>(0)
            val mockResult = mockk<Task<AuthResult>>()
            every { mockResult.isSuccessful } returns false
            listener.onComplete(mockResult)
            mockTask
        }

        viewModel.signInWithFirebase(mockCredential, "token", "test@test.com", "Test")
        testScheduler.advanceUntilIdle()

        assertEquals(LoginState.Error, viewModel.loginState.value)
    }

    @Test
    fun `getFreshToken returns token when user is logged in`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        val mockTask = mockk<Task<GetTokenResult>>(relaxed = true)
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.getIdToken(true) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = arg<OnSuccessListener<GetTokenResult>>(0)
            val mockTokenResult = mockk<GetTokenResult>()
            every { mockTokenResult.token } returns "freshToken"
            listener.onSuccess(mockTokenResult)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        val result = LoginViewModel.getFreshToken()

        assertEquals("freshToken", result)
    }

    @Test
    fun `getFreshToken returns null when no user is logged in`() = runTest {
        every { mockAuth.currentUser } returns null

        val result = LoginViewModel.getFreshToken()

        assertEquals(null, result)
    }

    @Test
    fun `handleRegistration success sets isAuthenticated to true`() = runTest {
        viewModel.onEvent(LoginUiEvent.UsernameChanged("max123"))
        viewModel.onEvent(LoginUiEvent.RegisterUsernameClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `handleRegistration with invalid username does nothing`() = runTest {
        viewModel.onEvent(LoginUiEvent.UsernameChanged("a"))
        viewModel.onEvent(LoginUiEvent.RegisterUsernameClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isAuthenticated)
        coVerify(exactly = 0) { modelData.register(any()) }
    }

    @Test
    fun `handleRegistration failure sets error message`() = runTest {
        coEvery { modelData.register(any()) } returns Result.failure(Exception("Username vergeben"))

        viewModel.onEvent(LoginUiEvent.UsernameChanged("max123"))
        viewModel.onEvent(LoginUiEvent.RegisterUsernameClicked)
        testScheduler.advanceUntilIdle()

        assertEquals("Registrierung fehlgeschlagen: Username vergeben", viewModel.uiState.value.generalErrorMessage)
        assertEquals(false, viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `logout calls FirebaseAuth signOut`() {
        viewModel.logout()

        verify { mockAuth.signOut() }
    }

    @Test
    fun `handleLogin success sets isAuthenticated to true`() = runTest {
        coEvery { modelData.login() } returns Result.success(Unit)

        viewModel.onEvent(LoginUiEvent.LoginClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isAuthenticated)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `handleLogin failure sets error message`() = runTest {
        coEvery { modelData.login() } returns Result.failure(Exception("Server nicht erreichbar"))

        viewModel.onEvent(LoginUiEvent.LoginClicked)
        testScheduler.advanceUntilIdle()

        assertEquals("Login fehlgeschlagen: Server nicht erreichbar", viewModel.uiState.value.generalErrorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(false, viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `handleLogin sets isLoading to true while loading`() = runTest {
        coEvery { modelData.login() } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        }

        viewModel.uiState.test {
            awaitItem() // Initial
            viewModel.onEvent(LoginUiEvent.LoginClicked)

            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            testScheduler.advanceUntilIdle()

            val finalState = awaitItem()
            assertEquals(false, finalState.isLoading)
        }
    }

    @Test
    fun `instance is set after initialization`() {
        assertEquals(viewModel, LoginViewModel.instance)
    }
}