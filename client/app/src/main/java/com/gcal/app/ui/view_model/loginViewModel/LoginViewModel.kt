package com.gcal.app.ui.view_model.loginViewModel

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcal.app.model.MyContext
import com.gcal.app.model.localData.LocalData
import com.gcal.app.model.localData.entity.ProfileEntity
import com.gcal.app.model.modelData.ModelData
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelFacade.ModelFacade
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class LoginState {
    object Idle : LoginState()
    object Success : LoginState()
    object Error : LoginState()
}

/**
 * Orchestrates the authentication journey, acting as the bridge between the View's
 * user intents and the Model's authentication APIs.
 * It manages the complex logic of OIDC login flows, onboarding branching, and
 * real-time local input validation, keeping the UI completely passive.
 */
class LoginViewModel(
    private val model: ModelFacade = ModelData.instance
) : ViewModel() {

    private val loginFail = "Login fehlgeschlagen: "
    private val registrationFail = "Registrierung fehlgeschlagen: "
    private val enterName = "3-20 Zeichen. Kleinbuchstaben (a-z), 0-9, _. Start mit Buchstabe."
    // Encapsulated mutable state to guarantee that state changes can only
    // originate from within this ViewModel, preventing the View from modifying it directly.
    private val _uiState = MutableStateFlow(LoginUiState())

    // Read-only stream exposed to the View to enforce unidirectional data flow.
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    private val auth = FirebaseAuth.getInstance()
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    /**
     * Defines backend-aligned username constraints locally.
     * We duplicate this rule here to provide immediate, zero-latency feedback
     * to the user during typing, preventing unnecessary API calls to the Model
     * for obviously invalid inputs.
     * Rules: Starts with letter, 1-18 middle chars (alphanumeric/_), ends with letter/digit.
     */
    private val usernameRegex = Regex("^[a-z][a-z0-9_]{1,18}[a-z0-9]$")

    init {
        instance = this
    }

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.LoginClicked -> handleLogin()
            is LoginUiEvent.UsernameChanged -> handleUsernameChange(event.input)
            is LoginUiEvent.RegisterUsernameClicked -> handleRegistration()
            is LoginUiEvent.ErrorDismissed -> _uiState.update { it.copy(generalErrorMessage = null) }
        }
    }

    /**
     * Phase 1: Authentication via OIDC.
     * Initiates the handshake with the Model layer. The critical architectural decision
     * here is branching the UX based on the backend's response:
     * We must determine if the authenticated OIDC token belongs to an existing profile
     * or a brand-new user requiring a domain-specific username.
     */
    private fun handleLogin() {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
            val result = model.login()

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )}
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        generalErrorMessage = loginFail + exception.message
                    )}
                }
            )
        }
    }

    /**
     * Phase 2: Onboarding – process and validate user input.
     * Triggered on every keystroke to enable real-time form validation.
     */
    private fun handleUsernameChange(input: String) {
        val trimmedInput = input.trim()

        // We canonicalize the input to lowercase for validation to ensure it matches
        // strict database/backend constraints.
        val canonicalForm = trimmedInput.lowercase(Locale.ROOT)
        val isValid = usernameRegex.matches(canonicalForm)

        val errorMsg = if (trimmedInput.isNotEmpty() && !isValid) {
            enterName
        } else {
            null
        }

        _uiState.update {
            it.copy(
                // We deliberately store and show the original, non-canonicalized input.
                // Forcing lowercase directly on the input field while the user is typing
                // creates a jarring UX (unexpected cursor jumps and forced text replacement).
                usernameInput = trimmedInput,
                usernameError = errorMsg,
                // Automatically controls the View's submit button state securely.
                isSubmitEnabled = isValid && trimmedInput.isNotEmpty()
            )
        }
    }

    /**
     * Phase 2: Onboarding – completion.
     * Finalizes the domain-specific registration after OIDC auth.
     */
    private fun handleRegistration() {
        val currentInput = _uiState.value.usernameInput
        val canonicalUsername = currentInput.trim().lowercase(Locale.ROOT)

        if (!usernameRegex.matches(canonicalUsername)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = model.register(canonicalUsername)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        generalErrorMessage = registrationFail + exception.message
                    )}
                }
            )
        }
    }

    fun setClientUserName(email: String) {
        ClientAPI.setUserName(email)
    }

    fun handleCredentialResult(credential: Credential, name: String) {
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val credential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    signInWithFirebase(credential, googleCredential.idToken, googleCredential.id, name)
                }
            }
        }
    }

    fun signInWithFirebase(credential: AuthCredential, idToken: String, email: String, name: String) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        onLoginSuccess(idToken, email, name)
                    }
                } else {
                    _loginState.value = LoginState.Error
                }
            }
    }

    suspend fun onLoginSuccess(idToken: String, email: String, name: String) {
        val data = LocalData.getDatabase(MyContext.appContext).userDao()
            .observeProfile().first()
        val profile = ProfileEntity(email, name, data?.xpToday ?: 0, data?.xpTotal ?: 0)
        LocalData.getDatabase(MyContext.appContext).userDao().insertProfile(profile)
        ModelData.instance.api.login(idToken, name)
        model.getEvents()
        _loginState.value = LoginState.Success
    }

    companion object {
        lateinit var instance: LoginViewModel
            private set
        suspend fun getFreshToken(): String? = suspendCoroutine { continuation ->
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            user.getIdToken(true)
                .addOnSuccessListener { continuation.resume(it.token) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        _loginState.update { _ -> LoginState.Idle }
    }
}