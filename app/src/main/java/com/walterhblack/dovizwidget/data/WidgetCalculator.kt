package com.walterhblack.dovizwidget.data

import java.math.BigDecimal
import java.math.MathContext

/** Widget ifadelerini kod çalıştırmadan, çarpma/bölme önceliğiyle hesaplar. */
object WidgetCalculator {
    private val operators = setOf('+', '−', '×', '÷')
    private val math = MathContext.DECIMAL128

    fun edit(expression: String, key: String): String {
        if (key == "C") return "0"
        if (key == "⌫") return expression.dropLast(1).ifEmpty { "0" }
        if (expression.length >= 80) return expression
        if (key.length == 1 && key[0] in operators) {
            return if (expression.lastOrNull() in operators) expression.dropLast(1) + key else expression + key
        }
        val operand = expression.takeLastWhile { it !in operators }
        if (key == ",") {
            if (',' in operand) return expression
            return expression + if (operand.isEmpty()) "0," else ","
        }
        if (key !in listOf("0", "00", "1", "2", "3", "4", "5", "6", "7", "8", "9")) return expression
        val prefix = expression.dropLast(operand.length)
        val next = if (operand == "0") key.trimStart('0').ifEmpty { "0" } else operand + key
        if (next.count { it.isDigit() } > 24) return expression
        return (prefix + next).take(80)
    }

    fun evaluate(expression: String): BigDecimal {
        require(expression.isNotBlank() && expression.length <= 80) { "Tutarı yaz." }
        var index = 0
        fun number(): BigDecimal {
            val start = index
            if (index < expression.length && expression[index] == '−') index++
            while (index < expression.length && (expression[index].isDigit() || expression[index] == ',')) index++
            val raw = expression.substring(start, index).replace('−', '-').replace(',', '.')
            return raw.toBigDecimalOrNull() ?: throw IllegalArgumentException("İşlemi tamamla.")
        }
        fun term(): BigDecimal {
            var value = number()
            while (index < expression.length && expression[index] in setOf('×', '÷')) {
                val op = expression[index++]
                val right = number()
                if (op == '÷' && right.signum() == 0) throw IllegalArgumentException("Sıfıra bölünemez.")
                value = if (op == '×') value.multiply(right, math) else value.divide(right, math)
            }
            return value
        }
        var result = term()
        while (index < expression.length) {
            val op = expression[index++]
            require(op == '+' || op == '−') { "Geçersiz işlem." }
            val right = term()
            result = if (op == '+') result.add(right, math) else result.subtract(right, math)
        }
        require(result.abs() <= BigDecimal("999999999999999999999999")) { "Sonuç çok büyük." }
        return result
    }

    fun input(value: BigDecimal): String = value.setScale(6, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString().replace('.', ',').replace('-', '−')
}
