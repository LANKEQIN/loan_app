package com.superloan.app.ui.loan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superloan.app.core.RateLevel
import com.superloan.app.core.RateType
import com.superloan.app.core.LoanResult
import com.superloan.app.core.RepaymentRow
import com.superloan.app.core.RepaymentType
import com.superloan.app.core.calculateLoan
import com.superloan.app.core.comparisonTarget
import com.superloan.app.core.formatMoney
import com.superloan.app.core.formatPercent
import com.superloan.app.core.isBasicallyFlat
import com.superloan.app.core.loanLevel
import com.superloan.app.core.rateLevelOf
import com.superloan.app.core.toMonthlyRate
import com.superloan.app.ui.LocalAppSettings
import com.superloan.app.ui.components.Badge
import com.superloan.app.ui.components.ChoiceChips
import com.superloan.app.ui.components.InfoRow
import com.superloan.app.ui.components.NumberField
import com.superloan.app.ui.components.RateBadge
import com.superloan.app.ui.components.SectionCard
import com.superloan.app.ui.components.sanitizeDecimalInput
import com.superloan.app.ui.components.sanitizeIntInput

/** 还款计划表期数上限，防止误输入超大值 */
private const val MAX_MONTHS = 600

// 还款计划表列宽权重
private const val W_PERIOD = 0.6f
private const val W_PAYMENT = 1.15f
private const val W_PRINCIPAL = 1.15f
private const val W_INTEREST = 1.0f
private const val W_REMAINING = 1.45f

/**
 * 模块三：月供计算器。
 *
 * 输入本金 / 期数 / 利率 / 利率类型 / 还款方式，
 * 输出月供、总息费与完整还款计划表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current

    var principalText by rememberSaveable { mutableStateOf("") }
    var monthsText by rememberSaveable { mutableStateOf("") }
    var rateText by rememberSaveable { mutableStateOf("") }
    var rateTypeIndex by rememberSaveable { mutableIntStateOf(0) }
    // 默认还款方式取自设置项
    var repaymentIndex by rememberSaveable {
        mutableIntStateOf(
            RepaymentType.entries
                .indexOfFirst { it.name == settings.defaultRepaymentType }
                .coerceAtLeast(0)
        )
    }

    val focusPrincipal = remember { FocusRequester() }
    val focusMonths = remember { FocusRequester() }
    val focusRate = remember { FocusRequester() }

    val rateTypes = RateType.entries
    val repaymentTypes = RepaymentType.entries
    val rateType = rateTypes[rateTypeIndex.coerceIn(0, rateTypes.size - 1)]
    val repaymentType = repaymentTypes[repaymentIndex.coerceIn(0, repaymentTypes.size - 1)]

    val principal = principalText.toDoubleOrNull()
    val months = monthsText.toIntOrNull()
    val rateValue = rateText.toDoubleOrNull()

    val inputValid = principal != null && principal > 0.0 &&
        months != null && months in 1..MAX_MONTHS &&
        rateValue != null && rateValue >= 0.0

    // 切换利率类型或还款方式时会自动重算
    val result = remember(principal, months, rateValue, rateTypeIndex, repaymentIndex, inputValid) {
        if (!inputValid) {
            null
        } else {
            calculateLoan(
                principal = principal!!,
                months = months!!,
                monthlyRate = toMonthlyRate(rateValue!!, rateType),
                type = repaymentType
            )
        }
    }

    val otherType = comparisonTarget(repaymentType)
    val otherInterest = remember(principal, months, rateValue, rateTypeIndex, repaymentIndex, inputValid) {
        if (!inputValid) {
            null
        } else {
            calculateLoan(principal!!, months!!, toMonthlyRate(rateValue!!, rateType), otherType).totalInterest
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("月供计算器") }) }
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
                        label = "本金（元）",
                        focusRequester = focusPrincipal,
                        supportingText = "要借的本金"
                    )
                    NumberField(
                        value = monthsText,
                        onValueChange = { monthsText = sanitizeIntInput(it) },
                        label = "期数（月）",
                        focusRequester = focusMonths,
                        keyboardType = KeyboardType.Number,
                        supportingText = "1 - $MAX_MONTHS 期"
                    )
                    NumberField(
                        value = rateText,
                        onValueChange = { rateText = sanitizeDecimalInput(it) },
                        label = "利率（%）",
                        focusRequester = focusRate,
                        supportingText = if (rateType == RateType.ANNUAL) {
                            "年利率，例如 7.2 表示年化 7.2%"
                        } else {
                            "月利率 / 月费率，例如 0.75 表示每期 0.75%"
                        }
                    )

                    Text(
                        text = "利率类型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ChoiceChips(
                        options = rateTypes.map { it.title },
                        selectedIndex = rateTypeIndex,
                        onSelect = { rateTypeIndex = it }
                    )

                    Text(
                        text = "还款方式",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ChoiceChips(
                        options = repaymentTypes.map { it.title },
                        selectedIndex = repaymentIndex,
                        onSelect = { repaymentIndex = it }
                    )
                }
            }

            if (result != null) {
                item {
                    LoanResultCard(
                        result = result,
                        otherType = otherType,
                        otherInterest = otherInterest
                    )
                }
                item {
                    Text(
                        text = "还款计划表（共 ${result.rows.size} 期）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                item {
                    PlanTableHeader()
                }
                items(items = result.rows, key = { it.period }) { row ->
                    PlanTableRow(row)
                }
            } else {
                item {
                    SectionCard {
                        Text(
                            text = "填好本金、期数、利率并选择还款方式后，这里会显示月供、总息费与完整还款计划表。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** 月供结果卡片 */
@Composable
private fun LoanResultCard(
    result: LoanResult,
    otherType: RepaymentType,
    otherInterest: Double?
) {
    val level = loanLevel(result)

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
                "利率为 0，本金平摊，没有任何息费"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (level != null) level.color else MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        if (result.type == RepaymentType.PRINCIPAL) {
            InfoRow(
                label = "首月还款",
                value = formatMoney(result.firstPayment ?: 0.0),
                emphasize = true
            )
            InfoRow(
                label = "末月还款",
                value = formatMoney(result.lastPayment ?: 0.0)
            )
            // 仅 1 期时不显示递减
            if (result.rows.size > 1) {
                InfoRow(
                    label = "每月递减",
                    value = formatMoney(result.monthlyDecrease ?: 0.0)
                )
            }
        } else {
            InfoRow(
                label = "每月还款",
                value = formatMoney(result.monthlyPayment ?: 0.0),
                emphasize = true
            )
        }

        InfoRow(label = "总还款", value = formatMoney(result.totalPayment))
        InfoRow(label = "总息费", value = formatMoney(result.totalInterest))
        InfoRow(label = "名义年化利率", value = formatPercent(result.nominalAnnualRate))

        // 等本等费必须展示真实年化
        if (result.type == RepaymentType.FEE) {
            val realAnnual = result.realAnnualRate
            if (realAnnual != null) {
                InfoRow(
                    label = "真实年化 IRR",
                    value = "${formatPercent(realAnnual)}（名义 ${formatPercent(result.nominalAnnualRate)}）",
                    emphasize = true,
                    valueColor = rateLevelOf(realAnnual).color
                )
            }
        }

        if (otherInterest != null) {
            val difference = result.totalInterest - otherInterest
            val text = when {
                isBasicallyFlat(difference) -> "较${otherType.title}：基本持平"
                difference > 0.0 -> "较${otherType.title}：多付利息 ${formatMoney(difference)}"
                else -> "较${otherType.title}：少付利息 ${formatMoney(-difference)}"
            }
            InfoRow(
                label = "方式对比",
                value = text,
                valueColor = if (isBasicallyFlat(difference)) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/** 还款计划表表头 */
@Composable
private fun PlanTableHeader() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell("期数", W_PERIOD, TextAlign.Center, FontWeight.SemiBold)
            TableCell("月供", W_PAYMENT, TextAlign.End, FontWeight.SemiBold)
            TableCell("本金", W_PRINCIPAL, TextAlign.End, FontWeight.SemiBold)
            TableCell("利息", W_INTEREST, TextAlign.End, FontWeight.SemiBold)
            TableCell("剩余本金", W_REMAINING, TextAlign.End, FontWeight.SemiBold)
        }
    }
}

/** 还款计划表数据行 */
@Composable
private fun PlanTableRow(row: RepaymentRow) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(row.period.toString(), W_PERIOD, TextAlign.Center, FontWeight.Normal)
            TableCell(formatMoney(row.payment), W_PAYMENT, TextAlign.End, FontWeight.Normal)
            TableCell(formatMoney(row.principalPart), W_PRINCIPAL, TextAlign.End, FontWeight.Normal)
            TableCell(formatMoney(row.interest), W_INTEREST, TextAlign.End, FontWeight.Normal)
            TableCell(formatMoney(row.remaining), W_REMAINING, TextAlign.End, FontWeight.Normal)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** 表格单元格 */
@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    textAlign: TextAlign,
    fontWeight: FontWeight,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
        modifier = Modifier.weight(weight)
    )
}
