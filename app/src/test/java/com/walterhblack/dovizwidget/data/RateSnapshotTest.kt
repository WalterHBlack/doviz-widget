package com.walterhblack.dovizwidget.data

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class RateSnapshotTest {
    private val valid = """[{"date":"2026-09-18","base":"EUR","quote":"USD","rate":1.146},{"date":"2026-09-18","base":"EUR","quote":"TRY","rate":55.9077},{"date":"2026-09-18","base":"EUR","quote":"GBP","rate":0.8588}]"""
    private val liveFrankfurterResponse = """{"amount":1.0,"base":"EUR","date":"2026-09-18","rates":{"USD":1.146,"TRY":55.9077,"GBP":0.8588}}"""
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
