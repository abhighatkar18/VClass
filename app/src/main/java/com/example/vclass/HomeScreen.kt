package com.example.vclass

import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.vclass.HomeScreen.ClassDetailsDialog
import com.example.vclass.HomeScreen.ClassViewScreen
import com.example.vclass.HomeScreen.CreateJoinClassDialog
import com.example.vclass.HomeScreen.DeleteClass
import com.example.vclass.HomeScreen.ManageMembersDialog
import com.example.vclass.HomeScreen.PostAnnouncementDialog
import com.example.vclass.HomeScreen.includesMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun HomeScreen() {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var classes by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedClass by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showClassDetails by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManageMembers by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPostDialog by remember { mutableStateOf(false) }
    var viewingClass by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showClassOptions by remember { mutableStateOf(false) }

    fun fetchUserClasses() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            user?.uid?.let { userId ->
                try {
                    val userDoc = db.collection("users").document(userId).get().await()
                    val classCodes = (userDoc.get("classes") as? List<*>).orEmpty().filterIsInstance<String>()
                    val classList = mutableListOf<Pair<String, String>>()

                    for (code in classCodes) {
                        val classDoc = try {
                            db.collection("classes").document(code).get().await()
                        } catch (e: FirebaseFirestoreException) {
                            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) continue
                            throw e
                        }
                        if (classDoc.exists() && classDoc.getTimestamp("archivedAt") == null && classDoc.includesMember(userId)) {
                            val name = classDoc.getString("name") ?: "Unknown Class"
                            classList.add(code to name)
                        }
                    }

                    classes = classList
                    isLoading = false
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    isLoading = false
                    errorMessage = "Could not load your classes."
                }
            } ?: run { isLoading = false }
        }
    }

    LaunchedEffect(user?.uid) {
        fetchUserClasses()
    }

    fun closeClass() {
        viewingClass = null
        selectedClass = null
        showClassOptions = false
        fetchUserClasses()
    }

    BackHandler(enabled = viewingClass != null) { closeClass() }
    if (viewingClass != null) {
        ClassViewScreen(
            classCode = viewingClass!!.first,
            className = viewingClass!!.second,
            onBack = { closeClass() },
            onShowOptions = { showClassOptions = true },
            showClassOptions = showClassOptions,
            onDismissOptions = { showClassOptions = false },
            onViewDetails = { showClassDetails = true },
            onDeleteClass = { showDeleteDialog = true },
            onManageMembers = { showManageMembers = true },
            onPostAnnouncement = { showPostDialog = true }
        )
    }  else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create or Join Class",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        ) { contentPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Text(text = "Your Classes", fontSize = 24.sp, modifier = Modifier.padding(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    TextButton(onClick = { fetchUserClasses() }) { Text("Retry") }
                }

                if (classes.isEmpty() && !isLoading && errorMessage == null) {
                    Text(text = "You don't have any classes yet.", modifier = Modifier.padding(16.dp))
                }

                LazyColumn {
                    items(classes.size) { index ->
                        val (code, name) = classes[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    viewingClass = code to name
                                    selectedClass = code to name
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = name, fontSize = 18.sp)
                                Text(text = "Code: $code", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            CreateJoinClassDialog(
                onDismiss = { showDialog = false },
                db = db,
                user = user,
                context = context,
                onClassCreated = { fetchUserClasses() }
            )
        }

    }

    if (showDeleteDialog && selectedClass != null) {
        DeleteClass(
            classCode = selectedClass!!.first,
            onDismiss = { showDeleteDialog = false },
            onClassDeleted = {
                showDeleteDialog = false
                closeClass()
            }
        )
    }
    if (showClassDetails && selectedClass != null) {
        ClassDetailsDialog(
            classCode = selectedClass!!.first,
            onDismiss = { showClassDetails = false }
        )
    }

    if (showManageMembers && selectedClass != null) {
        ManageMembersDialog(
            classCode = selectedClass!!.first,
            onDismiss = { showManageMembers = false }
        )
    }

    if (showPostDialog && selectedClass != null) {
        PostAnnouncementDialog(
            classCode = selectedClass!!.first,
            onDismiss = { showPostDialog = false }
        )
    }
}
