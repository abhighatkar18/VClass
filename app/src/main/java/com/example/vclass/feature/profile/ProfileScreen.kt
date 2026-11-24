package com.example.vclass.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.data.firebase.ProfileDetails
import com.example.vclass.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text(text = "Profile", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.loading) {
                CircularProgressIndicator(color = Color.Black)
            } else if (profile == null || details == null) {
                Text("Could not load your profile.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = model::load) { Text("Retry") }
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable(enabled = !saving) { pickImageLauncher.launch("image/*") }
                ) {
                    profile.photo?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Image(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = details.name, color = Color.Black, fontSize = 24.sp)
                Text(text = "Class: ${details.className} | Roll No: ${details.rollNumber}", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Address: ${details.address}", color = Color.Black)
                Text("Phone: ${details.phone}", color = Color.Black)
                Spacer(modifier = Modifier.height(32.dp))
                if (!isEditing) state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(enabled = !saving, onClick = { model.clearSaveError(); isEditing = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                    Text(text = "Edit Profile", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAboutUs = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text(text = "About Us", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { AppContainer.auth.signOut() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text(text = "Sign Out", color = Color.White)
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
