package com.example.vclass.feature.auth

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.vclass.core.ui.theme.VclassTheme
import com.example.vclass.navigation.AppNavigation

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            VclassTheme {
                AuthForm(
                    signUp = false,
                    onSignedIn = { AppNavigation.openDashboard(this) },
                    onSwitchMode = { AppNavigation.openSignup(this) }
                )
            }
        }
    }
}
