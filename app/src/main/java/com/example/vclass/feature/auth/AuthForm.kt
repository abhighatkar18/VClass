package com.example.vclass.feature.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.core.ui.AppSpacing
import com.example.vclass.core.ui.theme.BrandBackground
import com.example.vclass.core.ui.theme.BrandForeground
import com.example.vclass.di.AppContainer

@Composable
internal fun AuthForm(signUp: Boolean, onSignedIn: () -> Unit, onSwitchMode: () -> Unit) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val model: AuthViewModel = viewModel(factory = viewModelFactory {
        initializer { AuthViewModel(AppContainer.auth) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.result) {
        val result = state.result?.takeIf { it.signedIn } ?: return@LaunchedEffect
        model.consumeResult()
        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
        onSignedIn()
    }

    val action = if (signUp) "Sign Up" else "Login"
    val submit: () -> Unit = {
        if (!state.loading) {
            keyboard?.hide()
            if (signUp) model.register(name, email, password) else model.login(email, password)
        }
    }

    Surface(Modifier.fillMaxSize(), color = BrandBackground, contentColor = BrandForeground) {
        BoxWithConstraints(Modifier.safeDrawingPadding().imePadding()) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight).padding(AppSpacing.medium),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(AppSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("VClass", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)

                        if (signUp) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    model.consumeResult()
                                },
                                label = { Text("Full Name") },
                                singleLine = true,
                                enabled = !state.loading,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                model.consumeResult()
                            },
                            label = { Text("Email") },
                            singleLine = true,
                            enabled = !state.loading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                model.consumeResult()
                            },
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = !state.loading,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!signUp) {
                            TextButton(
                                onClick = {
                                    keyboard?.hide()
                                    model.resetPassword(email)
                                },
                                enabled = !state.loading
                            ) { Text("Forgot Password?") }
                        }

                        val result = state.result?.takeUnless { it.signedIn }
                        val feedback = result?.message ?: state.progress
                        if (feedback != null) {
                            Text(
                                feedback,
                                Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                                color = if (result?.isError == true) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(onClick = submit, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) {
                            Text(action)
                        }
                        TextButton(onClick = onSwitchMode, enabled = !state.loading) {
                            Text(if (signUp) "Already have an account? Log in" else "Don't have an account? Sign up")
                        }
                    }
                }
            }
        }
    }
}
