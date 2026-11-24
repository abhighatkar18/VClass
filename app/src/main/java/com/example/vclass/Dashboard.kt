package com.example.vclass
import android.content.Intent
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
import com.example.vclass.ui.theme.VclassTheme
import com.google.firebase.auth.FirebaseAuth

class Dashboard : ComponentActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val sessionListener = FirebaseAuth.AuthStateListener {
        if (it.currentUser == null && !isFinishing) {
            getSystemService(NotificationManager::class.java).cancelAll()
            startActivity(Intent(this, signIn::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) {
            sessionListener.onAuthStateChanged(auth)
            return
        }
        enableEdgeToEdge()
        setContent {
            VclassTheme {
                MainScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isFinishing) auth.addAuthStateListener(sessionListener)
    }

    override fun onStop() {
        auth.removeAuthStateListener(sessionListener)
        super.onStop()
    }
}
@Composable
fun MainScreen() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> HomeScreen()
                1 -> CalendarScreen()
                2 -> ChatbotScreenContent()
                3 -> ProfileScreen()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(icon = Icons.Filled.Home, label = "Home", selected = selectedTabIndex == 0) { selectedTabIndex = 0 }
            BottomNavItem(icon = Icons.Filled.Event, label = "Calendar", selected = selectedTabIndex == 1) { selectedTabIndex = 1 }
            BottomNavItem(icon = Icons.Filled.SmartToy, label = "Chatbot", selected = selectedTabIndex == 2) { selectedTabIndex = 2 }
            BottomNavItem(icon = Icons.Filled.Person, label = "Profile", selected = selectedTabIndex == 3) { selectedTabIndex = 3 }
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
        Text(text = label, fontSize = 12.sp)
    }
}
