package com.superloan.app.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superloan.app.core.currentMonth
import com.superloan.app.core.formatMonthLabel
import com.superloan.app.core.splitMonth
import java.util.Locale

/**
 * 月份选择弹窗：上一年 / 下一年 + 12 个月份芯片。
 */
@Composable
fun MonthPickerDialog(
    initialMonth: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val today = remember { currentMonth() }
    val base = remember(initialMonth, today) { splitMonth(initialMonth ?: today) }
    val baseYear = base?.first ?: 2026
    val baseMon = base?.second ?: 1

    var year by remember { mutableIntStateOf(baseYear) }
    var month by remember { mutableIntStateOf(baseMon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择首期账单月份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 年份切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year -= 1 }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上一年")
                    }
                    Text(
                        text = "$year 年",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { year += 1 }) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下一年")
                    }
                }

                // 12 个月份，每行 4 个
                (1..12).chunked(4).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowMonths.forEach { mon ->
                            FilterChip(
                                selected = mon == month,
                                onClick = { month = mon },
                                label = { Text("$mon 月", textAlign = TextAlign.Center) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Text(
                    text = "当前选择：${formatMonthLabel(String.format(Locale.CHINA, "%04d-%02d", year, month))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(String.format(Locale.CHINA, "%04d-%02d", year, month))
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
