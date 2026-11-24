package com.example.vclass.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vclass.MainActivity
import com.example.vclass.di.AppContainer
import com.example.vclass.feature.auth.SignupActivity
import com.example.vclass.feature.dashboard.DashboardActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun launcherRoutesSignedOutUsersToSignup() = assertSignupFrom(MainActivity::class.java)

    @Test
    fun directlyOpeningDashboardStillRequiresASession() = assertSignupFrom(DashboardActivity::class.java)

    private fun assertSignupFrom(entry: Class<out Activity>) {
        // Run on a disposable emulator with a signed-out app session.
        assertNull(AppContainer.auth.currentUserId)
        val context = instrumentation.targetContext
        val monitor = instrumentation.addMonitor(SignupActivity::class.java.name, null, false)
        var destination: Activity? = null
        try {
            context.startActivity(Intent(context, entry).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            destination = instrumentation.waitForMonitorWithTimeout(monitor, 10_000)
            assertNotNull("Expected a new sign-up activity from ${entry.simpleName}", destination)
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("Sign Up").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            }
            compose.onNodeWithText("Sign Up").assertIsDisplayed()
            compose.onNodeWithText("Your Classes").assertDoesNotExist()
        } finally {
            instrumentation.removeMonitor(monitor)
            destination?.let { activity -> instrumentation.runOnMainSync { activity.finish() } }
        }
    }
}
