package com.walterhblack.dovizwidget.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class RateSnapshot(val date: String, val fetchedAt: Long, val rates: Map<String, BigDecimal>) {
    fun encode(): String = JSONObject().put("date", date).put("fetchedAt", fetchedAt)
        .put("rates", JSONObject(rates.mapValues { it.value.toPlainString() })).toString()

    companion object {
        fun decode(raw: String): RateSnapshot {
            val json = JSONObject(raw)
            val values = json.getJSONObject("rates")
            return RateSnapshot(json.getString("date"), json.getLong("fetchedAt"),
                currencyNames.keys.associateWith { values.getString(it).toBigDecimal().also { n -> require(n.signum() > 0) } })
        }

        fun fromResponse(raw: String, fetchedAt: Long): RateSnapshot {
            runCatching { JSONObject(raw) }.getOrNull()?.takeIf { it.has("rates") }?.let { objectResponse ->
                require(objectResponse.getString("base") == "EUR")
                val values = objectResponse.getJSONObject("rates")
                val rates = mutableMapOf("EUR" to BigDecimal.ONE)
                val targetCodes = currencyNames.keys.filter { it != "EUR" }
                for (code in targetCodes) {
                    require(values.has(code))
                    val rawRate = values.get(code)
                    val rate = rawRate.toString().toBigDecimal().also { require(it.signum() > 0) }
                    rates[code] = rate
                }
                require(rates.keys == currencyNames.keys)
                return RateSnapshot(objectResponse.getString("date"), fetchedAt, rates)
            }

            val array = JSONArray(raw)
            val rates = mutableMapOf("EUR" to BigDecimal.ONE)
            val dates = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                require(row.getString("base") == "EUR")
                val code = row.getString("quote")
                require(code in setOf("USD", "TRY", "GBP") && code !in rates)
                rates[code] = row.get("rate").toString().toBigDecimal().also { require(it.signum() > 0) }
                dates += LocalDate.parse(row.getString("date")).toString()
            }
            require(rates.keys == currencyNames.keys && dates.size == 1)
            return RateSnapshot(dates.single(), fetchedAt, rates)
        }
    }
}

// Repository, ekran ile veri kaynağı arasındaki köprüdür. Başarısız istekte son kayıt silinmez.
class RateRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rates", Context.MODE_PRIVATE)
    fun cached(): RateSnapshot? = prefs.getString("snapshot", null)?.let { runCatching { RateSnapshot.decode(it) }.getOrNull() }
    fun favorites(): Set<String> = prefs.getStringSet("favorites", setOf("USD", "EUR"))!!.toSet()
    fun setFavorites(codes: Set<String>) { prefs.edit().putStringSet("favorites", codes).apply() }
    fun theme(): String = prefs.getString("theme", "system")!!
    fun setTheme(value: String) { prefs.edit().putString("theme", value).apply() }

    suspend fun refresh(): RateSnapshot = withContext(Dispatchers.IO) {
        networkLock.withLock {
            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode}")
                val raw = connection.inputStream.bufferedReader().use { it.readText() }
                val snapshot = try { RateSnapshot.fromResponse(raw, System.currentTimeMillis()) }
                    catch (e: Exception) { throw IOException("Geçersiz kur yanıtı", e) }
                if (!prefs.edit().putString("snapshot", snapshot.encode()).commit()) throw IOException("Kayıt yapılamadı")
                snapshot
            } finally { connection.disconnect() }
        }
    }

    companion object {
        const val ENDPOINT = "https://api.frankfurter.dev/v2/providers/ecb/rates?base=EUR&quotes=USD,TRY,GBP"
        private val networkLock = Mutex()
    }
}
