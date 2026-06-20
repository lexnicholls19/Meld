package com.lexnicholls.lovecounter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.lexnicholls.lovecounter.ui.navigation.ThemeMode
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
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val strings = t()
    var tempName by remember { mutableStateOf(currentName) }
    var tempTitle by remember { mutableStateOf(currentMainTitle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = strings.settings,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = strings.userName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = tempName,
                onValueChange = { tempName = it },
                placeholder = { Text(strings.writeName) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    val trimmed = tempName.trim()
                    onNameChange(trimmed)
                    Toast.makeText(context, "${strings.save}: $trimmed", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text(strings.save)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.mainTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = tempTitle,
                onValueChange = { tempTitle = it },
                placeholder = { Text(strings.mainTitle) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    onMainTitleChange(tempTitle)
                    Toast.makeText(context, strings.apply, Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text(strings.apply)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = strings.language,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        var languageExpanded by remember { mutableStateOf(false) }
        val currentLangLabel = AppLanguage.entries.find { it.code == currentLanguage }?.label ?: t().system

        ExposedDropdownMenuBox(
            expanded = languageExpanded,
            onExpandedChange = { languageExpanded = !languageExpanded },
            modifier = Modifier.width(200.dp)
        ) {
            OutlinedTextField(
                value = currentLangLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false }
            ) {
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label) },
                        onClick = {
                            onLanguageChange(language.code)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = strings.visibleCategoriesLabel,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val toggleVisible = { category: String, isVisible: Boolean ->
                    val newSet = if (isVisible) currentVisibleCategories + category else currentVisibleCategories - category
                    onVisibleCategoriesChange(newSet)
                }

                WidgetOption(
                    text = strings.reminders,
                    isSelected = currentVisibleCategories.contains("reminders"),
                    onToggle = { toggleVisible("reminders", it) }
                )
                WidgetOption(
                    text = strings.dates,
                    isSelected = currentVisibleCategories.contains("dates"),
                    onToggle = { toggleVisible("dates", it) }
                )
                WidgetOption(
                    text = strings.market,
                    isSelected = currentVisibleCategories.contains("market"),
                    onToggle = { toggleVisible("market", it) }
                )
                WidgetOption(
                    text = strings.bucket,
                    isSelected = currentVisibleCategories.contains("bucket"),
                    onToggle = { toggleVisible("bucket", it) }
                )
                WidgetOption(
                    text = strings.movies,
                    isSelected = currentVisibleCategories.contains("movies"),
                    onToggle = { toggleVisible("movies", it) }
                )
                WidgetOption(
                    text = strings.daily,
                    isSelected = currentVisibleCategories.contains("daily"),
                    onToggle = { toggleVisible("daily", it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = t().widgetContent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val toggleConfig = { config: String, isSelected: Boolean ->
                    val newSet = if (isSelected) currentWidgetConfigs + config else currentWidgetConfigs - config
                    if (newSet.isNotEmpty()) onWidgetConfigsChange(newSet)
                }

                WidgetOption(
                    text = t().timer,
                    isSelected = currentWidgetConfigs.contains("Timer"),
                    onToggle = { toggleConfig("Timer", it) }
                )
                WidgetOption(
                    text = t().reminders,
                    isSelected = currentWidgetConfigs.contains("Reminders"),
                    onToggle = { toggleConfig("Reminders", it) }
                )
                WidgetOption(
                    text = t().dates,
                    isSelected = currentWidgetConfigs.contains("Dates"),
                    onToggle = { toggleConfig("Dates", it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t().dynamicWidget,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = t().dynamicWidgetDesc,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isAutoRotateEnabled,
                    onCheckedChange = onAutoRotateChange
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = t().localCurrency,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val currencies = listOf("COP", "USD", "EUR")
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(150.dp)
        ) {
            OutlinedTextField(
                value = currentCurrency,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencyChange(currency)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = t().currencyDesc,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = t().appTheme,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThemeOption(t().light, ThemeMode.Light, currentTheme, onThemeChange)
                ThemeOption(t().dark, ThemeMode.Dark, currentTheme, onThemeChange)
                ThemeOption(t().system, ThemeMode.System, currentTheme, onThemeChange)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = t().deviceInfo,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "ID: $currentDeviceId",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextButton(
            onClick = onResetDeviceId,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(t().resetId)
        }
        
        Text(
            text = t().idWarning,
            fontSize = 11.sp,
            color = Color.Gray,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
        ) {
            Text("Cerrar Sesión")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun WidgetOption(
    text: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    tooltip: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle(it) })
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 16.sp)
        }
        if (tooltip != null) {
            Text(
                text = tooltip,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun RowScope.ThemeOption(
    text: String,
    mode: ThemeMode,
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val isSelected = mode == currentMode
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clickable { onSelect(mode) },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
