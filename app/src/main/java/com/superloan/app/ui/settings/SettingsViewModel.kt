package com.superloan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superloan.app.data.AppSettings
import com.superloan.app.data.BillRepository
import com.superloan.app.data.SettingsRepository
import com.superloan.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页的状态持有者：读改设置项，并代为执行数据类操作。
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val billRepository: BillRepository
) : ViewModel() {

    /** 当前设置 */
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setPaletteId(paletteId: String) = update { it.copy(paletteId = paletteId) }

    fun setBillListExpanded(expanded: Boolean) = update { it.copy(billListExpanded = expanded) }

    fun setNewBillStartMonthCurrent(enabled: Boolean) =
        update { it.copy(newBillStartMonthCurrent = enabled) }

    fun setDefaultRepaymentType(name: String) = update { it.copy(defaultRepaymentType = name) }

    /** 清空全部账单与还款记录 */
    fun clearAllBills() {
        viewModelScope.launch { billRepository.clearAll() }
    }

    /** 恢复初始示例数据 */
    fun restoreSeedBills() {
        viewModelScope.launch { billRepository.resetToSeed() }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            billRepository: BillRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository, billRepository) }
        }
    }
}
