package com.xiaocheng.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaocheng.app.ui.theme.CardWhite
import com.xiaocheng.app.ui.theme.Coral
import com.xiaocheng.app.ui.theme.Ink
import com.xiaocheng.app.ui.theme.Line
import com.xiaocheng.app.ui.theme.Muted
import com.xiaocheng.app.ui.theme.Paper
import com.xiaocheng.app.ui.theme.Pine
import com.xiaocheng.app.ui.theme.SoftPine
import com.xiaocheng.app.ui.theme.categoryColor
import com.xiaocheng.app.ui.theme.categorySoft

@Composable
fun CategoryBadge(category: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(categorySoft(category), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = category,
            color = categoryColor(category),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SectionHeader(title: String, count: Int, muted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (muted) Muted else Ink,
        )
        if (count >= 0) {
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .background(if (muted) Line.copy(alpha = 0.6f) else SoftPine, RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = if (muted) Muted else Pine)
            }
        }
        Spacer(Modifier.weight(1f))
        if (muted) Text("已过期或已完成", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
fun StatCell(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(SoftPine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Pine, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Muted)
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

@Composable
fun SoftLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.65f)))
}

@Composable
fun EngineChip(configured: Boolean, model: String) {
    Row(
        modifier = Modifier
            .background(if (configured) SoftPine else Color(0xFFF1EDE3), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(7.dp).background(if (configured) Pine else Coral, CircleShape)
        )
        Spacer(Modifier.size(7.dp))
        Text(
            text = if (configured) "大模型 · " + model else "本地规则引擎",
            style = MaterialTheme.typography.labelMedium,
            color = if (configured) Pine else Color(0xFF8A5A3B),
        )
    }
}

@Composable
fun RoundedTag(text: String, color: Color, soft: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(soft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
fun CircleIcon(icon: ImageVector, tint: Color = Pine, background: Color = SoftPine, size: Int = 38) {
    Box(
        modifier = Modifier.size(size.dp).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.46).dp))
    }
}

@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(22.dp), color = Paper, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        content()
    }
}
