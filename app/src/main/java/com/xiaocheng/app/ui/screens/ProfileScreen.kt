package com.xiaocheng.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.compose.runtime.collectAsState
import com.xiaocheng.app.MainViewModel
import com.xiaocheng.app.ui.ClearFocusOnKeyboardHidden
import com.xiaocheng.app.data.AiProviders
import com.xiaocheng.app.data.ScheduleEvent
import com.xiaocheng.app.ui.CircleIcon
import com.xiaocheng.app.ui.SectionHeader
import com.xiaocheng.app.ui.theme.CardWhite
import com.xiaocheng.app.ui.theme.Coral
import com.xiaocheng.app.ui.theme.Ink
import com.xiaocheng.app.ui.theme.Line
import com.xiaocheng.app.ui.theme.Muted
import com.xiaocheng.app.ui.theme.Paper
import com.xiaocheng.app.ui.theme.Pine
import com.xiaocheng.app.ui.theme.PineDeep
import com.xiaocheng.app.ui.theme.SoftPine
import com.xiaocheng.app.util.formatFull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val events by vm.events.collectAsState()
    val context = LocalContext.current
    var showName by remember { mutableStateOf(false) }
    var showApi by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }
    var transferMode by remember { mutableStateOf<String?>(null) }
    var transferEvents by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }
    var pendingExport by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(vm.buildIcsText().toByteArray(Charsets.UTF_8))
                } ?: error("无法写入文件")
            }.onSuccess {
                Toast.makeText(context, "日历文件已保存，点开即可导入手机日历", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "保存失败：" + (it.message ?: "未知错误"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportJsonDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(vm.buildTransferJson(pendingExport).toByteArray(Charsets.UTF_8))
                } ?: error("无法写入文件")
            }.onSuccess {
                Toast.makeText(context, "已导出 " + pendingExport.size + " 条", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "导出失败：" + (it.message ?: "未知错误"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importJsonDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            val parsed = vm.parseTransferJson(text.orEmpty())
            if (parsed.isEmpty()) {
                Toast.makeText(context, "没有读到可导入的日程", Toast.LENGTH_SHORT).show()
            } else {
                transferEvents = parsed
                transferMode = "import"
            }
        }
    }

    val upcoming = events.count { !it.isOverdue() }
    val done = events.count { it.done }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("个人", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text("让信息变成行动。", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(16.dp))

        ProfileCard(vm.userName, upcoming, done, events.size) { showName = true }

        Spacer(Modifier.height(20.dp))
        SectionHeader("AI", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Settings,
            title = "AI 配置",
            subtitle = if (vm.hasApiKey) "已配置 · " + vm.apiModel else "未配置",
            onClick = { showApi = true },
            accent = vm.hasApiKey,
        )
        SettingRow(
            icon = Icons.Filled.Build,
            title = "功能设置",
            subtitle = "模式、要求",
            onClick = { showFeatures = true },
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader("日程工具", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Share,
            title = "导出 .ics 日历",
            subtitle = "提前 30 分钟提醒",
            onClick = { showExport = true },
        )
        SettingRow(
            icon = Icons.Filled.DateRange,
            title = "导入 / 导出",
            subtitle = "选择日程，跨设备转移",
            onClick = { transferMode = "choose" },
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader("数据管理", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Share,
            title = "导出诊断日志",
            subtitle = "对话、错误和操作记录",
            onClick = { shareDiagnostics(context, vm) },
        )
        SettingRow(
            icon = Icons.Filled.Refresh,
            title = "清空全部日程",
            subtitle = "共 " + events.size + " 条",
            onClick = { confirmClear = true },
            danger = true,
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader("更新", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Refresh,
            title = "检查更新",
            subtitle = "当前 1.4.1",
            onClick = { vm.checkForUpdates() },
        )
        Spacer(Modifier.height(30.dp))
        Text(
            "校程 · Campus to Action",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showName) {
        NameDialog(vm.userName, onDismiss = { showName = false }, onSave = {
            vm.saveUserName(it); showName = false
        })
    }
    if (showApi) {
        ApiDialog(vm, onDismiss = { showApi = false })
    }
    if (showFeatures) {
        FeatureSettingsDialog(
            vm,
            onDismiss = { showFeatures = false },
            onOpenAiConfig = {
                showFeatures = false
                showApi = true
            },
        )
    }
    if (vm.updateCheck.show) {
        UpdateCheckDialog(vm, context)
    }
    when (transferMode) {
        "choose" -> TransferChoiceDialog(
            onDismiss = { transferMode = null },
            onExport = {
                transferEvents = events
                transferMode = "export"
            },
            onImport = {
                transferMode = null
                importJsonDoc.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
        )
        "export" -> TransferSelectDialog(
            title = "导出日程",
            events = transferEvents,
            confirmText = "导出选中",
            onDismiss = { transferMode = null },
            onConfirm = { selected ->
                pendingExport = selected
                transferMode = null
                val name = "校程日程_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json"
                exportJsonDoc.launch(name)
            },
        )
        "import" -> TransferSelectDialog(
            title = "导入日程",
            events = transferEvents,
            confirmText = "保存选中",
            onDismiss = { transferMode = null },
            onConfirm = { selected ->
                vm.importEvents(selected)
                transferMode = null
            },
        )
    }
    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("导出日历") },
            text = { Text("保存一个标准 .ics 文件，用手机日历打开即可导入；文件里已包含提前 30 分钟的提醒。") },
            confirmButton = {
                TextButton(onClick = {
                    showExport = false
                    val name = "校程日程_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".ics"
                    createDoc.launch(name)
                }) { Text("保存到文件", color = Pine) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExport = false
                    shareIcs(context, vm)
                }) { Text("分享到应用", color = Muted) }
            },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部日程？") },
            text = { Text("所有手动添加和 AI 提取的日程都会被删除，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { vm.clearEvents(); confirmClear = false }) { Text("清空", color = Coral) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ProfileCard(name: String, upcoming: Int, done: Int, total: Int, onEditName: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(PineDeep, Pine)))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(Paper.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.take(1),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Paper,
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Paper)
                    Spacer(Modifier.height(2.dp))
                    Text("校程 · 让信息变成行动", fontSize = 12.sp, color = Paper.copy(alpha = 0.66f))
                }
                Box(
                    modifier = Modifier.size(34.dp).background(Paper.copy(alpha = 0.12f), CircleShape).clickable(onClick = onEditName),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Edit, "修改名字", tint = Paper, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            Row {
                ProfileStat(upcoming.toString(), "未到期", Modifier.weight(1f))
                ProfileStat(done.toString(), "已完成", Modifier.weight(1f))
                ProfileStat(total.toString(), "全部日程", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Paper)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Paper.copy(alpha = 0.62f))
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    danger: Boolean = false,
    showArrow: Boolean = true,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIcon(
                icon,
                tint = if (danger) Coral else if (accent) Pine else Ink.copy(alpha = 0.78f),
                background = if (danger) Color(0xFFF8E9E5) else SoftPine,
                size = 38,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) Coral else Ink)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showArrow) Icon(Icons.Filled.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("怎么称呼你？") },
        text = {
            Column {
                ClearFocusOnKeyboardHidden()
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text("昵称") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, cursorColor = Pine),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text("保存", color = Pine) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AppSheet(
    title: String,
    onDismiss: () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
    footer: @Composable RowScope.() -> Unit,
) {
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
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { body() }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) { footer() }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApiDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activeSaved = vm.apiProfiles.firstOrNull { it.id == vm.activeProfileId }
    var base by remember { mutableStateOf(vm.apiBase) }
    var key by remember { mutableStateOf(vm.apiKey) }
    var model by remember { mutableStateOf(vm.apiModel) }
    var selectedRecommended by remember {
        mutableStateOf<String?>(if (activeSaved == null) AiProviders.match(vm.apiBase, vm.apiModel)?.id ?: "custom" else null)
    }
    var editingId by remember { mutableStateOf<String?>(activeSaved?.id) }
    var auto by remember { mutableStateOf(vm.autoApi) }
    val selectedProvider = AiProviders.all.firstOrNull { it.id == selectedRecommended }

    AppSheet(
        title = "AI 配置",
        onDismiss = onDismiss,
        body = {
            Text("OpenAI 兼容，Key 只存本机。", style = MaterialTheme.typography.bodySmall, color = Muted)
            if (vm.apiProfiles.any { it.apiKey.isNotBlank() }) {
                Text("已保存", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    vm.apiProfiles.filter { it.apiKey.isNotBlank() }.forEach { profile ->
                        ProviderChip(profile.displayName.ifBlank { profile.model.ifBlank { profile.name } }, editingId == profile.id) {
                            editingId = profile.id
                            selectedRecommended = null
                            base = profile.baseUrl
                            model = profile.model
                            key = profile.apiKey
                        }
                    }
                }
            }
            Text("推荐", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AiProviders.all.forEach { provider ->
                    ProviderChip(provider.name, selectedRecommended == provider.id) {
                        selectedRecommended = provider.id
                        editingId = null
                        base = provider.baseUrl
                        model = provider.model
                        key = ""
                    }
                }
                ProviderChip("自定义", selectedRecommended == "custom") {
                    selectedRecommended = "custom"
                    editingId = null
                    base = ""
                    model = ""
                    key = ""
                }
            }
            if (selectedProvider != null) {
                Text(selectedProvider.note, style = MaterialTheme.typography.labelSmall, color = Muted)
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(selectedProvider.keyUrl)))
                        }
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
                ) { Text("申请 Key", color = Pine, fontSize = 12.sp) }
            } else {
                Text("可保存多个自定义接口。", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动选择", style = MaterialTheme.typography.bodyMedium)
                    Text("已保存 Key 间轮换，限流自动跳过", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
                Switch(checked = auto, onCheckedChange = { auto = it })
            }
            Text("已保存 " + vm.savedApiCount + " 个", style = MaterialTheme.typography.labelSmall, color = Muted)
            OutlinedTextField(
                value = base,
                onValueChange = {
                    base = it
                    if (selectedRecommended != "custom") {
                        selectedRecommended = "custom"
                        editingId = null
                    }
                },
                label = { Text("接口地址") },
                placeholder = { Text("https://api.deepseek.com/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, cursorColor = Pine),
            )
            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it
                    if (selectedRecommended != "custom") {
                        selectedRecommended = "custom"
                        editingId = null
                    }
                },
                label = { Text("模型名称") },
                placeholder = { Text("deepseek-chat") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, cursorColor = Pine),
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("API Key") },
                placeholder = { Text("sk-…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, cursorColor = Pine),
            )
            Text("不填 Key 用本地规则。", style = MaterialTheme.typography.labelSmall, color = Muted)
            if (editingId != null) {
                TextButton(onClick = {
                    val id = editingId ?: return@TextButton
                    vm.deleteProfile(id)
                    editingId = null
                    selectedRecommended = "custom"
                    base = ""
                    model = ""
                    key = ""
                }) {
                    Text("删除此配置", color = Coral, fontSize = 13.sp)
                }
            }
        },
        footer = {
            TextButton(onClick = onDismiss) { Text("完成", color = Muted) }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = {
                    val id = vm.saveApi(base, key, model, editingId)
                    if (id.isNotBlank()) {
                        // 保存后清空表单，下一次保存一定是一条新配置。
                        editingId = null
                        selectedRecommended = null
                        base = ""
                        model = ""
                        key = ""
                    } else {
                        editingId = vm.activeProfileId.ifBlank { null }
                    }
                    if (auto != vm.autoApi) vm.setAutoApiEnabled(auto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
                shape = RoundedCornerShape(14.dp),
            ) { Text("保存") }
        },
    )
}

@Composable
private fun ProviderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Pine else CardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Pine else Line),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CardWhite else Ink,
        )
    }
}

@Composable
private fun TransferChoiceDialog(
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入 / 导出") },
        text = { Text("导出时选择要带走的日程；导入文件后也可以选择保存哪些。") },
        confirmButton = { TextButton(onClick = onExport) { Text("导出", color = Pine) } },
        dismissButton = { TextButton(onClick = onImport) { Text("导入", color = Pine) } },
    )
}

@Composable
private fun TransferSelectDialog(
    title: String,
    events: List<ScheduleEvent>,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (List<ScheduleEvent>) -> Unit,
) {
    var selected by remember(events) { mutableStateOf(events.map { it.id }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("已选 " + selected.size + " / " + events.size, style = MaterialTheme.typography.labelMedium, color = Muted)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { selected = events.map { it.id }.toSet() }) { Text("全选", fontSize = 12.sp) }
                    TextButton(onClick = { selected = emptySet() }) { Text("清空", fontSize = 12.sp) }
                }
                if (events.isEmpty()) {
                    Text("没有日程", style = MaterialTheme.typography.bodySmall, color = Muted)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        items(events, key = { it.id }) { event ->
                            val checked = event.id in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (checked) selected - event.id else selected + event.id
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selected = if (it) selected + event.id else selected - event.id
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val time = event.startAt?.let { formatFull(it) }
                                        ?: event.deadlineAt?.let { "截止 " + formatFull(it) }
                                    if (time != null) {
                                        Text(time, style = MaterialTheme.typography.labelSmall, color = Muted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(events.filter { it.id in selected }) },
                enabled = selected.isNotEmpty(),
            ) { Text(confirmText, color = Pine) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Muted) } },
    )
}

@Composable
private fun FeatureSettingsDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onOpenAiConfig: () -> Unit,
) {
    var mode by remember { mutableStateOf(vm.aiMode) }
    var auto by remember { mutableStateOf(vm.autoApplyActions) }
    var req by remember { mutableStateOf(vm.aiRequirements) }
    var webSearch by remember { mutableStateOf(vm.webSearchEnabled) }

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
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("功能设置", style = MaterialTheme.typography.headlineSmall)
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI 供应商", style = MaterialTheme.typography.titleSmall)
                            Text(
                                AiProviders.nameOf(vm.apiBase, vm.apiModel) +
                                    (if (vm.autoApi) " · 自动选择" else "") +
                                    " · " + if (vm.hasApiKey) "已配置" else "未配置",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                        }
                        TextButton(onClick = onOpenAiConfig) { Text("切换", color = Pine) }
                    }

                    Text("AI 模式", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeOption("经典", mode != "combined", true) { mode = "classic" }
                        ModeOption("综合", mode == "combined", vm.hasApiKey) {
                            if (vm.hasApiKey) mode = "combined"
                        }
                    }
                    Text(
                        "经典：批量整理。综合：对话式助手。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    if (!vm.hasApiKey) {
                        Text("未配置 API 时只能用经典。", style = MaterialTheme.typography.labelSmall, color = Coral)
                    }

                    if (mode == "combined") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("自动执行修改", style = MaterialTheme.typography.bodyMedium)
                                Text("关闭后先确认", style = MaterialTheme.typography.labelSmall, color = Muted)
                            }
                            Switch(checked = auto, onCheckedChange = { auto = it })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("联网搜索", style = MaterialTheme.typography.bodyMedium)
                                Text("涉及实时信息时检索公开网页", style = MaterialTheme.typography.labelSmall, color = Muted)
                            }
                            Switch(checked = webSearch, onCheckedChange = { webSearch = it })
                        }

                        Text("常驻要求", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = req,
                            onValueChange = { req = it },
                            placeholder = { Text("例如：说话简洁，随机日程也可以生成…") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Pine,
                                unfocusedBorderColor = Line,
                                focusedContainerColor = CardWhite,
                                unfocusedContainerColor = CardWhite,
                                cursorColor = Pine,
                            ),
                        )
                        Text("修改后点「保存」生效。", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }

                    TextButton(onClick = { vm.testApi() }, enabled = vm.hasApiKey) {
                        Text("测试当前 API", color = Pine)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            vm.selectAiMode(mode)
                            vm.changeAutoApply(auto)
                            vm.setWebSearch(webSearch)
                            vm.saveAiRequirements(req)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun ModeOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Pine else CardWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Pine else if (enabled) Line else Line.copy(alpha = 0.4f),
        ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) CardWhite else if (enabled) Ink else Muted.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun UpdateCheckDialog(vm: MainViewModel, context: Context) {
    val state = vm.updateCheck
    val title = when {
        state.checking -> "检查更新"
        state.error != null -> "检查失败"
        state.isNew -> "发现新版本 v" + (state.latest ?: "")
        else -> "已是最新版本"
    }
    AlertDialog(
        onDismissRequest = { vm.dismissUpdateCheck() },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                when {
                    state.downloading -> Column {
                        Text("正在下载安装包… " + state.downloadProgress + "%", color = Muted)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Pine,
                            trackColor = Line.copy(alpha = 0.4f),
                        )
                    }
                    state.checking -> Text("检查中…", color = Muted)
                    state.error != null -> Text(state.error, color = Coral)
                    state.isNew -> {
                        Text("当前 1.4.1 → v" + (state.latest ?: ""), style = MaterialTheme.typography.bodyMedium)
                        if (state.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(state.notes.take(600), style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                    else -> Text("当前已是最新版本 " + (state.latest ?: "1.4.1") + "。")
                }
            }
        },
        confirmButton = {
            if (state.isNew) {
                Row {
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.url)))
                        }
                        vm.dismissUpdateCheck()
                    }) { Text("下载页", color = Muted) }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = { vm.downloadUpdate(context) },
                        enabled = !state.downloading && state.apkUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(if (state.downloading) "下载中 " + state.downloadProgress + "%" else "一键更新") }
                }
            } else {
                TextButton(onClick = { vm.dismissUpdateCheck() }) { Text("知道了", color = Pine) }
            }
        },
        dismissButton = if (state.isNew) {
            { TextButton(onClick = { vm.dismissUpdateCheck() }) { Text("关闭", color = Muted) } }
        } else null,
    )
}

private fun shareDiagnostics(context: Context, vm: MainViewModel) {
    try {
        val file = vm.exportDiagnostics(context)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "校程诊断日志")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "导出校程诊断日志"))
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败：" + (e.message ?: "未知错误"), Toast.LENGTH_SHORT).show()
    }
}

private fun shareIcs(context: Context, vm: MainViewModel) {
    try {
        val file = vm.exportIcs(context)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "校程日程")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "导出校程日程"))
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败：" + (e.message ?: "未知错误"), Toast.LENGTH_SHORT).show()
    }
}