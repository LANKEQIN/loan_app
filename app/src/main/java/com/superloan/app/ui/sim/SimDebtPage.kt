package com.superloan.app.ui.sim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superloan.app.core.RateLevel
import com.superloan.app.core.SimLoanStatus
import com.superloan.app.core.SimPurchase
import com.superloan.app.core.SimStatus
import com.superloan.app.core.formatDateLabel
import com.superloan.app.core.formatMoney
import com.superloan.app.core.rateLevelOf
import com.superloan.app.ui.components.Badge
import com.superloan.app.ui.components.InfoRow
import com.superloan.app.ui.components.RateBadge
import com.superloan.app.ui.components.SectionCard

/**
 * 「我的负债」页：模拟器的核心。
 *
 * 上半部分是仪表盘（余额 / 欠款 / 逾期 / 征信分）与时间控制，
 * 下半部分是每一笔借款的还款卡。
 */
@Composable
fun SimDebtPage(
    status: SimStatus,
    fastForwardDays: Int,
    purchases: List<SimPurchase>,
    onFastForward: (Int) -> Unit,
    onBackToReality: () -> Unit,
    onRepay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val realElapsed = (status.dayIndex - fastForwardDays).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------- 逾期警告 ----------
        if (status.unpaidPeriods > 0) {
            item {
                SectionCard {
                    Text(
                        text = "⚠ 账单已逾期 ${status.unpaidPeriods} 期",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "催收电话正在路上。逾期满 3 期，就会迎来那个结局。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------- 负债仪表盘 ----------
        item {
            SectionCard {
                Text(
                    text = "负债总览",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                InfoRow(label = "手上余额", value = formatMoney(status.wallet))
                InfoRow(label = "剩余应还总额", value = formatMoney(status.remainingAmount), emphasize = true)
                InfoRow(
                    label = "其中已逾期",
                    value = formatMoney(status.overdueAmount),
                    valueColor = if (status.overdueAmount > 0.0) MaterialTheme.colorScheme.error else Color.Unspecified
                )
                InfoRow(label = "累计罚息违约金", value = formatMoney(status.totalPenalty))
                InfoRow(label = "累计已还", value = formatMoney(status.totalRepaid))
                InfoRow(
                    label = "净资产（余额 − 欠款）",
                    value = formatMoney(status.netWorth),
                    valueColor = if (status.netWorth < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                    emphasize = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "征信分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${status.creditScore}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = creditColor(status.creditScore)
                    )
                }
                LinearProgressIndicator(
                    progress = { status.creditScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
                Text(
                    text = "已逾期 ${status.unpaidPeriods} 期 × 每期扣 15 分；还清了分数就回来（现实里可没这么划算）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- 时间控制 ----------
        item {
            SectionCard {
                Text(
                    text = "时间",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDateLabel(status.virtualDate),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "开局至今：真实时间走了 $realElapsed 天" +
                        if (fastForwardDays > 0) "，手动快进了 $fastForwardDays 天" else "（正跟随现实时间）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "+1 天", 7 to "+7 天", 30 to "+30 天").forEach { (days, label) ->
                        OutlinedButton(
                            onClick = { onFastForward(days) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label)
                        }
                    }
                }

                if (fastForwardDays > 0) {
                    TextButton(
                        onClick = onBackToReality,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("回到现实（重新跟随真实日期）")
                    }
                }
            }
        }

        // ---------- 借款卡片 ----------
        if (status.loans.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = "还没有任何借款。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "去「借钱」挑一个产品，钱马上到账 —— 至于什么时候还，那是以后的事。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "我的借款（${status.loans.size} 笔）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            items(items = status.loans, key = { it.loan.id }) { loanStatus ->
                LoanCard(
                    loanStatus = loanStatus,
                    wallet = status.wallet,
                    onRepay = { onRepay(loanStatus.loan.id) }
                )
            }
        }

        // ---------- 消费记录 ----------
        if (purchases.isNotEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = "已买 ${purchases.size} 件 · 共 ${formatMoney(status.totalSpent)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    purchases.takeLast(6).reversed().forEach { purchase ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = purchase.emoji)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = purchase.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatMoney(purchase.price),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (purchases.size > 6) {
                        Text(
                            text = "……以及更早的 ${purchases.size - 6} 件（不提也罢）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---------- 说明 ----------
        item {
            Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = "本页所有数字都是虚构的，与「分期账单」里的真实数据完全无关。破产也不会伤到真账单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 单笔借款卡片 */
@Composable
private fun LoanCard(
    loanStatus: SimLoanStatus,
    wallet: Double,
    onRepay: () -> Unit
) {
    val loan = loanStatus.loan
    val affordable = wallet >= loanStatus.nextDueAmount

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = loan.productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (loan.annualRate <= 0.0) {
                Badge(text = "免息", color = RateLevel.GREAT.color)
            } else {
                RateBadge(rateLevelOf(loan.annualRate))
            }
        }

        Text(
            text = "本金 ${formatMoney(loan.principal)} · 月供 ${formatMoney(loan.monthlyPayment)} × ${loan.termMonths} 期",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "放款日 ${loan.borrowedDate} · 每月 ${loan.dueDayOfMonth} 日还款",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "已还 ${loan.paidPeriods} / ${loan.termMonths} 期",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(loanStatus.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { loanStatus.progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }

        if (loanStatus.isOverdue) {
            Text(
                text = "⚠ 已逾期 ${loanStatus.overdueDays} 天 · 罚息 ${formatMoney(loanStatus.penalty)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (loanStatus.remainingPeriods > 0) {
            Text(
                text = "下一期 ${loanStatus.nextDueDate?.let { formatDateLabel(it) } ?: "—"} · " +
                    "应还 ${formatMoney(loanStatus.nextDueAmount)}" +
                    if (loanStatus.isOverdue) "（含罚息）" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRepay,
                enabled = affordable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (affordable) {
                        "还本期 ${formatMoney(loanStatus.nextDueAmount)}"
                    } else {
                        "余额不足，去「借钱」再借一笔"
                    }
                )
            }
        } else {
            Text(
                text = "✓ 这笔已经还完了，难得。",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = RateLevel.GREAT.color
            )
        }
    }
}

/** 征信分颜色：100 绿 → 0 红 */
private fun creditColor(score: Int): Color = when {
    score >= 85 -> RateLevel.GREAT.color
    score >= 60 -> RateLevel.TOP.color
    score >= 40 -> RateLevel.GOOD.color
    score >= 20 -> RateLevel.NPC.color
    else -> RateLevel.DONE.color
}
