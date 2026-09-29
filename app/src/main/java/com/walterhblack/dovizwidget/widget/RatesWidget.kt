package com.walterhblack.dovizwidget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.work.*
import com.walterhblack.dovizwidget.MainActivity
import com.walterhblack.dovizwidget.data.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

private val snapshotKey = stringPreferencesKey("snapshot")
private val favoritesKey = stringPreferencesKey("favorites")
private val statusKey = stringPreferencesKey("status")
private val expressionKey = stringPreferencesKey("calculator_expression")
private val sourceKey = stringPreferencesKey("calculator_source")
private val evaluatedKey = booleanPreferencesKey("calculator_evaluated")
private val keyParameter = ActionParameters.Key<String>("calculator_key")
private val partialInputParameter = ActionParameters.Key<Boolean>("partial_input_v3")
private val calculatorInputLock = Mutex()
// Kısmi güncellemeden sonra açık Glance oturumu boyut değiştirirse son girdiyi
// kullanır. Kalıcı kaynak DataStore'dur; bu önbellek yalnızca oturum içindir.
private val calculatorStates = ConcurrentHashMap<GlanceId, Preferences>()
private val accent = ColorProvider(Color(0xFFA5F3CF))
private val foreground = ColorProvider(Color(0xFFF0FFF7))
private val widgetBackground = Color(0xFF101B17)
private val widgetSurface = Color(0xFF182720)
private val muted = ColorProvider(Color(0xFF96ABA0))
private val numberKey = Color(0xFF29332F)
private val operationKey = Color(0xFF205A42)
private val utilityKey = Color(0xFF25463A)
private val equalsKey = Color(0xFF357C5B)
private val keyDivider = Color(0xFF101512)

// Aynı kur verisi her tuşta değişmez; tekrar tekrar JSON çözümlemeyelim.
private object WidgetSnapshotCache {
    private var raw: String? = null
    private var snapshot: RateSnapshot? = null

    @Synchronized
    fun get(value: String): RateSnapshot? {
        if (raw != value) {
            snapshot = runCatching { RateSnapshot.decode(value) }.getOrNull()
            raw = value
        }
        return snapshot
    }
}

private fun calculatorAction(key: String, partialInput: Boolean = true): Action = actionRunCallback<CalculatorAction>(
    actionParametersOf(keyParameter to key, partialInputParameter to partialInput))

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
        val initialSnapshot by lazy(LazyThreadSafetyMode.NONE) { repository.cached() }
        val initialFavorites by lazy(LazyThreadSafetyMode.NONE) { repository.favorites() }
        provideContent {
            val state = currentState<Preferences>()
            val inputState = calculatorStates[id] ?: state
            val snapshot = state[snapshotKey]?.let { WidgetSnapshotCache.get(it) } ?: initialSnapshot
            val favorites = state[favoritesKey]?.split(',')?.filter { it in currencyNames }?.take(4)?.toSet() ?: initialFavorites
            val expression = inputState[expressionKey] ?: "0,00"
            val source = inputState[sourceKey]?.takeIf { it in favorites }
                ?: currencyNames.keys.firstOrNull { it in favorites } ?: "USD"
            val size = LocalSize.current
            // Bazı launcher'lar XML'deki minimum yüksekliği daha küçük bir alana sıkıştırır.
            // Ölçeği gerçek yüksekliğe göre düşürerek son klavye satırını dışarı taşırmayız.
            val widthScale = size.width.value / 300f
            val heightScale = size.height.value / 460f
            val scale = min(widthScale, heightScale).coerceIn(0.58f, 1.65f)
            // Dört klavye satırı, 220x320 dp'lik minimum widget'ta da tamamen görünür.
            val keyHeight = 36.dp * scale
            val sectionGap = 4.dp * scale
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
                Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    AndroidRemoteViews(valueViews(context, expression, source, snapshot, favorites, scale,
                        (id as AppWidgetId).appWidgetId), modifier = GlanceModifier.fillMaxWidth())
                }
                Spacer(GlanceModifier.height(sectionGap))
                Text(state[statusKey]?.takeIf { it.isNotBlank() } ?: "Kur: ${snapshot?.date ?: "—"} · Günlük referans",
                    modifier = GlanceModifier.padding(horizontal = 16.dp * scale),
                    style = TextStyle(color = muted, fontSize = (10f * scale).sp), maxLines = 1)
                Spacer(GlanceModifier.height(sectionGap))
                // Glance Row/Column en fazla 10 doğrudan çocuk taşır. Dört satırı
                // gruplamak, son satırın ana Column sınırında düşmesini önler.
                Column(GlanceModifier.fillMaxWidth().height(keyHeight * 4)) {
                    listOf(
                        listOf("7", "8", "9", "÷", "C"),
                        listOf("4", "5", "6", "×", "⌫"),
                        listOf("1", "2", "3", "−", "↻"),
                        listOf("0", "00", ",", "+", "=")
                    ).forEach { row ->
                        Row(GlanceModifier.fillMaxWidth().height(keyHeight)) {
                            row.forEach { key ->
                                WidgetKey(key, GlanceModifier.defaultWeight().fillMaxHeight(),
                                    if (key == "↻") actionRunCallback<RefreshAction>()
                                    else calculatorAction(key), scale)
                            }
                        }
                    }
                }
            }
        }
    }
}

class CalculatorAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        calculatorInputLock.withLock {
            val key = parameters[keyParameter] ?: return
            lateinit var updatedState: Preferences
            updateAppWidgetState(context, glanceId) { state ->
                val expression = state[expressionKey] ?: "0,00"
                when {
                    key.startsWith("source:") -> {
                        val code = key.removePrefix("source:")
                        val favorites = state[favoritesKey]?.split(',')?.filter { it in currencyNames }?.take(4)?.toSet()
                            ?: RateRepository(context).favorites()
                        if (code in favorites) {
                            state[sourceKey] = code
                            state[evaluatedKey] = true
                        }
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
                    }
                }
                updatedState = state.toPreferences()
            }
            calculatorStates[glanceId] = updatedState
            // Eski APK'daki tuş ilk basışta yeni satır düzenini kurar.
            if (parameters[partialInputParameter] == true && glanceId is AppWidgetId) {
                val snapshot = updatedState[snapshotKey]?.let { WidgetSnapshotCache.get(it) }
                    ?: RateRepository(context).cached()
                val favorites = updatedState[favoritesKey]?.split(',')?.filter { it in currencyNames }?.take(4)?.toSet()
                    ?: RateRepository(context).favorites()
                try {
                    updateWidgetValues(context, glanceId.appWidgetId,
                        updatedState[expressionKey] ?: "0,00",
                        updatedState[sourceKey]?.takeIf { it in favorites }
                            ?: currencyNames.keys.firstOrNull { it in favorites } ?: "USD", snapshot, favorites)
                } catch (_: RuntimeException) {
                    RatesWidget().update(context, glanceId)
                }
            } else {
                RatesWidget().update(context, glanceId)
            }
        }
    }
}

class RatesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RatesWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { calculatorStates.remove(AppWidgetId(it)) }
        super.onDeleted(context, appWidgetIds)
    }
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
class SelectWidgetSourceReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        val id = intent.getIntExtra("widget_id", -1)
        val code = intent.getStringExtra("source") ?: return
        if (id < 0 || code !in currencyNames) return
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                CalculatorAction().onAction(context, AppWidgetId(id),
                    actionParametersOf(keyParameter to "source:$code", partialInputParameter to true))
            } finally { pending.finish() }
        }
    }
}
