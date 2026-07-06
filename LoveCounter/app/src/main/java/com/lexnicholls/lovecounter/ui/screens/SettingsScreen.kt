package com.lexnicholls.lovecounter.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.lexnicholls.lovecounter.util.ProfileImageManager
import com.lexnicholls.lovecounter.util.t
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    currentName: String,
    currentWidgetConfigs: Set<String>,
    isAutoRotateEnabled: Boolean,
    currentAutoRotateInterval: Int,
    currentCurrency: String,
    currentMainTitle: String,
    currentLanguage: String,
    currentVisibleCategories: Set<String>,
    currentRelationshipDate: Long?,
    currentBgColor1: String?,
    currentBgColor2: String?,
    onThemeChange: (ThemeMode) -> Unit,
    onNameChange: (String) -> Unit,
    onMainTitleChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onVisibleCategoriesChange: (Set<String>) -> Unit,
    onWidgetConfigsChange: (Set<String>) -> Unit,
    onAutoRotateChange: (Boolean) -> Unit,
    onAutoRotateIntervalChange: (Int) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onRelationshipDateChange: (Long?) -> Unit,
    onFabMessagesChange: (String, String, String, String) -> Unit,
    onBackgroundColorsChange: (String?, String?) -> Unit,
    onLogout: () -> Unit,
    onNavigateToAdvanced: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = t()
    val sharedPrefs = remember { context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE) }
    val loveViewModel: com.lexnicholls.lovecounter.viewmodel.LoveViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val scope = rememberCoroutineScope()
    
    val relationId by loveViewModel.relationId
    val linkingCode by loveViewModel.linkingCode
    val isLinking by loveViewModel.isLinking
    val currentUserProfile by loveViewModel.currentUserProfile

    val availableRelations by loveViewModel.availableRelations
    var showSwitchProfileDialog by remember { mutableStateOf(false) }
    var showRenameRelationDialog by remember { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var renameValue by remember { mutableStateOf("") }

    val sharedId by loveViewModel.sharedId
    val prefix = remember(sharedId) { if (sharedId != null) "rel_${sharedId}_" else "" }

    var tempName by remember(currentName) { mutableStateOf(currentName) }
    var tempTitle by remember(currentMainTitle) { mutableStateOf(currentMainTitle) }
    var tempFab1 by remember(prefix) { 
        mutableStateOf(sharedPrefs.getString("${prefix}fab_message_1", "") ?: "") 
    }
    var tempFab2 by remember(prefix) { 
        mutableStateOf(sharedPrefs.getString("${prefix}fab_message_2", "") ?: "") 
    }
    var tempFab1Icon by remember(prefix) { 
        mutableStateOf(sharedPrefs.getString("${prefix}fab_icon_1", "") ?: "") 
    }
    var tempFab2Icon by remember(prefix) { 
        mutableStateOf(sharedPrefs.getString("${prefix}fab_icon_2", "") ?: "")
    }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showRelationDetailsDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf(TextFieldValue("")) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    val cinemaViewModel: com.lexnicholls.lovecounter.viewmodel.CinemaViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val members by loveViewModel.members
    
    var profilePicUri by remember(sharedId) { 
        mutableStateOf<Any?>(
            ProfileImageManager.getLocalProfileFile(context, sharedId) ?: 
            sharedPrefs.getString("${prefix}profile_pic_uri", null)?.let { Uri.parse(it) } ?:
            currentUserProfile?.profilePicUrl
        )
    }

    LaunchedEffect(currentUserProfile?.profilePicUrl, sharedId) {
        if (profilePicUri == null || profilePicUri is String) {
            profilePicUri = ProfileImageManager.getLocalProfileFile(context, sharedId) ?: currentUserProfile?.profilePicUrl
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profilePicUri = it
            sharedPrefs.edit().putString("${prefix}profile_pic_uri", it.toString()).apply()
            ProfileImageManager.saveToInternalStorage(context, it, sharedId)
            scope.launch { ProfileImageManager.uploadToCloud(context, it, sharedId) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = strings.settings,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
        )

        // --- SECCIÓN: PERFIL HERO ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture Circle
                Box(
                    modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(3.dp, LovePink, CircleShape),
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
                                modifier = Modifier.padding(16.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                    
                    // Small Edit Icon Overlay
                    Surface(
                        modifier = Modifier.size(26.dp).align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = LovePink,
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tempName.ifBlank { strings.userName },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val userEmail = FirebaseAuth.getInstance().currentUser?.email
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- SECCIÓN: PAREJA Y ENLACE ---
        SettingsGroup(title = strings.relation) {
            val relationName = relationId?.let { availableRelations[it] } ?: strings.relation
            SettingsInputRow(
                label = strings.relationName,
                value = relationName,
                onValueChange = { /* Solo vista previa */ },
                onSave = { 
                    relationId?.let { 
                        renameValue = availableRelations[it] ?: ""
                        showRenameRelationDialog = true 
                    }
                },
                icon = Icons.AutoMirrored.Filled.Label,
                placeholder = strings.relation
            )
            
            SettingsClickableRow(
                label = strings.changeActiveProfile,
                value = relationName,
                icon = Icons.Default.SwitchAccount,
                onClick = { showSwitchProfileDialog = true }
            )

            SettingsClickableRow(
                label = strings.relationStatus,
                value = if (relationId != null && members.size > 1) strings.linkedStatus.format(members.size) else "Personal",
                icon = Icons.Default.Groups,
                onClick = { showRelationDetailsDialog = true }
            )

            if (relationId != null) {
                Button(
                    onClick = { loveViewModel.unlinkPartner() },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.error)
                ) {
                    val isAlone = members.size <= 1
                    Text(if (isAlone) strings.deleteProfile else strings.unlinkPartner)
                }
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

            // FAB Actions
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = tempFab1Icon,
                        onValueChange = { if (it.length <= 4) tempFab1Icon = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = strings.quickAction1, fontSize = 14.sp, color = Color.Gray)
                    BasicTextField(
                        value = tempFab1,
                        onValueChange = { tempFab1 = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(onClick = { 
                    sharedPrefs.edit().apply {
                        putString("${prefix}fab_message_1", tempFab1)
                        putString("${prefix}fab_icon_1", tempFab1Icon)
                        apply()
                    }
                    onFabMessagesChange(tempFab1, tempFab2, tempFab1Icon, tempFab2Icon)
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = tempFab2Icon,
                        onValueChange = { if (it.length <= 4) tempFab2Icon = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = strings.quickAction2, fontSize = 14.sp, color = Color.Gray)
                    BasicTextField(
                        value = tempFab2,
                        onValueChange = { tempFab2 = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(onClick = { 
                    sharedPrefs.edit().apply {
                        putString("${prefix}fab_message_2", tempFab2)
                        putString("${prefix}fab_icon_2", tempFab2Icon)
                        apply()
                    }
                    onFabMessagesChange(tempFab1, tempFab2, tempFab1Icon, tempFab2Icon)
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Relationship Start Date
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = key(sharedId) {
                rememberDatePickerState(
                    initialSelectedDateMillis = currentRelationshipDate,
                    yearRange = 1900..java.time.LocalDate.now().year,
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            return utcTimeMillis <= System.currentTimeMillis()
                        }
                    }
                )
            }

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
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.pointerInput(Unit) {}
                    )
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

            SettingsClickableRow(
                label = strings.backgroundColors,
                value = strings.customizeBackground,
                icon = Icons.Default.Palette,
                onClick = { showColorPickerDialog = true }
            )
        }

        // --- SECCIÓN: BIENESTAR ---
        SettingsGroup(title = strings.wellness) {
            SettingsSwitchRow(
                label = strings.trackPeriodQuestion,
                subtitle = strings.wellnessDesc,
                checked = currentUserProfile?.trackWellness ?: false,
                onCheckedChange = { 
                    loveViewModel.updateWellnessPreferences(it, currentUserProfile?.shareWellness ?: false) 
                },
                icon = Icons.Default.Favorite
            )
            
            if (currentUserProfile?.trackWellness == true) {
                SettingsSwitchRow(
                    label = strings.shareWellnessQuestion,
                    subtitle = if (currentUserProfile?.shareWellness == true) strings.yes else strings.cancel,
                    checked = currentUserProfile?.shareWellness ?: false,
                    onCheckedChange = { 
                        loveViewModel.updateWellnessPreferences(currentUserProfile?.trackWellness ?: true, it) 
                    },
                    icon = Icons.Default.Share
                )
            }
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

            if (isAutoRotateEnabled && currentWidgetConfigs.size >= 2) {
                SettingsDropdownRow(
                    label = strings.autoRotateInterval,
                    currentValue = strings.seconds.format(currentAutoRotateInterval),
                    icon = Icons.Default.Timer
                ) { onDismiss ->
                    listOf(15, 30, 60).forEach { seconds ->
                        DropdownMenuItem(
                            text = { Text(strings.seconds.format(seconds)) },
                            onClick = {
                                onAutoRotateIntervalChange(seconds)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }

        // --- SECCIÓN: SISTEMA ---
        var showChangePasswordDialog by remember { mutableStateOf(false) }
        var showDeleteAccountDialog by remember { mutableStateOf(false) }

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
            
            SettingsClickableRow(
                label = strings.changePassword,
                value = "********",
                icon = Icons.Default.Lock,
                onClick = { showChangePasswordDialog = true }
            )
            
            SettingsClickableRow(
                label = strings.deleteAccount,
                value = strings.delete,
                icon = Icons.Default.DeleteForever,
                onClick = { showDeleteAccountDialog = true }
            )
        }

        // --- SECCIÓN: CUENTA Y ADMIN ---
        val privateUserIds = remember { setOf("CX4z9DcQYxTJeaIdyNgzpDQqw6U2", "pW562p0UqNfEicrVd0q3oRRE9373") }
        val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

        if (currentUserId in privateUserIds) {
            SettingsGroup(title = strings.admin) {
                SettingsClickableRow(
                    label = strings.advancedConfig,
                    value = strings.details,
                    icon = Icons.Default.SettingsSuggest,
                    onClick = onNavigateToAdvanced
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // BOTÓN CERRAR SESIÓN
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(strings.logout, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- DIÁLOGOS ---
        if (showChangePasswordDialog) {
            var newPass by remember { mutableStateOf("") }
            var confirmPass by remember { mutableStateOf("") }
            
            com.lexnicholls.lovecounter.ui.components.LoveAlertDialog(
                onDismissRequest = { showChangePasswordDialog = false },
                title = strings.changePassword,
                onConfirm = {
                    if (newPass == confirmPass && newPass.length >= 6) {
                        loveViewModel.changePassword(newPass) {
                            showChangePasswordDialog = false
                        }
                    } else if (newPass != confirmPass) {
                        Toast.makeText(context, strings.passwordsDontMatch, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, strings.invalidEmailOrPassword, Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Column {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text(strings.newPassword) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                            autoCorrect = false
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text(strings.confirmPassword) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                            autoCorrect = false
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        if (showDeleteAccountDialog) {
            var confirmText by remember { mutableStateOf("") }
            val deleteKeyword = strings.delete
            
            com.lexnicholls.lovecounter.ui.components.LoveAlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = strings.deleteAccount,
                confirmButtonText = strings.delete,
                onConfirm = {
                    if (confirmText.equals(deleteKeyword, ignoreCase = true)) {
                        loveViewModel.deleteAccount {
                            onLogout()
                        }
                        showDeleteAccountDialog = false
                    }
                },
                enabledConfirm = confirmText.equals(deleteKeyword, ignoreCase = true)
            ) {
                Column {
                    Text(strings.deleteAccountDesc)
                    Spacer(Modifier.height(16.dp))
                    Text(strings.typeDeleteToConfirm.replace("Delete", deleteKeyword), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        placeholder = { Text(deleteKeyword) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (showSwitchProfileDialog) {
            AlertDialog(
                onDismissRequest = { showSwitchProfileDialog = false },
                title = { Text(strings.selectProfile) },
                text = {
                    Column {
                        availableRelations.forEach { (id, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        loveViewModel.switchProfile(id)
                                        showSwitchProfileDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = id == relationId, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(name)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(
                                onClick = { 
                                    showSwitchProfileDialog = false
                                    showCreateProfileDialog = true 
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.createProfile, fontSize = 12.sp)
                            }
                            if (relationId == null) {
                                TextButton(
                                    onClick = { 
                                        showSwitchProfileDialog = false
                                        showLinkDialog = true 
                                    }
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.joinWithCode, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSwitchProfileDialog = false }) {
                        Text(strings.close)
                    }
                }
            )
        }

        if (showCreateProfileDialog) {
            var linkingCodeInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateProfileDialog = false },
                title = { Text(strings.createProfile) },
                text = {
                    Column {
                        Text(strings.profileName, fontSize = 14.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            placeholder = { Text("Ej: Familia, Amigos...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(strings.haveCode + " (" + strings.optional + ")", fontSize = 14.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = linkingCodeInput,
                            onValueChange = { 
                                val digits = it.filter { c -> c.isDigit() }
                                linkingCodeInput = if (digits.length > 16) digits.take(16) else digits 
                            },
                            placeholder = { Text("1234-5678-...") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newProfileName.isNotBlank() || linkingCodeInput.length == 16) {
                            loveViewModel.createProfile(newProfileName, linkingCodeInput.ifBlank { null })
                            newProfileName = ""
                            linkingCodeInput = ""
                            showCreateProfileDialog = false
                        }
                    }) {
                        Text(strings.confirm)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showCreateProfileDialog = false
                        newProfileName = ""
                        linkingCodeInput = ""
                    }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        if (showRenameRelationDialog) {
            AlertDialog(
                onDismissRequest = { showRenameRelationDialog = false },
                title = { Text(strings.renameRelation) },
                text = {
                    Column {
                        Text(strings.relationName, fontSize = 14.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = renameValue,
                            onValueChange = { renameValue = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (renameValue.isNotBlank() && relationId != null) {
                            loveViewModel.renameRelation(relationId!!, renameValue)
                            showRenameRelationDialog = false
                        }
                    }) {
                        Text(strings.save)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameRelationDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        if (showRelationDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showRelationDetailsDialog = false },
                title = { Text(strings.relationDetails) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val isLinked = relationId != null && members.size > 1
                        
                        if (isLinked) {
                            Text(strings.members, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            members.forEach { member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isMe = member.uid == FirebaseAuth.getInstance().currentUser?.uid
                                    val picModel = if (isMe) {
                                        ProfileImageManager.getBestProfileUri(context, member.profilePicUrl, sharedId)
                                    } else {
                                        member.profilePicUrl
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, LovePink, CircleShape),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        if (picModel != null) {
                                            AsyncImage(
                                                model = picModel,
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
                                    
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = if (member.name.isNotBlank()) member.name else strings.unnamedUser,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            com.lexnicholls.lovecounter.ui.components.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            Text("Perfil Personal", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Text("Este perfil no está vinculado a una relación todavía. Puedes generar un código para que alguien se una, o ingresar el código de otra persona.", fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                        }
                        
                        Text(strings.linkingCodeLabel, fontWeight = FontWeight.Bold)
                        val codeToShow = linkingCode ?: strings.noCodeGenerated
                        val formatted = if (linkingCode != null) {
                            codeToShow.chunked(4).joinToString("-")
                        } else {
                            codeToShow
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (linkingCode != null) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Código de Enlace", linkingCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, strings.codeCopied, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        loveViewModel.generateLinkingCode()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerar", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Text(strings.shareCodeDesc, fontSize = 12.sp, color = Color.Gray)

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { 
                                showRelationDetailsDialog = false
                                showLinkDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.joinWithCode)
                        }
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
                        val categories = mutableListOf(
                            "reminders" to strings.reminders,
                            "dates" to strings.dates,
                            "market" to strings.market,
                            "bucket" to strings.bucket,
                            "drawing" to strings.drawing,
                            "movies" to strings.movies,
                            "daily" to strings.daily
                        )
                        
                        val someoneTracksAndShares = members.any { 
                            it.trackWellness && (it.uid == currentUserId || it.shareWellness)
                        }
                        if (someoneTracksAndShares) {
                            categories.add("wellness" to strings.wellness)
                        }

                        categories.forEach { (id, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val newSet = if (currentVisibleCategories.contains(id)) {
                                        currentVisibleCategories - id
                                    } else {
                                        currentVisibleCategories + id
                                    }
                                    onVisibleCategoriesChange(newSet)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = currentVisibleCategories.contains(id),
                                    onCheckedChange = { isChecked ->
                                        val newSet = if (isChecked) {
                                            currentVisibleCategories + id
                                        } else {
                                            currentVisibleCategories - id
                                        }
                                        onVisibleCategoriesChange(newSet)
                                    }
                                )
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
                confirmButton = { 
                    TextButton(onClick = { 
                        showWidgetDialog = false 
                        Toast.makeText(context, strings.widgetUpdateWarning, Toast.LENGTH_LONG).show()
                    }) { Text(strings.confirm) } 
                }
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

        if (showColorPickerDialog) {
            val isDark = when (currentTheme) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val def1 = if (isDark) Color(0xFF0F172A) else Color(0xFFEBE3FF)
            val def2 = if (isDark) Color(0xFF4C0519) else Color(0xFFFFD9E2)

            val c1 = try { 
                if (currentBgColor1 != null) Color(android.graphics.Color.parseColor(currentBgColor1)) 
                else def1 
            } catch (e: Exception) { def1 }
            val c2 = try { 
                if (currentBgColor2 != null) Color(android.graphics.Color.parseColor(currentBgColor2)) 
                else def2 
            } catch (e: Exception) { def2 }

            com.lexnicholls.lovecounter.ui.components.ColorPickerDialog(
                initialColor1 = c1,
                initialColor2 = c2,
                defaultColor1 = def1,
                defaultColor2 = def2,
                isUsingDefault = currentBgColor1 == null,
                onColorsSelected = { color1, color2 ->
                    if (color1 == null || color2 == null) {
                        onBackgroundColorsChange(null, null)
                    } else {
                        onBackgroundColorsChange(
                            String.format("#%08X", color1.toArgb()),
                            String.format("#%08X", color2.toArgb())
                        )
                    }
                    showColorPickerDialog = false
                },
                onDismiss = { showColorPickerDialog = false }
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
