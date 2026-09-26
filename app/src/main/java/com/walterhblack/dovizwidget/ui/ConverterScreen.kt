package com.walterhblack.dovizwidget.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.walterhblack.dovizwidget.data.CurrencyMath
import com.walterhblack.dovizwidget.data.WidgetCalculator
import com.walterhblack.dovizwidget.data.currencyNames
import com.walterhblack.dovizwidget.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
            var rowCodes by rememberSaveable { mutableStateOf(listOf("TRY", "USD", "EUR", "GBP")) }
            var keyboardMode by rememberSaveable { mutableStateOf(KeyboardMode.Docked) }
            val parsed = runCatching { WidgetCalculator.evaluate(amount) }.getOrNull()
            val snapshot = model.snapshot
            val density = LocalDensity.current
            val updateBarColor = if (dark) Color(0xFF101715) else Color(0xFFE6EEE8)
            val selectedRowColor = if (dark) Color(0xFF1A332B) else Color(0xFFDCEFE4)
            var manageCurrencies by remember { mutableStateOf(false) }
            if (manageCurrencies) {
                AlertDialog(
                    onDismissRequest = { manageCurrencies = false },
                    title = { Text("Para birimlerini yönet") },
                    text = {
                        Column {
                            Text("Yıldızlı kurlar widget’ta gösterilir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant)
                            currencyNames.forEach { (code, name) ->
                                Row(Modifier.fillMaxWidth().clickable { model.toggleFavorite(code) }
                                    .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("$code · $name", modifier = Modifier.weight(1f))
                                    Checkbox(checked = code in model.favorites,
                                        onCheckedChange = { model.toggleFavorite(code) })
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { manageCurrencies = false }) { Text("Tamam") } }
                )
            }
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
              val collapsedHeightPx = with(density) { 72.dp.toPx() }
              val dockedHeightPx = with(density) { 304.dp.toPx() }
              val targetKeyboardHeightPx = when (keyboardMode) {
                  KeyboardMode.Collapsed -> collapsedHeightPx
                  KeyboardMode.Docked -> dockedHeightPx
              }
              var keyboardHeightPx by remember(density) { mutableFloatStateOf(targetKeyboardHeightPx) }
              var isKeyboardDragging by remember { mutableStateOf(false) }
              LaunchedEffect(targetKeyboardHeightPx, isKeyboardDragging) {
                  if (isKeyboardDragging) return@LaunchedEffect
                  animate(
                      initialValue = keyboardHeightPx,
                      targetValue = targetKeyboardHeightPx,
                      animationSpec = tween(260)
                  ) { height, _ ->
                      keyboardHeightPx = height
                  }
              }
              val currentKeyboardHeightPx = keyboardHeightPx.coerceIn(collapsedHeightPx, dockedHeightPx)
              val currentKeyboardHeight = with(density) { currentKeyboardHeightPx.toDp() }
              Box(Modifier.fillMaxSize()) {
                Column(
                  Modifier.fillMaxSize()
                    .padding(bottom = currentKeyboardHeight)
                ) {
              Column(
                  Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                      .padding(horizontal = 16.dp, vertical = 18.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                  Text(
                      snapshot?.let {
                          "Güncellendi · " + DateTimeFormatter.ofPattern("HH:mm · dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
                              .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it.fetchedAt))
                      } ?: "Güncel kurlar yükleniyor",
                      Modifier.fillMaxWidth().background(updateBarColor).padding(vertical = 10.dp),
                      color = colors.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                      style = MaterialTheme.typography.bodyMedium
                  )
                  if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                  model.error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }

                  Column(Modifier.fillMaxWidth()) {
                  rowCodes.forEachIndexed { index, code ->
                      val isSource = code == from
                      var menuOpen by remember(code) { mutableStateOf(false) }
                      val converted = parsed?.let { value ->
                          if (isSource) value else snapshot?.let {
                              runCatching {
                                  CurrencyMath.convert(value.abs(), from, code, it.rates)
                                      .let { result -> if (value.signum() < 0) result.negate() else result }
                              }.getOrNull()
                          }
                      }
                      Row(
                          Modifier.fillMaxWidth()
                              .background(if (isSource) selectedRowColor else colors.background)
                              .clickable { from = code }
                              .padding(horizontal = 12.dp, vertical = 18.dp),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          Image(
                              painter = painterResource(
                                  when (code) {
                                      "TRY" -> R.drawable.flag_tr
                                      "USD" -> R.drawable.flag_us
                                      "EUR" -> R.drawable.flag_eu
                                      else -> R.drawable.flag_gb
                                  }
                              ),
                              contentDescription = "$code bayrağı",
                              modifier = Modifier.size(width = 64.dp, height = 44.dp)
                          )
                          Spacer(Modifier.width(10.dp))
                          Box {
                              TextButton(onClick = { menuOpen = true }, contentPadding = PaddingValues(0.dp)) {
                                  Column {
                                      Text("$code  ▾", fontWeight = FontWeight.Medium, fontSize = 23.sp,
                                          color = if (isSource) colors.primary else colors.onSurface)
                                      if (isSource) Text("Kaynak", style = MaterialTheme.typography.labelSmall,
                                          color = colors.primary)
                                  }
                              }
                              DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                  currencyNames.forEach { (newCode, name) ->
                                      DropdownMenuItem(
                                          text = { Text("$newCode · $name") },
                                          onClick = {
                                              val previousIndex = rowCodes.indexOf(newCode)
                                              rowCodes = rowCodes.toMutableList().apply {
                                                  this[index] = newCode
                                                  this[previousIndex] = code
                                              }
                                              if (isSource) from = newCode
                                              menuOpen = false
                                          }
                                      )
                                  }
                              }
                          }
                          Spacer(Modifier.weight(1f))
                          Column(horizontalAlignment = Alignment.End) {
                              Text(converted?.let { CurrencyMath.format(it) } ?: "—",
                                  fontWeight = FontWeight.Normal, fontSize = 26.sp,
                                  color = if (isSource) colors.primary else colors.onSurface,
                                  maxLines = 1)
                              Text(code, style = MaterialTheme.typography.bodySmall,
                                  color = colors.onSurfaceVariant)
                          }
                      }
                  }
                  }
                  OutlinedButton(onClick = { manageCurrencies = true },
                      modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                      Text("☷   Para birimlerini yönet", fontSize = 18.sp)
                  }
                  Text("Satıra dokununca kaynak kur değişir · Günlük referans kurları",
                      style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
              }
                }
                CurrencyKeyboard(
                    value = amount,
                    onValueChange = { amount = it },
                    onRefresh = model::refresh,
                    colors = colors,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(currentKeyboardHeight),
                    minHeightPx = collapsedHeightPx,
                    dockedHeightPx = dockedHeightPx,
                    currentHeightPx = currentKeyboardHeightPx,
                    onDragStart = {
                        isKeyboardDragging = true
                        currentKeyboardHeightPx
                    },
                    onDragHeightChange = { keyboardHeightPx = it },
                    onDragFinish = { mode, height ->
                        keyboardHeightPx = height
                        keyboardMode = mode
                        isKeyboardDragging = false
                    }
                )
              }
            }
        }
    }
}

private enum class KeyboardMode { Collapsed, Docked }

/** Widget düzenindeki sayı ve dört işlem tuşlarını taşıyan, açılıp kapanan klavye. */
@Composable
private fun CurrencyKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    onRefresh: () -> Unit,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
    minHeightPx: Float,
    dockedHeightPx: Float,
    currentHeightPx: Float,
    onDragStart: () -> Float,
    onDragHeightChange: (Float) -> Unit,
    onDragFinish: (KeyboardMode, Float) -> Unit
) {
    val startDrag by rememberUpdatedState(onDragStart)
    val keyDivider = Color.Black
    var calculated by rememberSaveable { mutableStateOf(false) }
    fun press(key: String) {
        if (key == "=") {
            runCatching { WidgetCalculator.evaluate(value) }.getOrNull()?.let {
                onValueChange(WidgetCalculator.input(it))
                calculated = true
            }
            return
        }
        val startsNew = calculated && key in listOf("0", "00", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",")
        onValueChange(WidgetCalculator.edit(if (startsNew) "0" else value, key))
        calculated = false
    }

    Column(modifier.fillMaxWidth().clipToBounds().background(colors.surfaceContainerHighest)) {
        Box(
            Modifier.fillMaxWidth().height(32.dp)
                .border(width = 1.dp, color = colors.outline)
                .pointerInput(minHeightPx, dockedHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var dragging = false
                        var rejected = false
                        var finished = false
                        var currentHeightPx = 0f

                        while (!finished) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val deltaX = change.position.x - change.previousPosition.x
                            val deltaY = change.position.y - change.previousPosition.y

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
                                        currentHeightPx = startDrag()
                                    }
                                }
                                if (dragging) {
                                    change.consume()
                                    val dragDelta = if (justStartedDragging) totalY else deltaY
                                    currentHeightPx = (currentHeightPx - dragDelta).coerceIn(minHeightPx, dockedHeightPx)
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
                                KeyboardMode.Docked to dockedHeightPx
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
        // Tuşlar sıkışmaz: sabit boydaki ızgara kapanırken panelin altına kayar.
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val closedFraction = ((dockedHeightPx - currentHeightPx) /
                (dockedHeightPx - minHeightPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val gridOffset = 40.dp * closedFraction
            val rows = listOf(
                listOf("7", "8", "9", "÷", "C"),
                listOf("4", "5", "6", "×", "⌫"),
                listOf("1", "2", "3", "−", "↻"),
                listOf("0", "00", ",", "+", "=")
            )
            Column(Modifier.fillMaxWidth().offset(y = gridOffset).height(272.dp)) {
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { key ->
                            val special = key in listOf("C", "⌫", "↻", "=", "+", "−", "×", "÷")
                            KeyboardKey(
                                label = key,
                                background = if (special) Color(0xFF285640) else Color(0xFF343B38),
                                foreground = if (special) Color(0xFFA5F3CF) else Color(0xFFF0FFF7),
                                border = keyDivider,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentDescription = if (key == "↻") "Kurları yenile" else null,
                                onClick = if (key == "↻") onRefresh else { { press(key) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    label: String?, background: Color, foreground: Color, border: Color,
    modifier: Modifier, contentDescription: String? = null, onClick: (() -> Unit)?
) {
    val accessibility = if (contentDescription == null) Modifier else Modifier.semantics {
        this.contentDescription = contentDescription
    }
    Box(
        modifier.background(background).border(1.dp, border).then(accessibility).then(
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
