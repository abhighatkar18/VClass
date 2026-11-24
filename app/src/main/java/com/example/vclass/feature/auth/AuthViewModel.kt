package com.example.vclass.feature.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vclass.data.firebase.AuthActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AuthResult(val message: String, val signedIn: Boolean = false, val isError: Boolean = false)

internal data class AuthState(val progress: String? = null, val result: AuthResult? = null) {
    val loading get() = progress != null
}

internal class AuthViewModel(private val auth: AuthActions) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()

    fun login(email: String, password: String) {
        val address = email.trim()
        if (!validEmail(address)) {
            showMessage("Enter a valid email address.")
            return
        }
        if (password.isEmpty()) {
            showMessage("Enter your password.")
            return
        }
        execute("Logging in…", "Could not sign in. Check your details and connection.") {
            auth.login(address, password)
            AuthResult("Login Successful!", signedIn = true)
        }
    }

    fun resetPassword(email: String) {
        val address = email.trim()
        if (!validEmail(address)) {
            showMessage("Enter a valid email address.")
            return
        }
        execute("Sending reset email…", "Could not send the reset email. Please try again.") {
            auth.resetPassword(address)
            AuthResult("Password reset email sent!")
        }
    }

    fun register(name: String, email: String, password: String) {
        val displayName = name.trim()
        val address = email.trim()
        val validation = when {
            displayName.length !in 1..120 -> "Enter a name between 1 and 120 characters."
            !validEmail(address) -> "Enter a valid email address."
            password.isBlank() || password.length < 6 -> "Enter a password with at least six characters."
            else -> null
        }
        if (validation != null) {
            showMessage(validation)
            return
        }
        execute("Creating account…", "Could not create the account. Check your details or try signing in.") {
            val message = if (auth.register(displayName, address, password)) "Signup successful!"
                else "Account created. Open Profile to retry setup."
            AuthResult(message, signedIn = true)
        }
    }

    fun consumeResult() = mutableState.update { it.copy(result = null) }

    private fun showMessage(message: String) {
        mutableState.update { if (it.loading) it else it.copy(result = AuthResult(message, isError = true)) }
    }

    private fun execute(progress: String, failureMessage: String, operation: suspend () -> AuthResult) {
        if (mutableState.value.loading) return
        mutableState.value = AuthState(progress = progress)
        viewModelScope.launch {
            val result = try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                AuthResult(failureMessage, isError = true)
            }
            mutableState.value = AuthState(result = result)
        }
    }

    private fun validEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
