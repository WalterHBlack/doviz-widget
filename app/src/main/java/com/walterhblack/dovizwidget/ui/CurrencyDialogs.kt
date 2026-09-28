package com.walterhblack.dovizwidget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.walterhblack.dovizwidget.R
import com.walterhblack.dovizwidget.data.currencyNames
import java.text.Normalizer
import java.util.Locale

@Composable
internal fun CurrencyFlag(code: String, modifier: Modifier = Modifier) {
    val resource = when (code) {
        "TRY" -> R.drawable.flag_tr
        "USD" -> R.drawable.flag_us
        "EUR" -> R.drawable.flag_eu
        "GBP" -> R.drawable.flag_gb
        "JPY" -> R.drawable.flag_jp
        "CHF" -> R.drawable.flag_ch
        "CAD" -> R.drawable.flag_ca
        "AUD" -> R.drawable.flag_au
        "CNY" -> R.drawable.flag_cn
        "INR" -> R.drawable.flag_in
        "NOK" -> R.drawable.flag_no
        "SEK" -> R.drawable.flag_se
        "DKK" -> R.drawable.flag_dk
        "PLN" -> R.drawable.flag_pl
        "CZK" -> R.drawable.flag_cz
        "HUF" -> R.drawable.flag_hu
        "NZD" -> R.drawable.flag_nz
        "SGD" -> R.drawable.flag_sg
        "HKD" -> R.drawable.flag_hk
        "ZAR" -> R.drawable.flag_za
        "KRW" -> R.drawable.flag_kr
        "BRL" -> R.drawable.flag_br
        "MXN" -> R.drawable.flag_mx
        "THB" -> R.drawable.flag_th
        "IDR" -> R.drawable.flag_id
        "MYR" -> R.drawable.flag_my
        "PHP" -> R.drawable.flag_ph
        "RON" -> R.drawable.flag_ro
        "ILS" -> R.drawable.flag_il
        "ISK" -> R.drawable.flag_is
        else -> R.drawable.ic_currency
    }
    Image(painterResource(resource), "$code bayrağı", modifier.clip(RoundedCornerShape(6.dp)),
        contentScale = ContentScale.Crop)
}

private fun searchKey(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.forLanguageTag("tr-TR")).replace('ı', 'i'),
        Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencySelectionSheet(
    favorites: Set<String>,
    selected: String?,
    onFavorite: (String) -> Unit,
    onSelect: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    val visible = remember(query, onlyFavorites, favorites) {
        val search = searchKey(query.trim())
        currencyNames.entries.filter { (code, name) ->
            (!onlyFavorites || code in favorites) &&
                (search.isEmpty() || searchKey("$code $name").contains(search))
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding().padding(horizontal = 20.dp)) {
            Text(if (onSelect == null) "Widget favorileri" else "Para birimi seç",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(if (onSelect == null) "Yıldızladığın para birimleri ana ekran widget’ında görünür."
                else "İsme veya koda göre bul, satıra dokunarak seç.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Para birimi ara") },
                placeholder = { Text("Dolar, euro, GBP…") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = if (query.isNotEmpty()) { { TextButton(onClick = { query = "" }) { Text("Sil") } } } else null)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                FilterChip(!onlyFavorites, { onlyFavorites = false }, label = { Text("Tümü · ${currencyNames.size}") })
                FilterChip(onlyFavorites, { onlyFavorites = true }, label = { Text("Favoriler · ${favorites.size}") })
            }
            if (visible.isEmpty()) {
                Text(if (onlyFavorites && query.isBlank()) "Henüz favori seçmedin. Tümü sekmesinden yıldız ekleyebilirsin."
                    else "Eşleşen para birimi bulunamadı.", modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(visible, key = { it.key }) { (code, name) ->
                    Surface(color = if (selected == code) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().clickable {
                            if (onSelect == null) onFavorite(code) else onSelect(code)
                        }.padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            CurrencyFlag(code, Modifier.size(44.dp, 32.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(code, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(name, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            if (selected == code) Text("✓", color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { onFavorite(code) }) {
                                Text(if (code in favorites) "★" else "☆", fontSize = 26.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.semantics {
                                        contentDescription = if (code in favorites) "$code favorilerden çıkar" else "$code favorilere ekle"
                                    })
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(model: ConverterViewModel, onFavorites: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("‹  Geri") }
                    Text("Ayarlar", modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f).widthIn(max = 600.dp).fillMaxWidth()
                    .align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Sana göre Döviz Cepte", style = MaterialTheme.typography.headlineSmall)
                    SettingsSection("Görünüm", "Telefonunla uyumlu bir tema seç.") {
                        ChoiceButtons(listOf("system" to "Sistem", "light" to "Açık", "dark" to "Koyu"),
                            model.theme, model::setAppearance)
                    }
                    SettingsSection("Arayüz boyutu", "Para birimi satırları ve klavye birlikte ölçeklenir.") {
                        ChoiceButtons(listOf("compact" to "Küçük", "normal" to "Normal", "large" to "Büyük"),
                            model.uiScale, model::setInterfaceScale)
                    }
                    val scale = when (model.uiScale) { "compact" -> 0.9f; "large" -> 1.1f; else -> 1f }
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.padding(20.dp)) {
                            Text("ÖNİZLEME", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CurrencyFlag("USD", Modifier.size(44.dp * scale, 32.dp * scale))
                                Text("USD", Modifier.padding(start = 12.dp), fontSize = (20f * scale).sp)
                                Spacer(Modifier.weight(1f))
                                Text("0,00", fontSize = (26f * scale).sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    SettingsSection("Widget favorileri", "${model.favorites.size} para birimi seçili. Ana ekranda görmek istediklerini düzenle.") {
                        OutlinedButton(onClick = onFavorites, modifier = Modifier.fillMaxWidth()) { Text("Favorileri düzenle") }
                    }
                    SettingsSection("Kur verileri", "Frankfurter / Avrupa Merkez Bankası günlük referans kurları. Banka alış ve satış fiyatları değildir.") {
                        Text("Desteklenen para birimi: ${currencyNames.size}", style = MaterialTheme.typography.bodyMedium)
                        model.snapshot?.let { Text("Kur tarihi: ${it.date}", style = MaterialTheme.typography.bodySmall) }
                        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        OutlinedButton(onClick = model::refresh, enabled = !model.loading, modifier = Modifier.fillMaxWidth()) {
                            Text(if (model.loading) "Güncelleniyor…" else "Kurları güncelle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun ChoiceButtons(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected == value, onClick = { onSelect(value) },
                label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                modifier = Modifier.weight(1f))
        }
    }
}
