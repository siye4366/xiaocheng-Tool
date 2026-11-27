package com.xiaocheng.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 用于每个 Compose Dialog 自己的窗口。
 * Dialog 是独立 Window，根布局的监听收不到它的键盘 insets，
 * 所以有输入框的弹窗都要在自己的内容里调用一次。
 */
@Composable
fun ClearFocusOnKeyboardHidden() {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    DisposableEffect(view) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var pending: Runnable? = null
        val hideCheck = Runnable {
            val visible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            if (!visible) focusManager.clearFocus(force = true)
            pending = null
        }
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val visible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            if (visible) {
                pending?.let { handler.removeCallbacks(it) }
                pending = null
            } else if (pending == null) {
                pending = hideCheck
                handler.postDelayed(hideCheck, 500L)
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            pending?.let { handler.removeCallbacks(it) }
        }
    }
}
