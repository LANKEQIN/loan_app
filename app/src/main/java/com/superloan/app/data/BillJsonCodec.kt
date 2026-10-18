package com.superloan.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 手写 JSON 编解码（使用 Android 内置 org.json）。
 *
 * 之所以不用 kotlinx.serialization：本应用数据结构非常简单，
 * 手写可避免额外的编译期插件依赖，让工程更轻、编译更稳。
 */
object BillJsonCodec {

    fun encode(data: BillStoreData): String {
        val root = JSONObject()

        val billsArray = JSONArray()
        data.bills.forEach { bill ->
            val obj = JSONObject()
            obj.put("id", bill.id)
            obj.put("platform", bill.platform)
            obj.put("item", bill.item)
            obj.put("principal", bill.principal)
            obj.put("totalAmount", bill.totalAmount)
            obj.put("months", bill.months)
            obj.put("startMonth", bill.startMonth)
            obj.put("billingDay", bill.billingDay ?: JSONObject.NULL)
            obj.put("repaymentDay", bill.repaymentDay ?: JSONObject.NULL)
            billsArray.put(obj)
        }
        root.put("bills", billsArray)

        val paidObject = JSONObject()
        data.paid.forEach { (billId, months) ->
            paidObject.put(billId, JSONArray(months))
        }
        root.put("paid", paidObject)
        root.put("seeded", data.seeded)

        return root.toString()
    }

    fun decode(json: String?): BillStoreData {
        if (json.isNullOrBlank()) return BillStoreData()
        return try {
            val root = JSONObject(json)

            val billsArray = root.optJSONArray("bills") ?: JSONArray()
            val bills = ArrayList<Bill>(billsArray.length())
            for (i in 0 until billsArray.length()) {
                val obj = billsArray.optJSONObject(i)
                if (obj == null) continue
                val id = obj.optString("id", "")
                if (id.isEmpty()) continue
                bills += Bill(
                    id = id,
                    platform = obj.optString("platform", ""),
                    item = obj.optString("item", ""),
                    principal = obj.optDouble("principal", 0.0),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    months = obj.optInt("months", 0),
                    startMonth = obj.optString("startMonth", ""),
                    billingDay = obj.optNullableInt("billingDay"),
                    repaymentDay = obj.optNullableInt("repaymentDay")
                )
            }

            val paid = LinkedHashMap<String, List<String>>()
            val paidObject = root.optJSONObject("paid")
            if (paidObject != null) {
                val keys = paidObject.keys()
                while (keys.hasNext()) {
                    val billId = keys.next()
                    val monthsArray = paidObject.optJSONArray(billId) ?: continue
                    val months = ArrayList<String>(monthsArray.length())
                    for (i in 0 until monthsArray.length()) {
                        val month = monthsArray.optString(i, "")
                        if (month.isNotEmpty()) months += month
                    }
                    paid[billId] = months
                }
            }

            BillStoreData(
                bills = bills,
                paid = paid,
                seeded = root.optBoolean("seeded", false)
            )
        } catch (e: Exception) {
            // 数据损坏时降级为空数据，保证应用仍可正常启动
            BillStoreData()
        }
    }

    /** 读取可为空的整数字段 */
    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val value = optInt(key, -1)
        return if (value in 1..31) value else null
    }
}
