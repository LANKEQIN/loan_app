package com.superloan.app.core

import androidx.compose.ui.graphics.Color

/**
 * 利率评级（三个模块共用）。
 *
 * 按年化利率分档，颜色由绿到红渐进：
 *  < 4%        夯（一级）   神仙利率，闭眼借
 *  4% – 8%     顶级（二级） 银行良心价
 *  8% – 15%    人上人（三级）正常消费贷
 *  15% – 24%   NPC（四级）  网贷韭菜区
 *  ≥ 24%       拉完了（五级）快跑，是高利贷
 */
enum class RateLevel(
    val title: String,
    val levelText: String,
    val desc: String,
    val color: Color
) {
    GREAT("夯", "一级", "神仙利率，闭眼借", Color(0xFF16A34A)),
    TOP("顶级", "二级", "银行良心价", Color(0xFF4D9E1F)),
    GOOD("人上人", "三级", "正常消费贷", Color(0xFFC08A00)),
    NPC("NPC", "四级", "网贷韭菜区", Color(0xFFE2600C)),
    DONE("拉完了", "五级", "快跑，是高利贷", Color(0xFFDC2626));

    /** 完整标签：夯（一级） */
    val label: String get() = "$title（$levelText）"
}

/**
 * 根据年化利率取评级。
 * @param annualRate 小数形式的年化利率，例如 0.131541
 */
fun rateLevelOf(annualRate: Double): RateLevel {
    val percent = annualRate * 100.0
    return when {
        percent < 4.0 -> RateLevel.GREAT
        percent < 8.0 -> RateLevel.TOP
        percent < 15.0 -> RateLevel.GOOD
        percent < 24.0 -> RateLevel.NPC
        else -> RateLevel.DONE
    }
}
