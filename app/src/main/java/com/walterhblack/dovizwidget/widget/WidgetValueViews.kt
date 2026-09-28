package com.walterhblack.dovizwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.walterhblack.dovizwidget.R
import com.walterhblack.dovizwidget.data.CurrencyMath
import com.walterhblack.dovizwidget.data.RateSnapshot
import com.walterhblack.dovizwidget.data.WidgetCalculator
import kotlin.math.roundToInt

private data class ValueRow(
    val code: String,
    val row: Int,
    val content: Int,
    val codeView: Int,
    val value: Int,
    val target: Int,
)

private val valueRows = listOf(
    ValueRow("TRY", R.id.widget_row_try, R.id.widget_content_try, R.id.widget_code_try,
        R.id.widget_value_try, R.id.widget_target_try),
    ValueRow("USD", R.id.widget_row_usd, R.id.widget_content_usd, R.id.widget_code_usd,
        R.id.widget_value_usd, R.id.widget_target_usd),
    ValueRow("EUR", R.id.widget_row_eur, R.id.widget_content_eur, R.id.widget_code_eur,
        R.id.widget_value_eur, R.id.widget_target_eur),
    ValueRow("GBP", R.id.widget_row_gbp, R.id.widget_content_gbp, R.id.widget_code_gbp,
        R.id.widget_value_gbp, R.id.widget_target_gbp),
    ValueRow("JPY", R.id.widget_row_jpy, R.id.widget_content_jpy, R.id.widget_code_jpy,
        R.id.widget_value_jpy, R.id.widget_target_jpy),
    ValueRow("CHF", R.id.widget_row_chf, R.id.widget_content_chf, R.id.widget_code_chf,
        R.id.widget_value_chf, R.id.widget_target_chf),
    ValueRow("CAD", R.id.widget_row_cad, R.id.widget_content_cad, R.id.widget_code_cad,
        R.id.widget_value_cad, R.id.widget_target_cad),
    ValueRow("AUD", R.id.widget_row_aud, R.id.widget_content_aud, R.id.widget_code_aud,
        R.id.widget_value_aud, R.id.widget_target_aud),
    ValueRow("CNY", R.id.widget_row_cny, R.id.widget_content_cny, R.id.widget_code_cny,
        R.id.widget_value_cny, R.id.widget_target_cny),
    ValueRow("INR", R.id.widget_row_inr, R.id.widget_content_inr, R.id.widget_code_inr,
        R.id.widget_value_inr, R.id.widget_target_inr),
)

internal fun expressionViews(context: Context, expression: String, scale: Float) =
    RemoteViews(context.packageName, R.layout.widget_expression).apply {
        setTextViewText(R.id.widget_expression, expression)
        setTextViewTextSize(R.id.widget_expression, TypedValue.COMPLEX_UNIT_SP, 24f * scale)
    }

internal fun valueViews(
    context: Context,
    expression: String,
    target: String,
    snapshot: RateSnapshot?,
    favorites: Set<String>,
    scale: Float,
): RemoteViews = RemoteViews(context.packageName, R.layout.widget_values).apply {
    val density = context.resources.displayMetrics.density
    val verticalPadding = (8f * scale * density).roundToInt()
    val targetPadding = (6f * scale * density).roundToInt()
    valueRows.forEach { row ->
        setViewVisibility(row.row, if (row.code in favorites) View.VISIBLE else View.GONE)
        setViewPadding(row.content, 0, verticalPadding, 0, verticalPadding)
        setViewPadding(row.target, targetPadding, 0, 0, 0)
        setTextViewTextSize(row.codeView, TypedValue.COMPLEX_UNIT_SP, 15f * scale)
        setTextViewTextSize(row.value, TypedValue.COMPLEX_UNIT_SP, 18f * scale)
        setTextViewTextSize(row.target, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        setTextViewText(row.target, target)
    }
    setViewVisibility(R.id.widget_empty, if (favorites.isEmpty()) View.VISIBLE else View.GONE)
    setTextViewTextSize(R.id.widget_calculation_message, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
    applyValues(expression, target, snapshot)
}

// Sabit XML kimlikleri sayesinde Glance kompozisyonu ve klavye kurulmadan
// yalnızca değişen metinler launcher'a gönderilir. Boyut/stil bu yamada değişmez.
internal fun updateWidgetValues(
    context: Context,
    appWidgetId: Int,
    expression: String,
    target: String,
    snapshot: RateSnapshot?,
) {
    val patch = RemoteViews(context.packageName, R.layout.widget_expression).apply {
        setTextViewText(R.id.widget_expression, expression)
        applyValues(expression, target, snapshot)
    }
    AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetId, patch)
}

private fun RemoteViews.applyValues(expression: String, target: String, snapshot: RateSnapshot?) {
    val calculation = runCatching { WidgetCalculator.evaluate(expression) }
    val amount = calculation.getOrNull()
    valueRows.forEach { row ->
        val converted = amount?.let { value ->
            if (row.code == target) value else snapshot?.let {
                val positive = CurrencyMath.convert(value.abs(), row.code, target, it.rates)
                if (value.signum() < 0) positive.negate() else positive
            }
        }
        setTextViewText(row.value, converted?.let { CurrencyMath.format(it) } ?: "—")
    }
    val message = when {
        calculation.isFailure -> calculation.exceptionOrNull()?.message ?: "İşlemi tamamla."
        snapshot == null -> "Kurlar için ↻ tuşuna dokun."
        else -> ""
    }
    setTextViewText(R.id.widget_calculation_message, message)
    setViewVisibility(R.id.widget_calculation_message, if (message.isEmpty()) View.GONE else View.VISIBLE)
}
