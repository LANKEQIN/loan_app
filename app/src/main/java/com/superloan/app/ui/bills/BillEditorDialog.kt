package com.superloan.app.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.superloan.app.core.addMonths
import com.superloan.app.core.formatMonthLabel
import com.superloan.app.core.formatMoney
import com.superloan.app.data.Bill
import com.superloan.app.data.newBillId
import com.superloan.app.ui.components.NumberField
import com.superloan.app.ui.components.PlainTextField
import com.superloan.app.ui.components.sanitizeDecimalInput
import com.superloan.app.ui.components.sanitizeIntInput
import kotlinx.coroutines.launch
import java.util.Locale

/** 输入框最长期数上限，避免用户误输入超大值导致界面卡死 */
private const val MAX_MONTHS = 600

/**
 * 账单新增 / 编辑表单（全屏弹窗）。
 *
 * 校验失败时用 Snackbar 提示，并把焦点移动到对应输入框。
 */
@Composable
fun BillEditorDialog(
    initial: Bill?,
    onDismiss: () -> Unit,
    onSave: (Bill) -> Unit,
    /** 新增账单时的默认首期月份（来自设置项「新增账单起始月默认当前月」），可为空 */
    defaultStartMonth: String? = null
) {
    val stateKey = initial?.id ?: "new_bill"

    var platform by remember(stateKey) { mutableStateOf(initial?.platform.orEmpty()) }
    var item by remember(stateKey) { mutableStateOf(initial?.item.orEmpty()) }
    var principalText by remember(stateKey) { mutableStateOf(initial?.principal.toEditText()) }
    var totalText by remember(stateKey) { mutableStateOf(initial?.totalAmount.toEditText()) }
    var monthsText by remember(stateKey) { mutableStateOf(initial?.months?.toString().orEmpty()) }
    var startMonth by remember(stateKey) { mutableStateOf(initial?.startMonth ?: defaultStartMonth) }
    var billingDayText by remember(stateKey) { mutableStateOf(initial?.billingDay?.toString().orEmpty()) }
    var repaymentDayText by remember(stateKey) { mutableStateOf(initial?.repaymentDay?.toString().orEmpty()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val focusPlatform = remember { FocusRequester() }
    val focusItem = remember { FocusRequester() }
    val focusPrincipal = remember { FocusRequester() }
    val focusTotal = remember { FocusRequester() }
    val focusMonths = remember { FocusRequester() }
    val focusStartMonth = remember { FocusRequester() }
    val focusBillingDay = remember { FocusRequester() }
    val focusRepaymentDay = remember { FocusRequester() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 提示 + 聚焦到出错字段
    fun fail(message: String, target: FocusRequester) {
        scope.launch { snackbarHostState.showSnackbar(message) }
        target.requestFocus()
    }

    fun submit() {
        val platformValue = platform.trim()
        if (platformValue.isEmpty()) {
            fail("请填写平台名称", focusPlatform)
            return
        }
        val itemValue = item.trim()
        if (itemValue.isEmpty()) {
            fail("请填写商品 / 用途", focusItem)
            return
        }
        val principalValue = principalText.toDoubleOrNull()
        if (principalValue == null) {
            fail("本金必须是数字", focusPrincipal)
            return
        }
        if (principalValue <= 0.0) {
            fail("本金必须大于 0", focusPrincipal)
            return
        }
        val totalValue = totalText.toDoubleOrNull()
        if (totalValue == null) {
            fail("总还款额必须是数字", focusTotal)
            return
        }
        if (totalValue < principalValue) {
            fail("总还款额不能小于本金", focusTotal)
            return
        }
        val monthsValue = monthsText.toIntOrNull()
        if (monthsValue == null) {
            fail("期数必须是数字", focusMonths)
            return
        }
        if (monthsValue < 1) {
            fail("期数至少为 1", focusMonths)
            return
        }
        if (monthsValue > MAX_MONTHS) {
            fail("期数不能超过 $MAX_MONTHS", focusMonths)
            return
        }
        val startValue = startMonth
        if (startValue == null) {
            fail("请选择首期账单月份", focusStartMonth)
            return
        }
        val billingValue = billingDayText.toIntOrNull()
        if (billingDayText.isNotEmpty() && (billingValue == null || billingValue !in 1..31)) {
            fail("出账日需为 1-31 的数字", focusBillingDay)
            return
        }
        val repaymentValue = repaymentDayText.toIntOrNull()
        if (repaymentDayText.isNotEmpty() && (repaymentValue == null || repaymentValue !in 1..31)) {
            fail("还款日需为 1-31 的数字", focusRepaymentDay)
            return
        }

        onSave(
            Bill(
                id = initial?.id ?: newBillId(),
                platform = platformValue,
                item = itemValue,
                principal = principalValue,
                totalAmount = totalValue,
                months = monthsValue,
                startMonth = startValue,
                billingDay = billingValue,
                repaymentDay = repaymentValue
            )
        )
    }

    // 实时预览：每期月供与末月
    val previewMonths = monthsText.toIntOrNull()
    val previewTotal = totalText.toDoubleOrNull()
    val previewMonthly = if (previewMonths != null && previewMonths > 0 && previewTotal != null) {
        previewTotal / previewMonths
    } else {
        null
    }
    val previewEndMonth = if (startMonth != null && previewMonths != null && previewMonths >= 1) {
        addMonths(startMonth!!, previewMonths - 1)
    } else {
        null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 顶栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                        Text(
                            text = if (initial == null) "新增账单" else "编辑账单",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()

                    // 表单
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlainTextField(
                                value = platform,
                                onValueChange = { platform = it },
                                label = "平台 *",
                                focusRequester = focusPlatform,
                                supportingText = "例如：京东白条 / 花呗 / 抖音月付 / 信用卡分期"
                            )
                            PlainTextField(
                                value = item,
                                onValueChange = { item = it },
                                label = "商品 / 用途 *",
                                focusRequester = focusItem,
                                supportingText = "买的是什么，或者这笔钱花在哪"
                            )
                            NumberField(
                                value = principalText,
                                onValueChange = { principalText = sanitizeDecimalInput(it) },
                                label = "本金（元）*",
                                focusRequester = focusPrincipal,
                                supportingText = "必须大于 0"
                            )
                            NumberField(
                                value = totalText,
                                onValueChange = { totalText = sanitizeDecimalInput(it) },
                                label = "总还款额（元）*",
                                focusRequester = focusTotal,
                                supportingText = "本金 + 全部利息 / 手续费，不能小于本金"
                            )
                            NumberField(
                                value = monthsText,
                                onValueChange = { monthsText = sanitizeIntInput(it) },
                                label = "分期期数 *",
                                focusRequester = focusMonths,
                                keyboardType = KeyboardType.Number,
                                supportingText = "1 - $MAX_MONTHS 期"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "首期账单月份 *",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { showMonthPicker = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusStartMonth)
                                ) {
                                    Text(startMonth?.let { formatMonthLabel(it) } ?: "请选择月份")
                                }
                            }

                            NumberField(
                                value = billingDayText,
                                onValueChange = { billingDayText = sanitizeIntInput(it) },
                                label = "每月出账日（选填）",
                                focusRequester = focusBillingDay,
                                keyboardType = KeyboardType.Number,
                                supportingText = "1 - 31，可留空"
                            )
                            NumberField(
                                value = repaymentDayText,
                                onValueChange = { repaymentDayText = sanitizeIntInput(it) },
                                label = "每月还款日（选填）",
                                focusRequester = focusRepaymentDay,
                                keyboardType = KeyboardType.Number,
                                supportingText = "1 - 31，可留空",
                            )

                            // 实时预览
                            if (previewMonthly != null || previewEndMonth != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (previewMonthly != null) {
                                            Text(
                                                text = "每期月供：${formatMoney(previewMonthly)}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        if (previewEndMonth != null) {
                                            Text(
                                                text = "末月账单：${formatMonthLabel(previewEndMonth)}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    HorizontalDivider()
                    // 底部操作
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = { submit() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initialMonth = startMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = {
                startMonth = it
                showMonthPicker = false
            }
        )
    }
}

/** 编辑态数字回填：整数不显示小数点，否则保留两位 */
private fun Double?.toEditText(): String {
    val value = this ?: return ""
    return if (value % 1.0 == 0.0) {
        String.format(Locale.CHINA, "%.0f", value)
    } else {
        String.format(Locale.CHINA, "%.2f", value)
    }
}
