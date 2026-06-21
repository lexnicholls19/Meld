package com.lexnicholls.lovecounter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.ui.theme.LovePink

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    
    // Definimos los colores del gradiente para ambos temas
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF0F172A), // Azul marino muy oscuro
            Color(0xFF1E293B), // Azul noche
            Color(0xFF4C0519)  // Fucsia/Borgonia muy oscuro para el toque final
        )
    } else {
        listOf(
            Color(0xFFEBE3FF), // Lila pastel vibrante
            Color(0xFFF7F2FF), // Lavanda muy claro
            Color(0xFFFFD9E2)  // Rosado pastel vibrante
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        content()
    }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = Color.LightGray) {
    Box(
        modifier = modifier
            .height(1.dp)
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            // Simulating a divider with a Box if Material 3 one isn't desired
    )
}

@Composable
fun LoveAlertDialog(
    onDismissRequest: () -> Unit,
    title: String = "",
    confirmButtonText: String = t().confirm,
    dismissButtonText: String = t().cancel,
    onConfirm: () -> Unit,
    showDismissButton: Boolean = true,
    titleContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            if (titleContent != null) {
                titleContent()
            } else if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = LovePink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Box(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
            }) {
                Text(confirmButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (showDismissButton) {
            {
                TextButton(onClick = onDismissRequest) {
                    Text(dismissButtonText)
                }
            }
        } else null
    )
}

@Composable
fun LoveTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String = "",
    isOptional: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    val labelText = if (isOptional) "$label (${t().optionalShort})" else label
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(labelText) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LovePink,
            focusedLabelColor = LovePink
        )
    )
}

@Composable
fun LoveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isOptional: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    val labelText = if (isOptional) "$label (${t().optionalShort})" else label
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(labelText) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LovePink,
            focusedLabelColor = LovePink
        )
    )
}
