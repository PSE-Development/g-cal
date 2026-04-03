package com.gcal.app
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.gcal.app.model.modelData.ModelData
import com.gcal.app.model.modelData.data.system.PersonalUser
import com.gcal.app.model.modelData.repo.ClientAPI
import com.gcal.app.model.modelFacade.Response
import com.gcal.app.ui.view_model.loginViewModel.LoginState
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoginActivity : ComponentActivity() {
    private lateinit var credentialManager: CredentialManager
    private val loginFail = "Login fehlgeschlagen"
    private val okButton = "OK"
    private val saveButton = "Speichern"
    private val noCredential = "Kein Google-Konto gefunden."
    private val noCredentialMessage = "Bitte füge zuerst ein Google-Konto auf deinem Gerät hinzu."
    private val nameDialogTitle = "Name eingeben"
    private val nameDialogMessage = "Bitte geben sie ihren Namen ein."
    private val nameDialogMessageRepeat = "Name darf nicht leer sein."
    private val nameDialogHint = "Name"
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser != null) {
            ClientAPI.setUserName(FirebaseAuth.getInstance().currentUser?.email.toString())
            ClientAPI.AuthState.setLoggedIn()
            finish()
            return
        }
        credentialManager = CredentialManager.create(this)
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Error -> {
                        ClientAPI.AuthState.setFailed()
                        runOnUiThread {
                            android.app.AlertDialog.Builder(this@LoginActivity)
                                .setTitle(loginFail)
                                .setPositiveButton(okButton) { _, _ -> finishAffinity() }
                                .setCancelable(false)
                                .show()
                        }
                    }
                    is LoginState.Success -> { finish() }
                    else -> {}
                }
            }
        }
        lifecycleScope.launch {
            signInWithGoogle()
        }
    }

    private suspend fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        try {
            val result = credentialManager.getCredential(request = request, context = this@LoginActivity)
            val credential = result.credential
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val email = GoogleIdTokenCredential.createFrom(credential.data).id
            viewModel.setClientUserName(email)
            val name = when (val response = ModelData.instance.userRepo.api.getActiveUser(token)) {
                is Response.Error -> showNameDialog(true)
                is Response.Success<PersonalUser> -> response.data.name()
            }
            viewModel.handleCredentialResult(credential, name)
        } catch (_: androidx.credentials.exceptions.NoCredentialException) {
            ClientAPI.AuthState.setFailed()
            runOnUiThread {
                android.app.AlertDialog.Builder(this@LoginActivity)
                    .setTitle(noCredential)
                    .setMessage(noCredentialMessage)
                    .setPositiveButton(okButton) { _, _ -> finishAffinity() }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                android.app.AlertDialog.Builder(this@LoginActivity)
                    .setTitle(loginFail)
                    .setPositiveButton(okButton) { _, _ -> finishAffinity() }
                    .setCancelable(false)
                    .show()
            }
            ClientAPI.AuthState.setFailed()
            finishAffinity()
        }
    }

    private suspend fun showNameDialog(firstDialog: Boolean): String =
        suspendCoroutine { continuation ->
            val input = android.widget.EditText(this)
            input.hint = nameDialogHint
            val hintMessage: String = if (firstDialog) {
                nameDialogMessage
            } else {
                nameDialogMessageRepeat
            }
            android.app.AlertDialog.Builder(this)
                .setTitle(nameDialogTitle)
                .setMessage(hintMessage)
                .setView(input)
                .setPositiveButton(saveButton) { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        continuation.resume(name)
                    } else {
                        lifecycleScope.launch {
                            showNameDialog(false)
                        }
                    }
                }
                .setCancelable(false)
                .show()
        }
}