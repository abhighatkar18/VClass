package com.example.vclass.HomeScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Member(
    val id: String,
    val data: Map<String, Any>
)

@Composable
fun ManageMembersDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var creatorId by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf<Member?>(null) }
    var teachers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var students by remember { mutableStateOf<List<Member>>(emptyList()) }
    var selectedMember by remember { mutableStateOf<Member?>(null) }

    fun refreshData() {
        coroutineScope.launch {
            val classDoc = db.collection("classes").document(classCode).get().await()
            creatorId = classDoc.getString("creatorId") ?: ""

            val teacherIds = classDoc.get("teachers") as? List<String> ?: emptyList()
            val memberIds = classDoc.get("members") as? List<String> ?: emptyList()

            val teacherList = mutableListOf<Member>()
            val studentList = mutableListOf<Member>()

            for (id in teacherIds) {
                val userDoc = db.collection("users").document(id).get().await()
                teacherList.add(Member(id, userDoc.data ?: emptyMap()))
            }

            for (id in memberIds) {
                if (!teacherIds.contains(id) && id != creatorId) {
                    val userDoc = db.collection("users").document(id).get().await()
                    studentList.add(Member(id, userDoc.data ?: emptyMap()))
                }
            }

            // Fetch creator details
            val creatorDoc = db.collection("users").document(creatorId).get().await()
            creator = Member(creatorId, creatorDoc.data ?: emptyMap())

            teachers = teacherList
            students = studentList
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    if (selectedMember != null) {
        MemberDetailsDialog(
            member = selectedMember!!,
            onDismiss = { selectedMember = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Members", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Creator", style = MaterialTheme.typography.titleMedium)
                creator?.let {
                    MemberItem(
                        member = it,
                        onDetails = { selectedMember = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Teachers", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(teachers) { teacher ->
                        MemberItem(
                            member = teacher,
                            onDetails = { selectedMember = it },
                            onRemove = {
                                coroutineScope.launch {
                                    val classRef = db.collection("classes").document(classCode)
                                    classRef.update("teachers", FieldValue.arrayRemove(teacher.id)).await()
                                    refreshData()
                                }
                            },
                            onDemote = {
                                coroutineScope.launch {
                                    val classRef = db.collection("classes").document(classCode)
                                    classRef.update("teachers", FieldValue.arrayRemove(teacher.id)).await()
                                    classRef.update("members", FieldValue.arrayUnion(teacher.id)).await()
                                    refreshData()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Students", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(students) { student ->
                        MemberItem(
                            member = student,
                            onDetails = { selectedMember = it },
                            onRemove = {
                                coroutineScope.launch {
                                    val classRef = db.collection("classes").document(classCode)
                                    classRef.update("members", FieldValue.arrayRemove(student.id)).await()
                                    refreshData()
                                }
                            },
                            onPromote = {
                                coroutineScope.launch {
                                    val classRef = db.collection("classes").document(classCode)
                                    classRef.update("teachers", FieldValue.arrayUnion(student.id)).await()
                                    classRef.update("members", FieldValue.arrayRemove(student.id)).await()
                                    refreshData()
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Close")
            }
        }
    )
}

@Composable
fun MemberItem(
    member: Member,
    onDetails: (Member) -> Unit,
    onRemove: (() -> Unit)? = null,
    onPromote: (() -> Unit)? = null,
    onDemote: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onDetails(member) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(member.data["fullName"] as? String ?: "N/A", style = MaterialTheme.typography.bodyLarge)
                Text("Class: ${member.data["class"] as? String ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                Text("Roll No: ${member.data["rollNo"] as? String ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                onPromote?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Promote to Teacher")
                    }
                }
                onDemote?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.PersonRemove, contentDescription = "Demote to Student")
                    }
                }
                onRemove?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from Class")
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDetailsDialog(member: Member, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Member Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val data = member.data
                Text("Full Name: ${data["fullName"] ?: "N/A"}")
                Text("Name: ${data["name"] ?: "N/A"}")
                Text("Email: ${data["email"] ?: "N/A"}")
                Text("Class: ${data["class"] ?: "N/A"}")
                Text("Roll No: ${data["rollNo"] ?: "N/A"}")
                Text("Phone: ${data["phoneNumber"] ?: "N/A"}")
                Text("Address: ${data["address"] ?: "N/A"}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}