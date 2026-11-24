package com.example.vclass.feature.dashboard
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        enableEdgeToEdge()
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
private enum class DashboardTab { HOME, CALENDAR, CHATBOT, PROFILE }

@Composable
private fun DashboardScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                DashboardTab.HOME -> ClassroomScreen()
                DashboardTab.CALENDAR -> CalendarScreen()
                DashboardTab.CHATBOT -> ChatbotScreen()
                DashboardTab.PROFILE -> ProfileScreen()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(icon = Icons.Filled.Home, label = "Home") { selectedTab = DashboardTab.HOME }
            BottomNavItem(icon = Icons.Filled.Event, label = "Calendar") { selectedTab = DashboardTab.CALENDAR }
            BottomNavItem(icon = Icons.Filled.SmartToy, label = "Chatbot") { selectedTab = DashboardTab.CHATBOT }
            BottomNavItem(icon = Icons.Filled.Person, label = "Profile") { selectedTab = DashboardTab.PROFILE }
        }
    }
}

@Composable
private fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
        Text(text = label, fontSize = 12.sp)
    }
}
