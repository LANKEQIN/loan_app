package com.superloan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superloan.app.data.AppSettings
import com.superloan.app.ui.AppRoot
import com.superloan.app.ui.LocalAppSettings
import com.superloan.app.ui.theme.SuperLoanTheme

/**
 * 单 Activity 宿主，全部界面由 Compose 绘制。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val settingsRepository = (application as SuperLoanApp).settingsRepository

        setContent {
            // 设置在最顶层订阅：主题模式与配色方案要立刻生效
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            SuperLoanTheme(settings = settings) {
                CompositionLocalProvider(LocalAppSettings provides settings) {
                    AppRoot()
                }
            }
        }
    }
}
