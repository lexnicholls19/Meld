package com.lexnicholls.lovecounter.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    currentWidgetConfigs: Set<String>,
    isAutoRotateEnabled: Boolean,
    currentCurrency: String,
    currentMainTitle: String,
    currentLanguage: String,
    currentVisibleCategories: Set<String>,
    currentRelationshipDate: Long?,
    onThemeChange: (ThemeMode) -> Unit,
    onNameChange: (String) -> Unit,
    onMainTitleChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onVisibleCategoriesChange: (Set<String>) -> Unit,
    onWidgetConfigsChange: (Set<String>) -> Unit,
    onAutoRotateChange: (Boolean) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onRelationshipDateChange: (Long?) -> Unit,
    onLogout: () -> Unit,
    onSyncQuestions: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = t()
    val sharedPrefs = remember { context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE) }
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
    
    var profilePicUri by remember { 
        mutableStateOf(sharedPrefs.getString("profile_pic_uri", null)?.let { Uri.parse(it) }) 
    }
    var showProfileMenu by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profilePicUri = it
            sharedPrefs.edit().putString("profile_pic_uri", it.toString()).apply()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.settings,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Profile Picture Circle
            Box {
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, LovePink, CircleShape)
                        .clickable { showProfileMenu = true },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (profilePicUri != null) {
                        AsyncImage(
                            model = profilePicUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = Color.Gray
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = showProfileMenu,
                    onDismissRequest = { showProfileMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.changeProfilePic) },
                        leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                        onClick = {
                            showProfileMenu = false
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.changeActiveProfile) },
                        leadingIcon = { Icon(Icons.Default.SwitchAccount, contentDescription = null) },
                        onClick = {
                            showProfileMenu = false
                            // Add logic here if needed
                            Toast.makeText(context, strings.comingSoon, Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text(strings.logout, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showProfileMenu = false
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        }
                    )
                }
            }
        }

        // --- SECCIÓN: PAREJA Y ENLACE ---
        SettingsGroup(title = strings.relation) {
            if (relationId != null) {
                SettingsClickableRow(
                    label = strings.relationStatus,
                    value = strings.linkedStatus.format(members.size),
                    icon = Icons.Default.Groups,
                    onClick = { showRelationDetailsDialog = true }
                )
                Button(
                    onClick = { loveViewModel.unlinkPartner() },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.unlinkPartner)
                }
            } else {
                SettingsClickableRow(
                    label = strings.linkWithPartner,
                    value = if (linkingCode != null) {
                        val formatted = linkingCode?.chunked(4)?.joinToString("-") ?: ""
                        "${strings.linkingCodeLabel.replace(":", "")} $formatted"
                    } else strings.generateCode,
                    icon = Icons.Default.Link,
                    onClick = { 
                        if (linkingCode == null) loveViewModel.generateLinkingCode()
                        else {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Código de Enlace", linkingCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, strings.codeCopied, Toast.LENGTH_SHORT).show()
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
                        Text(strings.generateAnotherCode, fontSize = 12.sp)
                    }
                }
                SettingsClickableRow(
                    label = strings.haveCode,
                    value = strings.enterPartnerCode,
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { showLinkDialog = true }
                )
            }
        }

        // --- SECCIÓN: PERFIL Y PERSONALIZACIÓN ---
        SettingsGroup(title = strings.customization) {
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
                icon = Icons.Default.Title,
                onDelete = {
                    tempTitle = ""
                    onMainTitleChange("")
                },
                placeholder = strings.mainTitleTooltip
            )

            // Relationship Start Date
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentRelationshipDate)
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            onRelationshipDateChange(datePickerState.selectedDateMillis)
                            showDatePicker = false
                        }) { Text(strings.confirm) }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showDatePicker = false 
                        }) { Text(strings.cancel) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            val dateDisplay = currentRelationshipDate?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            } ?: strings.notSelected

            SettingsClickableRow(
                label = strings.relationshipDate,
                value = dateDisplay,
                icon = Icons.Default.CalendarToday,
                onClick = { showDatePicker = true },
                onDelete = { onRelationshipDateChange(null) }
            )
        }

        // --- SECCIÓN: APARIENCIA ---
        SettingsGroup(title = strings.appearance) {
            // Tema
            SettingsDropdownRow(
                label = strings.appTheme,
                currentValue = when(currentTheme) {
                    ThemeMode.Light -> strings.light
                    ThemeMode.Dark -> strings.dark
                    ThemeMode.System -> strings.system
                },
                icon = Icons.Default.Palette
            ) { onDismiss ->
                ThemeMode.entries.forEach { mode ->
                    val label = when(mode) {
                        ThemeMode.Light -> strings.light
                        ThemeMode.Dark -> strings.dark
                        ThemeMode.System -> strings.system
                    }
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { 
                            onThemeChange(mode)
                            onDismiss()
                        }
                    )
                }
            }

            // Idioma
            val currentLangLabel = AppLanguage.entries.find { it.code == currentLanguage }?.label ?: strings.system
            SettingsDropdownRow(
                label = strings.language,
                currentValue = currentLangLabel,
                icon = Icons.Default.Language
            ) { onDismiss ->
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label) },
                        onClick = { 
                            onLanguageChange(language.code)
                            onDismiss()
                        }
                    )
                }
            }
        }

        // --- SECCIÓN: CONTENIDO Y WIDGET ---
        SettingsGroup(title = strings.content) {
            // Categorías Visibles (Diálogo para no saturar)
            SettingsClickableRow(
                label = strings.visibleCategoriesLabel,
                value = strings.selectedCount.format(currentVisibleCategories.size),
                icon = Icons.Default.Visibility,
                onClick = { showCategoriesDialog = true }
            )

            // Configuración Widget
            SettingsClickableRow(
                label = strings.widgetContent,
                value = strings.modulesCount.format(currentWidgetConfigs.size),
                icon = Icons.Default.Widgets,
                onClick = { showWidgetDialog = true }
            )

            // Auto-rotar Switch
            SettingsSwitchRow(
                label = strings.dynamicWidget,
                subtitle = if (currentWidgetConfigs.size < 2) strings.selectAtLeastTwo else strings.dynamicWidgetDesc,
                checked = isAutoRotateEnabled && currentWidgetConfigs.size >= 2,
                onCheckedChange = onAutoRotateChange,
                icon = Icons.AutoMirrored.Filled.RotateRight,
                enabled = currentWidgetConfigs.size >= 2
            )
        }

        // --- SECCIÓN: SISTEMA ---
        SettingsGroup(title = strings.system) {
            // Moneda
            SettingsDropdownRow(
                label = strings.localCurrency,
                currentValue = currentCurrency,
                icon = Icons.Default.Payments
            ) { onDismiss ->
                listOf("COP", "USD", "EUR").forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = { 
                            onCurrencyChange(currency)
                            onDismiss()
                        }
                    )
                }
            }
        }

        // --- SECCIÓN: CUENTA Y ADMIN ---
        val privateUserIds = remember { setOf("CX4z9DcQYxTJeaIdyNgzpDQqw6U2", "pW562p0UqNfEicrVd0q3oRRE9373") }
        val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

        if (currentUserId in privateUserIds) {
            SettingsGroup(title = strings.admin) {
                SettingsClickableRow(
                    label = strings.syncData,
                    value = strings.updateFirebase,
                    icon = Icons.Default.CloudSync,
                    onClick = {
                        onSyncQuestions()
                        Toast.makeText(context, strings.synchronizing, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- DIÁLOGOS ---
        if (showRelationDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showRelationDetailsDialog = false },
                title = { Text(strings.relationDetails) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(strings.members, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        members.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = LovePink)
                                Spacer(Modifier.width(8.dp))
                                Text(if (member.name.isNotBlank()) member.name else strings.unnamedUser)
                            }
                        }
                        
                        com.lexnicholls.lovecounter.ui.components.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text(strings.linkingCodeLabel, fontWeight = FontWeight.Bold)
                        val codeToShow = linkingCode ?: strings.noCodeGenerated
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
                                    Toast.makeText(context, strings.codeCopied, Toast.LENGTH_SHORT).show()
                                } else {
                                    loveViewModel.generateLinkingCode()
                                }
                            }) {
                                Icon(if (linkingCode != null) Icons.Default.ContentCopy else Icons.Default.Refresh, contentDescription = "Acción")
                            }
                        }
                        Text(strings.shareCodeDesc, fontSize = 12.sp, color = Color.Gray)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRelationDetailsDialog = false }) {
                        Text(strings.close)
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
                            "drawing" to strings.drawing,
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
                confirmButton = { TextButton(onClick = { showCategoriesDialog = false }) { Text(strings.confirm) } }
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
                confirmButton = { TextButton(onClick = { showWidgetDialog = false }) { Text(strings.confirm) } }
            )
        }

        if (showLinkDialog) {
            com.lexnicholls.lovecounter.ui.components.LoveAlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = strings.linkWithPartner,
                onConfirm = {
                    val rawCode = inputCode.text.filter { it.isDigit() }
                    if (rawCode.length == 16) {
                        loveViewModel.linkWithPartner(rawCode)
                        showLinkDialog = false
                    } else {
                        Toast.makeText(context, strings.codeMustBe16Digits, Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Column {
                    Text(strings.linkDialogDesc, fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    com.lexnicholls.lovecounter.ui.components.LoveTextField(
                        value = inputCode,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.text.filter { it.isDigit() }
                            val limitedDigits = if (digitsOnly.length > 16) digitsOnly.take(16) else digitsOnly
                            
                            val formatted = limitedDigits.chunked(4).joinToString("-")
                            
                            if (formatted != inputCode.text) {
                                inputCode = TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                            } else {
                                inputCode = newValue
                            }
                        },
                        label = strings.linkingCodeLabel.replace(":", ""),
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
fun SettingsInputRow(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    onSave: () -> Unit, 
    icon: ImageVector,
    onDelete: (() -> Unit)? = null,
    placeholder: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = Color.Gray)
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder, 
                        color = LovePink.copy(alpha = 0.5f), 
                        fontSize = 16.sp, 
                        fontStyle = FontStyle.Italic
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Row {
            if (onDelete != null && value.isNotBlank()) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SettingsDropdownRow(
    label: String, 
    currentValue: String, 
    icon: ImageVector, 
    content: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var itemWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth().onGloballyPositioned { itemWidth = it.size.width }) {
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
        
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { itemWidth.toDp() })
        ) {
            content { expanded = false }
        }
    }
}

@Composable
fun SettingsClickableRow(
    label: String, 
    value: String, 
    icon: ImageVector, 
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
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
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null && value != t().notSelected) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun SettingsSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.3f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
