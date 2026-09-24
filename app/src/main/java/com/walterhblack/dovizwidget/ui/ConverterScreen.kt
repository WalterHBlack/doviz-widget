package com.walterhblack.dovizwidget.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walterhblack.dovizwidget.data.CurrencyMath
import com.walterhblack.dovizwidget.data.currencyNames
import com.walterhblack.dovizwidget.widget.RatesWidgetReceiver
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// @Composable: Bu fonksiyon veriden bir ekran parçası üretir; veri değişince ekran yenilenir.
@Composable
fun ConverterScreen(model: ConverterViewModel) {
    val dark = when (model.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val colors = if (dark) darkColorScheme(primary = Color(0xFFA5F3CF), background = Color(0xFF0B1513), surface = Color(0xFF14241F))
        else lightColorScheme(primary = Color(0xFF146747), background = Color(0xFFF4F8F3), surface = Color.White)
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            var amount by rememberSaveable { mutableStateOf("1") }
            var from by rememberSaveable { mutableStateOf("USD") }
            var to by rememberSaveable { mutableStateOf("TRY") }
            val parsed = CurrencyMath.parseAmount(amount)
            val snapshot = model.snapshot
            val result = parsed?.let { value ->
                if (from == to) value else snapshot?.let { CurrencyMath.convert(value, from, to, it.rates) }
            }
            val context = LocalContext.current
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("DÖVİZ CEPTE", color = colors.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                        Text("Bir bakışta döviz.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Hesapla, karşılaştır, ana ekranında takip et.", color = colors.onSurfaceVariant)
                Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Döviz çevirici", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = amount, onValueChange = {}, readOnly = true,
                            label = { Text("Tutar") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            isError = amount.isNotEmpty() && parsed == null,
                            supportingText = { if (amount.isNotEmpty() && parsed == null) Text("Örnek: 1250,50 • Binlik ayırıcı kullanma") })
                        CurrencyKeyboard(
                            value = amount,
                            onValueChange = { amount = it },
                            colors = colors
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CurrencyPicker("Kaynak", from, { from = it }, Modifier.weight(1f))
                            CurrencyPicker("Hedef", to, { to = it }, Modifier.weight(1f))
                        }
                        TextButton(onClick = { val previous = from; from = to; to = previous }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("⇄  Para birimlerini değiştir")
                        }
                        HorizontalDivider()
                        Text("KARŞILIĞI", color = colors.onSurfaceVariant, fontSize = 11.sp, letterSpacing = 2.sp)
                        Text(result?.let { "${CurrencyMath.format(it)} $to" } ?: "—", style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold, color = colors.primary)
                        if (snapshot != null) Text("1 $from = ${CurrencyMath.format(CurrencyMath.convert(BigDecimal.ONE, from, to, snapshot.rates), 4)} $to",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TL karşılıkları", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = model::refresh, enabled = !model.loading) { Text(if (model.loading) "Yükleniyor…" else "Yenile ↻") }
                }
                if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                model.error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodyMedium) }
                listOf("USD", "EUR", "GBP").forEach { code ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(currencyNames.getValue(code), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Text(snapshot?.let { "${CurrencyMath.format(CurrencyMath.convert(BigDecimal.ONE, code, "TRY", it.rates), 4)} ₺" } ?: "—",
                            fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { model.toggleFavorite(code) }) {
                            Text(if (code in model.favorites) "★" else "☆", fontSize = 24.sp,
                                modifier = Modifier.semanticsFavorite(code, code in model.favorites))
                        }
                    }
                }
                Text("Yıldızlı para birimleri widget’ında görünür.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Card(colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kurlar ana ekranında", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Uygulamayı açmadan favorilerini gör. Widget’taki kurlara dokunarak çeviriciyi açabilirsin.")
                        Button(onClick = {
                            val manager = AppWidgetManager.getInstance(context)
                            if (manager.isRequestPinAppWidgetSupported) {
                                manager.requestPinAppWidget(ComponentName(context, RatesWidgetReceiver::class.java), null, null)
                            } else Toast.makeText(context, "Ana ekrana uzun bas → Widget’lar → Döviz Cepte", Toast.LENGTH_LONG).show()
                        }) { Text("Widget ekle") }
                    }
                }
                Text("Görünüm", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "Sistem", "light" to "Açık", "dark" to "Koyu").forEach { (key, label) ->
                        FilterChip(selected = model.theme == key, onClick = { model.setAppearance(key) }, label = { Text(label) })
                    }
                }
                Text(buildString {
                    append("Günlük referans kuru · Frankfurter / ECB")
                    snapshot?.let {
                        append("\nKur tarihi: ${it.date}")
                        append("\nSon alınma: " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it.fetchedAt)))
                    }
                    append("\nAnlık banka alış/satış fiyatı değildir. Hafta sonu son iş gününün kuru gösterilebilir.")
                }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Telefon klavyesini açmadan tutar girmek için uygulamanın renkli sayı klavyesi. */
@Composable
private fun CurrencyKeyboard(value: String, onValueChange: (String) -> Unit, colors: ColorScheme) {
    fun press(key: String) {
        when (key) {
            "C" -> onValueChange("0")
            "⌫" -> onValueChange(value.dropLast(1).ifEmpty { "0" })
            "," -> if (!value.contains(',')) onValueChange("$value,")
            else -> if (value.length < 20) onValueChange(if (value == "0") key else "$value$key")
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHighest)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tutar klavyesi", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        KeyboardKey(key, colors.surfaceVariant, colors.onSurface, Modifier.weight(1f)) { press(key) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyboardKey("C", colors.errorContainer, colors.onErrorContainer, Modifier.weight(1f)) { press("C") }
                KeyboardKey("0", colors.surfaceVariant, colors.onSurface, Modifier.weight(1f)) { press("0") }
                KeyboardKey(",", colors.tertiaryContainer, colors.onTertiaryContainer, Modifier.weight(1f)) { press(",") }
                KeyboardKey("⌫", colors.secondaryContainer, colors.onSecondaryContainer, Modifier.weight(1f)) { press("⌫") }
            }
        }
    }
}

@Composable
private fun KeyboardKey(label: String, background: Color, foreground: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CurrencyPicker(label: String, code: String, onSelect: (String) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$code ▾") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                currencyNames.forEach { (key, name) -> DropdownMenuItem(text = { Text("$key · $name") }, onClick = { onSelect(key); expanded = false }) }
            }
        }
    }
}
