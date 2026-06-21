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
import androidx.compose.ui.text.font.FontWeight
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
    var tempName by remember { mutableStateOf(currentName) }
    var tempTitle by remember { mutableStateOf(currentMainTitle) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }

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
