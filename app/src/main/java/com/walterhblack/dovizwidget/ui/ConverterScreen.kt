package com.walterhblack.dovizwidget.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.math.abs

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
            var keyboardMode by rememberSaveable { mutableStateOf(KeyboardMode.Docked) }
            val parsed = CurrencyMath.parseAmount(amount)
            val snapshot = model.snapshot
            val result = parsed?.let { value ->
                if (from == to) value else snapshot?.let { CurrencyMath.convert(value, from, to, it.rates) }
            }
            val context = LocalContext.current
            val density = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
              val collapsedHeightPx = with(density) { 72.dp.toPx() }
              val dockedHeightPx = with(density) { 304.dp.toPx() }
              val expandedHeightPx = with(density) { maxHeight.toPx() }
              val targetKeyboardHeightPx = when (keyboardMode) {
                  KeyboardMode.Collapsed -> collapsedHeightPx
                  KeyboardMode.Docked -> dockedHeightPx
                  KeyboardMode.Expanded -> expandedHeightPx
              }
              val keyboardHeight = remember(density) { Animatable(dockedHeightPx) }
              var isKeyboardDragging by remember { mutableStateOf(false) }
              var draggedKeyboardHeightPx by remember { mutableFloatStateOf(dockedHeightPx) }
              var releasedKeyboardHeightPx by remember { mutableFloatStateOf(Float.NaN) }
              LaunchedEffect(targetKeyboardHeightPx, isKeyboardDragging, releasedKeyboardHeightPx) {
                  if (isKeyboardDragging) return@LaunchedEffect
                  if (releasedKeyboardHeightPx.isFinite()) {
                      keyboardHeight.snapTo(releasedKeyboardHeightPx.coerceIn(collapsedHeightPx, expandedHeightPx))
                  }
                  keyboardHeight.animateTo(targetKeyboardHeightPx, animationSpec = tween(260))
              }
              val currentKeyboardHeightPx = if (isKeyboardDragging) draggedKeyboardHeightPx else keyboardHeight.value
              val currentKeyboardHeight = with(density) { currentKeyboardHeightPx.toDp() }
              val expansionRangePx = (expandedHeightPx - dockedHeightPx).coerceAtLeast(1f)
              val expansionProgress = ((currentKeyboardHeightPx - dockedHeightPx) / expansionRangePx).coerceIn(0f, 1f)
              val collapseRangePx = (dockedHeightPx - collapsedHeightPx).coerceAtLeast(1f)
              val collapseProgress = ((currentKeyboardHeightPx - collapsedHeightPx) / collapseRangePx).coerceIn(0f, 1f)
              Box(Modifier.fillMaxSize()) {
                Column(
                  Modifier.fillMaxSize()
                    .padding(bottom = currentKeyboardHeight)
                    .graphicsLayer { alpha = 1f - expansionProgress }
                ) {
              Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
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
                CurrencyKeyboard(
                    value = amount,
                    onValueChange = { amount = it },
                    colors = colors,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(currentKeyboardHeight),
                    minHeightPx = collapsedHeightPx,
                    dockedHeightPx = dockedHeightPx,
                    maxHeightPx = expandedHeightPx,
                    currentHeightPx = currentKeyboardHeightPx,
                    expansionProgress = expansionProgress,
                    collapseProgress = collapseProgress,
                    isDragging = isKeyboardDragging,
                    onDragStart = {
                        draggedKeyboardHeightPx = keyboardHeight.value
                        isKeyboardDragging = true
                        keyboardHeight.value
                    },
                    onDragHeightChange = { draggedKeyboardHeightPx = it },
                    onDragFinish = { mode, height ->
                        releasedKeyboardHeightPx = height
                        keyboardMode = mode
                        isKeyboardDragging = false
                    }
                )
              }
            }
        }
    }
}

private enum class KeyboardMode { Collapsed, Docked, Expanded }

/** Çizimdeki hesap makinesi: üç sayı sütunu, sağda C / virgül / silme sütunu. */
@Composable
private fun CurrencyKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
    minHeightPx: Float,
    dockedHeightPx: Float,
    maxHeightPx: Float,
    currentHeightPx: Float,
    expansionProgress: Float,
    collapseProgress: Float,
    isDragging: Boolean,
    onDragStart: () -> Float,
    onDragHeightChange: (Float) -> Unit,
    onDragFinish: (KeyboardMode, Float) -> Unit
) {
    fun press(key: String) {
        when (key) {
            "C" -> onValueChange("0")
            "⌫" -> onValueChange(value.dropLast(1).ifEmpty { "0" })
            "," -> if (!value.contains(',')) onValueChange("$value,")
            else -> if (value.length < 20) onValueChange(if (value == "0") key else "$value$key")
        }
    }

    Column(modifier.fillMaxWidth().background(colors.surfaceContainerHighest)) {
        var isDragging by remember { mutableStateOf(false) }
        Box(
            Modifier.fillMaxWidth().height(32.dp)
                .border(width = 1.dp, color = colors.outline)
                .pointerInput(mode, minHeightPx, maxHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastX = down.position.x
                        var lastY = down.position.y
                        var totalX = 0f
                        var totalY = 0f
                        var dragging = false
                        var rejected = false
                        var finished = false
                        var currentHeightPx = 0f

                        while (!finished) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val deltaX = change.position.x - lastX
                            val deltaY = change.position.y - lastY
                            lastX = change.position.x
                            lastY = change.position.y

                            if (change.pressed) {
                                totalX += deltaX
                                totalY += deltaY
                                var justStartedDragging = false
                                if (!dragging && !rejected) {
                                    if (abs(totalX) > viewConfiguration.touchSlop && abs(totalX) > abs(totalY)) {
                                        rejected = true
                                    } else if (abs(totalY) > viewConfiguration.touchSlop && abs(totalY) > abs(totalX)) {
                                        dragging = true
                                        justStartedDragging = true
                                        currentHeightPx = onDragStart()
                                    }
                                }
                                if (dragging) {
                                    change.consume()
                                    val dragDelta = if (justStartedDragging) totalY else deltaY
                                    currentHeightPx = (currentHeightPx - dragDelta).coerceIn(minHeightPx, maxHeightPx)
                                    onDragHeightChange(currentHeightPx)
                                    totalY = 0f
                                }
                            } else {
                                finished = true
                            }
                        }

                        if (dragging) {
                            val stops = listOf(
                                KeyboardMode.Collapsed to minHeightPx,
                                KeyboardMode.Docked to dockedHeightPx.coerceIn(minHeightPx, maxHeightPx),
                                KeyboardMode.Expanded to maxHeightPx
                            )
                            onDragFinish(stops.minBy { abs(it.second - currentHeightPx) }.first, currentHeightPx)
                        }
                    }
                }
                .semantics { contentDescription = "Klavyeyi tutup yukarı veya aşağı sürükle" },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.width(48.dp).height(6.dp).background(colors.primary))
        }
        // Kapalıyken tutma yerini Android'in alt gezinme hareketinden uzak tut.
        if (currentHeightPx <= minHeightPx + 1f && !isDragging) Spacer(Modifier.height(40.dp))
        if (currentHeightPx > minHeightPx + 1f || isDragging) {
            val density = LocalDensity.current
            val expandedHeaderHeight = with(density) { (72.dp.toPx() * expansionProgress).toDp() }
            Column(
                Modifier.fillMaxWidth().height(expandedHeaderHeight)
                    .graphicsLayer { alpha = expansionProgress }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Tutar", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
            }
            Row(Modifier.weight(1f).fillMaxWidth().graphicsLayer { alpha = collapseProgress }) {
                Column(Modifier.weight(3f).fillMaxHeight()) {
                    listOf(
                        listOf("7", "8", "9"),
                        listOf("4", "5", "6"),
                        listOf("1", "2", "3"),
                        listOf(null, "0", null)
                    ).forEach { row ->
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            row.forEach { key ->
                                KeyboardKey(
                                    label = key,
                                    background = colors.surfaceVariant,
                                    foreground = colors.onSurface,
                                    border = colors.outlineVariant,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = key?.let { { press(it) } }
                                )
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    KeyboardKey("C", Color(0xFFF4A62A), Color(0xFF382000), colors.outlineVariant,
                        Modifier.weight(2f).fillMaxWidth()) { press("C") }
                    KeyboardKey(",", Color(0xFFF4A62A), Color(0xFF382000), colors.outlineVariant,
                        Modifier.weight(1f).fillMaxWidth()) { press(",") }
                    KeyboardKey("⌫", colors.secondaryContainer, colors.onSecondaryContainer, colors.outlineVariant,
                        Modifier.weight(1f).fillMaxWidth()) { press("⌫") }
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    label: String?, background: Color, foreground: Color, border: Color,
    modifier: Modifier, onClick: (() -> Unit)?
) {
    Box(
        modifier.background(background).border(1.dp, border).then(
            if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
        ),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) Text(label, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = foreground)
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
