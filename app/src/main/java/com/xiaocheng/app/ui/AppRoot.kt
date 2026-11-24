package com.xiaocheng.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaocheng.app.MainViewModel
import com.xiaocheng.app.ui.screens.AiScreen
import com.xiaocheng.app.ui.screens.HomeScreen
import com.xiaocheng.app.ui.screens.ProfileScreen
import com.xiaocheng.app.ui.theme.Coral
import com.xiaocheng.app.ui.theme.Ink
import com.xiaocheng.app.ui.theme.Paper
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

private data class TabItem(val label: String, val kind: Int)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XiaoChengApp(vm: MainViewModel) {
    val pager = rememberPagerState(pageCount = { 3 })
    var selected by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        snapshotFlow { pager.currentPage }.collect { selected = it }
    }
    LaunchedEffect(vm.toast) {
        val msg = vm.toast
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.consumeToast()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Paper, contentColor = Ink) {
    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            XcBottomBar(selected) { target ->
                if (target != selected) {
                    if (target == 1 || selected == 1) vm.clearAiNotice()
                    scope.launch {
                        pager.animateScrollToPage(target, animationSpec = tween(420, easing = FastOutSlowInEasing))
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
        ) { page ->
            when (page) {
                0 -> HomeScreen(vm)
                1 -> AiScreen(vm)
                else -> ProfileScreen(vm)
            }
        }
    }
    }
}

@Composable
private fun XcBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(TabItem("首页", 0), TabItem("AI", 1), TabItem("个人", 2))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Paper)
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(30.dp),
            color = Ink,
            shadowElevation = 10.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val active = index == selected
                    val bg by animateColorAsState(
                        if (active) Color(0xFF2C3C36) else Color.Transparent,
                        animationSpec = tween(260), label = "tab-bg",
                    )
                    val contentColor by animateColorAsState(
                        if (active) Paper else Color(0xFF8C9C96),
                        animationSpec = tween(260), label = "tab-fg",
                    )
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onSelect(index) }
                                .background(bg)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            when (tab.kind) {
                                0 -> Icon(Icons.Filled.Home, null, tint = contentColor, modifier = Modifier.size(19.dp))
                                1 -> Text("✦", fontSize = 17.sp, color = contentColor)
                                else -> Icon(Icons.Filled.Person, null, tint = contentColor, modifier = Modifier.size(19.dp))
                            }
                            if (active) {
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    tab.label,
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}