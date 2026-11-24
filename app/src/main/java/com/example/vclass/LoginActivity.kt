package com.example.vclass

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(this)
        }
    }
}

@Composable
fun LoginScreen(activity: LoginActivity, viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            modifier = Modifier.padding(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VClass",
                    fontSize = 30.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ClickableText(
                    text = AnnotatedString("Forgot Password?"),
                    onClick = {
                        if (email.isNotEmpty()) {
                            viewModel.resetPassword(email) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Enter email to reset password", Toast.LENGTH_LONG).show()
                        }
                    },
                    style = androidx.compose.ui.text.TextStyle(color = Color.Blue, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_LONG).show()
                        } else {
                            loading = true
                            viewModel.loginUser(email, password) { success, message ->
                                loading = false
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                if (success) {
                                    val intent = Intent(context, Dashboard::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    context.startActivity(intent)
                                    activity.finish()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black
                    )
                ) {
                    Text(text = if (loading) "Logging in..." else "Login", color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                ClickableText(
                    text = AnnotatedString("Don't have an account? Sign up"),
                    onClick = {
                        val intent = Intent(context, signIn::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    style = androidx.compose.ui.text.TextStyle(color = Color.Black)
                )
            }
        }
    }
}

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    fun loginUser(email: String, password: String, callback: (Boolean, String) -> Unit) {
        val address = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(address).matches() || password.isEmpty()) {
            callback(false, "Enter a valid email and your password.")
            return
        }
        auth.signInWithEmailAndPassword(address, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, "Login Successful!")
            } else {
                callback(false, "Could not sign in. Check your details and connection.")
            }
        }
    }

    fun resetPassword(email: String, callback: (Boolean, String) -> Unit) {
        val address = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
            callback(false, "Enter a valid email address.")
            return
        }
        auth.sendPasswordResetEmail(address).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, "Password reset email sent!")
            } else {
                callback(false, "Could not send the reset email. Please try again.")
            }
        }
    }
}
