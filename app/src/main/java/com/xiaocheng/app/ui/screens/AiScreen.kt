package com.xiaocheng.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaocheng.app.MainViewModel
import com.xiaocheng.app.data.ExtractedEvent
import com.xiaocheng.app.data.NoticeItem
import com.xiaocheng.app.ui.CategoryBadge
import com.xiaocheng.app.ui.EngineChip
import com.xiaocheng.app.ui.ScreenTitle
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
import com.xiaocheng.app.util.formatFull

private const val EXAMPLE = "【讲座通知】人工智能前沿讲座\n" +
    "时间：下周三 19:00\n" +
    "地点：图书馆报告厅\n" +
    "报名截止：本周五 18:00\n" +
    "主办：计算机学院学生会\n" +
    "报名链接：https://example.com/ai-lecture"

@Composable
fun AiScreen(vm: MainViewModel) {
    var mode by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    val ai = vm.aiState
    val update = vm.updateState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .imePadding(),
    ) {
        Spacer(Modifier.height(12.dp))
        ScreenTitle("AI 提取", "粘贴一段通知，校程帮你拆成日期、地点和截止时间。")
        Spacer(Modifier.height(12.dp))
        EngineChip(vm.hasApiKey, vm.apiModel)
        Spacer(Modifier.height(16.dp))

        SourceTabs(mode) { mode = it }
        Spacer(Modifier.height(14.dp))

        when (mode) {
            0 -> TextSource(
                text = text,
                onTextChange = { text = it },
                loading = ai.loading,
                onExtract = { vm.extractText(text) },
                onExample = { text = EXAMPLE },
            )
            1 -> LinkSource(
                url = url,
                onUrlChange = { url = it },
                loading = ai.loading,
                onExtract = { vm.extractUrl(url.trim()) },
            )
            else -> UpdateSource(
                url = updateUrl,
                onUrlChange = { updateUrl = it },
                loading = update.loading,
                onCheck = { vm.checkUpdates(updateUrl.trim()) },
            )
        }

        if (ai.warning != null) {
            Spacer(Modifier.height(12.dp))
            MessageCard(ai.warning, Amber, SoftAmber, Icons.Filled.Warning)
        }
        if (ai.error != null) {
            Spacer(Modifier.height(12.dp))
            MessageCard(ai.error, Coral, SoftCoral, Icons.Filled.Warning)
        }
        if (mode == 2 && ai.loading) {
            Spacer(Modifier.height(12.dp))
            LoadingCard("正在解析条目并抽取…")
        }
        if (ai.results.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            ResultSection(ai.results, ai.engine, ai.sourceLabel, onSaveAll = { vm.saveAllExtracted() }, onSaveOne = { vm.saveOne(it) })
        } else if (ai.loading && mode != 2) {
            Spacer(Modifier.height(16.dp))
            LoadingCard(if (vm.hasApiKey) "大模型正在阅读通知…" else "正在用本地规则抽取…")
        }
        if (mode == 2 && (update.items.isNotEmpty() || update.error != null || update.firstRun)) {
            Spacer(Modifier.height(14.dp))
            UpdateResult(vm, update)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SourceTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("粘贴文本", "文章链接", "网页更新")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFEFEBE2),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                val bg by animateColorAsState(if (active) Pine else Color.Transparent, tween(220), label = "seg-bg")
                val fg by animateColorAsState(if (active) CardWhite else Muted, tween(220), label = "seg-fg")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(bg, RoundedCornerShape(12.dp))
                        .clickable { onSelect(index) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun TextSource(text: String, onTextChange: (String) -> Unit, loading: Boolean, onExtract: () -> Unit, onExample: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.8f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("例如：\n【活动通知】下周三晚 7 点，图书馆报告厅…", color = Muted, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(176.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Pine,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onExample) { Text("填入示例", color = Pine, fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                Text(text.length.toString() + " 字", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onExtract,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CardWhite, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                } else {
                    Text("✦", fontSize = 16.sp, color = CardWhite)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (loading) "抽取中…" else "开始提取", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text("未配置大模型时使用本地规则，同样可以离线抽取常见中文时间表达。", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
    }
}

@Composable
private fun LinkSource(url: String, onUrlChange: (String) -> Unit, loading: Boolean, onExtract: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.8f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("单篇公开文章", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("支持公众号文章、校园新闻等公开页面，只解析你主动提交的这一篇。", style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("文章链接") },
                placeholder = { Text("https://mp.weixin.qq.com/s/…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, cursorColor = Pine),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onExtract,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CardWhite, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (loading) "解析中…" else "解析并提取", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text("合规说明：不批量爬取、不模拟登录，仅请求你粘贴的公开链接。", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
    }
}

@Composable
private fun UpdateSource(url: String, onUrlChange: (String) -> Unit, loading: Boolean, onCheck: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.8f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("学校通知页 · 手动检查更新", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("第一次检查会保存页面条目快照；以后每次点击，只列出新增条目。不常驻后台。", style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("公开通知页链接") },
                placeholder = { Text("https://www.example.edu.cn/notice/") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pine, unfocusedBorderColor = Line, focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite, cursorColor = Pine),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onCheck,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CardWhite, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                } else {
                    Icon(Icons.Filled.Refresh, null, tint = CardWhite, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (loading) "检查中…" else "检查更新", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UpdateResult(vm: MainViewModel, update: com.xiaocheng.app.UpdateUiState) {
    Column {
        if (update.error != null) {
            MessageCard(update.error, Coral, SoftCoral, Icons.Filled.Warning)
            return
        }
        if (update.firstRun) {
            MessageCard("已建立首次快照，共识别 " + update.total + " 条。下次检查只显示新增条目，下面可以先试提取。", Pine, SoftPine, Icons.Filled.CheckCircle)
            Spacer(Modifier.height(10.dp))
        } else if (update.items.isEmpty()) {
            MessageCard("没有发现新增条目。", Pine, SoftPine, Icons.Filled.CheckCircle)
            return
        } else {
            Text("新增 " + update.items.size + " 条", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
        }
        update.items.forEach { item ->
            NoticeRow(item, vm.aiState.loading) { vm.extractNotice(item) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NoticeRow(item: NoticeItem, loading: Boolean, onExtract: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.7f)),
    ) {
        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.dateHint.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(item.dateHint, style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onExtract, enabled = !loading) {
                Text("提取", color = Pine, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ResultSection(
    results: List<ExtractedEvent>,
    engine: String,
    sourceLabel: String,
    onSaveAll: () -> Unit,
    onSaveOne: (ExtractedEvent) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("抽取结果", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.background(SoftPine, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(engine, style = MaterialTheme.typography.labelSmall, color = Pine)
            }
        }
        if (sourceLabel.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(sourceLabel, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        results.forEach { ev ->
            ResultCard(ev, onSave = { onSaveOne(ev) })
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = onSaveAll,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardWhite),
        ) {
            Text("全部保存到日程 (" + results.size + ")", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Text("保存后可在「首页」查看，时间识别不准确的条目可以直接点开修改。", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun ResultCard(ev: ExtractedEvent, onSave: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.75f)),
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(ev.category)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSave) { Text("保存", color = Pine, fontSize = 13.sp) }
            }
            Text(ev.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(7.dp))
            if (ev.startAt != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, null, tint = Pine, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("开始 " + formatFull(ev.startAt), style = MaterialTheme.typography.bodySmall, color = Ink)
                }
                Spacer(Modifier.height(3.dp))
            }
            if (ev.deadlineAt != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, null, tint = Coral, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("截止 " + formatFull(ev.deadlineAt), style = MaterialTheme.typography.bodySmall, color = Coral)
                }
                Spacer(Modifier.height(3.dp))
            }
            if (ev.startAt == null && ev.deadlineAt == null) {
                Text("未识别到时间，保存后请手动补充", style = MaterialTheme.typography.bodySmall, color = Amber)
                Spacer(Modifier.height(3.dp))
            }
            if (ev.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, null, tint = Muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(ev.location, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            if (ev.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(ev.note, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (ev.link.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "打开原文链接",
                    style = MaterialTheme.typography.labelMedium,
                    color = Pine,
                    modifier = Modifier.clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ev.link))) }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SoftPine,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Pine, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Pine)
        }
    }
}

@Composable
private fun MessageCard(text: String, color: Color, background: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = background,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp).padding(top = 2.dp))
            Spacer(Modifier.width(9.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}