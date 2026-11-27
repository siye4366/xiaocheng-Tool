package com.xiaocheng.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xiaocheng.app.data.ScheduleEvent
import com.xiaocheng.app.ui.theme.CardWhite
import com.xiaocheng.app.ui.theme.Coral
import com.xiaocheng.app.ui.theme.Ink
import com.xiaocheng.app.ui.theme.Line
import com.xiaocheng.app.ui.theme.Muted
import com.xiaocheng.app.ui.theme.Paper
import com.xiaocheng.app.ui.theme.Pine
import com.xiaocheng.app.ui.theme.SoftCoral
import com.xiaocheng.app.ui.theme.categoryColor
import com.xiaocheng.app.ui.theme.categorySoft
import com.xiaocheng.app.util.formatFull
import java.util.Calendar

private val categories = listOf("竞赛", "讲座", "活动", "就业", "志愿", "考试", "其他")

@Composable
fun EventEditorDialog(
    event: ScheduleEvent?,
    onDismiss: () -> Unit,
    onSave: (ScheduleEvent) -> Unit,
    onDelete: (String) -> Unit,
) {
    val context = LocalContext.current
    var title by remember(event?.id) { mutableStateOf(event?.title ?: "") }
    var start by remember(event?.id) { mutableStateOf(event?.startAt) }
    var deadline by remember(event?.id) { mutableStateOf(event?.deadlineAt) }
    var location by remember(event?.id) { mutableStateOf(event?.location ?: "") }
    var category by remember(event?.id) { mutableStateOf(event?.category ?: "其他") }
    var note by remember(event?.id) { mutableStateOf(event?.note ?: "") }
    var link by remember(event?.id) { mutableStateOf(event?.link ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 14.dp).imePadding(),
            shape = RoundedCornerShape(28.dp),
            color = Paper,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ClearFocusOnKeyboardHidden()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (event == null) "新建日程" else "编辑日程",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier.size(34.dp).background(Color(0xFFEFEBE2), CircleShape).clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "关闭", tint = Muted, modifier = Modifier.size(18.dp))
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; titleError = false },
                        label = { Text("事件名称") },
                        placeholder = { Text("例如：程序设计大赛报名") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("请填写事件名称") }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )
                    TimeField("开始时间", start, onPick = {
                        pickDateTime(context, start) { start = it }
                    }, onClear = { start = null })
                    TimeField("截止时间", deadline, onPick = {
                        pickDateTime(context, deadline) { deadline = it }
                    }, onClear = { deadline = null })

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("地点") },
                        placeholder = { Text("图书馆报告厅 / 腾讯会议") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )

                    Column {
                        Text("分类", style = MaterialTheme.typography.labelMedium, color = Muted)
                        Spacer(Modifier.height(7.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            categories.forEach { c ->
                                val selected = c == category
                                Box(
                                    modifier = Modifier
                                        .background(if (selected) categorySoft(c) else Color.Transparent, RoundedCornerShape(10.dp))
                                        .border(1.dp, if (selected) categoryColor(c) else Line, RoundedCornerShape(10.dp))
                                        .clickable { category = c }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        c,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) categoryColor(c) else Muted,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注") },
                        placeholder = { Text("原文要求、联系方式、注意事项…") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("相关链接") },
                        placeholder = { Text("https://…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (event != null) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("删除", color = Coral)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
                    Spacer(Modifier.size(6.dp))
                    Button(
                        onClick = {
                            val t = title.trim()
                            if (t.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            val base = event ?: ScheduleEvent()
                            onSave(
                                base.copy(
                                    title = t,
                                    startAt = start,
                                    deadlineAt = deadline,
                                    location = location.trim(),
                                    category = category,
                                    note = note.trim(),
                                    link = link.trim(),
                                    updatedAt = System.currentTimeMillis(),
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("保存") }
                }
            }
        }
    }

    if (confirmDelete && event != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条日程？") },
            text = { Text("删除后无法恢复，但你仍可以在「个人 - 日历导出」前重新添加。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(event.id)
                }) { Text("删除", color = Coral) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun TimeField(label: String, value: Long?, onPick: () -> Unit, onClear: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (value == null) "未设置" else formatFull(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value == null) Muted else Ink,
                )
            }
            if (value != null) {
                Box(
                    modifier = Modifier.size(30.dp).clickable(onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "清除", tint = Muted, modifier = Modifier.size(15.dp))
                }
            }
            TextButton(onClick = onPick) { Text("选择", color = Pine, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pine,
    unfocusedBorderColor = Line,
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite,
    cursorColor = Pine,
    focusedLabelColor = Pine,
)

private fun pickDateTime(context: android.content.Context, initial: Long?, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    if (initial != null) cal.timeInMillis = initial
    DatePickerDialog(
        context,
        { _, y, m, d ->
            val tcal = Calendar.getInstance()
            if (initial != null) tcal.timeInMillis = initial
            TimePickerDialog(
                context,
                { _, h, min ->
                    val out = Calendar.getInstance()
                    out.set(y, m, d, h, min, 0)
                    out.set(Calendar.MILLISECOND, 0)
                    onPicked(out.timeInMillis)
                },
                tcal.get(Calendar.HOUR_OF_DAY),
                tcal.get(Calendar.MINUTE),
                true,
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH),
    ).show()
}