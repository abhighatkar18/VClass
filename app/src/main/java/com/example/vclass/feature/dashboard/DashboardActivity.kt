package com.example.vclass.feature.dashboard
import android.app.NotificationManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import com.example.vclass.core.ui.theme.VclassTheme
import com.example.vclass.di.AppContainer
import com.example.vclass.feature.calendar.CalendarScreen
import com.example.vclass.feature.chatbot.ChatbotScreen
import com.example.vclass.feature.classroom.ClassroomScreen
import com.example.vclass.feature.profile.ProfileScreen
import com.example.vclass.navigation.AppNavigation

class DashboardActivity : ComponentActivity() {
    private var session: AutoCloseable? = null

    private fun onSessionChanged(uid: String?) {
        if (uid == null && !isFinishing) {
            getSystemService(NotificationManager::class.java).cancelAll()
            AppNavigation.openSignedOut(this)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppContainer.auth.currentUserId == null) {
            onSessionChanged(null)
            return
        }
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            VclassTheme {
                DashboardScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isFinishing) session = AppContainer.auth.observeSession(::onSessionChanged)
    }

    override fun onStop() {
        session?.close()
        session = null
        super.onStop()
    }
}
private enum class DashboardTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    CALENDAR("Calendar", Icons.Filled.Event),
    CHATBOT("Chatbot", Icons.Filled.SmartToy),
    PROFILE("Profile", Icons.Filled.Person)
}

@Composable
private fun DashboardScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            if (!keyboardVisible) {
                NavigationBar {
                    DashboardTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (selectedTab) {
                DashboardTab.HOME -> ClassroomScreen()
                DashboardTab.CALENDAR -> CalendarScreen()
                DashboardTab.CHATBOT -> ChatbotScreen()
                DashboardTab.PROFILE -> ProfileScreen()
            }
        }
    }
}
