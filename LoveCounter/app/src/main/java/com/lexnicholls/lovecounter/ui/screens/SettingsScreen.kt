package com.lexnicholls.lovecounter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.lexnicholls.lovecounter.ui.navigation.ThemeMode
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.AppLanguage
import com.lexnicholls.lovecounter.util.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    currentName: String,
    currentDeviceId: String,
    currentWidgetConfigs: Set<String>,
    isAutoRotateEnabled: Boolean,
    currentCurrency: String,
    currentMainTitle: String,
    currentLanguage: String,
    currentVisibleCategories: Set<String>,
    onThemeChange: (ThemeMode) -> Unit,
    onNameChange: (String) -> Unit,
    onMainTitleChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onVisibleCategoriesChange: (Set<String>) -> Unit,
    onWidgetConfigsChange: (Set<String>) -> Unit,
    onAutoRotateChange: (Boolean) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onResetDeviceId: () -> Unit,
    onLogout: () -> Unit,
    onSyncQuestions: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = t()
    val loveViewModel: com.lexnicholls.lovecounter.viewmodel.LoveViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    val relationId by loveViewModel.relationId
    val linkingCode by loveViewModel.linkingCode
    val isLinking by loveViewModel.isLinking

    var tempName by remember { mutableStateOf(currentName) }
    var tempTitle by remember { mutableStateOf(currentMainTitle) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showRelationDetailsDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf(TextFieldValue("")) }

    val members by loveViewModel.members

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = strings.settings,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // --- SECCIÓN: PAREJA Y ENLACE ---
        SettingsGroup(title = "Relación") {
            if (relationId != null) {
                SettingsClickableRow(
                    label = "Estado de Relación",
                    value = "¡Enlazado! (${members.size} integrantes)",
                    icon = Icons.Default.Favorite,
                    onClick = { showRelationDetailsDialog = true }
                )
                SettingsClickableRow(
                    label = "Detalles de Relación",
                    value = "Ver integrantes y código",
                    icon = Icons.Default.Groups,
                    onClick = { showRelationDetailsDialog = true }
                )
                Button(
                    onClick = { loveViewModel.unlinkPartner() },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Desvincular pareja")
                }
            } else {
                SettingsClickableRow(
                    label = "Vincular con mi pareja",
                    value = if (linkingCode != null) {
                        val formatted = linkingCode?.chunked(4)?.joinToString("-") ?: ""
                        "Código: $formatted"
                    } else "Generar código",
                    icon = Icons.Default.Link,
                    onClick = { 
                        if (linkingCode == null) loveViewModel.generateLinkingCode()
                        else {
                            // Copiar al portapapeles (el código sin guiones para que sea fácil de pegar)
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Código de Enlace", linkingCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                if (linkingCode != null) {
                    TextButton(
                        onClick = { loveViewModel.generateLinkingCode() },
                        modifier = Modifier.align(Alignment.End).padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Generar otro código", fontSize = 12.sp)
                    }
                }
                SettingsClickableRow(
                    label = "Tengo un código",
                    value = "Ingresar código de pareja",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { showLinkDialog = true }
                )
            }
        }

        // --- SECCIÓN: PERFIL Y PERSONALIZACIÓN ---
        SettingsGroup(title = "Personalización") {
            SettingsInputRow(
                label = strings.userName,
                value = tempName,
                onValueChange = { tempName = it },
                onSave = { onNameChange(tempName.trim()) },
                icon = Icons.Default.Person
            )
            
            SettingsInputRow(
                label = strings.mainTitle,
                value = tempTitle,
                onValueChange = { tempTitle = it },
                onSave = { onMainTitleChange(tempTitle) },
                icon = Icons.Default.Title
            )
        }

        // --- SECCIÓN: APARIENCIA ---
        SettingsGroup(title = "Apariencia") {
            // Tema
            SettingsDropdownRow(
                label = strings.appTheme,
                currentValue = when(currentTheme) {
                    ThemeMode.Light -> strings.light
                    ThemeMode.Dark -> strings.dark
                    ThemeMode.System -> strings.system
                },
                icon = Icons.Default.Palette
            ) {
                ThemeMode.entries.forEach { mode ->
                    val label = when(mode) {
                        ThemeMode.Light -> strings.light
                        ThemeMode.Dark -> strings.dark
                        ThemeMode.System -> strings.system
                    }
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onThemeChange(mode) }
                    )
                }
            }

            // Idioma
            val currentLangLabel = AppLanguage.entries.find { it.code == currentLanguage }?.label ?: strings.system
            SettingsDropdownRow(
                label = strings.language,
                currentValue = currentLangLabel,
                icon = Icons.Default.Language
            ) {
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label) },
                        onClick = { onLanguageChange(language.code) }
                    )
                }
            }
        }

        // --- SECCIÓN: CONTENIDO Y WIDGET ---
        SettingsGroup(title = "Contenido") {
            // Categorías Visibles (Diálogo para no saturar)
            SettingsClickableRow(
                label = strings.visibleCategoriesLabel,
                value = "${currentVisibleCategories.size} seleccionadas",
                icon = Icons.Default.Visibility,
                onClick = { showCategoriesDialog = true }
            )

            // Configuración Widget
            SettingsClickableRow(
                label = strings.widgetContent,
                value = "${currentWidgetConfigs.size} módulos",
                icon = Icons.Default.Widgets,
                onClick = { showWidgetDialog = true }
            )

            // Auto-rotar Switch
            SettingsSwitchRow(
                label = strings.dynamicWidget,
                subtitle = strings.dynamicWidgetDesc,
                checked = isAutoRotateEnabled,
                onCheckedChange = onAutoRotateChange,
                icon = Icons.AutoMirrored.Filled.RotateRight
            )
        }

        // --- SECCIÓN: SISTEMA ---
        SettingsGroup(title = "Sistema") {
            // Moneda
            SettingsDropdownRow(
                label = strings.localCurrency,
                currentValue = currentCurrency,
                icon = Icons.Default.Payments
            ) {
                listOf("COP", "USD", "EUR").forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = { onCurrencyChange(currency) }
                    )
                }
            }

            // Info de dispositivo
            SettingsClickableRow(
                label = "ID del Dispositivo",
                value = currentDeviceId.take(8) + "...",
                icon = Icons.Default.Fingerprint,
                onClick = { /* Solo informativo o copiar */ }
            )
        }

        // --- SECCIÓN: CUENTA Y ADMIN ---
        val privateUserIds = remember { setOf("CX4z9DcQYxTJeaIdyNgzpDQqw6U2", "pW562p0UqNfEicrVd0q3oRRE9373") }
        val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

        if (currentUserId in privateUserIds) {
            SettingsGroup(title = "Administración") {
                SettingsClickableRow(
                    label = "Sincronizar Datos",
                    value = "Actualizar Firebase",
                    icon = Icons.Default.CloudSync,
                    onClick = {
                        onSyncQuestions()
                        Toast.makeText(context, "Sincronizando...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onResetDeviceId,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.resetId)
        }

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar Sesión")
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- DIÁLOGOS ---
        if (showRelationDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showRelationDetailsDialog = false },
                title = { Text("Detalles de la Relación") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Integrantes:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        members.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = LovePink)
                                Spacer(Modifier.width(8.dp))
                                Text(if (member.name.isNotBlank()) member.name else "Usuario sin nombre")
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text("Código de Enlace:", fontWeight = FontWeight.Bold)
                        val codeToShow = linkingCode ?: "Sin código generado"
                        val formatted = codeToShow.chunked(4).joinToString("-")
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatted, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            IconButton(onClick = {
                                if (linkingCode != null) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Código de Enlace", linkingCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                } else {
                                    loveViewModel.generateLinkingCode()
                                }
                            }) {
                                Icon(if (linkingCode != null) Icons.Default.ContentCopy else Icons.Default.Refresh, contentDescription = "Acción")
                            }
                        }
                        Text("Comparte este código para que más personas se unan a esta relación.", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRelationDetailsDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        if (showCategoriesDialog) {
            AlertDialog(
                onDismissRequest = { showCategoriesDialog = false },
                title = { Text(strings.visibleCategoriesLabel) },
                text = {
                    Column {
                        val categories = listOf(
                            "reminders" to strings.reminders,
                            "dates" to strings.dates,
                            "market" to strings.market,
                            "bucket" to strings.bucket,
                            "movies" to strings.movies,
                            "daily" to strings.daily
                        )
                        categories.forEach { (id, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val newSet = if (currentVisibleCategories.contains(id)) currentVisibleCategories - id else currentVisibleCategories + id
                                    onVisibleCategoriesChange(newSet)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = currentVisibleCategories.contains(id), onCheckedChange = {
                                    val newSet = if (it) currentVisibleCategories + id else currentVisibleCategories - id
                                    onVisibleCategoriesChange(newSet)
                                })
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showCategoriesDialog = false }) { Text("OK") } }
            )
        }

        if (showWidgetDialog) {
            AlertDialog(
                onDismissRequest = { showWidgetDialog = false },
                title = { Text(strings.widgetContent) },
                text = {
                    Column {
                        val configs = listOf("Timer" to strings.timer, "Reminders" to strings.reminders, "Dates" to strings.dates)
                        configs.forEach { (id, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val newSet = if (currentWidgetConfigs.contains(id)) currentWidgetConfigs - id else currentWidgetConfigs + id
                                    if (newSet.isNotEmpty()) onWidgetConfigsChange(newSet)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = currentWidgetConfigs.contains(id), onCheckedChange = {
                                    val newSet = if (it) currentWidgetConfigs + id else currentWidgetConfigs - id
                                    if (newSet.isNotEmpty()) onWidgetConfigsChange(newSet)
                                })
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showWidgetDialog = false }) { Text("OK") } }
            )
        }

        if (showLinkDialog) {
            com.lexnicholls.lovecounter.ui.components.LoveAlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = "Vincular con pareja",
                onConfirm = {
                    val rawCode = inputCode.text.filter { it.isDigit() }
                    if (rawCode.length == 16) {
                        loveViewModel.linkWithPartner(rawCode)
                        showLinkDialog = false
                    } else {
                        Toast.makeText(context, "El código debe tener 16 dígitos", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Column {
                    Text("Ingresa el código de 16 dígitos generado por tu pareja.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    com.lexnicholls.lovecounter.ui.components.LoveTextField(
                        value = inputCode,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.text.filter { it.isDigit() }
                            val limitedDigits = if (digitsOnly.length > 16) digitsOnly.take(16) else digitsOnly
                            
                            // Re-formatear con guiones
                            val formatted = limitedDigits.chunked(4).joinToString("-")
                            
                            // Si el texto ha cambiado, movemos el cursor al final de lo nuevo
                            if (formatted != inputCode.text) {
                                inputCode = TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                            } else {
                                // Si solo ha cambiado la selección (el cursor), la mantenemos
                                inputCode = newValue
                            }
                        },
                        label = "Código de Enlace",
                        placeholder = "1234-5678-9012-3456",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    if (isLinking) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LovePink,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsInputRow(label: String, value: String, onValueChange: (String) -> Unit, onSave: () -> Unit, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = Color.Gray)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            )
        }
        IconButton(onClick = onSave) {
            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SettingsDropdownRow(label: String, currentValue: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 14.sp, color = Color.Gray)
                Text(text = currentValue, fontSize = 16.sp)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Pasamos una lambda que cierre el menú además de la acción
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsClickableRow(label: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = Color.Gray)
            Text(text = value, fontSize = 16.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun SettingsSwitchRow(label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 16.sp)
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
