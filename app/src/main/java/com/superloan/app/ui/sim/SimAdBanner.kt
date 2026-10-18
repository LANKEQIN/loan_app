package com.superloan.app.ui.sim

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 伪装广告位卡片：故意做得又土又浮夸，模仿真实网贷 App 的信息流广告。
 *
 * 这是「作死模式」的入口 —— 它本来就长在账单列表里，所以不突兀；
 * 但点进去之后会发生什么，只有点过的人才知道。
 */
@Composable
fun SimAdBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF8A18), Color(0xFFE8262D))
                )
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 限时专属额度",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                // 真实广告位都会带这么个小角标
                Text(
                    text = "广告",
                    color = Color(0xCCFFFFFF),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }

            Text(
                text = "¥200,000",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "日息低至 0.03% · 3 分钟到账 · 新人首借免息",
                color = Color(0xE6FFFFFF),
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFE8262D)
                    )
                ) {
                    Text("立即领取", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "（纯属虚构，点进去就懂了）",
                    color = Color(0x99FFFFFF),
                    fontSize = 10.sp
                )
            }
        }
    }
}
