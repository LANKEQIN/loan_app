package com.superloan.app.ui.sim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superloan.app.core.RateLevel
import com.superloan.app.core.SimProduct
import com.superloan.app.core.SIM_PRODUCTS
import com.superloan.app.core.SimStatus
import com.superloan.app.core.formatAmount
import com.superloan.app.core.formatMoney
import com.superloan.app.core.formatPercent
import com.superloan.app.core.rateLevelOf
import com.superloan.app.core.simMonthlyPayment
import com.superloan.app.ui.components.Badge
import com.superloan.app.ui.components.InfoRow
import com.superloan.app.ui.components.RateBadge
import com.superloan.app.ui.components.SectionCard
import kotlin.math.roundToInt

/**
 * 借钱页：把市面上常见的几款「产品」摆成一排，画风从正经银行一路崩到闪电贷。
 *
 * 年化利率用的是真实折算值，所以评级标签直接复用全项目共用的 [rateLevelOf]。
 */
@Composable
fun SimMarketPage(
    status: SimStatus,
    onBorrow: (SimProduct, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // 每个产品的借款金额滑块位置
    val amounts = remember { mutableStateMapOf<String, Float>() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard {
                InfoRow(label = "累计借到手", value = formatMoney(status.totalBorrowed), emphasize = true)
                InfoRow(label = "手上余额", value = formatMoney(status.wallet))
                Text(
                    text = "选一个产品、拖一拖滑块，钱立刻到账。「日息」「月费率」这类说法都是包装，" +
                        "左边这个年化才是真实成本。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(items = SIM_PRODUCTS, key = { it.id }) { product ->
            val amount = amounts.getOrPut(product.id) { product.maxAmount.toFloat() }
            ProductCard(
                product = product,
                amount = amount.toDouble(),
                enabled = !status.bankrupt,
                onAmountChange = { amounts[product.id] = it },
                onBorrow = { onBorrow(product, amount.toDouble()) }
            )
        }
    }
}

/** 单个借贷产品卡片 */
@Composable
private fun ProductCard(
    product: SimProduct,
    amount: Double,
    enabled: Boolean,
    onAmountChange: (Float) -> Unit,
    onBorrow: () -> Unit
) {
    val monthly = simMonthlyPayment(amount, product.annualRate, product.termMonths)
    val totalPay = monthly * product.termMonths

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            // 免息产品按项目规则不显示评级，改挂一个「免息」标签
            if (product.annualRate <= 0.0) {
                Badge(text = "免息（人情另算）", color = RateLevel.GREAT.color)
            } else {
                RateBadge(rateLevelOf(product.annualRate))
            }
        }

        Text(
            text = product.slogan,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "年化 ${formatPercent(product.annualRate)} · ${product.termMonths} 期 · " +
                "¥${formatAmount(product.minAmount)} ~ ¥${formatAmount(product.maxAmount)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "借款金额",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatMoney(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Slider(
            value = amount.toFloat(),
            onValueChange = { raw ->
                // 滑块按 100 元取整，避免出现 ¥3,247.83 这种手抖金额
                val stepped = (raw / 100.0).roundToInt() * 100.0
                onAmountChange(
                    stepped.coerceIn(product.minAmount, product.maxAmount).toFloat()
                )
            },
            valueRange = product.minAmount.toFloat()..product.maxAmount.toFloat()
        )

        InfoRow(label = "每月应还", value = formatMoney(monthly))
        InfoRow(label = "总还款额", value = formatMoney(totalPay), emphasize = true)

        Text(
            text = product.comment,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onBorrow,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("立即借款 ${formatMoney(amount)}")
        }
    }
}
