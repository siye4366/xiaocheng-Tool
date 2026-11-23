package com.xiaocheng.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaocheng.app.ChatMessage
import com.xiaocheng.app.MainViewModel
import com.xiaocheng.app.ui.theme.Amber
import com.xiaocheng.app.ui.theme.CardWhite
import com.xiaocheng.app.ui.theme.Coral
import com.xiaocheng.app.ui.theme.Ink
import com.xiaocheng.app.ui.theme.Line
import com.xiaocheng.app.ui.theme.Muted
import com.xiaocheng.app.ui.theme.Paper
import com.xiaocheng.app.ui.theme.Pine
import com.xiaocheng.app.ui.theme.SoftAmber
import com.xiaocheng.app.ui.theme.SoftCoral
import com.xiaocheng.app.ui.theme.SoftPine

@Composable
fun CombinedChat(vm: MainViewModel, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(vm.chatMessages.size, vm.chatLoading, vm.pendingActions.size) {
        if (vm.chatMessages.isNotEmpty()) {
            val index = vm.chatMessages.size + if (vm.pendingActions.isNotEmpty()) 1 else 0
            runCatching { listState.animateScrollToItem(index) }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("综合版 · 日程助手", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { vm.clearChat() },
                enabled = !vm.chatLoading,
            ) { Text("清空对话", color = Muted, fontSize = 12.sp) }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (vm.chatMessages.isEmpty()) {
                item { WelcomeCard { input = it } }
            }
            items(vm.chatMessages) { message -> ChatBubble(message) }
            if (vm.chatLoading) {
                item { TypingBubble() }
            }
            if (vm.pendingActions.isNotEmpty()) {
                item { PendingCard(vm) }
            }
        }

        if (vm.chatError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = SoftCoral,
            ) {
                Text(
                    vm.chatError.orEmpty(),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Coral,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("问日程，或让我帮你改日程…", color = Muted, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Pine,
                    unfocusedBorderColor = Line,
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    cursorColor = Pine,
                ),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty() && !vm.chatLoading) {
                        vm.sendChat(text)
                        input = ""
                    }
                },
                enabled = !vm.chatLoading && input.isNotBlank(),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Send, contentDescription = "发送", tint = if (input.isNotBlank()) Pine else Muted)
            }
        }
    }
}

@Composable
private fun WelcomeCard(onPrompt: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = SoftPine,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("你好，我是校程的日程助手", style = MaterialTheme.typography.titleMedium, color = Pine)
            Spacer(Modifier.height(4.dp))
            Text(
                "可以问今天的安排，也可以让我帮你新增、修改、完成或删除日程。与日程无关的题目我不会展开。",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(10.dp))
            listOf(
                "今天有什么安排？",
                "帮我把明天的讲座改到 20:00",
                "帮我新增一条周五下午的社团活动",
            ).forEach { prompt ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(CardWhite, RoundedCornerShape(10.dp))
                        .clickable { onPrompt(prompt) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(prompt, style = MaterialTheme.typography.bodySmall, color = Ink)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 0.92f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) Pine else CardWhite,
            border = if (isUser) null else BorderStroke(1.dp, Line.copy(alpha = 0.7f)),
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) CardWhite else Ink,
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Pine, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("正在想…", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
private fun PendingCard(vm: MainViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SoftAmber,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("准备修改日程", style = MaterialTheme.typography.titleSmall, color = Amber)
            Spacer(Modifier.height(6.dp))
            vm.pendingDescriptions.forEach { line ->
                Text("· " + line, style = MaterialTheme.typography.bodySmall, color = Ink)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { vm.applyPendingActions() },
                    colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("应用修改") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { vm.discardPendingActions() }) {
                    Text("取消", color = Muted)
                }
            }
        }
    }
}
