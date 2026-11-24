package com.example.vclass.feature.chatbot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.core.ui.AppSpacing
import com.example.vclass.core.ui.AppTopBar
import com.example.vclass.core.ui.EmptyContent
import com.example.vclass.di.AppContainer

@Composable
internal fun ChatbotScreen() {
    val model: ChatViewModel = viewModel(factory = viewModelFactory {
        initializer { ChatViewModel(AppContainer.newChat()::generateReply) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    ChatbotContent(state, model::sendMessage)
}

@Composable
internal fun ChatbotContent(state: ChatUiState, onSend: (String) -> Boolean) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Chatbot")
        MessageList(Modifier.weight(1f), state.messages)
        if (state.sending) {
            Text(
                "Waiting for reply…",
                Modifier.padding(horizontal = AppSpacing.medium).semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium))
        }
        MessageInput(state.sending, onSend)
    }
}

@Composable
private fun MessageInput(sending: Boolean, onSend: (String) -> Boolean) {
    var message by rememberSaveable { mutableStateOf("") }
    val submit: () -> Unit = {
        if (!sending && onSend(message)) message = ""
    }
    Row(
        Modifier.fillMaxWidth().padding(AppSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() })
        )
        FilledIconButton(onClick = submit, enabled = !sending && message.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
        }
    }
}

@Composable
private fun MessageList(modifier: Modifier, messages: List<MessageModel>) {
    // Follow new messages if the reader was at the end of the transcript.
    val listState = rememberLazyListState()
    var previousCount by rememberSaveable { mutableIntStateOf(messages.size) }
    LaunchedEffect(messages.size) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (messages.size > previousCount && lastVisibleIndex >= previousCount - 1) {
            listState.scrollToItem(messages.lastIndex)
        }
        previousCount = messages.size
    }
    if (messages.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmptyContent("Ask a study question to start the conversation.")
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        contentPadding = PaddingValues(AppSpacing.medium)
    ) {
        // Transcript entries only append, so their original indices stay stable.
        items(messages.size, key = { it }) { index ->
            MessageRow(messages[index])
        }
    }
}

@Composable
private fun MessageRow(message: MessageModel) {
    val isModel = message.role == "model"
    val colors = MaterialTheme.colorScheme
    val cleanMessage = remember(message.message) { message.message.replace("*", "") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(0.9f),
            shape = MaterialTheme.shapes.medium,
            color = if (isModel) colors.surfaceVariant else colors.primary,
            contentColor = if (isModel) colors.onSurfaceVariant else colors.onPrimary
        ) {
            Text(cleanMessage, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
