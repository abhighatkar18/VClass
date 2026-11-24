package com.example.vclass.feature.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vclass.data.firebase.AuthActions
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun invalidInputsDoNotReachAuthentication() {
        val auth = FakeAuthActions()
        val model = AuthViewModel(auth)
        instrumentation.runOnMainSync {
            model.login("", "secret")
            assertTrue(model.state.value.result!!.isError)
            model.login("missing-domain@", "secret")
            assertTrue(model.state.value.result!!.isError)
            model.login("student@example.test", "")
            assertTrue(model.state.value.result!!.message.contains("password"))
            model.register(" ", "student@example.test", "secret")
            assertTrue(model.state.value.result!!.message.contains("name"))
            model.register("Student", "student@example.test", "     ")
            assertTrue(model.state.value.result!!.message.contains("password"))
            model.register("Student", "student@example.test", "short")
            assertTrue(model.state.value.result!!.message.contains("password"))
            model.resetPassword("invalid")
            assertTrue(model.state.value.result!!.isError)
            assertFalse(model.state.value.loading)
            assertTrue(auth.calls.isEmpty())
        }
    }

    @Test
    fun pendingLoginAcceptsOneRequestAndSuccessfulResultCanBeConsumed() {
        val answer = CompletableDeferred<Unit>()
        val auth = FakeAuthActions().apply { loginResult = { answer.await() } }
        val model = AuthViewModel(auth)
        instrumentation.runOnMainSync {
            assertNull(model.state.value.result)
            model.login("  student@example.test  ", "secret")
            assertTrue(model.state.value.loading)
            model.login("student@example.test", "secret")
            model.resetPassword("student@example.test")
            assertEquals(listOf("login:student@example.test"), auth.calls)
            answer.complete(Unit)
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertTrue(model.state.value.result!!.signedIn)
            assertFalse(model.state.value.loading)
            model.consumeResult()
            assertNull(model.state.value.result)
        }
    }

    @Test
    fun invalidCredentialsAndDuplicateSignupHaveSafeFeedbackAndAllowRetry() {
        val auth = FakeAuthActions().apply {
            loginResult = { error("backend: invalid credentials") }
            signupResult = { error("backend: duplicate email") }
        }
        val model = AuthViewModel(auth)
        instrumentation.runOnMainSync {
            model.login("student@example.test", "incorrect")
            assertEquals(
                AuthResult("Could not sign in. Check your details and connection.", isError = true),
                model.state.value.result
            )
            model.register("Student", "student@example.test", "secret")
            assertTrue(model.state.value.result!!.isError)
            assertFalse(model.state.value.result!!.message.contains("backend"))
            assertFalse(model.state.value.loading)

            auth.signupResult = { false }
            model.register(" Student ", " student@example.test ", "secret")
            assertEquals("signup:Student:student@example.test", auth.calls.last())
            assertEquals(AuthResult("Account created. Open Profile to retry setup.", signedIn = true), model.state.value.result)
        }
    }

    @Test
    fun resetUsesItsOwnProgressAndDoesNotSignTheUserIn() {
        val answer = CompletableDeferred<Unit>()
        val auth = FakeAuthActions().apply { resetResult = { answer.await() } }
        val model = AuthViewModel(auth)
        instrumentation.runOnMainSync {
            model.resetPassword(" student@example.test ")
            assertTrue(model.state.value.progress!!.startsWith("Sending reset"))
            assertEquals(listOf("reset:student@example.test"), auth.calls)
            answer.complete(Unit)
        }
        instrumentation.waitForIdleSync()
        assertFalse(model.state.value.loading)
        assertEquals(AuthResult("Password reset email sent!"), model.state.value.result)
    }

    private class FakeAuthActions : AuthActions {
        val calls = mutableListOf<String>()
        var loginResult: suspend () -> Unit = {}
        var signupResult: suspend () -> Boolean = { true }
        var resetResult: suspend () -> Unit = {}

        override suspend fun login(email: String, password: String) {
            calls += "login:$email"
            loginResult()
        }

        override suspend fun resetPassword(email: String) {
            calls += "reset:$email"
            resetResult()
        }

        override suspend fun register(name: String, email: String, password: String): Boolean {
            calls += "signup:$name:$email"
            return signupResult()
        }
    }
}
