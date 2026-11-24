package com.example.vclass.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


@Composable
fun ProfileEditDialog(currentName: String, currentClass: String, currentRollNo: String, currentAddress: String, currentPhone: String, saving: Boolean, errorMessage: String?, onDismiss: () -> Unit, onSave: (String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    var userClass by remember { mutableStateOf(currentClass) }
    var rollNo by remember { mutableStateOf(currentRollNo) }
    var address by remember { mutableStateOf(currentAddress) }
    var phone by remember { mutableStateOf(currentPhone) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(text = "Edit Profile") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
                TextField(value = userClass, onValueChange = { userClass = it }, label = { Text("Class") })
                TextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") })
                TextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") })
                errorMessage?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = { onSave(name, userClass, rollNo, address, phone) }) {
                Text(if (saving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            Button(enabled = !saving, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
