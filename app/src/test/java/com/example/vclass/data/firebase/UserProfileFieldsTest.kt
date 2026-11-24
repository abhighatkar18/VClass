package com.example.vclass.data.firebase

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileFieldsTest {
    @Test
    fun newProfileContainsOnlyTheDisplayNameAndEmail() {
        assertEquals(
            mapOf("name" to "Test Student", "email" to "student@example.test"),
            newUserProfileFields("  Test Student  ", "student@example.test")
        )
    }

    @Test
    fun missingFieldsAndOversizedNamesKeepTheExistingFallbacks() {
        assertEquals(mapOf("name" to "", "email" to ""), newUserProfileFields(null, null))
        assertEquals("x".repeat(120), newUserProfileFields("x".repeat(140), null)["name"])
    }
}
