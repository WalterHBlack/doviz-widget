package com.walterhblack.dovizwidget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.math.BigDecimal

private val snapshotKey = stringPreferencesKey("snapshot")
private val favoritesKey = stringPreferencesKey("favorites")
private val statusKey = stringPreferencesKey("status")

// Widget ayrı bir Android yüzeyidir; uygulama ekranının küçültülmüş kopyası değildir.
class RatesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = RateRepository(context)
        val initialSnapshot = repository.cached()
        val initialFavorites = repository.favorites()
        provideContent {
            val state = currentState<Preferences>()
            val snapshot = state[snapshotKey]?.let { runCatching { RateSnapshot.decode(it) }.getOrNull() } ?: initialSnapshot
            val favorites = state[favoritesKey]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: initialFavorites
            val foreground = ColorProvider(Color(0xFFF0FFF7))
            val accent = ColorProvider(Color(0xFFA5F3CF))
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF14241F)).padding(16.dp)) {
                Column(GlanceModifier.fillMaxWidth().defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                    Text("DÖVİZ CEPTE", style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.height(8.dp))
                    if (snapshot == null) {
                        Text("Kurlar için Yenile’ye dokun.", style = TextStyle(color = foreground, fontSize = 14.sp))
                    } else if (favorites.isEmpty()) {
                        Text("Uygulamadan favori seç.", style = TextStyle(color = foreground, fontSize = 14.sp))
                    } else {
                        listOf("USD", "EUR", "GBP").filter { it in favorites }.forEach { code ->
                            Text("$code   ${CurrencyMath.format(CurrencyMath.convert(BigDecimal.ONE, code, "TRY", snapshot.rates), 4)} ₺",
                                style = TextStyle(color = foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    Text("Kur: ${snapshot?.date ?: "—"} · Günlük", style = TextStyle(color = accent, fontSize = 10.sp))
                    state[statusKey]?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = TextStyle(color = foreground, fontSize = 10.sp))
                    }
                }
                Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text("Yenile ↻", modifier = GlanceModifier.padding(10.dp).clickable(actionRunCallback<RefreshAction>()),
                        style = TextStyle(color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
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
