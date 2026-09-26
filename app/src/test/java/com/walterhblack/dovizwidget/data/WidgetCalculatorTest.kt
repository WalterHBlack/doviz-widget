package com.walterhblack.dovizwidget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetCalculatorTest {
    @Test fun respectsMultiplicationPrecedence() {
        assertEquals("14", WidgetCalculator.input(WidgetCalculator.evaluate("2+3×4")))
    }

    @Test fun evaluatesTurkishDecimalComma() {
        assertEquals("3,5", WidgetCalculator.input(WidgetCalculator.evaluate("1,5+2")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDivisionByZero() {
        WidgetCalculator.evaluate("10÷0")
    }

    @Test fun editReplacesTrailingOperator() {
        assertEquals("10−", WidgetCalculator.edit("10+", "−"))
    }
}
