package com.example.vclass.HomeScreen

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun DeleteClass(classCode: String, onDismiss: () -> Unit, onClassDeleted: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Class") },
        text = { Text("Are you sure you want to delete this class? This action cannot be undone.") },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    val classDoc = db.collection("classes").document(classCode).get().await()
                    val creatorId = classDoc.getString("creatorId")

                    if (user?.uid == creatorId) {
                        db.collection("classes").document(classCode).delete().await()
                        val usersQuery = db.collection("users").whereArrayContains("classes", classCode).get().await()
                        for (doc in usersQuery.documents) {
                            db.collection("users").document(doc.id)
                                .update("classes", doc["classes"].let { (it as List<*>).filter { id -> id != classCode } })
                        }
                        Toast.makeText(context, "Class deleted successfully", Toast.LENGTH_SHORT).show()
                        onClassDeleted()
                    } else {
                        Toast.makeText(context, "Only the creator can delete this class", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            }) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
