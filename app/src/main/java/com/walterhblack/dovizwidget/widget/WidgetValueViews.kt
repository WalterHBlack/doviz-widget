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
import com.walterhblack.dovizwidget.data.currencyNames
import kotlin.math.roundToInt

private data class ValueRow(
    val row: Int,
    val content: Int,
    val codeView: Int,
    val value: Int,
    val target: Int,
)

private val valueRows = listOf(
    ValueRow(R.id.widget_row_0, R.id.widget_content_0, R.id.widget_code_0,
        R.id.widget_value_0, R.id.widget_target_0),
    ValueRow(R.id.widget_row_1, R.id.widget_content_1, R.id.widget_code_1,
        R.id.widget_value_1, R.id.widget_target_1),
    ValueRow(R.id.widget_row_2, R.id.widget_content_2, R.id.widget_code_2,
        R.id.widget_value_2, R.id.widget_target_2),
    ValueRow(R.id.widget_row_3, R.id.widget_content_3, R.id.widget_code_3,
        R.id.widget_value_3, R.id.widget_target_3),
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
    appWidgetId: Int,
    rowPadding: Float,
    decimalPlaces: Int,
    showRoundedSource: Boolean,
): RemoteViews = RemoteViews(context.packageName, R.layout.widget_values).apply {
    val density = context.resources.displayMetrics.density
    val verticalPadding = (rowPadding * density).roundToInt()
    val targetPadding = (6f * scale * density).roundToInt()
    val codes = currencyNames.keys.filter { it in favorites }.take(4)
    valueRows.forEachIndexed { index, row ->
        val code = codes.getOrNull(index)
        setViewVisibility(row.row, if (code != null) View.VISIBLE else View.GONE)
        setTextViewText(row.codeView, code.orEmpty())
        setViewPadding(row.content, (16f * scale * density).roundToInt(), verticalPadding,
            (16f * scale * density).roundToInt(), verticalPadding)
        setViewPadding(row.target, targetPadding, 0, 0, 0)
        setTextViewTextSize(row.codeView, TypedValue.COMPLEX_UNIT_SP, 15f * scale)
        setTextViewTextSize(row.value, TypedValue.COMPLEX_UNIT_SP, 18f * scale)
        setTextViewTextSize(row.target, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
        setTextViewText(row.target, code.orEmpty())
        if (code != null) {
            val intent = android.content.Intent(context, SelectWidgetSourceReceiver::class.java)
                .setData(android.net.Uri.parse("doviz://widget/$appWidgetId/source/$code"))
                .putExtra("widget_id", appWidgetId).putExtra("source", code)
            setOnClickPendingIntent(row.row, android.app.PendingIntent.getBroadcast(context, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
        }
    }
    setViewVisibility(R.id.widget_empty, if (favorites.isEmpty()) View.VISIBLE else View.GONE)
    setTextViewTextSize(R.id.widget_calculation_message, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
    setInt(R.id.widget_calculation_message, "setMaxLines", 2)
    setTextViewTextSize(R.id.widget_empty, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
    applyValues(expression, target, snapshot, favorites, decimalPlaces, showRoundedSource)
}

// Sabit XML kimlikleri sayesinde Glance kompozisyonu ve klavye kurulmadan
// yalnızca değişen metinler launcher'a gönderilir. Boyut/stil bu yamada değişmez.
internal fun updateWidgetValues(
    context: Context,
    appWidgetId: Int,
    expression: String,
    target: String,
    snapshot: RateSnapshot?,
    favorites: Set<String>,
    decimalPlaces: Int,
    showRoundedSource: Boolean,
) {
    val patch = RemoteViews(context.packageName, R.layout.widget_values).apply {
        applyValues(expression, target, snapshot, favorites, decimalPlaces, showRoundedSource)
    }
    AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetId, patch)
}

private fun RemoteViews.applyValues(expression: String, target: String, snapshot: RateSnapshot?, favorites: Set<String>,
    decimalPlaces: Int, showRoundedSource: Boolean) {
    val calculation = runCatching { WidgetCalculator.evaluate(expression) }
    val amount = calculation.getOrNull()
    val codes = currencyNames.keys.filter { it in favorites }.take(4)
    valueRows.forEachIndexed { index, row ->
        val code = codes.getOrNull(index)
        val converted = amount?.let { value ->
            if (code == target) value else if (code == null) null else snapshot?.let {
                runCatching {
                    val positive = CurrencyMath.convert(value.abs(), target, code, it.rates)
                    if (value.signum() < 0) positive.negate() else positive
                }.getOrNull()
            }
        }
        val selected = code == target
        setTextViewText(row.value, if (selected && !showRoundedSource) expression
            else converted?.let { CurrencyMath.format(it, decimalPlaces) } ?: "—")
        setInt(row.content, "setBackgroundColor", android.graphics.Color.parseColor(if (selected) "#18392E" else "#101B17"))
        val color = android.graphics.Color.parseColor(if (selected) "#A5F3CF" else "#F0FFF7")
        setTextColor(row.codeView, color)
        setTextColor(row.value, color)
        setContentDescription(row.row, "${code.orEmpty()}${if (selected) ", kaynak para birimi" else ", kaynak olarak seç"}")
    }
    val message = when {
        calculation.isFailure -> calculation.exceptionOrNull()?.message ?: "İşlemi tamamla."
        snapshot == null -> "Kurlar için ↻ tuşuna dokun."
        else -> ""
    }
    setTextViewText(R.id.widget_calculation_message, message)
    setViewVisibility(R.id.widget_calculation_message, if (message.isEmpty()) View.GONE else View.VISIBLE)
}
