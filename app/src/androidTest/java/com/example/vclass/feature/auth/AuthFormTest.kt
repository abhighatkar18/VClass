package com.example.vclass.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFormTest {
    @get:Rule val compose = createAndroidComposeRule<SignupActivity>()

    @Test
    fun showsInlineSignupValidationAndKeepsLoginReachable() {
        compose.onNodeWithContentDescription("Show password").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        compose.onNodeWithText("Sign Up").performScrollTo().performClick()
        compose.onNodeWithText("Enter a name between 1 and 120 characters.").assertIsDisplayed()
        compose.onNodeWithText("Already have an account? Log in").performScrollTo().performClick()
        compose.onNodeWithText("Forgot Password?").performScrollTo().performClick()
        compose.onNodeWithText("Enter a valid email address.").assertIsDisplayed()
    }
}
