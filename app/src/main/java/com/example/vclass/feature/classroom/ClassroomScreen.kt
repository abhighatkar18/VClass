package com.example.vclass.feature.classroom

import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.core.ui.ContentState
import com.example.vclass.core.ui.valueOrNull
import com.example.vclass.di.AppContainer
import com.example.vclass.domain.Classroom



@Composable
internal fun ClassroomScreen() {
    val uid = AppContainer.auth.currentUserId ?: return
    val model: ClassroomViewModel = viewModel(key = "classrooms:$uid", factory = viewModelFactory {
        initializer { ClassroomViewModel { AppContainer.classrooms.list(uid) } }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val classes = state.valueOrNull().orEmpty()
    val isLoading = state == ContentState.Loading
    val errorMessage = "Could not load your classes.".takeIf { state == ContentState.Failed }
    var showDialog by remember { mutableStateOf(false) }
    var selectedClass by remember { mutableStateOf<Classroom?>(null) }
    var showClassDetails by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showManageMembers by remember { mutableStateOf(false) }
    var showPostDialog by remember { mutableStateOf(false) }
    var showClassOptions by remember { mutableStateOf(false) }

    fun closeClass() {
        selectedClass = null
        showClassOptions = false
        model.reload()
    }

    BackHandler(enabled = selectedClass != null) { closeClass() }
    if (selectedClass != null) {
        ClassDetailScreen(
            classCode = selectedClass!!.code,
            className = selectedClass!!.name,
            onBack = { closeClass() },
            onShowOptions = { showClassOptions = true },
            showClassOptions = showClassOptions,
            onDismissOptions = { showClassOptions = false },
            onViewDetails = { showClassDetails = true },
            onArchiveClass = { showArchiveDialog = true },
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
                    TextButton(onClick = { model.reload() }) { Text("Retry") }
                }

                if (classes.isEmpty() && !isLoading && errorMessage == null) {
                    Text(text = "You don't have any classes yet.", modifier = Modifier.padding(16.dp))
                }

                LazyColumn {
                    items(classes, key = { it.code }) { classroom ->
                        val (code, name) = classroom
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    selectedClass = classroom
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
                onClassCreated = { model.reload() }
            )
        }

    }

    if (showArchiveDialog && selectedClass != null) {
        ArchiveClassDialog(
            classCode = selectedClass!!.code,
            onDismiss = { showArchiveDialog = false },
            onArchived = {
                showArchiveDialog = false
                closeClass()
            }
        )
    }
    if (showClassDetails && selectedClass != null) {
        ClassDetailsDialog(
            classCode = selectedClass!!.code,
            onDismiss = { showClassDetails = false }
        )
    }

    if (showManageMembers && selectedClass != null) {
        ManageMembersDialog(
            classCode = selectedClass!!.code,
            onDismiss = { showManageMembers = false }
        )
    }

    if (showPostDialog && selectedClass != null) {
        PostAnnouncementDialog(
            classCode = selectedClass!!.code,
            onDismiss = { showPostDialog = false }
        )
    }
}
