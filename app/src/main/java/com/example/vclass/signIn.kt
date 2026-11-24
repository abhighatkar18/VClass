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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class signIn : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignupScreen(this)
        }
    }
}

@Composable
fun SignupScreen(activity: signIn, viewModel: SignupViewModel = viewModel()) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

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
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError
                )
                if (nameError) {
                    Text("Name cannot be empty", color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = false
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = emailError
                )
                if (emailError) {
                    Text("Email cannot be empty", color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = passwordError
                )
                if (passwordError) {
                    Text("Password cannot be empty", color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        nameError = name.isBlank()
                        emailError = email.isBlank()
                        passwordError = password.isBlank()

                        if (!nameError && !emailError && !passwordError) {
                            loading = true
                            viewModel.registerUser(name, email, password) { success, message ->
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text(text = if (loading) "Signing up..." else "Sign Up", color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                ClickableText(
                    text = AnnotatedString("Already have an account? Log in"),
                    onClick = {
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    style = androidx.compose.ui.text.TextStyle(color = Color.Black)
                )
            }
        }
    }
}

class SignupViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun registerUser(name: String, email: String, password: String, callback: (Boolean, String) -> Unit) {
        val displayName = name.trim()
        val emailAddress = email.trim()
        if (displayName.length !in 1..120 || !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches() || password.length < 6) {
            callback(false, "Enter a name, valid email, and password of at least six characters.")
            return
        }

        viewModelScope.launch {
            val user = try {
                checkNotNull(auth.createUserWithEmailAndPassword(emailAddress, password).await().user)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                callback(false, "Could not create the account. Check your details or try signing in.")
                return@launch
            }
            try {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Firestore can still save the name if updating the Auth display name fails.
            }
            try {
                ensureUserProfile(firestore, user, displayName)
                callback(true, "Signup successful!")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                callback(true, "Account created. Open Profile to retry setup.")
            }
        }
    }
}
