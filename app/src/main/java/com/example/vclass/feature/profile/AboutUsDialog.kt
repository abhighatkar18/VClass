package com.example.vclass.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun AboutUsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("About VClass", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "VClass is an innovative educational platform designed to enhance learning experiences. It provides a seamless and interactive environment where students and educators can connect, share knowledge, and collaborate effectively.",
                    textAlign = TextAlign.Justify,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Key Features:", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text("✔️ Live Interactive Sessions", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("✔️ AI-powered Learning Assistance", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("✔️ Easy Resource Sharing", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("✔️ Collaborative Learning Tools", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("✔️ Secure & User-Friendly Interface", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Created by Abhishek Ghatkar",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Committed to revolutionizing education.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
