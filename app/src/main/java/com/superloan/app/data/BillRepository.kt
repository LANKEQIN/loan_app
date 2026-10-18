package com.superloan.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 账单数据在 DataStore 中的键 */
private val STORE_KEY = stringPreferencesKey("bill_store_json")

/**
 * 账单仓库：所有数据读写都落在本地 DataStore 中，完全离线。
 */
class BillRepository(private val appContext: Context) {

    private val store: DataStore<Preferences> = appContext.appDataStore

    /** 响应式数据流，UI 直接订阅 */
    val data: Flow<BillStoreData> = store.data.map { prefs ->
        BillJsonCodec.decode(prefs[STORE_KEY])
    }

    /** 读取当前快照 */
    suspend fun current(): BillStoreData = data.first()

    /**
     * 首次启动写入种子数据（仅当本地既没有账单、也没有写过种子时）。
     */
    suspend fun ensureSeeded() {
        store.edit { prefs ->
            val current = BillJsonCodec.decode(prefs[STORE_KEY])
            if (current.seeded || current.bills.isNotEmpty()) {
                // 已有数据：补写标记，避免后续被再次判定为首次启动
                if (!current.seeded) {
                    prefs[STORE_KEY] = BillJsonCodec.encode(current.copy(seeded = true))
                }
                return@edit
            }
            prefs[STORE_KEY] = BillJsonCodec.encode(
                BillStoreData(bills = seedBills(), paid = emptyMap(), seeded = true)
            )
        }
    }

    /**
     * 新增或更新账单。
     * 若期数 / 起始月被改小，会自动清理超出新周期的已还记录。
     */
    suspend fun upsert(bill: Bill) {
        store.edit { prefs ->
            val current = BillJsonCodec.decode(prefs[STORE_KEY])

            val bills = current.bills.toMutableList()
            val index = bills.indexOfFirst { it.id == bill.id }
            if (index >= 0) bills[index] = bill else bills.add(bill)

            val validMonths = bill.monthList.toSet()
            val keptMonths = current.paidMonthsOf(bill.id)
                .filter { it in validMonths }
                .sorted()

            val paid = current.paid.toMutableMap()
            if (keptMonths.isEmpty()) paid.remove(bill.id) else paid[bill.id] = keptMonths

            prefs[STORE_KEY] = BillJsonCodec.encode(
                current.copy(bills = bills, paid = paid)
            )
        }
    }

    /** 删除账单，并连带删除该账单的全部还款记录 */
    suspend fun delete(billId: String) {
        store.edit { prefs ->
            val current = BillJsonCodec.decode(prefs[STORE_KEY])
            val bills = current.bills.filterNot { it.id == billId }
            val paid = current.paid.toMutableMap()
            paid.remove(billId)
            prefs[STORE_KEY] = BillJsonCodec.encode(
                current.copy(bills = bills, paid = paid)
            )
        }
    }

    /** 勾选 / 取消勾选某一期 */
    suspend fun setPaid(billId: String, month: String, isPaid: Boolean) {
        store.edit { prefs ->
            val current = BillJsonCodec.decode(prefs[STORE_KEY])
            val months = current.paidMonthsOf(billId).toMutableList()
            if (isPaid) {
                if (month !in months) months.add(month)
            } else {
                months.remove(month)
            }
            months.sort()

            val paid = current.paid.toMutableMap()
            if (months.isEmpty()) paid.remove(billId) else paid[billId] = months

            prefs[STORE_KEY] = BillJsonCodec.encode(current.copy(paid = paid))
        }
    }

    /** 清空全部账单与还款记录（设置页的「清空全部账单」） */
    suspend fun clearAll() {
        store.edit { prefs ->
            val current = BillJsonCodec.decode(prefs[STORE_KEY])
            prefs[STORE_KEY] = BillJsonCodec.encode(
                current.copy(bills = emptyList(), paid = emptyMap(), seeded = true)
            )
        }
    }

    /** 重置为初始示例数据（设置页的「恢复示例数据」） */
    suspend fun resetToSeed() {
        store.edit { prefs ->
            val current = BillJsonCodec.decode(prefs[STORE_KEY])
            prefs[STORE_KEY] = BillJsonCodec.encode(
                current.copy(bills = seedBills(), paid = emptyMap(), seeded = true)
            )
        }
    }

    /** 首次启动的种子示例数据 */
    private fun seedBills(): List<Bill> = listOf(
        Bill(
            id = newBillId(),
            platform = "京东白条",
            item = "罗技G304X",
            principal = 198.07,
            totalAmount = 224.71,
            months = 24,
            startMonth = "2026-08",
            billingDay = 6,
            repaymentDay = 15
        ),
        Bill(
            id = newBillId(),
            platform = "抖音月付",
            item = "WPS超级会员1年",
            principal = 118.00,
            totalAmount = 127.96,
            months = 12,
            startMonth = "2026-08",
            billingDay = 10,
            repaymentDay = 20
        )
    )
}
