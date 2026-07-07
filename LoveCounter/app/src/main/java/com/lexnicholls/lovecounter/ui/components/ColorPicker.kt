package com.lexnicholls.lovecounter.ui.components

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ColorPickerDialog(
    initialColor1: Color,
    initialColor2: Color,
    defaultColor1: Color,
    defaultColor2: Color,
    isDarkMode: Boolean,
    isUsingDefault: Boolean = false,
    onColorsSelected: (Color?, Color?) -> Unit,
    onDismiss: () -> Unit
) {
    var color1 by remember { mutableStateOf(initialColor1) }
    var color2 by remember { mutableStateOf(initialColor2) }
    var selectingIndex by remember { mutableIntStateOf(0) } // 0 for color1, 1 for color2
    var useDefault by remember { mutableStateOf(isUsingDefault) }

    val strings = t()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.customizeBackground,
                    fontWeight = FontWeight.Bold,
                    color = LovePink,
                    fontSize = 20.sp
                )
                IconButton(onClick = {
                    color1 = defaultColor1
                    color2 = defaultColor2
                    useDefault = true
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = strings.restore, tint = if (useDefault) LovePink else Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector de qué color estamos editando
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ColorSelectorTab(
                        label = strings.color1,
                        isSelected = !useDefault && selectingIndex == 0,
                        color = color1,
                        onClick = { 
                            selectingIndex = 0
                            useDefault = false
                        }
                    )
                    ColorSelectorTab(
                        label = strings.color2,
                        isSelected = !useDefault && selectingIndex == 1,
                        color = color2,
                        onClick = { 
                            selectingIndex = 1
                            useDefault = false
                        }
                    )
                }

                // El Picker principal
                key(selectingIndex, useDefault, isDarkMode) {
                    if (useDefault) {
                        Text(
                            text = strings.system,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 40.dp)
                        )
                    } else {
                        AdvancedColorPicker(
                            initialColor = if (selectingIndex == 0) color1 else color2,
                            isDarkMode = isDarkMode,
                            onColorChange = {
                                if (selectingIndex == 0) color1 = it else color2 = it
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Vista previa del degradado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(color1, color2)))
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if (useDefault) onColorsSelected(null, null)
                else onColorsSelected(color1, color2) 
            }) {
                Text(strings.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun ColorSelectorTab(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(text = label, fontSize = 12.sp, color = if (isSelected) LovePink else Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) LovePink else Color.LightGray,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun AdvancedColorPicker(
    initialColor: Color,
    isDarkMode: Boolean,
    onColorChange: (Color) -> Unit
) {
    // Definir rangos según el tema
    val sRange = if (isDarkMode) 0.0f..1.0f else 0.0f..0.5f // Pasteles tienen poca saturación
    val vRange = if (isDarkMode) 0.05f..0.45f else 0.8f..1.0f // Oscuros vs Claros

    // Mantenemos el estado HSV estable para evitar saltos de Hue a 0 al tocar negros/blancos
    val hsvState = remember {
        val hsvArr = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsvArr)
        // Coerción inicial al crear el estado
        val h = hsvArr[0]
        val s = hsvArr[1].coerceIn(sRange)
        val v = hsvArr[2].coerceIn(vRange)
        mutableStateOf(Triple(h, s, v))
    }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    // Sincronizar si el color inicial cambia desde fuera (ej: presets)
    LaunchedEffect(initialColor, isDarkMode) {
        val hsvArr = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsvArr)
        val newHsv = Triple(hsvArr[0], hsvArr[1].coerceIn(sRange), hsvArr[2].coerceIn(vRange))
        
        val currentHsv = hsvState.value
        val currentColorInt = Color.hsv(currentHsv.first, currentHsv.second, currentHsv.third, alpha).toArgb()
        
        if (currentColorInt != initialColor.toArgb()) {
            hsvState.value = newHsv
            alpha = initialColor.alpha
        }
    }

    val updateColor = {
        val currentHsv = hsvState.value
        onColorChange(Color.hsv(currentHsv.first, currentHsv.second, currentHsv.third, alpha))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            // Anillo exterior de Hue
            HueRing(hue = hsvState.value.first, isDarkMode = isDarkMode, onHueChange = {
                hsvState.value = hsvState.value.copy(first = it)
                updateColor()
            })
            
            // Círculo/Cuadrado interior de Saturación y Valor
            SaturationValueArea(
                hue = hsvState.value.first,
                saturation = hsvState.value.second,
                value = hsvState.value.third,
                isDarkMode = isDarkMode,
                onSVChange = { s, v ->
                    hsvState.value = hsvState.value.copy(second = s.coerceIn(sRange), third = v.coerceIn(vRange))
                    updateColor()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Barra de intensidad (Alpha)
        Text(text = "Intensidad", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
        AlphaSlider(
            color = Color.hsv(hsvState.value.first, hsvState.value.second, hsvState.value.third),
            alpha = alpha,
            onAlphaChange = {
                alpha = it
                updateColor()
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Valores Hex / RGB
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentHsv = hsvState.value
            val colorInt = Color.hsv(currentHsv.first, currentHsv.second, currentHsv.third, alpha).toArgb()
            val hex = String.format("#%08X", colorInt)
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = hex, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Row {
                RgbBox("R", AndroidColor.red(colorInt))
                RgbBox("G", AndroidColor.green(colorInt))
                RgbBox("B", AndroidColor.blue(colorInt))
            }
        }
    }
}

@Composable
fun RgbBox(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(text = value.toString(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
        }
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun HueRing(hue: Float, isDarkMode: Boolean, onHueChange: (Float) -> Unit) {
    Canvas(modifier = Modifier
        .size(240.dp)
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val pos = change.position
                val dist = sqrt((pos.x - center.x).pow(2) + (pos.y - center.y).pow(2))
                
                // Solo reaccionar si el toque está en el área del anillo (ancho ~30dp)
                // Inicia aprox a los 85dp del centro
                if (dist > 80.dp.toPx()) {
                    val angle = atan2(pos.y - center.y, pos.x - center.x)
                    var newHue = (angle * 180f / PI.toFloat())
                    if (newHue < 0) newHue += 360f
                    onHueChange(newHue)
                }
            }
        }
    ) {
        val radius = size.minDimension / 2f
        val thickness = 30.dp.toPx()
        
        // Ajustar el brillo del anillo según el tema para feedback visual
        val brightness = if (isDarkMode) 0.35f else 1.0f
        fun getThemedColor(h: Float) = AndroidColor.HSVToColor(floatArrayOf(h, if (isDarkMode) 0.8f else 0.5f, brightness))

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = thickness
                shader = SweepGradient(size.width / 2f, size.height / 2f, 
                    intArrayOf(
                        getThemedColor(0f), getThemedColor(60f), getThemedColor(120f), 
                        getThemedColor(180f), getThemedColor(240f), getThemedColor(300f), getThemedColor(360f)
                    ), null)
            }
            canvas.nativeCanvas.drawCircle(size.width / 2f, size.height / 2f, radius - thickness / 2f, paint)
        }
        
        // Indicador
        val angleRad = (hue * PI / 180f).toFloat()
        val indicatorRadius = radius - thickness / 2f
        val indicatorPos = Offset(
            center.x + indicatorRadius * cos(angleRad),
            center.y + indicatorRadius * sin(angleRad)
        )
        
        drawCircle(
            color = Color.White,
            radius = 12.dp.toPx(),
            center = indicatorPos,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun SaturationValueArea(hue: Float, saturation: Float, value: Float, isDarkMode: Boolean, onSVChange: (Float, Float) -> Unit) {
    // Definimos el tamaño fijo para cálculos consistentes
    val sizeDp = 160.dp
    
    // Rangos para coerción visual y de datos
    val sRange = if (isDarkMode) 0.0f..1.0f else 0.0f..0.5f
    val vRange = if (isDarkMode) 0.05f..0.45f else 0.8f..1.0f

    Box(
        modifier = Modifier
            .size(sizeDp)
            .pointerInput(isDarkMode) {
                // Usamos un scope de bajo nivel para evitar el "touch slop" (demora inicial)
                // y permitir que el selector responda desde el primer milisegundo.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitFirstDown()
                        val update = { pos: Offset ->
                            val s = (pos.x / size.width).coerceIn(0f, 1f).coerceIn(sRange)
                            val v = (1f - (pos.y / size.height)).coerceIn(0f, 1f).coerceIn(vRange)
                            onSVChange(s, v)
                        }
                        
                        update(event.position)
                        
                        drag(event.id) { change ->
                            update(change.position)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        // El círculo visual: lo dibujamos dentro de la caja táctil
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Black
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.width / 2f
                
                drawIntoCanvas { canvas ->
                    // Ajustar los gradientes para que coincidan con los rangos permitidos
                    // Para Dark Mode, el fondo general es más oscuro.
                    // Para Light Mode, es más pastel.
                    
                    val paint = Paint().apply { isAntiAlias = true }
                    
                    // 1. Color base (Matiz) - Influenciado por V y S mínimos para mejor feedback visual
                    val baseV = if (isDarkMode) vRange.endInclusive else vRange.start
                    val baseS = if (isDarkMode) sRange.endInclusive else sRange.start
                    val hsvColor = AndroidColor.HSVToColor(floatArrayOf(hue, baseS, baseV))
                    
                    canvas.nativeCanvas.drawCircle(radius, radius, radius, paint.apply {
                        shader = null
                        color = hsvColor
                    })
                    
                    // 2. Gradiente de Saturación (Blanco -> Transparente o similar)
                    // En modo claro, limitamos visualmente la saturación
                    val sStartColor = if (isDarkMode) AndroidColor.WHITE else AndroidColor.WHITE
                    val sEndColor = if (isDarkMode) AndroidColor.TRANSPARENT else AndroidColor.argb(150, 255, 255, 255)
                    
                    val whiteGradient = LinearGradient(0f, 0f, size.width, 0f, sStartColor, sEndColor, Shader.TileMode.CLAMP)
                    canvas.nativeCanvas.drawCircle(radius, radius, radius, paint.apply {
                        shader = whiteGradient
                    })
                    
                    // 3. Gradiente de Brillo (Transparente -> Negro o similar)
                    val vStartColor = if (isDarkMode) AndroidColor.argb(100, 0, 0, 0) else AndroidColor.TRANSPARENT
                    val vEndColor = if (isDarkMode) AndroidColor.BLACK else AndroidColor.argb(50, 0, 0, 0)
                    
                    val blackGradient = LinearGradient(0f, 0f, 0f, size.height, vStartColor, vEndColor, Shader.TileMode.CLAMP)
                    canvas.nativeCanvas.drawCircle(radius, radius, radius, paint.apply {
                        shader = blackGradient
                    })
                }
            }
        }
        
        // Indicador de selección: Calculamos su posición restringida al círculo visual
        val density = androidx.compose.ui.platform.LocalDensity.current
        val sizePx = with(density) { sizeDp.toPx() }
        val radiusPx = sizePx / 2f
        
        // Posición teórica en un plano cuadrado
        val rawX = saturation * sizePx
        val rawY = (1f - value) * sizePx
        
        // Proyectamos la posición al círculo para que el indicador nunca se salga visualmente
        val dx = rawX - radiusPx
        val dy = rawY - radiusPx
        val dist = sqrt(dx*dx + dy*dy)
        
        val finalX = if (dist <= radiusPx) rawX else radiusPx + (dx / dist) * radiusPx
        val finalY = if (dist <= radiusPx) rawY else radiusPx + (dy / dist) * radiusPx
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(finalX, finalY),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun AlphaSlider(color: Color, alpha: Float, onAlphaChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(vertical = 4.dp)
    ) {
        // Fondo de cuadros (transparencia)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 8.dp.toPx()
            for (x in 0 until (size.width / step).toInt()) {
                for (y in 0 until (size.height / step).toInt()) {
                    if ((x + y) % 2 == 0) {
                        drawRect(Color.LightGray, Offset(x * step, y * step), IntSize(step.toInt(), step.toInt()).toSize())
                    }
                }
            }
        }
        
        // Gradiente de color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color)))
        )
        
        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            modifier = Modifier.fillMaxSize(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}

fun IntSize.toSize() = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
