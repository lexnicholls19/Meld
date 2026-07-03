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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
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
    onColorsSelected: (Color, Color) -> Unit,
    onDismiss: () -> Unit
) {
    var color1 by remember { mutableStateOf(initialColor1) }
    var color2 by remember { mutableStateOf(initialColor2) }
    var selectingIndex by remember { mutableIntStateOf(0) } // 0 for color1, 1 for color2

    val strings = t()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.customizeBackground,
                fontWeight = FontWeight.Bold,
                color = LovePink
            )
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
                        isSelected = selectingIndex == 0,
                        color = color1,
                        onClick = { selectingIndex = 0 }
                    )
                    ColorSelectorTab(
                        label = strings.color2,
                        isSelected = selectingIndex == 1,
                        color = color2,
                        onClick = { selectingIndex = 1 }
                    )
                }

                // El Picker principal
                AdvancedColorPicker(
                    initialColor = if (selectingIndex == 0) color1 else color2,
                    onColorChange = {
                        if (selectingIndex == 0) color1 = it else color2 = it
                    }
                )
                
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
            TextButton(onClick = { onColorsSelected(color1, color2) }) {
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
    onColorChange: (Color) -> Unit
) {
    var hsv by remember(initialColor) {
        val hsvArr = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsvArr)
        mutableStateOf(Triple(hsvArr[0], hsvArr[1], hsvArr[2]))
    }
    var alpha by remember(initialColor) { mutableFloatStateOf(initialColor.alpha) }

    val updateColor = {
        onColorChange(Color.hsv(hsv.first, hsv.second, hsv.third, alpha))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            // Anillo exterior de Hue
            HueRing(hue = hsv.first, onHueChange = {
                hsv = hsv.copy(first = it)
                updateColor()
            })
            
            // Círculo interior de Saturación y Valor
            SaturationValueCircle(
                hue = hsv.first,
                saturation = hsv.second,
                value = hsv.third,
                onSVChange = { s, v ->
                    hsv = hsv.copy(second = s, third = v)
                    updateColor()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Barra de intensidad (Alpha)
        Text(text = "Intensidad", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
        AlphaSlider(
            color = Color.hsv(hsv.first, hsv.second, hsv.third),
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
            val colorInt = Color.hsv(hsv.first, hsv.second, hsv.third, alpha).toArgb()
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
fun HueRing(hue: Float, onHueChange: (Float) -> Unit) {
    Canvas(modifier = Modifier
        .size(240.dp)
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val angle = atan2(change.position.y - center.y, change.position.x - center.x)
                var newHue = (angle * 180f / PI.toFloat())
                if (newHue < 0) newHue += 360f
                onHueChange(newHue)
            }
        }
    ) {
        val radius = size.minDimension / 2f
        val thickness = 30.dp.toPx()
        
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = thickness
                shader = SweepGradient(size.width / 2f, size.height / 2f, 
                    intArrayOf(
                        AndroidColor.RED, AndroidColor.MAGENTA, AndroidColor.BLUE, 
                        AndroidColor.CYAN, AndroidColor.GREEN, AndroidColor.YELLOW, AndroidColor.RED
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
fun SaturationValueCircle(hue: Float, saturation: Float, value: Float, onSVChange: (Float, Float) -> Unit) {
    Canvas(modifier = Modifier
        .size(160.dp)
        .clip(CircleShape)
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val dx = (change.position.x - center.x) / (size.width / 2f)
                val dy = (change.position.y - center.y) / (size.height / 2f)
                
                // Mapeo simple: X -> Saturation, Y -> Value (invertido)
                // Para que se mantenga dentro del círculo, limitamos por radio
                val dist = sqrt(dx * dx + dy * dy).coerceAtMost(1f)
                
                val newS = dist
                val newV = (1f - dy).coerceIn(0f, 1f) // Aproximación visual
                
                onSVChange(newS, newV)
            }
        }
    ) {
        val radius = size.width / 2f
        
        drawIntoCanvas { canvas ->
            val hsvColor = AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))
            
            // Simular el picker de la imagen:
            // 1. Fondo del color Hue saturado
            canvas.nativeCanvas.drawCircle(radius, radius, radius, Paint().apply {
                color = hsvColor
                isAntiAlias = true
            })
            
            // 2. Gradiente blanco (Saturación) de izquierda a derecha (o radial)
            val whiteGradient = LinearGradient(0f, 0f, size.width, 0f, AndroidColor.WHITE, AndroidColor.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.nativeCanvas.drawCircle(radius, radius, radius, Paint().apply {
                shader = whiteGradient
                isAntiAlias = true
            })
            
            // 3. Gradiente negro (Valor) de abajo a arriba
            val blackGradient = LinearGradient(0f, size.height, 0f, 0f, AndroidColor.BLACK, AndroidColor.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.nativeCanvas.drawCircle(radius, radius, radius, Paint().apply {
                shader = blackGradient
                isAntiAlias = true
            })
        }
        
        // Indicador - Para este mapeo simple:
        // Buscamos una posición que represente S y V
        // Como es un círculo, es mejor usar un mapeo cuadrado y clipear, 
        // pero el usuario pidió un círculo.
        // Usemos una posición fija basada en el S y V actuales (aproximado)
        val indicatorX = radius + (saturation * radius * 0.5f) // Muy aproximado
        val indicatorY = radius + ((0.5f - value) * radius) // Muy aproximado
        
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(indicatorX.coerceIn(0f, size.width), indicatorY.coerceIn(0f, size.height)),
            style = Stroke(width = 2.dp.toPx())
        )
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
