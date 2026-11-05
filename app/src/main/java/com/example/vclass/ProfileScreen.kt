package com.example.vclass

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
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
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = user?.uid ?: ""

    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userName by remember { mutableStateOf("") }
    var userClass by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showAboutUs by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, selectedUri)
            val base64Image = bitmapToBase64(bitmap)
            saveProfilePictureToFirestore(userId, base64Image)
            profileBitmap = bitmap
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userName = doc.getString("fullName") ?: ""
                    userClass = doc.getString("class") ?: ""
                    rollNo = doc.getString("rollNo") ?: ""
                    address = doc.getString("address") ?: ""
                    phoneNumber = doc.getString("phoneNumber") ?: ""

                    val base64Image = doc.getString("profileImage")
                    if (!base64Image.isNullOrEmpty()) {
                        profileBitmap = base64ToBitmap(base64Image)
                    }
                    isLoading = false
                }
            }
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
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable { pickImageLauncher.launch("image/*") }
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

                Button(onClick = { isEditing = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
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
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    context.startActivity(Intent(context, signIn::class.java))
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text(text = "Sign Out", color = Color.White)
                }
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
            onDismiss = { isEditing = false },
            onSave = { name, uClass, rNo, addr, phone ->
                db.collection("users").document(userId).update(
                    mapOf("fullName" to name, "class" to uClass, "rollNo" to rNo, "address" to addr, "phoneNumber" to phone)
                )
                userName = name
                userClass = uClass
                rollNo = rNo
                address = addr
                phoneNumber = phone
                isEditing = false
            }
        )
    }

    if (showAboutUs) {
        AboutUsDialog(onDismiss = { showAboutUs = false })
    }
}

/** Convert Bitmap to Base64 String */
fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

/** Convert Base64 String to Bitmap */
fun base64ToBitmap(base64String: String): Bitmap {
    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}

/** Save Profile Picture as Base64 in Firestore */
fun saveProfilePictureToFirestore(userId: String, base64Image: String) {
    if (userId.isNotEmpty()) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        userRef.update("profileImage", base64Image)
            .addOnSuccessListener { println("✅ Profile picture saved successfully!") }
            .addOnFailureListener { e -> println("❌ Error saving profile picture: ${e.message}") }
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}
