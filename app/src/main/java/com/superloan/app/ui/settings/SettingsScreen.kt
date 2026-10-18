package com.superloan.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.superloan.app.SuperLoanApp
import com.superloan.app.core.RepaymentType
import com.superloan.app.data.ThemeMode
import com.superloan.app.ui.components.ChoiceChips
import com.superloan.app.ui.components.InfoRow
import com.superloan.app.ui.components.SectionCard
import com.superloan.app.ui.theme.PaletteId
import com.superloan.app.ui.theme.isDynamicColorAvailable
import kotlinx.coroutines.launch

/** 设置页里的数据类危险操作 */
private enum class DataAction(
    val title: String,
    val message: String,
    val confirmText: String
) {
    CLEAR_ALL(
        title = "清空全部账单",
        message = "将删除所有账单及其还款打卡记录，此操作不可恢复。确定继续？",
        confirmText = "清空"
    ),
    RESTORE_SEED(
        title = "恢复示例数据",
        message = "将用初始的两条示例账单覆盖当前所有账单与还款记录。确定继续？",
        confirmText = "恢复"
    )
}

/**
 * 设置页：外观 / 账单 / 计算器 / 数据 / 关于。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as SuperLoanApp }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(app.settingsRepository, app.billRepository)
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingAction by remember { mutableStateOf<DataAction?>(null) }

    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.0" }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("设置") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------- 外观 ----------
            item {
                SettingsSection(title = "外观") {
                    Text(
                        text = "主题模式",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ChoiceChips(
                        options = ThemeMode.entries.map { it.title },
                        selectedIndex = ThemeMode.entries.indexOf(settings.themeMode).coerceAtLeast(0),
                        onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) }
                    )
                    Text(
                        text = settings.themeMode.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "配色方案",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PaletteChips(
                        selectedId = settings.paletteId,
                        onSelect = { viewModel.setPaletteId(it) }
                    )
                    Text(
                        text = if (isDynamicColorAvailable) {
                            "「动态取色」跟随系统壁纸取色（Material You），其余为内置色板"
                        } else {
                            "动态取色需要 Android 12 及以上，本机选它会自动回退到品牌绿"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- 账单 ----------
            item {
                SettingsSection(title = "账单") {
                    SwitchRow(
                        title = "列表默认展开全部期数",
                        desc = "打开后，每张账单卡片的期数清单默认处于展开状态",
                        checked = settings.billListExpanded,
                        onCheckedChange = { viewModel.setBillListExpanded(it) }
                    )
                    HorizontalDivider()
                    SwitchRow(
                        title = "新增账单起始月默认当前月",
                        desc = "新增账单时，首期账单月份自动填为当前月份",
                        checked = settings.newBillStartMonthCurrent,
                        onCheckedChange = { viewModel.setNewBillStartMonthCurrent(it) }
                    )
                }
            }

            // ---------- 计算器 ----------
            item {
                SettingsSection(title = "计算器") {
                    Text(
                        text = "月供计算器默认还款方式",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ChoiceChips(
                        options = RepaymentType.entries.map { it.title },
                        selectedIndex = RepaymentType.entries
                            .indexOfFirst { it.name == settings.defaultRepaymentType }
                            .coerceAtLeast(0),
                        onSelect = { viewModel.setDefaultRepaymentType(RepaymentType.entries[it].name) }
                    )
                    Text(
                        text = "打开「月供计算器」时默认选中的还款方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- 数据 ----------
            item {
                SettingsSection(title = "数据") {
                    Text(
                        text = "全部数据只保存在本机，应用不联网、不申请任何网络权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { pendingAction = DataAction.RESTORE_SEED },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("恢复示例数据")
                    }
                    OutlinedButton(
                        onClick = { pendingAction = DataAction.CLEAR_ALL },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清空全部账单")
                    }
                }
            }

            // ---------- 关于 ----------
            item {
                SettingsSection(title = "关于") {
                    InfoRow(label = "应用名称", value = "超级借钱")
                    InfoRow(label = "版本", value = versionName)
                    InfoRow(label = "联网权限", value = "无（完全离线）")
                    InfoRow(label = "数据存储", value = "仅本机 DataStore")
                    Text(
                        text = "一个纯粹的自用分期理财小工具：管账单、算真实利率、估月供。" +
                            "所有计算都在本机完成，不采集、不上传任何信息。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 危险操作二次确认
    val action = pendingAction
    if (action != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        DataAction.CLEAR_ALL -> {
                            viewModel.clearAllBills()
                            scope.launch { snackbarHostState.showSnackbar("已清空全部账单") }
                        }
                        DataAction.RESTORE_SEED -> {
                            viewModel.restoreSeedBills()
                            scope.launch { snackbarHostState.showSnackbar("已恢复示例数据") }
                        }
                    }
                    pendingAction = null
                }) {
                    Text(action.confirmText, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/** 设置分组：小标题 + 卡片 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        SectionCard(content = content)
    }
}

/** 开关行：整行可点 */
@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 色板选择：横向可滚动的色点芯片 */
@Composable
private fun PaletteChips(
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaletteId.entries.forEach { palette ->
            FilterChip(
                selected = palette.id == selectedId,
                onClick = { onSelect(palette.id) },
                label = { Text(palette.title) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(brushOf(palette))
                    )
                }
            )
        }
    }
}

/** 动态取色用彩虹渐变示意，其余色板用纯色 */
private fun brushOf(palette: PaletteId): Brush = if (palette.isDynamic) {
    Brush.sweepGradient(
        listOf(
            Color(0xFF2563EB),
            Color(0xFF16A34A),
            Color(0xFFEA580C),
            Color(0xFFDB2777),
            Color(0xFF2563EB)
        )
    )
} else {
    Brush.linearGradient(listOf(palette.swatch, palette.swatch))
}
