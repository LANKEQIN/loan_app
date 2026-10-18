package com.superloan.app.ui.irr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superloan.app.core.IrrResult
import com.superloan.app.core.RateLevel
import com.superloan.app.core.calculateIrr
import com.superloan.app.core.formatMoney
import com.superloan.app.core.formatPercent
import com.superloan.app.ui.components.Badge
import com.superloan.app.ui.components.InfoRow
import com.superloan.app.ui.components.NumberField
import com.superloan.app.ui.components.RateBadge
import com.superloan.app.ui.components.SectionCard
import com.superloan.app.ui.components.sanitizeDecimalInput
import com.superloan.app.ui.components.sanitizeIntInput
import androidx.compose.ui.focus.FocusRequester

/** 期数上限，避免误输入超大值导致计算 / 渲染卡顿 */
private const val MAX_MONTHS = 600

/**
 * 模块二：IRR 计算器。
 *
 * 输入本金 / 期数 / 每期还款金额，算出真实月利率与年化利率，并给出利率评级。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrrScreen(modifier: Modifier = Modifier) {
    var principalText by rememberSaveable { mutableStateOf("") }
    var monthsText by rememberSaveable { mutableStateOf("") }
    var paymentText by rememberSaveable { mutableStateOf("") }

    val focusPrincipal = remember { FocusRequester() }
    val focusMonths = remember { FocusRequester() }
    val focusPayment = remember { FocusRequester() }

    val principal = principalText.toDoubleOrNull()
    val months = monthsText.toIntOrNull()
    val payment = paymentText.toDoubleOrNull()

    val inputValid = principal != null && principal > 0.0 &&
        months != null && months in 1..MAX_MONTHS &&
        payment != null && payment > 0.0

    val result: IrrResult? = remember(principal, months, payment, inputValid) {
        if (inputValid) calculateIrr(principal!!, payment!!, months!!) else null
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("IRR 计算器") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text(
                        text = "输入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    NumberField(
                        value = principalText,
                        onValueChange = { principalText = sanitizeDecimalInput(it) },
                        label = "商品本金（元）",
                        focusRequester = focusPrincipal,
                        supportingText = "实际到手的钱"
                    )
                    NumberField(
                        value = monthsText,
                        onValueChange = { monthsText = sanitizeIntInput(it) },
                        label = "分期期数",
                        focusRequester = focusMonths,
                        keyboardType = KeyboardType.Number,
                        supportingText = "1 - $MAX_MONTHS 期"
                    )
                    NumberField(
                        value = paymentText,
                        onValueChange = { paymentText = sanitizeDecimalInput(it) },
                        label = "每期还款金额（元）",
                        focusRequester = focusPayment,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        supportingText = "支持多位小数（如 9.3629）以达到精确结果；总还款 = 每期还款 × 期数"
                    )
                    Text(
                        text = "提示：文档 T1 示例中 9.36 是 224.71 ÷ 24 的四舍五入值，" +
                            "想复现 13.1541% 请输入 9.3629（或更多位小数）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (result != null) {
                item { IrrResultCard(result) }
            } else {
                item {
                    SectionCard {
                        Text(
                            text = "填好本金、期数、每期还款金额后，这里会显示月利率、年化利率与评级。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SectionCard {
                    Text(
                        text = "计算说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "现金流视角：第 0 期拿到本金，第 1..n 期每期支付「每期还款金额」。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "求解 NPV(r) = −本金 + Σ 每期还款 ÷ (1+r)^t = 0 的月利率 r。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "年化利率 = (1 + 月利率)^12 − 1（复利折算，不是简单 ×12）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "求解用二分法：初始区间 [0, 1]，最多 64 次迭代，收敛阈值 1e-10。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "当总还款额 ≤ 本金时视为免息，月利率与年化均返回 0，且不显示评级。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** IRR 结果卡片 */
@Composable
private fun IrrResultCard(result: IrrResult) {
    val level = result.level

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "计算结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (level != null) {
                RateBadge(level)
            } else {
                Badge(text = "免息", color = RateLevel.GREAT.color)
            }
        }

        Text(
            text = if (level != null) {
                "${level.levelText} · ${level.desc}"
            } else {
                "总还款额不超过本金，视为免息，不显示评级"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (level != null) level.color else MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        InfoRow(
            label = "月利率（IRR）",
            value = formatPercent(result.monthlyRate),
            emphasize = true
        )
        InfoRow(
            label = "年化利率（复利折算）",
            value = formatPercent(result.annualRate),
            emphasize = true,
            valueColor = level?.color ?: MaterialTheme.colorScheme.onSurface
        )
        InfoRow(label = "总还款", value = formatMoney(result.totalRepayment))
        InfoRow(label = "总息费", value = formatMoney(result.totalInterest))
    }
}
