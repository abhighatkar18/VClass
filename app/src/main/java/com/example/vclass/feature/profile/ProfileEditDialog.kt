package com.example.vclass.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.vclass.core.ui.AppSpacing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = name, onValueChange = { name = it }, label = { Text("Full Name") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = userClass, onValueChange = { userClass = it }, label = { Text("Class") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = address, onValueChange = { address = it }, label = { Text("Address") })
                OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = phone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), onValueChange = { phone = it }, label = { Text("Phone Number") })
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = { onSave(name, userClass, rollNo, address, phone) }) {
                Text(if (saving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
