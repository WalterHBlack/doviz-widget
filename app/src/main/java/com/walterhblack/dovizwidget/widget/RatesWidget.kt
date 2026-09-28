package com.walterhblack.dovizwidget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.work.*
import com.walterhblack.dovizwidget.MainActivity
import com.walterhblack.dovizwidget.data.*
import kotlin.math.min

private val snapshotKey = stringPreferencesKey("snapshot")
private val favoritesKey = stringPreferencesKey("favorites")
private val statusKey = stringPreferencesKey("status")
private val expressionKey = stringPreferencesKey("calculator_expression")
private val targetKey = stringPreferencesKey("calculator_target")
private val choosingTargetKey = booleanPreferencesKey("choosing_target")
private val evaluatedKey = booleanPreferencesKey("calculator_evaluated")
private val keyParameter = ActionParameters.Key<String>("calculator_key")
private val accent = ColorProvider(Color(0xFFA5F3CF))
private val foreground = ColorProvider(Color(0xFFF0FFF7))
private val widgetBackground = Color(0xFF101B17)
private val widgetSurface = Color(0xFF182720)
private val muted = ColorProvider(Color(0xFF96ABA0))
private val rowDivider = Color(0xFF2B3C32)
private val numberKey = Color(0xFF29332F)
private val operationKey = Color(0xFF205A42)
private val utilityKey = Color(0xFF25463A)
private val equalsKey = Color(0xFF357C5B)
private val keyDivider = Color(0xFF101512)

private fun calculatorAction(key: String): Action = actionRunCallback<CalculatorAction>(
    actionParametersOf(keyParameter to key))

@Composable
private fun WidgetKey(label: String, modifier: GlanceModifier, action: Action, scale: Float) {
    val background = when (label) {
        "=" -> equalsKey
        "+", "−", "×", "÷" -> operationKey
        "C", "⌫", "↻" -> utilityKey
        else -> numberKey
    }
    val textColor = if (label in listOf("=", "+", "−", "×", "÷", "C", "⌫", "↻")) accent else foreground
    Box(modifier.background(keyDivider).padding(0.7.dp * scale)) {
        Box(GlanceModifier.fillMaxSize().background(background)
            .clickable(action), contentAlignment = Alignment.Center) {
            Text(label, style = TextStyle(color = textColor, fontSize = (20f * scale).sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

// Widget ayrı bir Android yüzeyidir; uygulama ekranının küçültülmüş kopyası değildir.
class RatesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = RateRepository(context)
        val initialSnapshot = repository.cached()
        val initialFavorites = repository.favorites()
        provideContent {
            val state = currentState<Preferences>()
            val snapshot = state[snapshotKey]?.let { runCatching { RateSnapshot.decode(it) }.getOrNull() } ?: initialSnapshot
            val favorites = state[favoritesKey]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: initialFavorites
            val expression = state[expressionKey] ?: "1"
            val target = state[targetKey]?.takeIf { it in currencyNames } ?: "TRY"
            val calculation = runCatching { WidgetCalculator.evaluate(expression) }
            val amount = calculation.getOrNull()
            val size = LocalSize.current
            val widthScale = (size.width.value / 300f).coerceIn(0.82f, 1.65f)
            val heightScale = (size.height.value / 560f).coerceIn(0.82f, 1.65f)
            val scale = min(widthScale, heightScale)
            val keyHeight = 42.dp * scale
            val sectionGap = 6.dp * scale
            Column(GlanceModifier.fillMaxSize().background(widgetBackground)) {
                Row(
                    GlanceModifier.fillMaxWidth()
                        .padding(horizontal = 16.dp * scale, vertical = 10.dp * scale)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text("DÖVİZ CEPTE", style = TextStyle(color = accent, fontSize = (13f * scale).sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    }
                    Text("↗", style = TextStyle(color = accent, fontSize = (17f * scale).sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
                Row(
                    GlanceModifier.fillMaxWidth().height(62.dp * scale).background(widgetSurface)
                        .padding(horizontal = 16.dp * scale),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text("Tutar", style = TextStyle(color = muted, fontSize = (10f * scale).sp))
                        Text(expression, style = TextStyle(color = foreground, fontSize = (24f * scale).sp), maxLines = 1)
                    }
                    Box(
                        GlanceModifier
                            .padding(horizontal = 10.dp * scale, vertical = 8.dp * scale)
                            .clickable(calculatorAction("target")),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Hedef", style = TextStyle(color = muted, fontSize = (10f * scale).sp))
                            Text("$target ▾", style = TextStyle(color = accent, fontSize = (16f * scale).sp, fontWeight = FontWeight.Bold), maxLines = 1)
                        }
                    }
                }
                Spacer(GlanceModifier.height(sectionGap))
                Column(
                    GlanceModifier.fillMaxWidth().defaultWeight()
                        .padding(horizontal = 16.dp * scale, vertical = 4.dp * scale)
                ) {
                    if (state[choosingTargetKey] == true) {
                        currencyNames.keys.toList().chunked(5).forEach { codes ->
                            Row(GlanceModifier.fillMaxWidth().height(40.dp * scale)) {
                                codes.forEach { code ->
                                    Box(GlanceModifier.defaultWeight().fillMaxHeight()
                                        .clickable(calculatorAction("target:$code")), contentAlignment = Alignment.Center) {
                                        Text(code, style = TextStyle(color = accent, fontSize = (12f * scale).sp), maxLines = 1)
                                    }
                                }
                            }
                        }
                    } else {
                            if (favorites.isEmpty()) Text("Uygulamadan yıldızla favori seç.",
                                style = TextStyle(color = foreground, fontSize = 13.sp))
                            currencyNames.keys.filter { it in favorites }.forEach { code ->
                                val converted = amount?.let { value ->
                                    if (code == target) value else snapshot?.let {
                                        val positive = CurrencyMath.convert(value.abs(), code, target, it.rates)
                                        if (value.signum() < 0) positive.negate() else positive
                                    }
                                }
                                Row(
                                    GlanceModifier.fillMaxWidth()
                                        .padding(vertical = 8.dp * scale),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(code, style = TextStyle(color = accent, fontSize = (15f * scale).sp, fontWeight = FontWeight.Bold))
                                    Text(converted?.let { CurrencyMath.format(it) } ?: "—",
                                        modifier = GlanceModifier.defaultWeight(),
                                        style = TextStyle(color = foreground, fontSize = (18f * scale).sp, textAlign = TextAlign.End), maxLines = 1)
                                    Text(target, modifier = GlanceModifier.padding(start = 6.dp * scale),
                                        style = TextStyle(color = muted, fontSize = (10f * scale).sp), maxLines = 1)
                                }
                                Box(GlanceModifier.fillMaxWidth().height(1.dp).background(rowDivider)) {}
                            }
                            if (calculation.isFailure) Text(calculation.exceptionOrNull()?.message ?: "İşlemi tamamla.",
                                style = TextStyle(color = accent, fontSize = (11f * scale).sp), maxLines = 1)
                            else if (snapshot == null) Text("Kurlar için ↻ tuşuna dokun.", style = TextStyle(color = accent, fontSize = (11f * scale).sp))
                        }
                    }
                Spacer(GlanceModifier.height(sectionGap))
                Text(state[statusKey]?.takeIf { it.isNotBlank() } ?: "Kur: ${snapshot?.date ?: "—"} · Günlük referans",
                    modifier = GlanceModifier.padding(horizontal = 16.dp * scale),
                    style = TextStyle(color = muted, fontSize = (10f * scale).sp), maxLines = 1)
                Spacer(GlanceModifier.height(sectionGap))
                    listOf(
                        listOf("7", "8", "9", "÷", "C"),
                        listOf("4", "5", "6", "×", "⌫"),
                        listOf("1", "2", "3", "−", "↻"),
                        listOf("0", "00", ",", "+", "=")
                    ).forEach { row ->
                        Row(GlanceModifier.fillMaxWidth().height(keyHeight)) {
                            row.forEach { key ->
                                WidgetKey(key, GlanceModifier.defaultWeight().fillMaxHeight(),
                                    if (key == "↻") actionRunCallback<RefreshAction>() else calculatorAction(key), scale)
                            }
                        }
                    }
            }
        }
    }
}

class CalculatorAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val key = parameters[keyParameter] ?: return
        updateAppWidgetState(context, glanceId) { state ->
            val expression = state[expressionKey] ?: "1"
            when {
                key == "target" -> state[choosingTargetKey] = !(state[choosingTargetKey] ?: false)
                key.startsWith("target:") -> {
                    val code = key.removePrefix("target:")
                    if (code in currencyNames) state[targetKey] = code
                    state[choosingTargetKey] = false
                }
                key == "=" -> {
                    runCatching { WidgetCalculator.evaluate(expression) }.getOrNull()?.let {
                        state[expressionKey] = WidgetCalculator.input(it)
                        state[evaluatedKey] = true
                    }
                }
                else -> {
                    val fresh = (state[evaluatedKey] ?: true) && (key.firstOrNull()?.isDigit() == true || key == ",")
                    state[expressionKey] = WidgetCalculator.edit(if (fresh) "0" else expression, key)
                    state[evaluatedKey] = false
                    state[choosingTargetKey] = false
                }
            }
        }
        RatesWidget().update(context, glanceId)
    }
}

class RatesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RatesWidget()
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        publishWidgetState(context, "Bağlantı bekleniyor…")
        WorkManager.getInstance(context).enqueueUniqueWork("manual-rates", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RateRefreshWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
}

// Kalıcı widget durumunu değiştirerek, açık widget oturumlarının da yeni veriyi görmesini sağlarız.
suspend fun publishWidgetState(context: Context, status: String = "") {
    val repository = RateRepository(context)
    GlanceAppWidgetManager(context).getGlanceIds(RatesWidget::class.java).forEach { id ->
        updateAppWidgetState(context, id) { prefs ->
            repository.cached()?.let { prefs[snapshotKey] = it.encode() }
            prefs[favoritesKey] = repository.favorites().joinToString(",")
            prefs[statusKey] = status
        }
    }
    RatesWidget().updateAll(context)
}
