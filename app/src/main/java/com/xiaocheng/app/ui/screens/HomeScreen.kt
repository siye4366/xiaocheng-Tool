@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.xiaocheng.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.xiaocheng.app.MainViewModel
import com.xiaocheng.app.data.ScheduleEvent
import com.xiaocheng.app.ui.CategoryBadge
import com.xiaocheng.app.ui.EmptyState
import com.xiaocheng.app.ui.EventEditorDialog
import com.xiaocheng.app.ui.SectionHeader
import com.xiaocheng.app.ui.StatCell
import com.xiaocheng.app.ui.theme.Amber
import com.xiaocheng.app.ui.theme.CardWhite
import com.xiaocheng.app.ui.theme.Coral
import com.xiaocheng.app.ui.theme.Ink
import com.xiaocheng.app.ui.theme.Line
import com.xiaocheng.app.ui.theme.Muted
import com.xiaocheng.app.ui.theme.Paper
import com.xiaocheng.app.ui.theme.Pine
import com.xiaocheng.app.ui.theme.SoftCoral
import com.xiaocheng.app.ui.theme.SoftAmber
import com.xiaocheng.app.ui.theme.SoftPine
import com.xiaocheng.app.ui.theme.categoryColor
import com.xiaocheng.app.util.deadlineCountdownText
import com.xiaocheng.app.util.startCountdownText
import com.xiaocheng.app.util.formatShort
import com.xiaocheng.app.util.greeting
import com.xiaocheng.app.util.todayLabel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: MainViewModel) {
    val events by vm.events.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ScheduleEvent?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    fun exitSelection() {
        selecting = false
        selectedIds = emptySet()
    }

    fun toggleSelect(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    BackHandler(enabled = selecting) { exitSelection() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    val today = remember(events, now) {
        events.filter { it.isActiveToday(now) }.sortedWith(
            compareBy<ScheduleEvent>(
                { it.sortTime(now) ?: Long.MAX_VALUE },
                { if (it.sortIsDeadline(now)) 0 else 1 },
                { it.title },
            )
        )
    }
    val upcoming = remember(events, now) {
        events.filter { !it.isOverdue(now) && !it.isActiveToday(now) }.sortedWith(
            compareBy<ScheduleEvent>(
                { it.sortTime(now) ?: Long.MAX_VALUE },
                { if (it.sortIsDeadline(now)) 0 else 1 },
                { it.title },
            )
        )
    }
    val expired = remember(events, now) {
        events.filter { it.isOverdue(now) }.sortedWith(
            compareByDescending<ScheduleEvent> { it.sortTime(now) ?: Long.MIN_VALUE }
                .thenBy { if (it.sortIsDeadline(now)) 0 else 1 }
        )
    }
    val todayCount = today.size
    val allSelected = events.isNotEmpty() && selectedIds.size == events.size

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        if (selecting) {
            SelectionHeader(
                count = selectedIds.size,
                allSelected = allSelected,
                onToggleAll = {
                    selectedIds = if (allSelected) emptySet() else events.map { it.id }.toSet()
                },
                onCancel = { exitSelection() },
            )
        }
        Box(modifier = Modifier.weight(1f)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (selecting) 6.dp else 10.dp,
                bottom = if (selecting) 104.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            HomeHeader(vm.userName, onAdd = { adding = true })
        }
        item {
            Column {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatCell(upcoming.size.toString(), "未到期", Pine, Modifier.weight(1f))
                    StatCell(todayCount.toString(), "今天", Coral, Modifier.weight(1f))
                    StatCell(expired.size.toString(), "已到期", Muted, Modifier.weight(1f))
                }
            }
        }
        if (events.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = "还没有日程",
                    subtitle = "去 AI 页粘贴一段群通知，或手动添加第一条安排。",
                    action = {
                        Button(
                            onClick = { adding = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
                        ) { Text("手动添加") }
                    },
                )
            }
        }
        if (today.isNotEmpty()) {
            item { SectionHeader("今天", today.size) }
            items(today, key = { "today-" + it.id }) { event ->
                EventCard(
                    event = event,
                    now = now,
                    expired = false,
                    selecting = selecting,
                    selected = event.id in selectedIds,
                    onClick = { editing = event },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selecting) toggleSelect(event.id) else {
                            selecting = true
                            selectedIds = setOf(event.id)
                        }
                    },
                    onToggleDone = { vm.toggleDone(event.id) },
                    onSelectToggle = { toggleSelect(event.id) },
                )
            }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionHeader("未到期", upcoming.size) }
            items(upcoming, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    now = now,
                    expired = false,
                    selecting = selecting,
                    selected = event.id in selectedIds,
                    onClick = { editing = event },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selecting) toggleSelect(event.id) else {
                            selecting = true
                            selectedIds = setOf(event.id)
                        }
                    },
                    onToggleDone = { vm.toggleDone(event.id) },
                    onSelectToggle = { toggleSelect(event.id) },
                )
            }
        }
        if (expired.isNotEmpty()) {
            item { SectionHeader("已到期", expired.size, muted = true) }
            items(expired, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    now = now,
                    expired = true,
                    selecting = selecting,
                    selected = event.id in selectedIds,
                    onClick = { editing = event },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selecting) toggleSelect(event.id) else {
                            selecting = true
                            selectedIds = setOf(event.id)
                        }
                    },
                    onToggleDone = { vm.toggleDone(event.id) },
                    onSelectToggle = { toggleSelect(event.id) },
                )
            }
        }
            }
            if (selecting) {
                Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    SelectionActions(
                        count = selectedIds.size,
                        onMarkDone = {
                            vm.markDone(selectedIds)
                            exitSelection()
                        },
                        onMarkUndone = {
                            vm.markUndone(selectedIds)
                            exitSelection()
                        },
                        onDelete = { confirmDelete = true },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除 " + selectedIds.size + " 条日程？") },
            text = { Text("删除后无法恢复，已完成的日程也会一起删除。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteEvents(selectedIds)
                    confirmDelete = false
                    exitSelection()
                }) { Text("删除", color = Coral) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }

    if (adding || editing != null) {
        val current = editing
        EventEditorDialog(
            event = current,
            onDismiss = { adding = false; editing = null },
            onSave = { saved ->
                if (current == null) vm.addManual(saved) else vm.updateEvent(saved)
                adding = false
                editing = null
            },
            onDelete = { id ->
                vm.deleteEvent(id)
                adding = false
                editing = null
            },
        )
    }
}

@Composable
private fun SelectionHeader(
    count: Int,
    allSelected: Boolean,
    onToggleAll: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Paper).padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "退出多选", tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text("已选 " + count + " 条", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onToggleAll) {
            Text(if (allSelected) "取消全选" else "全选", color = Pine, fontSize = 13.sp)
        }
        TextButton(onClick = onCancel) {
            Text("取消", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SelectionActions(
    count: Int,
    onMarkDone: () -> Unit,
    onMarkUndone: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Ink,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onMarkDone,
                enabled = count > 0,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Done, null, tint = if (count > 0) Paper else Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("完成", color = if (count > 0) Paper else Muted, fontSize = 14.sp)
            }
            Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFF3B4B45)))
            TextButton(
                onClick = onMarkUndone,
                enabled = count > 0,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Refresh, null, tint = if (count > 0) Paper else Muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("取消标记", color = if (count > 0) Paper else Muted, fontSize = 13.sp)
            }
            Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFF3B4B45)))
            TextButton(
                onClick = onDelete,
                enabled = count > 0,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Delete, null, tint = if (count > 0) Coral else Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("删除 " + count, color = if (count > 0) Coral else Muted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HomeHeader(name: String, onAdd: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(greeting(), style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.size(8.dp))
                Text(todayLabel(), style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Spacer(Modifier.height(6.dp))
            Text("把消息，变成日程。", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text("你好，" + name, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Pine)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加日程", tint = CardWhite, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun EventCard(
    event: ScheduleEvent,
    now: Long,
    expired: Boolean,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleDone: () -> Unit,
    onSelectToggle: () -> Unit,
) {
    val due = event.keyTime
    val done = event.done
    val urgent = !done && due != null && due >= now && due - now <= 24 * 3600 * 1000L
    val accent = when {
        done || expired -> Muted
        urgent -> Coral
        else -> Pine
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selecting) onSelectToggle() else onClick() },
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) SoftPine.copy(alpha = 0.6f) else CardWhite,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Pine else if (urgent) Coral.copy(alpha = 0.24f) else Line.copy(alpha = 0.7f),
        ),
        shadowElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selecting && selected) Pine else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (selecting) (if (selected) Pine else Line) else if (done) Pine else Line,
                        CircleShape,
                    )
                    .clickable { if (selecting) onSelectToggle() else onToggleDone() },
                contentAlignment = Alignment.Center,
            ) {
                if (selecting) {
                    if (selected) Icon(Icons.Filled.Check, "已选中", tint = CardWhite, modifier = Modifier.size(14.dp))
                } else if (done) {
                    Icon(Icons.Filled.Check, "完成", tint = Pine, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CategoryBadge(event.category)
                    if (done) {
                        CountdownBadge("已完成", Muted, Line.copy(alpha = 0.55f))
                    } else {
                        val times = listOfNotNull(
                            event.startAt?.let { it to false },
                            event.deadlineAt?.let { it to true },
                        ).sortedWith(compareBy({ it.first }, { if (it.second) 0 else 1 }))
                        times.forEach { pair ->
                            val at = pair.first
                            val isDeadline = pair.second
                            if (isDeadline) {
                                CountdownBadge(
                                    deadlineCountdownText(at, now),
                                    if (expired) Muted else Coral,
                                    if (expired) Line.copy(alpha = 0.55f) else SoftCoral,
                                    bold = true,
                                )
                            } else {
                                CountdownBadge(
                                    startCountdownText(at, now),
                                    if (expired) Muted else Pine,
                                    if (expired) Line.copy(alpha = 0.5f) else SoftPine,
                                )
                            }
                        }
                        if (times.isEmpty()) CountdownBadge("待补充时间", Amber, SoftAmber)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) Muted else Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (due != null) {
                    Spacer(Modifier.height(7.dp))
                    Metaline(
                        color = if (expired || done) Muted else accent,
                        text = (if (event.deadlineAt != null) "截止 " else "时间 ") + formatShort(due),
                    )
                }
                if (event.startAt != null && event.deadlineAt != null) {
                    Spacer(Modifier.height(3.dp))
                    Metaline(color = Muted, text = "活动 " + formatShort(event.startAt!!))
                }
                if (event.location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, null, tint = Muted, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownBadge(text: String, color: Color, soft: Color, bold: Boolean = false) {
    Box(
        modifier = Modifier
            .background(soft, RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun Metaline(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
        Spacer(Modifier.size(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}