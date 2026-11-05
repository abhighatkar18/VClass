package com.example.vclass.HomeScreen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@Composable
fun CreateJoinClassDialog(
    onDismiss: () -> Unit,
    db: FirebaseFirestore,
    user: FirebaseUser?,
    context: android.content.Context,
    onClassCreated: () -> Unit
) {
    var className by remember { mutableStateOf("") }
    var classCode by remember { mutableStateOf("") }
    var isCreateMode by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create or Join Class") },
        text = {
            Column {
                Row {
                    Button(onClick = { isCreateMode = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                        Text("Create", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { isCreateMode = false }, modifier = Modifier.weight(1f),colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                        Text("Join",color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isCreateMode) {
                    TextField(value = className, onValueChange = { className = it }, label = { Text("Class Name") })
                } else {
                    TextField(value = classCode, onValueChange = { classCode = it }, label = { Text("Class Code") })
                }
            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black),onClick = {
                user?.uid?.let { userId ->
                    if (isCreateMode && className.isNotEmpty()) {
                        val generatedCode = UUID.randomUUID().toString().substring(0, 6)
                        val classRef = db.collection("classes").document(generatedCode)

                        // Store class with only members and creator
                        val classData = mapOf(
                            "name" to className,
                            "members" to listOf(userId),  // Only creator initially
                            "creatorId" to userId
                        )

                        classRef.set(classData).addOnSuccessListener {
                            // Add class to the user’s document
                            db.collection("users").document(userId)
                                .update("classes", FieldValue.arrayUnion(generatedCode))
                                .addOnSuccessListener {
                                    onClassCreated()
                                    Toast.makeText(context, "Class Created! Code: $generatedCode", Toast.LENGTH_LONG).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to add class to user", Toast.LENGTH_SHORT).show()
                                }
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to create class", Toast.LENGTH_SHORT).show()
                        }
                    } else if (!isCreateMode && classCode.isNotEmpty()) {
                        val classRef = db.collection("classes").document(classCode)
                        classRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                // Add user to the class
                                classRef.update("members", FieldValue.arrayUnion(userId))
                                    .addOnSuccessListener {
                                        // Add class to the user’s document
                                        db.collection("users").document(userId)
                                            .update("classes", FieldValue.arrayUnion(classCode))
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Joined Class", Toast.LENGTH_SHORT).show()
                                                onClassCreated()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Failed to add class to user", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to join class", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, "Invalid Class Code", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(context, "Error getting class", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                onDismiss()
            }) {
                Text(if (isCreateMode) "Create" else "Join", color = Color.White)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss,colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Text("Cancel")
            }
        }
    )
}
