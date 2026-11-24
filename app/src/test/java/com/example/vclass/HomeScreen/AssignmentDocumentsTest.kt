package com.example.vclass.HomeScreen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentDocumentsTest {
    @Test
    fun acceptsGeneratedAndLegacyIdsWithoutChangingThem() {
        assertTrue(isFirestoreDocumentId("Lgxvi3YFM0FbKWnEoG4Z"))
        assertTrue(isFirestoreDocumentId("Week_One"))
        assertTrue(isFirestoreDocumentId("legacy id with spaces"))
    }

    @Test
    fun rejectsMissingInvalidAndMultiSegmentIds() {
        listOf("", " ", ".", "..", "week/one", "__reserved__").forEach { id ->
            assertFalse(id, isFirestoreDocumentId(id))
        }
    }

    @Test
    fun enforcesTheFirestoreLimitInUtf8Bytes() {
        assertTrue(isFirestoreDocumentId("é".repeat(750)))
        assertFalse(isFirestoreDocumentId("é".repeat(751)))
    }
}
