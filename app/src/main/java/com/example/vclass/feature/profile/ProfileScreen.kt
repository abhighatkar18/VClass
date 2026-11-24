package com.example.vclass.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.core.ui.AppTopBar
import com.example.vclass.data.firebase.ProfileDetails
import com.example.vclass.di.AppContainer

@Composable
fun ProfileScreen() {
    val uid = AppContainer.auth.currentUserId ?: return
    val context = LocalContext.current
    val model: ProfileViewModel = viewModel(key = "profile:$uid", factory = viewModelFactory {
        initializer { ProfileViewModel(uid, AppContainer.profiles(context)) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val profile = state.profile
    val details = profile?.details
    val saving = state.saving
    var isEditing by remember { mutableStateOf(false) }
    var showAboutUs by remember { mutableStateOf(false) }
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) model.savePhoto(uri)
    }

    LaunchedEffect(model) { model.load() }
    LaunchedEffect(state.editSaved) {
        if (state.editSaved) {
            model.consumeEditSaved()
            isEditing = false
        }
    }
    Scaffold(
        topBar = { AppTopBar("Profile") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.loading) {
                CircularProgressIndicator()
            } else if (profile == null || details == null) {
                Text("Could not load your profile.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = model::load) { Text("Retry") }
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(enabled = !saving, onClickLabel = "Change profile picture", role = Role.Button) { pickImageLauncher.launch("image/*") }
                ) {
                    profile.photo?.let {
                        Image(
                            bitmap = remember(it) { it.asImageBitmap() },
                            contentScale = ContentScale.Crop,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = details.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(text = "Class: ${details.className} | Roll No: ${details.rollNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Address: ${details.address}")
                Text("Phone: ${details.phone}")
                Spacer(modifier = Modifier.height(32.dp))
                if (saving && !isEditing) Text("Saving profile picture…")
                if (!isEditing) state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(enabled = !saving, onClick = { model.clearSaveError(); isEditing = true }, modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                    Text(text = "Edit Profile")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAboutUs = true },
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                ) {
                    Text(text = "About Us")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { AppContainer.auth.signOut() }, modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
                Text(text = "Sign Out")
            }
        }
    }

    if (isEditing && details != null) {
        ProfileEditDialog(
            currentName = details.name,
            currentClass = details.className,
            currentRollNo = details.rollNumber,
            currentAddress = details.address,
            currentPhone = details.phone,
            saving = saving,
            errorMessage = state.saveError,
            onDismiss = { isEditing = false; model.clearSaveError() },
            onSave = { name, className, rollNo, address, phone ->
                model.saveDetails(ProfileDetails(name, className, rollNo, address, phone))
            }
        )
    }

    if (showAboutUs) {
        AboutUsDialog(onDismiss = { showAboutUs = false })
    }
}
