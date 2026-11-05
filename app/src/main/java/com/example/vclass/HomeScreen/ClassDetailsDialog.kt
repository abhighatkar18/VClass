package com.example.vclass.HomeScreen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@Composable
fun ClassDetailsDialog(classCode: String, onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var className by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(classCode) {
        val classDoc = db.collection("classes").document(classCode).get().await()
        className = classDoc.getString("name") ?: "Unknown Class"
        val memberIds = classDoc.get("members") as? List<String> ?: emptyList()
        members = memberIds.mapNotNull { memberId ->
            val userDoc = db.collection("users").document(memberId).get().await()
            userDoc.getString("name") ?: "Unknown User"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Class Details: $className") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Class Code: $classCode", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Class Code", classCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Class Code Copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy Code")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Members:", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                LazyColumn {
                    items(members.size) { index ->
                        Text(text = members[index], fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss,colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Text("Close")
            }
        }
    )
}
