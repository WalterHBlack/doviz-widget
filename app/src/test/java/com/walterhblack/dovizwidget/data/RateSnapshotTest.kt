package com.walterhblack.dovizwidget.data

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class RateSnapshotTest {
    private val valid = """[{"date":"2026-09-18","base":"EUR","quote":"USD","rate":1.146},{"date":"2026-09-18","base":"EUR","quote":"TRY","rate":55.9077},{"date":"2026-09-18","base":"EUR","quote":"GBP","rate":0.8588},{"date":"2026-09-18","base":"EUR","quote":"JPY","rate":169.5},{"date":"2026-09-18","base":"EUR","quote":"CHF","rate":0.94},{"date":"2026-09-18","base":"EUR","quote":"CAD","rate":1.61},{"date":"2026-09-18","base":"EUR","quote":"AUD","rate":1.78},{"date":"2026-09-18","base":"EUR","quote":"CNY","rate":8.34},{"date":"2026-09-18","base":"EUR","quote":"INR","rate":96.4}]"""
    private val liveFrankfurterResponse = """{"amount":1.0,"base":"EUR","date":"2026-09-18","rates":{"USD":1.146,"TRY":55.9077,"GBP":0.8588,"JPY":169.5,"CHF":0.94,"CAD":1.61,"AUD":1.78,"CNY":8.34,"INR":96.4}}"""
    @Test fun completeResponseSurvivesCacheRoundTrip() {
        val value = RateSnapshot.fromResponse(valid, 123L)
        assertEquals(value, RateSnapshot.decode(value.encode()))
    }
    @Test fun liveFrankfurterResponseParses() {
        val value = RateSnapshot.fromResponse(liveFrankfurterResponse, 123L)
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
    @Test(expected = IllegalArgumentException::class) fun differentDatesCannotBeCombined() {
        RateSnapshot.fromResponse(valid.replaceFirst("2026-09-18", "2026-09-17"), 123L)
    }
    @Test(expected = IllegalArgumentException::class) fun negativeRateCannotBeCached() {
        RateSnapshot.fromResponse(valid.replace("55.9077", "-55"), 123L)
    }
}
