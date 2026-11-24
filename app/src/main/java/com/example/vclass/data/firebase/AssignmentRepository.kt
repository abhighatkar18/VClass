package com.example.vclass.data.firebase

import android.net.Uri
import com.example.vclass.domain.ClassroomAssignment
import com.example.vclass.domain.Submission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

internal class AssignmentRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private fun classroom(code: String) = db.collection("classes").document(code)

    fun assignments(code: String) = classroom(code).collection("assignments").observeSnapshots().map { snapshot ->
        snapshot.documents.map { doc ->
            val data = doc.data
            ClassroomAssignment(
                doc.id, data?.get("title") as? String ?: "Untitled",
                data?.get("dueDate") as? Long, data?.get("createdBy") as? String
            )
        }
    }

    suspend fun addAssignment(code: String, title: String, dueDate: Long?) {
        val uid = checkNotNull(auth.currentUser?.uid)
        classroom(code).collection("assignments").add(
            mapOf("title" to title, "dueDate" to dueDate, "createdBy" to uid, "createdAt" to System.currentTimeMillis())
        ).await()
    }

    suspend fun existsOnServer(code: String, assignmentId: String): Boolean =
        db.assignmentDocument(code, assignmentId).get(Source.SERVER).await().exists()

    data class SubmissionDraft(val id: String, val url: String, val uid: String, val name: String, val timestamp: Long)

    fun prepareSubmission(code: String, assignmentId: String, url: String): SubmissionDraft {
        val user = checkNotNull(auth.currentUser)
        val id = db.assignmentDocument(code, assignmentId).collection("submissions").document().id
        return SubmissionDraft(id, url, user.uid, user.displayName ?: "Class member", System.currentTimeMillis())
    }

    suspend fun saveSubmission(code: String, assignmentId: String, draft: SubmissionDraft) {
        val assignment = db.assignmentDocument(code, assignmentId)
        val submission = assignment.collection("submissions").document(draft.id)
        val data = mapOf(
            "fileUrl" to draft.url, "studentId" to draft.uid,
            "studentName" to draft.name, "submittedAt" to draft.timestamp
        )
        db.runTransaction { transaction ->
            check(transaction.get(assignment).exists())
            val previous = transaction.get(submission)
            if (previous.exists()) {
                check(previous.data == data)
            } else {
                transaction.set(submission, data)
            }
        }.await()
    }

    suspend fun submissions(code: String, assignmentId: String): List<Submission>? {
        val assignment = db.assignmentDocument(code, assignmentId)
        if (!assignment.get().await().exists()) return null
        val docs = assignment.collection("submissions").get().await().documents
        val names = try {
            classroom(code).rosterNames()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyMap()
        }
        return docs.map { doc ->
            val url = doc.getString("fileUrl").orEmpty()
            Submission(
                doc.id,
                doc.getString("studentId")?.let(names::get) ?: doc.getString("studentName") ?: "Class member",
                doc.getString("fileName") ?: Uri.parse(url).lastPathSegment.orEmpty(),
                url
            )
        }
    }
}
