package com.superloan.app.ui.sim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superloan.app.core.SIM_SHOP_ITEMS_FUN
import com.superloan.app.core.SIM_SHOP_ITEMS_SERIOUS
import com.superloan.app.core.SimShopItem
import com.superloan.app.core.SimStatus
import com.superloan.app.core.formatMoney
import com.superloan.app.ui.components.SectionCard

/**
 * 商城：买买买。余额不够时不会拦着你，只会温柔地提醒你「可以再借一笔」。
 */
@Composable
fun SimShopPage(
    status: SimStatus,
    purchasesCount: Int,
    onBuy: (SimShopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前余额",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(status.wallet),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "已购",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$purchasesCount 件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = if (status.wallet <= 0.0) {
                        "余额见底了？切到「借钱」再借一笔就好，它们的额度永远为你敞开。"
                    } else {
                        "钱在手上就是别人的，花出去才是自己的。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle("正经区 · 买了就吃灰")
        }
        items(items = SIM_SHOP_ITEMS_SERIOUS, key = { it.id }) { item ->
            ShopItemCard(item = item, affordable = status.wallet >= item.price, onBuy = { onBuy(item) })
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle("整活区 · 买了就后悔")
        }
        items(items = SIM_SHOP_ITEMS_FUN, key = { it.id }) { item ->
            ShopItemCard(item = item, affordable = status.wallet >= item.price, onBuy = { onBuy(item) })
        }
    }
}

/** 分区小标题 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

/** 单个商品卡片 */
@Composable
private fun ShopItemCard(
    item: SimShopItem,
    affordable: Boolean,
    onBuy: () -> Unit
) {
    SectionCard {
        Text(text = item.emoji, fontSize = 30.sp)

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = formatMoney(item.price),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(2.dp))

        Button(
            onClick = onBuy,
            enabled = affordable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (affordable) "买" else "余额不足")
        }
    }
}
