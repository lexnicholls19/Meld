package com.lexnicholls.lovecounter.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lexnicholls.lovecounter.ui.components.AppBackground
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.ProfileImageManager
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onContinue: (name: String, title: String, categories: Set<String>, date: Long?, profileUri: Uri?, currency: String, fab1: String, fab2: String) -> Unit,
    viewModel: LoveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = t()
    
    var userName by remember { mutableStateOf("") }
    var mainTitle by remember { mutableStateOf("") }
    var relationshipDate by remember { mutableStateOf<Long?>(null) }
    var profilePicUri by remember { mutableStateOf<Uri?>(null) }
    var localCurrency by remember { mutableStateOf("COP") }
    var fab1 by remember { mutableStateOf("${strings.missYou} 💛") }
    var fab2 by remember { mutableStateOf("${strings.loveYou} ✨") }
    
    val allCategories = listOf(
        "reminders" to strings.reminders,
        "dates" to strings.dates,
        "market" to strings.market,
        "bucket" to strings.bucket,
        "drawing" to strings.drawing,
        "movies" to strings.movies,
        "daily" to strings.daily
    )
    var visibleCategories by remember { mutableStateOf(allCategories.map { it.first }.toSet()) }
    
    val linkingCode by viewModel.linkingCode
    val isLinking by viewModel.isLinking
    val syncStatus by viewModel.syncStatus
    val scope = rememberCoroutineScope()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf(TextFieldValue("")) }

    BackHandler(enabled = true) {
        showSkipDialog = true
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profilePicUri = it }
    }

    val datePickerState = rememberDatePickerState()

    LaunchedEffect(syncStatus) {
        syncStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncStatus()
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.welcomeToApp,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = LovePink,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // Profile Picture
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, LovePink, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profilePicUri != null) {
                    AsyncImage(
                        model = profilePicUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                }
            }
            Text(
                text = strings.changeProfilePic,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Configuration Group
            SettingsGroupWelcome(title = strings.customization) {
                LoveTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = strings.userName,
                    placeholder = "Your name"
                )
                Spacer(Modifier.height(12.dp))
                LoveTextField(
                    value = mainTitle,
                    onValueChange = { mainTitle = it },
                    label = strings.mainTitle,
                    placeholder = strings.mainTitleTooltip
                )
                Spacer(Modifier.height(12.dp))
                
                LoveTextField(
                    value = fab1,
                    onValueChange = { fab1 = it },
                    label = strings.quickAction1,
                    placeholder = "${strings.missYou} 💛"
                )
                Spacer(Modifier.height(12.dp))
                LoveTextField(
                    value = fab2,
                    onValueChange = { fab2 = it },
                    label = strings.quickAction2,
                    placeholder = "${strings.loveYou} ✨"
                )
                Spacer(Modifier.height(12.dp))

                var expandedCurrency by remember { mutableStateOf(false) }
                Box {
                    OutlinedCard(
                        onClick = { expandedCurrency = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Payments, null, tint = LovePink)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(strings.localCurrency, fontSize = 12.sp, color = Color.Gray)
                                Text(localCurrency, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedCurrency,
                        onDismissRequest = { expandedCurrency = false }
                    ) {
                        listOf("COP", "USD", "EUR").forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    localCurrency = currency
                                    expandedCurrency = false
                                }
                            )
                        }
                    }
                }
            }

            // Relationship Group
            SettingsGroupWelcome(title = strings.relation) {
                val dateDisplay = relationshipDate?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: strings.notSelected

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = LovePink)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(strings.relationshipDate, fontSize = 12.sp, color = Color.Gray)
                            Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.generateLinkingCode() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LovePink)
                    ) {
                        Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (linkingCode != null) "Code: ${linkingCode?.chunked(4)?.joinToString("-")}" else strings.generateCode,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { showLinkDialog = true },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, LovePink)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp), tint = LovePink)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.haveCode, fontSize = 10.sp, color = LovePink, maxLines = 1)
                    }
                }
            }

            // Categories Group
            SettingsGroupWelcome(title = strings.visibleCategoriesLabel) {
                allCategories.forEach { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                visibleCategories = if (visibleCategories.contains(id)) {
                                    visibleCategories - id
                                } else {
                                    visibleCategories + id
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = visibleCategories.contains(id),
                            onCheckedChange = {
                                visibleCategories = if (it) visibleCategories + id else visibleCategories - id
                            },
                            colors = CheckboxDefaults.colors(checkedColor = LovePink)
                        )
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    profilePicUri?.let { uri ->
                        ProfileImageManager.saveToInternalStorage(context, uri)
                        scope.launch { ProfileImageManager.uploadToCloud(context, uri) }
                    }
                    onContinue(userName, mainTitle, visibleCategories, relationshipDate, profilePicUri, localCurrency, fab1, fab2)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LovePink)
            ) {
                Text(strings.start, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        relationshipDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }) { Text(strings.confirm) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.pointerInput(Unit) {}
                )
            }
        }

        // Link Dialog
        if (showLinkDialog) {
            LoveAlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = strings.linkWithPartner,
                onConfirm = {
                    val rawCode = inputCode.text.filter { it.isDigit() }
                    if (rawCode.length == 16) {
                        viewModel.linkWithPartner(rawCode)
                        showLinkDialog = false
                    } else {
                        Toast.makeText(context, strings.codeMustBe16Digits, Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Column {
                    Text(strings.linkDialogDesc, fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    LoveTextField(
                        value = inputCode,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.text.filter { it.isDigit() }
                            val limitedDigits = if (digitsOnly.length > 16) digitsOnly.take(16) else digitsOnly
                            val formatted = limitedDigits.chunked(4).joinToString("-")
                            inputCode = if (formatted != inputCode.text) {
                                TextFieldValue(text = formatted, selection = androidx.compose.ui.text.TextRange(formatted.length))
                            } else {
                                newValue
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

        // Skip Config Dialog
        if (showSkipDialog) {
            LoveAlertDialog(
                onDismissRequest = { showSkipDialog = false },
                title = strings.skipInitialConfig,
                confirmButtonText = strings.yes,
                dismissButtonText = strings.cancel,
                onConfirm = {
                    profilePicUri?.let { uri ->
                        ProfileImageManager.saveToInternalStorage(context, uri)
                        scope.launch { ProfileImageManager.uploadToCloud(context, uri) }
                    }
                    onContinue(userName, mainTitle, visibleCategories, relationshipDate, profilePicUri, localCurrency, fab1, fab2)
                }
            ) {
                Text(strings.skipInitialConfigConfirm)
            }
        }
    }
}

@Composable
fun SettingsGroupWelcome(title: String, content: @Composable ColumnScope.() -> Unit) {
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
