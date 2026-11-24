package com.example.vclass.feature.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vclass.data.firebase.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AuthResult(val message: String, val signedIn: Boolean = false)

internal data class AuthState(val loading: Boolean = false, val result: AuthResult? = null)

internal class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()

    fun login(email: String, password: String) {
        val address = email.trim()
        if (!validEmail(address) || password.isEmpty()) {
            showMessage("Enter a valid email and your password.")
            return
        }
        execute("Could not sign in. Check your details and connection.") {
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
        execute("Could not send the reset email. Please try again.") {
            auth.resetPassword(address)
            AuthResult("Password reset email sent!")
        }
    }

    fun register(name: String, email: String, password: String) {
        val displayName = name.trim()
        val address = email.trim()
        if (displayName.length !in 1..120 || !validEmail(address) || password.length < 6) {
            showMessage("Enter a name, valid email, and password of at least six characters.")
            return
        }
        execute("Could not create the account. Check your details or try signing in.") {
            val message = if (auth.register(displayName, address, password)) "Signup successful!"
                else "Account created. Open Profile to retry setup."
            AuthResult(message, signedIn = true)
        }
    }

    fun consumeResult() = mutableState.update { it.copy(result = null) }

    private fun showMessage(message: String) {
        mutableState.update { if (it.loading) it else it.copy(result = AuthResult(message)) }
    }

    private fun execute(failureMessage: String, operation: suspend () -> AuthResult) {
        if (mutableState.value.loading) return
        mutableState.value = AuthState(loading = true)
        viewModelScope.launch {
            val result = try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                AuthResult(failureMessage)
            }
            mutableState.value = AuthState(result = result)
        }
    }

    private fun validEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
