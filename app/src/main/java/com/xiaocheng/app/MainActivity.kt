package com.xiaocheng.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.xiaocheng.app.ui.XiaoChengApp
import com.xiaocheng.app.ui.theme.XiaoChengTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XiaoChengTheme {
                XiaoChengApp(vm)
            }
        }
    }
}
