package com.example.vclass.feature.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.di.AppContainer
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
internal fun ChatbotScreen() {
    val model: ChatViewModel = viewModel(factory = viewModelFactory {
        initializer { ChatViewModel(AppContainer.newChat()::generateReply) }
    })
    val messages by model.messageList.collectAsStateWithLifecycle()
    ChatbotContent(Modifier, messages, model::sendMessage)
}

@Composable
private fun ChatbotContent(modifier: Modifier, messages: List<MessageModel>, onSend: (String) -> Unit) {
    Column(modifier.fillMaxSize()) {
        Header()
        MessageList(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            messageList = messages
        )
        MessageInput(
            onMessageSend = { message ->
                onSend(message)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header() {
    TopAppBar(
        title = {
            Text(
                text = "Chatbot",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        modifier = Modifier.statusBarsPadding(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
}

@Composable
private fun MessageInput(onMessageSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Type a message...", color = Color.Black) },
            singleLine = true
        )
        IconButton(
            onClick = {
                if (message.isNotBlank()) {
                    onMessageSend(message)
                    message = ""
                }
            },
            modifier = Modifier
                .padding(8.dp)
                .background(Color.Black, shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
        }
    }
}

@Composable
private fun MessageList(modifier: Modifier, messageList: List<MessageModel>) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(messageList) { messageModel ->
            MessageRow(messageModel)
        }
    }
}

@Composable
private fun MessageRow(messageModel: MessageModel) {
    val isModel = messageModel.role == "model"
    val alignment = if (isModel) Alignment.Start else Alignment.End
    val cleanMessage = messageModel.message.replace("*", "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .background(if (isModel) Color.White else Color.Black, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(
                text = cleanMessage,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (isModel) Color.Black else Color.White
            )
        }
    }
}
