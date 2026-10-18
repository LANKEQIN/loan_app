package com.superloan.app.ui.sim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superloan.app.core.SimStatus
import com.superloan.app.core.formatDateLabel
import com.superloan.app.core.formatMoney
import com.superloan.app.core.simCollectionMessages
import com.superloan.app.ui.components.InfoRow
import com.superloan.app.ui.components.SectionCard

/**
 * 破产结局页：逾期满 3 期后接管整个屏幕。
 *
 * 这里不提供任何「继续挣扎」的按钮 —— 唯一的出路是清零重来。
 */
@Composable
fun SimBankruptPage(
    status: SimStatus,
    bankruptcyCount: Int,
    onReset: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages = remember(status.unpaidPeriods, status.overdueAmount) {
        simCollectionMessages(status.unpaidPeriods, status.overdueAmount)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- 结局标题 ----------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "你 已 破 产",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${formatDateLabel(status.virtualDate)} · 逾期 ${status.unpaidPeriods} 期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "第 ${bankruptcyCount} 次破产",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- 清算单 ----------
        item {
            SectionCard {
                Text(
                    text = "破产清算单",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                InfoRow(label = "累计借到手", value = formatMoney(status.totalBorrowed))
                InfoRow(label = "累计买买买", value = formatMoney(status.totalSpent))
                InfoRow(label = "累计还掉", value = formatMoney(status.totalRepaid))
                InfoRow(label = "罚息与违约金", value = formatMoney(status.totalPenalty))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                InfoRow(label = "还欠着", value = formatMoney(status.remainingAmount), emphasize = true)
                InfoRow(label = "手上余额", value = formatMoney(status.wallet))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ---------- 催收短信 ----------
        item {
            Text(
                text = "未读短信（${messages.size}）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        itemsIndexed(items = messages) { index, text ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "催收",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "第 ${index + 1} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ---------- 出路 ----------
        item {
            SectionCard {
                Text(
                    text = "唯一的出路",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "清零重来会把余额、借款和消费记录全部抹掉，从 ¥0 重新开始，" +
                        "并把这次的战绩（破产 +1）记下来。真实账单不受影响。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("清零重来，再当一次勇士", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("关掉这个页面，当作什么都没发生")
                }
            }
        }
    }
}
