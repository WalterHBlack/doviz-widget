package com.walterhblack.dovizwidget.data

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CurrencyMathTest {
    private val rates = mapOf("EUR" to BigDecimal.ONE, "USD" to BigDecimal("1.25"), "TRY" to BigDecimal("50"), "GBP" to BigDecimal("0.8"))
    @Test fun crossConversionUsesBothRates() {
        assertEquals(0, BigDecimal("400").compareTo(CurrencyMath.convert(BigDecimal.TEN, "USD", "TRY", rates)))
    }
    @Test fun reverseConversionPreservesAmount() {
        val converted = CurrencyMath.convert(BigDecimal("123.45"), "GBP", "TRY", rates)
        assertEquals(0, BigDecimal("123.45").compareTo(CurrencyMath.convert(converted, "TRY", "GBP", rates)))
    }
    @Test fun sameCurrencyNeedsNoNetwork() {
        assertEquals(BigDecimal.TEN, CurrencyMath.convert(BigDecimal.TEN, "USD", "USD", emptyMap()))
    }
    @Test fun decimalCommaIsAcceptedButAmbiguousInputIsRejected() {
        assertEquals(BigDecimal("1250.50"), CurrencyMath.parseAmount("1250,50"))
        listOf("-1", "1.250,50", "NaN", "1e3", "", "1,2,3", "1234567890123").forEach { assertNull(CurrencyMath.parseAmount(it)) }
        assertEquals(BigDecimal.ZERO, CurrencyMath.parseAmount("0"))
    }
    @Test(expected = IllegalArgumentException::class) fun zeroRateIsRejected() {
        CurrencyMath.convert(BigDecimal.ONE, "USD", "TRY", rates + ("USD" to BigDecimal.ZERO))
    }
}
