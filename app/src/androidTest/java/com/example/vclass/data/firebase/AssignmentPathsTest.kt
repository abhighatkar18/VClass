package com.example.vclass.data.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssignmentPathsTest {
    @Test
    fun submissionReferencesStaySeparateForDifferentAssignmentIds() {
        val firestore = FirebaseFirestore.getInstance()
        val first = firestore.assignmentDocument("ab1234", "generated-a").collection("submissions").document("draft")
        val second = firestore.assignmentDocument("ab1234", "generated-b").collection("submissions").document("draft")
        val legacy = firestore.assignmentDocument("ab1234", "Week_One").collection("submissions").document("draft")

        assertEquals("classes/ab1234/assignments/generated-a/submissions/draft", first.path)
        assertEquals("classes/ab1234/assignments/generated-b/submissions/draft", second.path)
        assertEquals("classes/ab1234/assignments/Week_One/submissions/draft", legacy.path)
        assertNotEquals(first, second)
    }

    @Test
    fun repositoryReferencesRejectMultiSegmentIds() {
        val firestore = FirebaseFirestore.getInstance()
        assertThrows(IllegalArgumentException::class.java) {
            firestore.assignmentDocument("ab1234", "path/invalid")
        }
        assertThrows(IllegalArgumentException::class.java) {
            firestore.assignmentDocument("ab/1234", "generated-a")
        }
    }
}
