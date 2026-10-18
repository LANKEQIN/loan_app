package com.superloan.app

import android.app.Application
import com.superloan.app.data.BillRepository
import com.superloan.app.data.SettingsRepository
import com.superloan.app.data.SimRepository

/**
 * 应用入口：持有全局唯一的本地仓库。
 * 本应用完全离线，不申请任何网络权限。
 */
class SuperLoanApp : Application() {

    /** 账单仓库（懒加载，进程内单例） */
    val billRepository: BillRepository by lazy { BillRepository(this) }

    /** 设置仓库（懒加载，进程内单例） */
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    /** 「作死模式」模拟器仓库（懒加载，进程内单例，与账单数据隔离） */
    val simRepository: SimRepository by lazy { SimRepository(this) }
}
