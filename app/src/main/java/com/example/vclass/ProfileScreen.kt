package com.example.vclass

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.vclass.profilescreen.AboutUsDialog
import com.example.vclass.profilescreen.ProfileEditDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = user.uid
    val profileRef = db.collection("users").document(userId)
    val scope = rememberCoroutineScope()

    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userName by remember { mutableStateOf("") }
    var userClass by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showAboutUs by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && !saving) {
            saving = true
            saveError = null
            scope.launch {
                try {
                    val (bitmap, encoded) = withContext(Dispatchers.IO) {
                        val image = readProfileImage(context.contentResolver, uri)
                        val bytes = ByteArrayOutputStream()
                        check(image.compress(Bitmap.CompressFormat.JPEG, 85, bytes))
                        val encoded = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
                        require(encoded.length <= 850_000)
                        image to encoded
                    }
                    profileRef.update("profileImage", encoded).await()
                    profileBitmap = bitmap
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    saveError = "Could not save the photo. Try a different image or retry online."
                } finally {
                    saving = false
                }
            }
        }
    }

    LaunchedEffect(userId, reload) {
        isLoading = true
        loadError = null
        try {
            var doc = profileRef.get().await()
            if (!doc.exists()) {
                ensureUserProfile(db, user)
                doc = profileRef.get().await()
            }
            userName = doc.displayName().orEmpty()
            userClass = doc.getString("class").orEmpty()
            rollNo = doc.getString("rollNo").orEmpty()
            address = doc.getString("address").orEmpty()
            phoneNumber = doc.getString("phoneNumber").orEmpty()
            val encoded = doc.getString("profileImage")
            profileBitmap = withContext(Dispatchers.IO) {
                encoded?.let {
                    try {
                        val bytes = Base64.decode(it, Base64.DEFAULT)
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        if (options.outWidth <= 0 || options.outHeight <= 0) return@let null
                        options.inSampleSize = profileImageSampleSize(options.outWidth, options.outHeight)
                        options.inJustDecodeBounds = false
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadError = "Could not load your profile."
        } finally {
            isLoading = false
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
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black)
            } else if (loadError != null) {
                Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { reload++ }) { Text("Retry") }
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable(enabled = !saving) { pickImageLauncher.launch("image/*") }
                ) {
                    profileBitmap?.let {
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
                Text(text = userName, color = Color.Black, fontSize = 24.sp)
                Text(text = "Class: $userClass | Roll No: $rollNo", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Address: $address", color = Color.Black)
                Text("Phone: $phoneNumber", color = Color.Black)
                Spacer(modifier = Modifier.height(32.dp))
                if (!isEditing) saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(enabled = !saving, onClick = { saveError = null; isEditing = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
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
            Button(onClick = { FirebaseAuth.getInstance().signOut() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text(text = "Sign Out", color = Color.White)
            }
        }
    }

    if (isEditing) {
        ProfileEditDialog(
            currentName = userName,
            currentClass = userClass,
            currentRollNo = rollNo,
            currentAddress = address,
            currentPhone = phoneNumber,
            saving = saving,
            errorMessage = saveError,
            onDismiss = { isEditing = false; saveError = null },
            onSave = { name, uClass, rNo, addr, phone ->
                if (!saving) {
                    val fullName = name.trim()
                    if (fullName.length !in 1..120) {
                        saveError = "Enter a name between 1 and 120 characters."
                    } else {
                        saving = true
                        saveError = null
                        scope.launch {
                            try {
                                profileRef.update(
                                    mapOf("fullName" to fullName, "class" to uClass, "rollNo" to rNo, "address" to addr, "phoneNumber" to phone)
                                ).await()
                                userName = fullName
                                userClass = uClass
                                rollNo = rNo
                                address = addr
                                phoneNumber = phone
                                isEditing = false
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                saveError = "Could not save your profile. Please try again."
                            } finally {
                                saving = false
                            }
                        }
                    }
                }
            }
        )
    }

    if (showAboutUs) {
        AboutUsDialog(onDismiss = { showAboutUs = false })
    }
}

private fun readProfileImage(resolver: ContentResolver, uri: Uri): Bitmap {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    require(options.outWidth > 0 && options.outHeight > 0)
    options.inJustDecodeBounds = false
    options.inSampleSize = profileImageSampleSize(options.outWidth, options.outHeight)
    return requireNotNull(resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) })
}

private fun profileImageSampleSize(width: Int, height: Int): Int {
    val longestEdge = maxOf(width, height)
    var sampleSize = 1
    while (longestEdge / sampleSize > 1024) {
        sampleSize *= 2
    }
    return sampleSize
}

@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}
