package com.superloan.app.ui.bills

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.superloan.app.core.RateLevel
import com.superloan.app.core.calculateIrr
import com.superloan.app.core.currentMonth
import com.superloan.app.core.formatDayLabel
import com.superloan.app.core.formatMonthLabel
import com.superloan.app.core.formatMoney
import com.superloan.app.core.formatPercent
import com.superloan.app.core.formatProgress
import com.superloan.app.data.Bill
import com.superloan.app.ui.components.Badge
import com.superloan.app.ui.components.RateBadge

/**
 * 单张账单卡片：概要信息 + 还款进度 + 可展开的全部期数清单。
 */
@Composable
fun BillCard(
    bill: Bill,
    paidMonths: List<String>,
    onTogglePaid: (String, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** 期数清单的初始展开状态（来自设置项「列表默认展开全部期数」） */
    defaultExpanded: Boolean = false
) {
    // 展开状态按账单 id 记忆，初始值取设置项
    var expanded by rememberSaveable(bill.id) { mutableStateOf(defaultExpanded) }

    val months = remember(bill.id, bill.startMonth, bill.months) { bill.monthList }
    val paidSet = remember(paidMonths) { paidMonths.toSet() }
    val paidCount = remember(months, paidSet) { months.count { it in paidSet } }
    val ratio = if (months.isEmpty()) 0f else paidCount.toFloat() / months.size

    val irr = remember(bill.id, bill.principal, bill.totalAmount, bill.months) {
        calculateIrr(bill.principal, bill.monthlyPayment, bill.months)
    }
    val todayMonth = remember { currentMonth() }
    val level = irr.level

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 平台名 + 评级标签 + 操作按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bill.platform,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                if (level != null) {
                    RateBadge(level)
                } else {
                    Badge(text = "免息", color = RateLevel.GREAT.color)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑账单")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除账单")
                }
            }

            // 商品名 · 月供 × 期数
            Text(
                text = "${bill.item} · ${formatMoney(bill.monthlyPayment)} × ${bill.months} 期",
                style = MaterialTheme.typography.bodyMedium
            )

            // 本金 · 含息/费金额
            Row {
                Text(
                    text = "本金 ${formatMoney(bill.principal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "含息/费 ${formatMoney(bill.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 出账 / 还款日
            Text(
                text = "出账 ${formatDayLabel(bill.billingDay)} · 还款 ${formatDayLabel(bill.repaymentDay)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 该笔账单的 IRR
            Text(
                text = if (irr.interestFree) {
                    "IRR 月利率 0.00% · 年化 0.00%（免息）"
                } else {
                    "IRR 月利率 ${formatPercent(irr.monthlyRate)} · 年化 ${formatPercent(irr.annualRate)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (level != null) level.color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // 还款进度
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "已还 $paidCount / ${months.size} 期",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatProgress(ratio.toDouble()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }

            // 展开 / 收起全部期数
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (expanded) "收起全部期数" else "展开全部期数（${months.size} 期）")
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    months.forEach { month ->
                        InstallmentRow(
                            month = month,
                            amount = bill.monthlyPayment,
                            isPaid = month in paidSet,
                            isCurrent = month == todayMonth,
                            onToggle = { checked -> onTogglePaid(month, checked) }
                        )
                    }
                }
            }
        }
    }
}

/** 单期还款行：复选框 + 月份 + 本月标签 + 金额 */
@Composable
private fun InstallmentRow(
    month: String,
    amount: Double,
    isPaid: Boolean,
    isCurrent: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val rowBackground = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackground)
            .clickable { onToggle(!isPaid) }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isPaid,
            onCheckedChange = { onToggle(it) }
        )
        Text(
            text = formatMonthLabel(month),
            style = MaterialTheme.typography.bodyMedium
        )
        if (isCurrent) {
            Spacer(Modifier.width(6.dp))
            Badge(
                text = "本月",
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = formatMoney(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isPaid) TextDecoration.LineThrough else null
        )
    }
}
