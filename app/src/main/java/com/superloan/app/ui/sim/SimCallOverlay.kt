package com.superloan.app.ui.sim

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superloan.app.core.SimCall
import com.superloan.app.core.simCallScript
import kotlinx.coroutines.delay

// 仿系统来电界面的配色，刻意不走主题色 —— 它模仿的是「手机系统」，不是本应用
private val CallBg = Color(0xFF0E1116)
private val CallText = Color(0xFFF2F4F7)
private val CallMuted = Color(0xFF98A1AC)
private val CallAnswer = Color(0xFF22C55E)
private val CallReject = Color(0xFFE5484D)
private val CallAvatarBg = Color(0xFF2A3138)
private val CallBubble = Color(0xFF1C2128)

/**
 * 模拟来电的全屏覆盖层。
 *
 * 两个阶段：
 *  1. **响铃中** —— 大圆头像 + 「正在呼叫…」+ 绿色接听 / 红色拒接；
 *  2. **通话中** —— 催收员的话一句一句蹦出来（纯文字，不放音频），随时可以挂断。
 *
 * 若快进一次性欠下了多通电话，[pendingCount] 会大于 1，此时额外给一个「全部拒接」的活路。
 */
@Composable
fun SimCallOverlay(
    call: SimCall,
    pendingCount: Int,
    overdueDays: Int,
    overdueAmount: Double,
    onAnswered: () -> Unit,
    onRejected: () -> Unit,
    onRejectAll: () -> Unit,
    onHangUp: () -> Unit
) {
    // 状态按「这一通电话」记忆
    var talking by remember(call.id) { mutableStateOf(false) }
    var shownLines by remember(call.id) { mutableIntStateOf(1) }
    val script = remember(call.id, overdueDays, overdueAmount) {
        simCallScript(call.caller, overdueDays, overdueAmount)
    }

    // 通话中每隔 2.6 秒蹦一句，营造对方真的在说话的感觉
    LaunchedEffect(talking) {
        if (!talking) return@LaunchedEffect
        while (shownLines < script.size) {
            delay(2600)
            shownLines += 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---------- 顶部：来电方信息 ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (talking) "通话中" else "来电",
                    color = CallMuted,
                    fontSize = 14.sp
                )

                Box(
                    modifier = Modifier
                        .size(if (talking) 64.dp else 92.dp)
                        .clip(CircleShape)
                        .background(CallAvatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = call.caller.take(1),
                        color = CallText,
                        fontSize = if (talking) 26.sp else 38.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = call.caller,
                    color = CallText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (talking) {
                        "对方正在说话…"
                    } else {
                        "${call.arriveDate} ${call.arriveClock} · 正在呼叫…"
                    },
                    color = CallMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // ---------- 中部：通话对白 ----------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (talking) {
                    script.take(shownLines).forEachIndexed { index, line ->
                        val isCaller = index % 2 == 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isCaller) Arrangement.Start else Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isCaller) CallBubble else CallAnswer.copy(alpha = 0.18f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = line,
                                    color = CallText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // ---------- 底部：按钮 ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!talking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundCallButton(
                            label = "拒接",
                            color = CallReject,
                            glyph = "✕",
                            onClick = onRejected
                        )
                        RoundCallButton(
                            label = "接听",
                            color = CallAnswer,
                            glyph = "✆",
                            onClick = {
                                onAnswered()
                                talking = true
                            }
                        )
                    }
                } else {
                    if (shownLines < script.size) {
                        TextButton(onClick = { shownLines = script.size }) {
                            Text("跳过，我大概知道他要说什么了", color = CallMuted)
                        }
                    }
                    RoundCallButton(
                        label = "挂断",
                        color = CallReject,
                        glyph = "✕",
                        onClick = onHangUp
                    )
                }

                if (pendingCount > 1) {
                    Text(
                        text = "还有 ${pendingCount - 1} 通电话在排队",
                        color = CallMuted,
                        fontSize = 12.sp
                    )
                    TextButton(onClick = onRejectAll) {
                        Text("全部拒接（世界清净了）", color = CallReject)
                    }
                }
            }
        }
    }
}

/** 圆形通话按钮 */
@Composable
private fun RoundCallButton(
    label: String,
    color: Color,
    glyph: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = glyph, color = Color.White, fontSize = 26.sp)
        }
        Text(text = label, color = CallText, fontSize = 13.sp)
    }
}
