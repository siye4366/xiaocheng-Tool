package com.xiaocheng.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Icon
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val events by vm.events.collectAsState()
    val context = LocalContext.current
    var showName by remember { mutableStateOf(false) }
    var showApi by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }

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
        Text("把校程，调成你顺手的样子。", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(16.dp))

        ProfileCard(vm.userName, upcoming, done, events.size) { showName = true }

        Spacer(Modifier.height(20.dp))
        SectionHeader("AI 引擎", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Settings,
            title = "AI 配置",
            subtitle = if (vm.hasApiKey) vm.apiModel + " · 已配置" else "未配置 API · 使用本地规则",
            onClick = { showApi = true },
            accent = vm.hasApiKey,
        )
        SettingRow(
            icon = Icons.Filled.Build,
            title = "功能设置",
            subtitle = "AI 模式、额度、常驻要求",
            onClick = { showFeatures = true },
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader("日程工具", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Share,
            title = "导出 .ics 日历",
            subtitle = "保存 .ics 文件，导入后提前 30 分钟提醒",
            onClick = { showExport = true },
        )
        SettingRow(
            icon = Icons.Filled.DateRange,
            title = "日程分组",
            subtitle = "首页按「未到期 / 已到期」自动整理",
            onClick = { Toast.makeText(context, "在首页查看两组日程", Toast.LENGTH_SHORT).show() },
            showArrow = false,
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader("数据管理", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Refresh,
            title = "清空全部日程",
            subtitle = "共 " + events.size + " 条，操作不可撤销",
            onClick = { confirmClear = true },
            danger = true,
        )

        Spacer(Modifier.height(18.dp))
        SectionHeader("关于", -1, muted = false)
        SettingRow(
            icon = Icons.Filled.Refresh,
            title = "检查更新",
            subtitle = "当前 1.2.0 · 从 GitHub 检查新版本",
            onClick = { vm.checkForUpdates() },
        )
        SettingRow(
            icon = Icons.Filled.Info,
            title = "关于校程",
            subtitle = "版本 1.2.0 · 开发者与项目信息",
            onClick = { showAbout = true },
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
        FeatureSettingsDialog(vm, onDismiss = { showFeatures = false })
    }
    if (vm.updateCheck.show) {
        UpdateCheckDialog(vm, context)
    }
    if (showAbout) {
        AboutDialog(context, onDismiss = { showAbout = false })
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
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("昵称") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, cursorColor = Pine),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text("保存", color = Pine) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ApiDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var base by remember { mutableStateOf(vm.apiBase) }
    var key by remember { mutableStateOf(vm.apiKey) }
    var model by remember { mutableStateOf(vm.apiModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 配置") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "兼容 OpenAI 格式的接口都可以填。Key 只保存在本机，不会上传到别处。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text("接口地址") },
                    placeholder = { Text("https://api.deepseek.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, cursorColor = Pine),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
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
                Text(
                    "不填 Key 也能使用：校程会用离线规则识别今天/明天/下周三、X月X日、具体时刻和截止日期。",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
                if (vm.hasApiKey) {
                    TextButton(onClick = { vm.clearApi(); onDismiss() }) {
                        Text("清除已保存的 Key", color = Coral, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.saveApi(base, key, model); onDismiss() }) {
                Text("保存", color = Pine)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AboutDialog(context: Context, onDismiss: () -> Unit) {
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.2.0"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于校程") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            ) {
                Text("校程", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(2.dp))
                Text("把散落的校园消息，变成清楚的日程。", style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(16.dp))
                AboutLine("版本", version)
                AboutLine("包名", context.packageName)
                AboutLine("构建", "2026-11-24 · v1.2.0 体验优化")
                Spacer(Modifier.height(16.dp))
                Text("开发者：校程 Studio", style = MaterialTheme.typography.labelSmall, color = Muted)
                Text("技术：Kotlin · Jetpack Compose · Python", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了", color = Pine) } },
    )
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted, modifier = Modifier.width(56.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = Ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FeatureSettingsDialog(vm: MainViewModel, onDismiss: () -> Unit) {
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("功能设置", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("关闭", color = Muted) }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("AI 模式", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeOption("经典", mode != "combined", true) { mode = "classic" }
                        ModeOption("综合", mode == "combined", vm.hasApiKey) {
                            if (vm.hasApiKey) mode = "combined"
                        }
                    }
                    Text(
                        "经典：粘贴文本 / 文章链接 / 网页更新，适合批量整理。综合：对话式助手，可以直接新增、修改、完成或删除日程。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    if (!vm.hasApiKey) {
                        Text("未配置 API 时只能使用经典模式。", style = MaterialTheme.typography.labelSmall, color = Coral)
                    }

                    if (mode == "combined") {
                        Text("综合版修改日程", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("自动执行 AI 的修改", style = MaterialTheme.typography.bodyMedium)
                                Text("仅作用于综合版；关闭后，AI 会先列出修改，等你点「应用修改」。", style = MaterialTheme.typography.labelSmall, color = Muted)
                            }
                            Switch(checked = auto, onCheckedChange = { auto = it })
                        }
                    }

                    if (mode == "combined") {
                        Text("综合版联网", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("允许联网搜索", style = MaterialTheme.typography.bodyMedium)
                                Text("对话涉及实时信息时会搜索公开网页；关键词会发送给搜索引擎。", style = MaterialTheme.typography.labelSmall, color = Muted)
                            }
                            Switch(checked = webSearch, onCheckedChange = { webSearch = it })
                        }

                        Text("常驻要求", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = req,
                            onValueChange = { req = it },
                            placeholder = { Text("留空使用内置人设：可以正常聊天，只对过于复杂且无关日程的问题简短拒绝…") },
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
                        Text(
                            "每次综合版对话都会先读取这段要求。建议写：说话简洁、只处理日程、过于复杂且无关的问题简短拒绝。修改后点「保存」生效。",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted,
                        )
                        TextButton(onClick = { req = "" }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)) {
                            Text("恢复内置人设", color = Pine, fontSize = 12.sp)
                        }
                    } else {
                        Text("经典版", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "经典版使用粘贴文本、文章链接和网页更新。大模型 / 本地规则开关在 AI 页顶部；这里没有额外的经典版设置。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }

                    Text("API 额度", style = MaterialTheme.typography.titleSmall)
                    val quotaText = when {
                        vm.quotaState.loading -> "正在查询…"
                        vm.quotaState.error != null -> "查询失败：" + vm.quotaState.error
                        vm.quotaState.text.isNotBlank() -> vm.quotaState.text
                        else -> "尚未查询"
                    }
                    Text(quotaText, style = MaterialTheme.typography.bodySmall, color = if (vm.quotaState.error != null) Coral else Ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { vm.refreshQuota() },
                            enabled = vm.hasApiKey && !vm.quotaState.loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(if (vm.quotaState.loading) "查询中…" else "刷新额度") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { vm.testApi() }, enabled = vm.hasApiKey) { Text("测试连接", color = Pine) }
                    }
                    Text(
                        "额度查询取决于接口是否开放（DeepSeek 支持 /user/balance）；不支持时会提示，不影响正常使用。",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                    )
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
                    state.checking -> Text("正在从 GitHub 检查新版本…", color = Muted)
                    state.error != null -> Text(state.error, color = Coral)
                    state.isNew -> {
                        Text("当前 1.2.0 → v" + (state.latest ?: ""), style = MaterialTheme.typography.bodyMedium)
                        if (state.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(state.notes.take(800), style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("下载后可直接覆盖安装，数据保留。", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                    else -> Text("当前已是最新版本 " + (state.latest ?: "1.2.0") + "。")
                }
            }
        },
        confirmButton = {
            if (state.isNew) {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.url)))
                    }
                    vm.dismissUpdateCheck()
                }) { Text("打开下载页", color = Pine) }
            } else {
                TextButton(onClick = { vm.dismissUpdateCheck() }) { Text("知道了", color = Pine) }
            }
        },
        dismissButton = if (state.isNew) {
            { TextButton(onClick = { vm.dismissUpdateCheck() }) { Text("关闭", color = Muted) } }
        } else null,
    )
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