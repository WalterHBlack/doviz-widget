package com.walterhblack.dovizwidget.data

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class RateSnapshotTest {
    // Yalnızca ayrıştırıcı için sentetik veriler; gerçek kur olarak kullanılmaz.
    private val fixtureRates = linkedMapOf(
        "USD" to "1.146", "TRY" to "55.9077", "GBP" to "0.8588",
        "JPY" to "169.5", "CHF" to "0.94", "CAD" to "1.61",
        "AUD" to "1.78", "CNY" to "8.34", "INR" to "96.4",
        "NOK" to "2.5",
        "SEK" to "2.5",
        "DKK" to "2.5",
        "PLN" to "2.5",
        "CZK" to "2.5",
        "HUF" to "2.5",
        "NZD" to "2.5",
        "SGD" to "2.5",
        "HKD" to "2.5",
        "ZAR" to "2.5",
        "KRW" to "2.5",
        "BRL" to "2.5",
        "MXN" to "2.5",
        "THB" to "2.5",
        "IDR" to "2.5",
        "MYR" to "2.5",
        "PHP" to "2.5",
        "RON" to "2.5",
        "ISK" to "2.5",
    ).apply {
        currencyNames.keys.filter { it != "EUR" }.forEach { putIfAbsent(it, "2.5") }
    }
    private val valid = org.json.JSONArray().apply {
        fixtureRates.forEach { (code, rate) ->
            put(org.json.JSONObject().put("date", "2026-09-18").put("base", "EUR")
                .put("quote", code).put("rate", BigDecimal(rate)))
        }
    }.toString()
    private val objectResponse = org.json.JSONObject().put("base", "EUR").put("date", "2026-09-18")
        .put("rates", org.json.JSONObject(fixtureRates.mapValues { BigDecimal(it.value) })).toString()
    @Test fun completeResponseSurvivesCacheRoundTrip() {
        val value = RateSnapshot.fromResponse(valid, 123L)
        assertEquals(value, RateSnapshot.decode(value.encode()))
    }
    @Test fun objectResponseParses() {
        val value = RateSnapshot.fromResponse(objectResponse, 123L)
        assertEquals("2026-09-18", value.date)
        assertEquals(BigDecimal("1.146"), value.rates["USD"])
        assertEquals(BigDecimal("55.9077"), value.rates["TRY"])
        assertEquals(BigDecimal("0.8588"), value.rates["GBP"])
        assertEquals(BigDecimal("169.5"), value.rates["JPY"])
        assertEquals(BigDecimal("96.4"), value.rates["INR"])
    }
    @Test(expected = IllegalArgumentException::class) fun incompleteResponseCannotReplaceGoodCache() {
        RateSnapshot.fromResponse("[]", 123L)
    }
    @Test fun differentDatesArePreserved() {
        val value = RateSnapshot.fromResponse(valid.replaceFirst("2026-09-18", "2026-09-17"), 123L)
        assertEquals("2026-09-17 – 2026-09-18", value.date)
        assertEquals("2026-09-17", value.rateDates["USD"])
        assertEquals("2026-09-18", value.rateDates["TRY"])
        assertEquals(value, RateSnapshot.decode(value.encode()))
    }
    @Test(expected = IllegalArgumentException::class) fun negativeRateCannotBeCached() {
        RateSnapshot.fromResponse(valid.replace("55.9077", "-55"), 123L)
    }
}
