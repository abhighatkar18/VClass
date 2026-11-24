package com.example.vclass.HomeScreen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassMemberPermissionsTest {
    @Test
    fun creatorAndTeachersCanManageMembers() {
        assertTrue(canManageClassMembers("creator", "creator", listOf("teacher")))
        assertTrue(canManageClassMembers("teacher", "creator", listOf("teacher")))
    }

    @Test
    fun studentAndSignedOutUserCannotManageMembers() {
        assertFalse(canManageClassMembers("student", "creator", listOf("teacher")))
        assertFalse(canManageClassMembers(null, "creator", listOf("teacher")))
    }

    @Test
    fun missingOrBlankViewerIdDoesNotMatchMalformedRoleData() {
        assertFalse(canManageClassMembers(null, null, emptyList()))
        assertFalse(canManageClassMembers("", "", listOf("")))
    }
}
