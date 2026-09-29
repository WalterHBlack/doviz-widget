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

data class RateSnapshot(
    val date: String,
    val fetchedAt: Long,
    val rates: Map<String, BigDecimal>,
    val rateDates: Map<String, String> = rates.keys.associateWith { date },
    val source: String = "ECB",
) {
    fun encode(): String = JSONObject().put("date", date).put("fetchedAt", fetchedAt)
        .put("rates", JSONObject(rates.mapValues { it.value.toPlainString() }))
        .put("rateDates", JSONObject(rateDates)).put("source", source).toString()

    companion object {
        fun decode(raw: String): RateSnapshot {
            val json = JSONObject(raw)
            val values = json.getJSONObject("rates")
            return RateSnapshot(json.getString("date"), json.getLong("fetchedAt"),
                // Önceki sürümün 10 birimlik kaydı internet yokken de kullanılabilir.
                currencyNames.keys.filter { values.has(it) }.associateWith {
                    values.getString(it).toBigDecimal().also { n -> require(n.signum() > 0) }
                }.also { require(it["EUR"]?.compareTo(BigDecimal.ONE) == 0) },
                currencyNames.keys.filter { values.has(it) }.associateWith {
                    json.optJSONObject("rateDates")?.optString(it, json.getString("date")) ?: json.getString("date")
                }, json.optString("source", "ECB"))
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
                return RateSnapshot(objectResponse.getString("date"), fetchedAt, rates, source = "Frankfurter")
            }

            val array = JSONArray(raw)
            val rates = mutableMapOf("EUR" to BigDecimal.ONE)
            val dates = mutableSetOf<String>()
            val rateDates = mutableMapOf<String, String>()
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                require(row.getString("base") == "EUR")
                val code = row.getString("quote")
                require(code in currencyNames.keys && code != "EUR" && code !in rates)
                rates[code] = row.get("rate").toString().toBigDecimal().also { require(it.signum() > 0) }
                val rateDate = LocalDate.parse(row.getString("date")).toString()
                dates += rateDate
                rateDates[code] = rateDate
            }
            require(setOf("USD", "TRY", "GBP").all { it in rates } && dates.isNotEmpty())
            // Kaynaklar farklı günlerde yayımlayabilir; eksik birimlere fiyat uydurmayız.
            val oldest = dates.min()
            val newest = dates.max()
            rateDates["EUR"] = newest
            val dateLabel = if (oldest == newest) newest else "$oldest – $newest"
            return RateSnapshot(dateLabel, fetchedAt, rates, rateDates, "Frankfurter")
        }
    }
}

// Repository, ekran ile veri kaynağı arasındaki köprüdür. Başarısız istekte son kayıt silinmez.
class RateRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rates", Context.MODE_PRIVATE)
    fun cached(): RateSnapshot? = prefs.getString("snapshot", null)?.let { runCatching { RateSnapshot.decode(it) }.getOrNull() }
    fun favorites(): Set<String> = prefs.getStringSet("favorites", setOf("USD", "EUR", "TRY", "GBP"))!!
        .filter { it in currencyNames }.take(4).toSet()
    fun setFavorites(codes: Set<String>) {
        prefs.edit().putStringSet("favorites", codes.filter { it in currencyNames }.take(4).toSet()).apply()
    }
    fun homeFavorites(): List<String> = prefs.getString("home_favorites", "USD,EUR,TRY,GBP")!!
        .split(',').filter { it in currencyNames }.distinct().ifEmpty { listOf("USD", "EUR", "TRY", "GBP") }
    fun setHomeFavorites(codes: List<String>) {
        val selected = codes.filter { it in currencyNames }.distinct()
        if (selected.isNotEmpty()) prefs.edit().putString("home_favorites", selected.joinToString(",")).apply()
    }
    fun theme(): String = prefs.getString("theme", "system")!!
    fun setTheme(value: String) { prefs.edit().putString("theme", value).apply() }
    fun uiScale(): String = prefs.getString("ui_scale", "normal")!!
    fun setUiScale(value: String) { prefs.edit().putString("ui_scale", value).apply() }
    fun sourceCurrency(): String = prefs.getString("source_currency", "USD")
        ?.takeIf { it in currencyNames } ?: "USD"
    fun setSourceCurrency(value: String) {
        if (value in currencyNames) prefs.edit().putString("source_currency", value).apply()
    }

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
        val ENDPOINT = "https://api.frankfurter.dev/v2/rates?base=EUR&quotes=" +
            currencyNames.keys.filter { it != "EUR" }.joinToString(",")
        private val networkLock = Mutex()
    }
}
