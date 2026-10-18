package com.superloan.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.superloan.app.ui.bills.BillsScreen
import com.superloan.app.ui.irr.IrrScreen
import com.superloan.app.ui.loan.LoanScreen
import com.superloan.app.ui.settings.SettingsScreen
import com.superloan.app.ui.sim.SimRoot

/** 底部导航的四个页签（标题从简，完整名称显示在各页顶部栏） */
private enum class AppTab(val title: String, val icon: ImageVector) {
    BILLS("账单", Icons.AutoMirrored.Filled.List),
    IRR("IRR", Icons.Filled.Info),
    LOAN("月供", Icons.Filled.DateRange),
    SETTINGS("设置", Icons.Filled.Settings)
}

/**
 * 应用根容器：上方内容区 + 下方底部导航。
 * 内容区各页面自带 Scaffold（只负责自己的 TopAppBar 与 FAB），
 * 底部导航条由这里统一管理，避免重复消费系统安全区。
 *
 * 「作死模式」模拟器是一层全屏覆盖，开启时连底部导航一起盖住，
 * 因此主界面结构完全不受影响。
 */
@Composable
fun AppRoot() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var simOpen by rememberSaveable { mutableStateOf(false) }

    if (simOpen) {
        SimRoot(onExit = { simOpen = false })
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> BillsScreen(onOpenSim = { simOpen = true })
                1 -> IrrScreen()
                2 -> LoanScreen()
                else -> SettingsScreen()
            }
        }

        NavigationBar {
            AppTab.entries.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                    label = { Text(tab.title) }
                )
            }
        }
    }
}
