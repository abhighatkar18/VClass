package com.example.vclass.domain

internal data class Classroom(val code: String, val name: String)

internal enum class ClassRole { CREATOR, TEACHER, STUDENT }

internal fun canManageClassMembers(viewerId: String?, creatorId: String?, teacherIds: List<String>): Boolean =
    !viewerId.isNullOrBlank() && (viewerId == creatorId || viewerId in teacherIds)

internal data class ClassMember(val id: String, val displayName: String)

internal data class ClassroomRoster(
    val name: String,
    val creator: ClassMember?,
    val teachers: List<ClassMember>,
    val students: List<ClassMember>,
    val canManage: Boolean
)

internal data class ClassroomAssignment(val id: String, val title: String, val dueDate: Long?, val createdBy: String?)

internal data class ClassAnnouncement(val id: String, val title: String, val description: String, val postedAt: Long?)

internal data class ClassResource(val id: String, val name: String, val uploadedBy: String, val uploadedAt: Long, val link: String)

internal data class Submission(val id: String, val submittedByName: String, val fileName: String, val fileUrl: String)
