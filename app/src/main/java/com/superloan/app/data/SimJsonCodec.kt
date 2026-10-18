package com.superloan.app.data

import com.superloan.app.core.SimCall
import com.superloan.app.core.SimCallStatus
import com.superloan.app.core.SimLoan
import com.superloan.app.core.SimMessage
import com.superloan.app.core.SimMessageKind
import com.superloan.app.core.SimPurchase
import org.json.JSONArray
import org.json.JSONObject

/**
 * 模拟器存档的 JSON 编解码（沿用 org.json，零额外依赖）。
 *
 * 约定：解码失败一律降级为空存档，保证 App 永远能正常启动。
 */
object SimJsonCodec {

    fun encode(data: SimStoreData): String {
        val root = JSONObject()
        root.put("startedToday", data.startedToday)
        root.put("fastForwardDays", data.fastForwardDays)
        root.put("wallet", data.wallet)
        root.put("bankruptcyCount", data.bankruptcyCount)

        val loans = JSONArray()
        data.loans.forEach { loan ->
            loans.put(
                JSONObject().apply {
                    put("id", loan.id)
                    put("productId", loan.productId)
                    put("productName", loan.productName)
                    put("annualRate", loan.annualRate)
                    put("principal", loan.principal)
                    put("termMonths", loan.termMonths)
                    put("monthlyPayment", loan.monthlyPayment)
                    put("startDay", loan.startDay)
                    put("dueDayOfMonth", loan.dueDayOfMonth)
                    put("paidPeriods", loan.paidPeriods)
                    put("borrowedDate", loan.borrowedDate)
                }
            )
        }
        root.put("loans", loans)

        val purchases = JSONArray()
        data.purchases.forEach { item ->
            purchases.put(
                JSONObject().apply {
                    put("itemId", item.itemId)
                    put("name", item.name)
                    put("emoji", item.emoji)
                    put("price", item.price)
                    put("boughtDate", item.boughtDate)
                }
            )
        }
        root.put("purchases", purchases)

        // 催收消息
        val messages = JSONArray()
        data.messages.forEach { message ->
            messages.put(
                JSONObject().apply {
                    put("id", message.id)
                    put("kind", message.kind.name)
                    put("sender", message.sender)
                    put("text", message.text)
                    put("arriveDay", message.arriveDay)
                    put("arriveDate", message.arriveDate)
                    put("arriveClock", message.arriveClock)
                }
            )
        }
        root.put("messages", messages)

        // 来电记录
        val calls = JSONArray()
        data.calls.forEach { call ->
            calls.put(
                JSONObject().apply {
                    put("id", call.id)
                    put("caller", call.caller)
                    put("arriveDay", call.arriveDay)
                    put("arriveDate", call.arriveDate)
                    put("arriveClock", call.arriveClock)
                    put("status", call.status.name)
                }
            )
        }
        root.put("calls", calls)

        root.put("readMessageCount", data.readMessageCount)
        root.put("watermarkDay", data.watermarkDay)

        return root.toString()
    }

    fun decode(json: String?): SimStoreData {
        if (json.isNullOrBlank()) return SimStoreData()
        return try {
            val root = JSONObject(json)

            val loans = ArrayList<SimLoan>()
            val loanArray = root.optJSONArray("loans")
            if (loanArray != null) {
                for (i in 0 until loanArray.length()) {
                    val obj = loanArray.optJSONObject(i) ?: continue
                    loans += SimLoan(
                        id = obj.optString("id"),
                        productId = obj.optString("productId"),
                        productName = obj.optString("productName"),
                        annualRate = obj.optDouble("annualRate", 0.0),
                        principal = obj.optDouble("principal", 0.0),
                        termMonths = obj.optInt("termMonths", 1),
                        monthlyPayment = obj.optDouble("monthlyPayment", 0.0),
                        startDay = obj.optInt("startDay", 0),
                        dueDayOfMonth = obj.optInt("dueDayOfMonth", 10).coerceIn(1, 28),
                        paidPeriods = obj.optInt("paidPeriods", 0).coerceAtLeast(0),
                        borrowedDate = obj.optString("borrowedDate")
                    )
                }
            }

            val purchases = ArrayList<SimPurchase>()
            val purchaseArray = root.optJSONArray("purchases")
            if (purchaseArray != null) {
                for (i in 0 until purchaseArray.length()) {
                    val obj = purchaseArray.optJSONObject(i) ?: continue
                    purchases += SimPurchase(
                        itemId = obj.optString("itemId"),
                        name = obj.optString("name"),
                        emoji = obj.optString("emoji"),
                        price = obj.optDouble("price", 0.0),
                        boughtDate = obj.optString("boughtDate")
                    )
                }
            }

            val messages = ArrayList<SimMessage>()
            val messageArray = root.optJSONArray("messages")
            if (messageArray != null) {
                for (i in 0 until messageArray.length()) {
                    val obj = messageArray.optJSONObject(i) ?: continue
                    messages += SimMessage(
                        id = obj.optString("id"),
                        kind = SimMessageKind.of(obj.optString("kind")),
                        sender = obj.optString("sender"),
                        text = obj.optString("text"),
                        arriveDay = obj.optInt("arriveDay", 0),
                        arriveDate = obj.optString("arriveDate"),
                        arriveClock = obj.optString("arriveClock", "10:00")
                    )
                }
            }

            val calls = ArrayList<SimCall>()
            val callArray = root.optJSONArray("calls")
            if (callArray != null) {
                for (i in 0 until callArray.length()) {
                    val obj = callArray.optJSONObject(i) ?: continue
                    calls += SimCall(
                        id = obj.optString("id"),
                        caller = obj.optString("caller"),
                        arriveDay = obj.optInt("arriveDay", 0),
                        arriveDate = obj.optString("arriveDate"),
                        arriveClock = obj.optString("arriveClock", "10:00"),
                        status = SimCallStatus.of(obj.optString("status"))
                    )
                }
            }

            SimStoreData(
                startedToday = root.optString("startedToday"),
                fastForwardDays = root.optInt("fastForwardDays", 0).coerceAtLeast(0),
                wallet = root.optDouble("wallet", 0.0),
                loans = loans,
                purchases = purchases,
                bankruptcyCount = root.optInt("bankruptcyCount", 0).coerceAtLeast(0),
                messages = messages,
                calls = calls,
                readMessageCount = root.optInt("readMessageCount", 0).coerceAtLeast(0),
                watermarkDay = root.optInt("watermarkDay", 0).coerceAtLeast(0)
            )
        } catch (e: Exception) {
            // 存档损坏时回退到空档，保证模拟器仍可打开
            SimStoreData()
        }
    }
}
