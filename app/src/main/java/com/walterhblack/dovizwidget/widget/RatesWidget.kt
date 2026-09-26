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

private fun calculatorAction(key: String): Action = actionRunCallback<CalculatorAction>(
    actionParametersOf(keyParameter to key))

@Composable
private fun WidgetKey(label: String, modifier: GlanceModifier, action: Action, scale: Float, operation: Boolean = false) {
    Box(modifier.background(Color.Black).padding(1.dp * scale)) {
        Box(GlanceModifier.fillMaxSize().background(if (operation) Color(0xFF285640) else Color(0xFF343B38))
            .clickable(action), contentAlignment = Alignment.Center) {
            Text(label, style = TextStyle(color = foreground, fontSize = (20f * scale).sp, fontWeight = FontWeight.Bold), maxLines = 1)
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
            val edgePadding = 8.dp * scale
            val keyHeight = 44.dp * scale
            val sectionGap = 4.dp * scale
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF14241F)).padding(edgePadding)) {
                Text("DÖVİZ CEPTE · Favoriler ↗", modifier = GlanceModifier.fillMaxWidth().padding(4.dp * scale)
                    .clickable(actionStartActivity<MainActivity>()),
                    style = TextStyle(color = accent, fontSize = (12f * scale).sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Row(GlanceModifier.fillMaxWidth().height(56.dp * scale), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight().padding(4.dp * scale)) {
                        Text("Tutar", style = TextStyle(color = accent, fontSize = (11f * scale).sp))
                        Text(expression, style = TextStyle(color = foreground, fontSize = (18f * scale).sp), maxLines = 1)
                    }
                    Text("Hedef: $target ▾", modifier = GlanceModifier.padding(10.dp * scale).clickable(calculatorAction("target")),
                        style = TextStyle(color = accent, fontSize = (14f * scale).sp, fontWeight = FontWeight.Bold))
                }
                Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    if (state[choosingTargetKey] == true) {
                        Row(GlanceModifier.fillMaxWidth().height(48.dp * scale)) {
                            currencyNames.keys.forEach { code ->
                                WidgetKey(code, GlanceModifier.defaultWeight().fillMaxHeight(), calculatorAction("target:$code"), scale, true)
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
                                Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp * scale), verticalAlignment = Alignment.CenterVertically) {
                                    Text(code, style = TextStyle(color = accent, fontSize = (15f * scale).sp, fontWeight = FontWeight.Bold))
                                    Text("${converted?.let { CurrencyMath.format(it) } ?: "—"} $target",
                                        modifier = GlanceModifier.defaultWeight(),
                                        style = TextStyle(color = foreground, fontSize = (15f * scale).sp, textAlign = TextAlign.End), maxLines = 1)
                                }
                            }
                            if (calculation.isFailure) Text(calculation.exceptionOrNull()?.message ?: "İşlemi tamamla.",
                                style = TextStyle(color = accent, fontSize = (11f * scale).sp), maxLines = 1)
                            else if (snapshot == null) Text("Kurlar için ↻ tuşuna dokun.", style = TextStyle(color = accent, fontSize = (11f * scale).sp))
                        }
                    }
                Text(state[statusKey]?.takeIf { it.isNotBlank() } ?: "Kur: ${snapshot?.date ?: "—"} · Günlük referans",
                    style = TextStyle(color = accent, fontSize = (10f * scale).sp), maxLines = 1)
                Spacer(GlanceModifier.height(sectionGap))
                    listOf(
                        listOf("7", "8", "9", "÷", "C"),
                        listOf("4", "5", "6", "×", "⌫"),
                        listOf("1", "2", "3", "−", "↻"),
                        listOf("0", "00", ",", "+", "=")
                    ).forEach { row ->
                        Row(GlanceModifier.fillMaxWidth().height(keyHeight)) {
                            row.forEachIndexed { column, key ->
                                WidgetKey(key, GlanceModifier.defaultWeight().fillMaxHeight(),
                                    if (key == "↻") actionRunCallback<RefreshAction>() else calculatorAction(key), scale, column >= 3)
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
