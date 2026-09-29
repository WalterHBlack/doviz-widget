package com.walterhblack.dovizwidget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
        "ISK" -> R.drawable.flag_is
        "AED" -> R.drawable.flag_ae
        "AFN" -> R.drawable.flag_af
        "ALL" -> R.drawable.flag_al
        "AMD" -> R.drawable.flag_am
        "AOA" -> R.drawable.flag_ao
        "ARS" -> R.drawable.flag_ar
        "AWG" -> R.drawable.flag_aw
        "AZN" -> R.drawable.flag_az
        "BAM" -> R.drawable.flag_ba
        "BBD" -> R.drawable.flag_bb
        "BDT" -> R.drawable.flag_bd
        "BHD" -> R.drawable.flag_bh
        "BIF" -> R.drawable.flag_bi
        "BMD" -> R.drawable.flag_bm
        "BND" -> R.drawable.flag_bn
        "BOB" -> R.drawable.flag_bo
        "BSD" -> R.drawable.flag_bs
        "BTN" -> R.drawable.flag_bt
        "BWP" -> R.drawable.flag_bw
        "BYN" -> R.drawable.flag_by
        "BZD" -> R.drawable.flag_bz
        "CDF" -> R.drawable.flag_cd
        "CLP" -> R.drawable.flag_cl
        "CNH" -> R.drawable.flag_cn
        "COP" -> R.drawable.flag_co
        "CRC" -> R.drawable.flag_cr
        "CUP" -> R.drawable.flag_cu
        "CVE" -> R.drawable.flag_cv
        "DJF" -> R.drawable.flag_dj
        "DOP" -> R.drawable.flag_do
        "DZD" -> R.drawable.flag_dz
        "EGP" -> R.drawable.flag_eg
        "ERN" -> R.drawable.flag_er
        "ETB" -> R.drawable.flag_et
        "FJD" -> R.drawable.flag_fj
        "FKP" -> R.drawable.flag_fk
        "GEL" -> R.drawable.flag_ge
        "GGP" -> R.drawable.flag_gg
        "GHS" -> R.drawable.flag_gh
        "GIP" -> R.drawable.flag_gi
        "GMD" -> R.drawable.flag_gm
        "GNF" -> R.drawable.flag_gn
        "GTQ" -> R.drawable.flag_gt
        "GYD" -> R.drawable.flag_gy
        "HNL" -> R.drawable.flag_hn
        "HTG" -> R.drawable.flag_ht
        "IMP" -> R.drawable.flag_im
        "IQD" -> R.drawable.flag_iq
        "IRR" -> R.drawable.flag_ir
        "JEP" -> R.drawable.flag_je
        "JMD" -> R.drawable.flag_jm
        "JOD" -> R.drawable.flag_jo
        "KES" -> R.drawable.flag_ke
        "KGS" -> R.drawable.flag_kg
        "KHR" -> R.drawable.flag_kh
        "KMF" -> R.drawable.flag_km
        "KPW" -> R.drawable.flag_kp
        "KWD" -> R.drawable.flag_kw
        "KYD" -> R.drawable.flag_ky
        "KZT" -> R.drawable.flag_kz
        "LAK" -> R.drawable.flag_la
        "LBP" -> R.drawable.flag_lb
        "LKR" -> R.drawable.flag_lk
        "LRD" -> R.drawable.flag_lr
        "LSL" -> R.drawable.flag_ls
        "LYD" -> R.drawable.flag_ly
        "MAD" -> R.drawable.flag_ma
        "MDL" -> R.drawable.flag_md
        "MGA" -> R.drawable.flag_mg
        "MKD" -> R.drawable.flag_mk
        "MMK" -> R.drawable.flag_mm
        "MNT" -> R.drawable.flag_mn
        "MOP" -> R.drawable.flag_mo
        "MRO" -> R.drawable.flag_mr
        "MRU" -> R.drawable.flag_mr
        "MUR" -> R.drawable.flag_mu
        "MVR" -> R.drawable.flag_mv
        "MWK" -> R.drawable.flag_mw
        "MZN" -> R.drawable.flag_mz
        "NAD" -> R.drawable.flag_na
        "NGN" -> R.drawable.flag_ng
        "NIO" -> R.drawable.flag_ni
        "NPR" -> R.drawable.flag_np
        "OMR" -> R.drawable.flag_om
        "PAB" -> R.drawable.flag_pa
        "PEN" -> R.drawable.flag_pe
        "PGK" -> R.drawable.flag_pg
        "PKR" -> R.drawable.flag_pk
        "PYG" -> R.drawable.flag_py
        "QAR" -> R.drawable.flag_qa
        "RSD" -> R.drawable.flag_rs
        "RUB" -> R.drawable.flag_ru
        "RWF" -> R.drawable.flag_rw
        "SAR" -> R.drawable.flag_sa
        "SBD" -> R.drawable.flag_sb
        "SCR" -> R.drawable.flag_sc
        "SDG" -> R.drawable.flag_sd
        "SHP" -> R.drawable.flag_sh
        "SLE" -> R.drawable.flag_sl
        "SOS" -> R.drawable.flag_so
        "SRD" -> R.drawable.flag_sr
        "SSP" -> R.drawable.flag_ss
        "STN" -> R.drawable.flag_st
        "SVC" -> R.drawable.flag_sv
        "SYP" -> R.drawable.flag_sy
        "SZL" -> R.drawable.flag_sz
        "TJS" -> R.drawable.flag_tj
        "TMT" -> R.drawable.flag_tm
        "TND" -> R.drawable.flag_tn
        "TOP" -> R.drawable.flag_to
        "TTD" -> R.drawable.flag_tt
        "TWD" -> R.drawable.flag_tw
        "TZS" -> R.drawable.flag_tz
        "UAH" -> R.drawable.flag_ua
        "UGX" -> R.drawable.flag_ug
        "UYU" -> R.drawable.flag_uy
        "UZS" -> R.drawable.flag_uz
        "VES" -> R.drawable.flag_ve
        "VND" -> R.drawable.flag_vn
        "VUV" -> R.drawable.flag_vu
        "WST" -> R.drawable.flag_ws
        "YER" -> R.drawable.flag_ye
        "ZMW" -> R.drawable.flag_zm
        "ZWG" -> R.drawable.flag_zw
        else -> null
    }
    if (resource != null) {
        Image(painterResource(resource), "$code bayrağı", modifier.clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop)
    } else {
        Box(modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center) {
            Text(code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun searchKey(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.forLanguageTag("tr-TR")).replace('ı', 'i'),
        Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencySelectionSheet(
    favorites: Set<String>,
    widgetFavorites: Set<String>,
    onWidgetFavorite: (String) -> Unit,
    selected: String?,
    onFavorite: (String) -> Unit,
    onSelect: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var widgetTab by rememberSaveable { mutableStateOf(false) }
    val managing = onSelect == null
    val homeAccent = MaterialTheme.colorScheme.primary
    val widgetAccent = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
        Color(0xFFFFC857) else Color(0xFF966000)
    val favoriteAccent = if (managing && widgetTab) widgetAccent else homeAccent
    val activeFavorites = if (managing && widgetTab) widgetFavorites else favorites
    val toggleFavorite = if (managing && widgetTab) onWidgetFavorite else onFavorite
    val visible = remember(query, onlyFavorites, activeFavorites) {
        val search = searchKey(query.trim())
        currencyNames.entries.filter { (code, name) ->
            (!onlyFavorites || code in activeFavorites) &&
                (search.isEmpty() || searchKey("$code $name").contains(search))
        }
    }
    HandleOnlySheet(onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding().padding(horizontal = 20.dp)) {
            Text(if (managing) "Favoriler" else "Para birimi seç",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (managing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!widgetTab, { widgetTab = false; onlyFavorites = false }, label = { Text("★ Ana sayfa", color = homeAccent) })
                    FilterChip(widgetTab, { widgetTab = true; onlyFavorites = false }, label = { Text("★ Widget · ${widgetFavorites.size}/4", color = widgetAccent) })
                }
            }
            Text(if (managing && widgetTab) "Widget için en fazla 4 para birimi seç. Değiştirmek için önce bir yıldızı kaldır."
                else if (managing) "Ana sayfada görmek istediğin para birimlerini seç. En az bir birim seçili kalır."
                else "İsme veya koda göre bul, satıra dokunarak seç.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Para birimi ara") },
                placeholder = { Text("Dolar, euro, GBP…") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = if (query.isNotEmpty()) { { TextButton(onClick = { query = "" }) { Text("Sil") } } } else null)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                FilterChip(!onlyFavorites, { onlyFavorites = false }, label = { Text("Tümü · ${currencyNames.size}") })
                FilterChip(onlyFavorites, { onlyFavorites = true }, label = { Text("Seçili · ${activeFavorites.size}") })
            }
            if (visible.isEmpty()) {
                Text(if (onlyFavorites && query.isBlank()) "Henüz favori seçmedin. Tümü sekmesinden yıldız ekleyebilirsin."
                    else "Eşleşen para birimi bulunamadı.", modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(visible, key = { it.key }) { (code, name) ->
                    val canToggle = if (managing && widgetTab) code in activeFavorites || activeFavorites.size < 4
                        else code !in activeFavorites || activeFavorites.size > 1
                    Surface(color = if (selected == code) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().clickable(enabled = !managing || canToggle) {
                            if (onSelect == null) toggleFavorite(code) else onSelect(code)
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
                            IconButton(onClick = { toggleFavorite(code) }, enabled = canToggle) {
                                Text(if (code in activeFavorites) "★" else "☆", fontSize = 26.sp,
                                    color = if (canToggle || code in activeFavorites) favoriteAccent else favoriteAccent.copy(alpha = 0.35f),
                                    modifier = Modifier.semantics {
                                        contentDescription = if (code in activeFavorites) "$code favorilerden çıkar" else "$code favorilere ekle"
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

// Liste kaydırması panele bağlı değildir; yalnızca üstteki tutma alanı sürüklenir.
@Composable
private fun HandleOnlySheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var offset by remember { mutableFloatStateOf(0f) }
    var panelHeight by remember { mutableIntStateOf(1) }
    var settling by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnClickOutside = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.BottomCenter) {
            Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .offset { IntOffset(0, offset.roundToInt()) }.onSizeChanged { panelHeight = it.height },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(40.dp)
                        .semantics { contentDescription = "Favoriler panelini kapatmak için aşağı sürükle" }
                        .draggable(rememberDraggableState { delta ->
                            offset = (offset + delta).coerceIn(0f, panelHeight.toFloat())
                        }, Orientation.Vertical,
                            onDragStarted = { settling?.cancel() },
                            onDragStopped = { velocity ->
                                val close = offset > threshold || velocity > 1000f
                                settling = scope.launch {
                                    animate(offset, if (close) panelHeight.toFloat() else 0f,
                                        animationSpec = tween(180)) { value, _ -> offset = value }
                                    if (close) onDismiss()
                                }
                            }), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(38.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                    }
                    content()
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
                    SettingsSection("Favoriler", "Ana sayfa: ${model.homeFavorites.size} birim · Widget: ${model.favorites.size}/4") {
                        OutlinedButton(onClick = onFavorites, modifier = Modifier.fillMaxWidth()) { Text("Favorileri düzenle") }
                    }
                    SettingsSection("Kur verileri", "Frankfurter’ın merkez bankaları ve resmî kaynaklardan derlediği kurlar. Banka alış ve satış fiyatları değildir; birimlerin kur tarihleri farklı olabilir.") {
                        Text("Desteklenen para birimi: ${currencyNames.size}", style = MaterialTheme.typography.bodyMedium)
                        model.snapshot?.let { Text("Kur tarihi: ${it.date}", style = MaterialTheme.typography.bodySmall) }
                        model.snapshot?.let { Text("Kayıt kaynağı: ${it.source}", style = MaterialTheme.typography.bodySmall) }
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
