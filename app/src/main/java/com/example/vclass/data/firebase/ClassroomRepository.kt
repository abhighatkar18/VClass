package com.example.vclass.data.firebase

import com.example.vclass.domain.ClassAnnouncement
import com.example.vclass.domain.ClassMember
import com.example.vclass.domain.ClassResource
import com.example.vclass.domain.ClassRole
import com.example.vclass.domain.Classroom
import com.example.vclass.domain.ClassroomRoster
import com.example.vclass.domain.canManageClassMembers
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

internal class ClassroomRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private fun classroom(code: String) = db.collection("classes").document(code)
    private fun profile(uid: String) = db.collection("users").document(uid)

    suspend fun list(uid: String): List<Classroom> {
        val codes = (profile(uid).get().await().get("classes") as? List<*>).orEmpty().filterIsInstance<String>()
        return loadClassroomIndex(codes) { code ->
            val doc = classroom(code).get().await()
            if (doc.exists() && doc.getTimestamp("archivedAt") == null && doc.includesMember(uid)) {
                Classroom(code, doc.getString("name") ?: "Unknown Class")
            } else null
        }
    }

    suspend fun createOrJoin(creating: Boolean, name: String, code: String) {
        val user = checkNotNull(auth.currentUser)
        ensureUserProfile(db, user)
        val userRef = profile(user.uid)
        val classRef = classroom(code)
        val rosterName = userRef.get().await().displayName()?.take(120)
        val rosterRef = classRef.collection("roster").document(user.uid)
        if (creating) {
            db.runTransaction { transaction ->
                check(!transaction.get(classRef).exists())
                transaction.set(classRef, mapOf("name" to name, "members" to listOf(user.uid), "creatorId" to user.uid))
                transaction.update(userRef, "classes", FieldValue.arrayUnion(code))
                rosterName?.let { transaction.set(rosterRef, mapOf("displayName" to it)) }
            }.await()
        } else {
            db.runBatch { batch ->
                batch.update(classRef, "members", FieldValue.arrayUnion(user.uid))
                batch.update(userRef, "classes", FieldValue.arrayUnion(code))
                rosterName?.let { batch.set(rosterRef, mapOf("displayName" to it)) }
            }.await()
        }
    }

    suspend fun leave(code: String, uid: String) {
        val classRef = classroom(code)
        db.runBatch { batch ->
            batch.update(profile(uid), "classes", FieldValue.arrayRemove(code))
            batch.update(classRef, "members", FieldValue.arrayRemove(uid))
            batch.delete(classRef.collection("roster").document(uid))
        }.await()
    }

    suspend fun archive(code: String) {
        val uid = checkNotNull(auth.currentUser?.uid)
        val classRef = classroom(code)
        db.runTransaction { transaction ->
            val doc = transaction.get(classRef)
            check(doc.getString("creatorId") == uid)
            if (doc.getTimestamp("archivedAt") == null) {
                transaction.update(classRef, "archivedAt", FieldValue.serverTimestamp())
            }
        }.await()
    }

    suspend fun role(code: String, uid: String): ClassRole? {
        val doc = classroom(code).get().await()
        return when {
            doc.getTimestamp("archivedAt") != null -> null
            uid == doc.getString("creatorId") -> ClassRole.CREATOR
            uid in doc.memberIds("teachers") -> ClassRole.TEACHER
            uid in doc.memberIds("members") -> ClassRole.STUDENT
            else -> null
        }
    }

    suspend fun roster(code: String, uid: String): ClassroomRoster? {
        val classRef = classroom(code)
        val doc = classRef.get().await()
        if (!doc.includesMember(uid) || doc.getTimestamp("archivedAt") != null) return null
        val creatorId = doc.getString("creatorId")
        val teacherIds = doc.memberIds("teachers")
        val names = classRef.rosterNames()
        fun member(id: String) = ClassMember(id, names[id] ?: "Class member")

        return ClassroomRoster(
            name = doc.getString("name") ?: "Unknown Class",
            creator = creatorId?.takeIf { it.isNotBlank() }?.let(::member),
            teachers = teacherIds.map(::member),
            students = doc.memberIds("members").filter { it !in teacherIds && it != creatorId }.map(::member),
            canManage = !doc.metadata.isFromCache && !doc.metadata.hasPendingWrites() &&
                canManageClassMembers(uid, creatorId, teacherIds)
        )
    }

    enum class MemberAction { REMOVE_TEACHER, DEMOTE, REMOVE_STUDENT, PROMOTE }

    suspend fun changeMember(code: String, viewerId: String, memberId: String, action: MemberAction): Boolean {
        val classRef = classroom(code)
        val doc = classRef.get(Source.SERVER).await()
        if (auth.currentUser?.uid != viewerId || doc.getTimestamp("archivedAt") != null ||
            doc.metadata.hasPendingWrites() ||
            !canManageClassMembers(viewerId, doc.getString("creatorId"), doc.memberIds("teachers"))
        ) return false

        when (action) {
            MemberAction.REMOVE_TEACHER, MemberAction.REMOVE_STUDENT -> db.runBatch { batch ->
                val field = if (action == MemberAction.REMOVE_TEACHER) "teachers" else "members"
                batch.update(classRef, field, FieldValue.arrayRemove(memberId))
                batch.delete(classRef.collection("roster").document(memberId))
            }.await()
            MemberAction.DEMOTE -> classRef.update(
                "teachers", FieldValue.arrayRemove(memberId), "members", FieldValue.arrayUnion(memberId)
            ).await()
            MemberAction.PROMOTE -> classRef.update(
                "teachers", FieldValue.arrayUnion(memberId), "members", FieldValue.arrayRemove(memberId)
            ).await()
        }
        return true
    }

    suspend fun publishRosterName(code: String, uid: String) {
        classroom(code).publishRosterName(uid, profile(uid).get().await())
    }

    fun announcements(code: String) = classroom(code).collection("announcements").observeSnapshots().map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
            doc.data?.let {
                ClassAnnouncement(doc.id, it["title"].toString(), it["description"].toString(), (it["timestamp"] as? Timestamp)?.toDate()?.time)
            }
        }
    }

    fun resources(code: String) = classroom(code).collection("resources").observeSnapshots().map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
            doc.data?.let {
                ClassResource(
                    doc.id, it["name"] as? String ?: "Unnamed", it["uploadedBy"] as? String ?: "Unknown",
                    it["uploadedAt"] as? Long ?: 0L, it["cloudinaryLink"] as? String ?: ""
                )
            }
        }
    }

    suspend fun postAnnouncement(code: String, title: String, description: String, uid: String) {
        classroom(code).collection("announcements")
            .add(mapOf("title" to title, "description" to description, "posterId" to uid, "timestamp" to Date())).await()
    }

    data class ResourceDraft(val id: String, val name: String, val link: String, val uploader: String, val createdAt: Long)

    fun prepareResource(code: String, name: String, link: String): ResourceDraft {
        val user = checkNotNull(auth.currentUser)
        return ResourceDraft(
            classroom(code).collection("resources").document().id,
            name, link, user.displayName ?: "Class member", System.currentTimeMillis()
        )
    }

    suspend fun saveResource(code: String, draft: ResourceDraft) {
        classroom(code).collection("resources").document(draft.id).set(
            mapOf("name" to draft.name, "cloudinaryLink" to draft.link, "uploadedBy" to draft.uploader, "uploadedAt" to draft.createdAt)
        ).await()
    }
}

// Keep the private index order and limit outstanding reads for accounts with large class lists.
internal suspend fun loadClassroomIndex(codes: List<String>, load: suspend (String) -> Classroom?): List<Classroom> =
    coroutineScope {
        codes.distinct().chunked(4).flatMap { batch ->
            batch.map { code ->
                async {
                    try {
                        load(code)
                    } catch (e: FirebaseFirestoreException) {
                        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) null else throw e
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }
