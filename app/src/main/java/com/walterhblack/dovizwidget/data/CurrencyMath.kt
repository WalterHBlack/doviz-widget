package com.walterhblack.dovizwidget.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Tüm kurlar EUR bazında saklanır. A → B: tutar / A_kuru × B_kuru.
object CurrencyMath {
    fun parseAmount(text: String): BigDecimal? {
        val clean = text.trim()
        if (!Regex("\\d{1,12}([.,]\\d{0,6})?").matches(clean)) return null
        return clean.replace(',', '.').toBigDecimalOrNull()
    }

    fun convert(amount: BigDecimal, from: String, to: String, rates: Map<String, BigDecimal>): BigDecimal {
        require(amount.signum() >= 0)
        if (from == to) return amount
        val source = requireNotNull(rates[from])
        val target = requireNotNull(rates[to])
        require(source.signum() > 0 && target.signum() > 0)
        return amount.divide(source, MathContext.DECIMAL128).multiply(target, MathContext.DECIMAL128)
    }

    fun format(value: BigDecimal, decimals: Int = 2): String =
        DecimalFormat("#,##0." + "0".repeat(decimals), DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")))
            .apply { roundingMode = RoundingMode.HALF_UP }.format(value)
}

val currencyNames = linkedMapOf(
    "TRY" to "Türk lirası",
    "USD" to "ABD doları",
    "EUR" to "Euro",
    "GBP" to "İngiliz sterlini",
    "JPY" to "Japon yeni",
    "CHF" to "İsviçre frangı",
    "CAD" to "Kanada doları",
    "AUD" to "Avustralya doları",
    "CNY" to "Çin yuanı",
    "INR" to "Hindistan rupisi"
)
