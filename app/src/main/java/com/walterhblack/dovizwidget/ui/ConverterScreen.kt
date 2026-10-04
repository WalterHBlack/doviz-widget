package com.walterhblack.dovizwidget.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

// @Composable: Bu fonksiyon veriden bir ekran parçası üretir; veri değişince ekran yenilenir.
@Composable
fun ConverterScreen(model: ConverterViewModel) {
    val dark = when (model.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFFA5F3CF),
            onPrimary = Color(0xFF003824),
            background = Color(0xFF091310),
            onBackground = Color(0xFFE3EAE5),
            surface = Color(0xFF111C18),
            onSurface = Color(0xFFE3EAE5),
            surfaceContainer = Color(0xFF16231F),
            surfaceContainerHighest = Color(0xFF202C28),
            onSurfaceVariant = Color(0xFFB8C4BD),
            outline = Color(0xFF65736C),
            outlineVariant = Color(0xFF34413B)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF146747),
            onPrimary = Color.White,
            background = Color(0xFFF4F7F4),
            onBackground = Color(0xFF18211D),
            surface = Color.White,
            onSurface = Color(0xFF18211D),
            surfaceContainer = Color(0xFFEAF0EC),
            surfaceContainerHighest = Color(0xFFDDE7E1),
            onSurfaceVariant = Color(0xFF56615B),
            outline = Color(0xFF76837C),
            outlineVariant = Color(0xFFD0D9D3)
        )
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp)
        )
    ) {
        Surface(Modifier.fillMaxSize()) {
            val amount = model.amount
            val from = model.sourceCurrency
            val rowCodes = model.homeFavorites
            val rowHeights = remember { mutableStateMapOf<String, Int>() }
            val dividerPx = with(LocalDensity.current) { 0.7.dp.roundToPx().toFloat() }
            val rowBounds by rememberUpdatedState(buildMap<String, Pair<Float, Float>> {
                var top = 0f
                rowCodes.forEach { code ->
                    val height = rowHeights[code]?.toFloat() ?: 0f
                    put(code, top to top + height)
                    top += height + dividerPx
                }
            })
            var draggedCode by remember { mutableStateOf<String?>(null) }
            var draggedCenter by remember { mutableFloatStateOf(0f) }
            var keyboardMode by rememberSaveable { mutableStateOf(KeyboardMode.Docked) }
            val calculation = runCatching { WidgetCalculator.evaluate(amount) }
            val parsed = calculation.getOrNull()
            val calculationError = calculation.exceptionOrNull()?.message
            val snapshot = model.snapshot
            val density = LocalDensity.current
            val updateBarColor = if (dark) Color(0xFF121F1B) else Color(0xFFE5EEE8)
            val selectedRowColor = if (dark) Color(0xFF18392E) else Color(0xFFD9F0E3)
            val listSurfaceColor = colors.background
            var manageCurrencies by remember { mutableStateOf(false) }
            var showSettings by remember { mutableStateOf(false) }
            var pickingCode by remember { mutableStateOf<String?>(null) }
            val uiScale = when (model.uiScale) {
                "compact" -> 0.90f
                "large" -> 1.10f
                else -> 1f
            }
            if (manageCurrencies || pickingCode != null) {
                CurrencySelectionSheet(
                    favorites = model.homeFavorites.toSet(),
                    widgetFavorites = model.favorites,
                    onWidgetFavorite = model::toggleFavorite,
                    selected = pickingCode,
                    onFavorite = model::toggleHomeFavorite,
                    onSelect = if (manageCurrencies) null else { newCode ->
                        pickingCode?.let { model.replaceHomeCurrency(it, newCode) }
                        pickingCode = null
                    },
                    onDismiss = { manageCurrencies = false; pickingCode = null }
                )
            }
            if (showSettings) {
                SettingsScreen(model, onFavorites = {
                    showSettings = false
                    manageCurrencies = true
                }, onDismiss = { showSettings = false })
            }
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
              val collapsedHeightPx = with(density) { 34.dp.toPx() * uiScale }
              val dockedHeightPx = with(density) { 304.dp.toPx() * uiScale }
              val targetKeyboardHeightPx = when (keyboardMode) {
                  KeyboardMode.Collapsed -> collapsedHeightPx
                  KeyboardMode.Docked -> dockedHeightPx
              }
              var keyboardHeightPx by remember(density, uiScale) { mutableFloatStateOf(targetKeyboardHeightPx) }
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
              Box(Modifier.fillMaxSize().clipToBounds()) {
                Column(
                  Modifier.fillMaxSize()
                    .layout { measurable, constraints ->
                        val visibleHeight = (constraints.maxHeight - keyboardHeightPx.roundToInt())
                            .coerceAtLeast(0)
                        val placeable = measurable.measure(
                            constraints.copy(minHeight = 0, maxHeight = visibleHeight)
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(0, 0)
                        }
                    }
                ) {
                  Column(
                  Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                      .padding(vertical = 18.dp * uiScale),
                  verticalArrangement = Arrangement.spacedBy(12.dp * uiScale)
              ) {
                  Row(
                      Modifier.fillMaxWidth()
                          .padding(horizontal = 16.dp * uiScale)
                          .clip(MaterialTheme.shapes.large)
                          .background(updateBarColor)
                          .padding(start = 18.dp * uiScale, end = 6.dp * uiScale, top = 8.dp * uiScale, bottom = 8.dp * uiScale),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      Column(Modifier.weight(1f)) {
                          Text(
                              "DÖVİZ CEPTE",
                              color = colors.primary,
                              fontWeight = FontWeight.Bold,
                              style = MaterialTheme.typography.labelLarge,
                              letterSpacing = 1.4.sp
                          )
                      }
                      TextButton(
                          onClick = { manageCurrencies = true },
                          contentPadding = PaddingValues(horizontal = 10.dp * uiScale)
                      ) { Text("Favoriler") }
                      FilledTonalButton(
                          onClick = { showSettings = true },
                          contentPadding = PaddingValues(horizontal = 12.dp * uiScale),
                          modifier = Modifier.heightIn(min = 40.dp * uiScale)
                      ) { Text("⚙", modifier = Modifier.semantics { contentDescription = "Ayarlar" }) }
                  }
                  if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                  model.error?.let { Text(it, Modifier.padding(horizontal = 16.dp * uiScale), color = colors.error, style = MaterialTheme.typography.bodySmall) }
                  calculationError?.let { Text(it, Modifier.padding(horizontal = 16.dp * uiScale), color = colors.error, style = MaterialTheme.typography.bodySmall) }

                  Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp * uiScale), verticalAlignment = Alignment.CenterVertically) {
                      Text("Para birimleri", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                          color = colors.onSurfaceVariant)
                      Text("${rowCodes.size} favori", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                  }
                  Column(
                      Modifier.fillMaxWidth()
                          .background(listSurfaceColor)
                          .pointerInput(model) {
                              detectDragGesturesAfterLongPress(
                                  onDragStart = { point ->
                                      draggedCode = model.homeFavorites.firstOrNull { code ->
                                          rowBounds[code]?.let { point.y >= it.first && point.y <= it.second } == true
                                      }
                                      draggedCode?.let { code ->
                                          rowBounds[code]?.let { draggedCenter = (it.first + it.second) / 2f }
                                      }
                                  },
                                  onDragEnd = { draggedCode = null },
                                  onDragCancel = { draggedCode = null },
                                  onDrag = { change, delta ->
                                      draggedCode?.let { code ->
                                          change.consume()
                                          draggedCenter += delta.y
                                          val target = model.homeFavorites.firstOrNull { other ->
                                              other != code && rowBounds[other]?.let {
                                                  draggedCenter >= it.first && draggedCenter <= it.second
                                              } == true
                                          }
                                          if (target != null) model.moveHomeCurrency(code, target)
                                      }
                                  }
                              )
                          }
                  ) {
                  rowCodes.forEachIndexed { index, code ->
                    key(code) {
                      val isSource = code == from
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
                              .onSizeChanged { rowHeights[code] = it.height }
                              .zIndex(if (draggedCode == code) 1f else 0f)
                              .graphicsLayer {
                                  translationY = if (draggedCode == code) {
                                      rowBounds[code]?.let { draggedCenter - (it.first + it.second) / 2f } ?: 0f
                                  } else 0f
                                  shadowElevation = if (draggedCode == code) 8.dp.toPx() else 0f
                              }
                              .background(if (isSource) selectedRowColor else listSurfaceColor)
                              .clickable {
                                  model.setSelectedSourceCurrency(code)
                              }
                              .padding(horizontal = 20.dp * uiScale, vertical = 14.dp * uiScale),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          CurrencyFlag(code, Modifier.size(48.dp * uiScale, 34.dp * uiScale))
                          Spacer(Modifier.width(10.dp * uiScale))
                          Column(Modifier.width(94.dp * uiScale)
                              .clickable { pickingCode = code }.padding(vertical = 4.dp)) {
                              Text("$code  ▾", fontWeight = FontWeight.SemiBold, fontSize = (20f * uiScale).sp,
                                  color = if (isSource) colors.primary else colors.onSurface)
                              Text(if (isSource) "Kaynak para birimi" else currencyNames[code].orEmpty(),
                                  style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                  overflow = TextOverflow.Ellipsis,
                                  color = if (isSource) colors.primary else colors.onSurfaceVariant)
                          }
                          Column(Modifier.weight(1f).padding(start = 8.dp), horizontalAlignment = Alignment.End) {
                              Text(if (isSource && !model.amountIsConversion && amount != "0,00") amount
                                  else converted?.let { CurrencyMath.format(it, model.decimalPlaces) } ?: "—",
                                  fontWeight = FontWeight.Normal, fontSize = (24.sp.value * uiScale).sp,
                                  color = if (isSource) colors.primary else colors.onSurface,
                                  maxLines = 1, overflow = TextOverflow.Ellipsis)
                              Text(code, style = MaterialTheme.typography.bodySmall,
                                  color = colors.onSurfaceVariant)
                          }
                      }
                      if (index < rowCodes.lastIndex) {
                          HorizontalDivider(
                              modifier = Modifier.padding(start = 70.dp * uiScale),
                              thickness = 0.7.dp,
                              color = colors.outlineVariant.copy(alpha = 0.75f)
                          )
                      }
                  }
                  }
                  }
                  Text(snapshot?.let {
                      "Güncellendi · " + DateTimeFormatter.ofPattern("HH:mm · dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
                          .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it.fetchedAt))
                  } ?: "Güncel kurlar yükleniyor",
                      modifier = Modifier.padding(horizontal = 16.dp * uiScale),
                      style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                  Text("Satıra dokununca kaynak kur değişir · Günlük referans kurları",
                      modifier = Modifier.padding(horizontal = 16.dp * uiScale),
                      style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
              }
                }
                CurrencyKeyboard(
                    value = amount,
                    sourceCurrency = from,
                    amountIsConversion = model.amountIsConversion,
                    decimalPlaces = model.decimalPlaces,
                    onValueChange = model::editAmount,
                    onCalculated = model::calculatedAmount,
                    onRefresh = model::refresh,
                    colors = colors,
                    onToggle = {
                        keyboardMode = if (keyboardMode == KeyboardMode.Docked) KeyboardMode.Collapsed else KeyboardMode.Docked
                    },
                    uiScale = uiScale,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(304.dp * uiScale)
                        .offset {
                            IntOffset(0, (dockedHeightPx - keyboardHeightPx).roundToInt())
                        },
                    minHeightPx = collapsedHeightPx,
                    dockedHeightPx = dockedHeightPx,
                    onDragStart = {
                        isKeyboardDragging = true
                        keyboardHeightPx
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
    sourceCurrency: String,
    amountIsConversion: Boolean,
    decimalPlaces: Int,
    onValueChange: (String) -> Unit,
    onCalculated: (String) -> Unit,
    onRefresh: () -> Unit,
    colors: ColorScheme,
    onToggle: () -> Unit,
    uiScale: Float,
    modifier: Modifier = Modifier,
    minHeightPx: Float,
    dockedHeightPx: Float,
    onDragStart: () -> Float,
    onDragHeightChange: (Float) -> Unit,
    onDragFinish: (KeyboardMode, Float) -> Unit
) {
    val startDrag by rememberUpdatedState(onDragStart)
    val keyboardSurface = Color(0xFF1A2420)
    val numberKey = Color(0xFF29322F)
    val operationKey = Color(0xFF205A42)
    val utilityKey = Color(0xFF254238)
    val equalsKey = Color(0xFF357C5B)
    val keyDivider = Color(0xFF101512)
    var calculated by rememberSaveable(sourceCurrency) { mutableStateOf(false) }
    var freshInput by rememberSaveable(sourceCurrency) { mutableStateOf(true) }
    fun press(key: String) {
        if (key == "⌫" && amountIsConversion) {
            // Dönüşüm hassasiyeti ilk düzenlemeye kadar korunur. Silme, görünen
            // iki ondalıklı değerden başlar; gizli basamaklar silinmez.
            val visibleInput = runCatching {
                WidgetCalculator.evaluate(value).setScale(decimalPlaces, java.math.RoundingMode.HALF_UP)
                    .toPlainString().replace('.', ',').replace('-', '−')
            }.getOrDefault(value)
            onValueChange(WidgetCalculator.edit(visibleInput, key))
            calculated = false
            freshInput = false
            return
        }
        if (key == "=") {
            runCatching { WidgetCalculator.evaluate(value) }.getOrNull()?.let {
                onCalculated(WidgetCalculator.input(it))
                calculated = true
                freshInput = false
            }
            return
        }
        if (key == "C") {
            onValueChange("0,00")
            calculated = false
            freshInput = true
            return
        }
        val numberKeys = listOf("0", "00", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",")
        val startsNew = (calculated || freshInput) && key in numberKeys
        onValueChange(WidgetCalculator.edit(if (startsNew) "0" else value, key))
        calculated = false
        freshInput = false
    }

    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .clipToBounds()
            .background(keyboardSurface)
    ) {
        Box(
            Modifier.fillMaxWidth().height(28.dp * uiScale)
                .background(keyboardSurface)
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
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = "Klavyeyi aç veya kapat; sürükleyerek de taşı" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.width(40.dp * uiScale)
                    .height(4.dp * uiScale)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.primary.copy(alpha = 0.88f))
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
        // Izgara sabit boyda kalır; panel ekrandan aşağı kayarak kapanır.
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val rows = listOf(
                listOf("7", "8", "9", "÷", "C"),
                listOf("4", "5", "6", "×", "⌫"),
                listOf("1", "2", "3", "−", "↻"),
                listOf("0", "00", ",", "+", "=")
            )
            Column(Modifier.fillMaxWidth().height(272.dp * uiScale)) {
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { key ->
                            val isOperation = key in listOf("=", "+", "−", "×", "÷")
                            val isUtility = key in listOf("C", "⌫", "↻")
                            KeyboardKey(
                                label = key,
                                background = when {
                                    key == "=" -> equalsKey
                                    isOperation -> operationKey
                                    isUtility -> utilityKey
                                    else -> numberKey
                                },
                                foreground = if (isOperation || isUtility) Color(0xFFA5F3CF) else Color(0xFFF2F7F4),
                                border = keyDivider,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontScale = uiScale,
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
    modifier: Modifier, fontScale: Float, contentDescription: String? = null, onClick: (() -> Unit)?
) {
    val accessibility = if (contentDescription == null) Modifier else Modifier.semantics {
        this.contentDescription = contentDescription
    }
    Box(
        modifier.background(background).border(0.6.dp, border).then(accessibility).then(
            if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
        ),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) Text(
            label,
            fontSize = (26.sp.value * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}
