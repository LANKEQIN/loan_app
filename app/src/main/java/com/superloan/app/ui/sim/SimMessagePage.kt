package com.superloan.app.ui.sim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superloan.app.core.SimCall
import com.superloan.app.core.SimCallStatus
import com.superloan.app.core.SimMessage
import com.superloan.app.core.SimMessageKind
import com.superloan.app.ui.components.Badge
import com.superloan.app.ui.components.ChoiceChips
import com.superloan.app.ui.components.SectionCard

/** 收件箱的筛选档位 */
private val MESSAGE_FILTERS = listOf("全部", "催收", "亲友")

/**
 * 催收短信收件箱 + 未接来电记录。
 *
 * 消息与来电都是「事件」，不会因为后来把钱还上而消失 —— 你确实收到过它们。
 */
@Composable
fun SimMessagePage(
    messages: List<SimMessage>,
    calls: List<SimCall>,
    unreadCount: Int,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }

    // 进入这一页时把当时未读的消息记下来（用来打「新」标记），随后立刻整体标记已读
    val initiallyUnread = remember(messages) {
        messages.drop((messages.size - unreadCount).coerceAtLeast(0)).map { it.id }.toSet()
    }
    LaunchedEffect(messages.size) {
        if (unreadCount > 0) onMarkRead()
    }

    val filtered = when (filterIndex) {
        1 -> messages.filter { it.kind != SimMessageKind.FAMILY }
        2 -> messages.filter { it.kind == SimMessageKind.FAMILY }
        else -> messages
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------- 说明 ----------
        item {
            SectionCard {
                Text(
                    text = "全部是虚构内容",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "这里的每一通电话、每一条短信都是本机算出来的假货，不涉及任何真实催收。" +
                        "现实中若遇到暴力催收，请保留证据并依法维权。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- 来电记录 ----------
        item {
            CallLogCard(calls = calls)
        }

        // ---------- 筛选 ----------
        item {
            ChoiceChips(
                options = MESSAGE_FILTERS,
                selectedIndex = filterIndex,
                onSelect = { filterIndex = it }
            )
        }

        // ---------- 消息列表 ----------
        if (filtered.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = if (messages.isEmpty()) {
                            "收件箱是空的。还没逾期，平台暂时懒得理你。"
                        } else {
                            "这个分类下暂时没有消息。"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(items = filtered.reversed(), key = { it.id }) { message ->
                MessageCard(
                    message = message,
                    isNew = message.id in initiallyUnread
                )
            }
        }
    }
}

/** 来电记录卡片：汇总 + 最近几通 */
@Composable
private fun CallLogCard(calls: List<SimCall>) {
    val answered = calls.count { it.status == SimCallStatus.ANSWERED }
    val rejected = calls.count { it.status == SimCallStatus.REJECTED }
    val pending = calls.count { it.status == SimCallStatus.PENDING }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "来电记录",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "共 ${calls.size} 通",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "未接 ${calls.size - answered - rejected} 通 · 已接听 $answered 通 · 已拒接 $rejected 通" +
                if (pending > 0) "（还有 $pending 通在排队）" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (calls.isEmpty()) {
            Text(
                text = "还没有人给你打过电话 —— 别急，快了。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            calls.takeLast(8).reversed().forEach { call ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📞",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = call.caller,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${call.arriveDate} ${call.arriveClock}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Badge(
                        text = call.status.title,
                        color = when (call.status) {
                            SimCallStatus.ANSWERED -> MaterialTheme.colorScheme.primary
                            SimCallStatus.REJECTED -> MaterialTheme.colorScheme.error
                            SimCallStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }
            if (calls.size > 8) {
                Text(
                    text = "……以及更早的 ${calls.size - 8} 通（都记着呢）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 单条消息卡片 */
@Composable
private fun MessageCard(
    message: SimMessage,
    isNew: Boolean
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (isNew) {
                Badge(text = "新", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
            }
            Badge(text = message.kind.title, color = kindColor(message.kind))
        }

        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "${message.arriveDate} ${message.arriveClock}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 消息类型配色：亲友用主色，威胁类用错误色，其余用第三色 */
@Composable
private fun kindColor(kind: SimMessageKind): Color = when (kind) {
    SimMessageKind.FAMILY -> MaterialTheme.colorScheme.primary
    SimMessageKind.LEGAL,
    SimMessageKind.BLAST,
    SimMessageKind.CONTACT -> MaterialTheme.colorScheme.error

    else -> MaterialTheme.colorScheme.tertiary
}
