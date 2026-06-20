package com.lexnicholls.lovecounter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import java.time.Duration
import java.time.LocalDateTime
import java.time.Period
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permissions", "Notification permission granted")
        } else {
            Log.d("Permissions", "Notification permission denied")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        FirebaseMessaging.getInstance().subscribeToTopic("recordatorios")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Suscrito al tema de recordatorios")
                }
            }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
            val deviceId = remember {
                val id = sharedPrefs.getString("device_id", null) ?: java.util.UUID.randomUUID().toString()
                sharedPrefs.edit().putString("device_id", id).commit()
                id
            }
            
            var currentScreen by rememberSaveable { mutableStateOf(Screen.Main) }
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
            var userName by rememberSaveable { mutableStateOf(sharedPrefs.getString("user_name", "") ?: "") }
            var widgetConfigs by rememberSaveable { 
                val saved = sharedPrefs.getString("widget_configs", "Timer") ?: "Timer"
                mutableStateOf(saved.split(",").filter { it.isNotBlank() }.toSet())
            }
            var autoRotateWidget by rememberSaveable {
                mutableStateOf(sharedPrefs.getBoolean("widget_auto_rotate", false))
            }
            var localCurrency by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("local_currency", "COP") ?: "COP")
            }
            var mainTitle by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("main_screen_title", null) ?: "")
            }
            var appLanguage by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("app_language", "system") ?: "system")
            }
            var visibleCategories by rememberSaveable {
                val saved = sharedPrefs.getString("visible_categories", "reminders,dates,market,bucket,daily,movies") ?: "reminders,dates,market,bucket,daily,movies"
                mutableStateOf(saved.split(",").filter { it.isNotBlank() }.toSet())
            }
            var categoryOrder by rememberSaveable {
                val saved = sharedPrefs.getString("category_order", "reminders,dates,market,bucket,daily,movies") ?: "reminders,dates,market,bucket,daily,movies"
                mutableStateOf(saved.split(",").filter { it.isNotBlank() })
            }

            val useDarkTheme = when (themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }

            // Confetti State
            var showConfetti by remember { mutableStateOf(false) }
            val heartDrawable = ContextCompat.getDrawable(context, R.drawable.ic_heart)
            val party = remember {
                Party(
                    speed = 0f,
                    maxSpeed = 30f,
                    damping = 0.9f,
                    spread = 360,
                    colors = listOf(0xFFFF4081.toInt(), 0xFFFFD700.toInt(), 0xFF7C4DFF.toInt()),
                    position = Position.Relative(0.5, 0.3),
                    shapes = if (heartDrawable != null) listOf(Shape.DrawableShape(heartDrawable)) else listOf(Shape.Circle),
                    emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
                )
            }

            LaunchedEffect(showConfetti) {
                if (showConfetti) {
                    delay(5000)
                    showConfetti = false
                }
            }

            val scope = rememberCoroutineScope()

            MaterialTheme(
                colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                ProvideStrings(appLanguage) {
                    val strings = t()
                    var showAddDialog by rememberSaveable { mutableStateOf(false) }
                    var isCompletedViewOpen by rememberSaveable { mutableStateOf(false) }
                    var showExitDialog by rememberSaveable { mutableStateOf(false) }
                    var isGiftExpanded by rememberSaveable { mutableStateOf(false) }

                    // Reset completed view state when changing screens
                    LaunchedEffect(currentScreen) {
                        isCompletedViewOpen = false
                        isGiftExpanded = false
                    }

                    // Handle System Back Button
                    BackHandler(enabled = true) {
                        if (currentScreen != Screen.Main) {
                            currentScreen = Screen.Main
                        } else {
                            showExitDialog = true
                        }
                    }

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text(strings.exit) },
                            text = { Text(strings.exitConfirm) },
                            confirmButton = {
                                Button(onClick = { finish() }) {
                                    Text(strings.yes)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) {
                                    Text(strings.cancel)
                                }
                            }
                        )
                    }

                    Scaffold(
                        floatingActionButton = {
                            if (currentScreen == Screen.Main) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    AnimatedVisibility(
                                        visible = isGiftExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shadowElevation = 2.dp
                                                ) {
                                                    Text(
                                                        text = "${t().missYou} 💛",
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                SmallFloatingActionButton(
                                                    onClick = { 
                                                        sendInterpretedNotification(context, strings.loveExtraLabel, "${strings.missYou} 💛", userName)
                                                        isGiftExpanded = false
                                                    },
                                                    containerColor = Color(0xFFFFB74D),
                                                    contentColor = Color.White
                                                ) {
                                                    Icon(Icons.Default.FavoriteBorder, contentDescription = strings.missYou)
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shadowElevation = 2.dp
                                                ) {
                                                    Text(
                                                        text = "${strings.loveYou} ✨",
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                SmallFloatingActionButton(
                                                    onClick = { 
                                                        sendInterpretedNotification(context, strings.loveExtraLabel, "${strings.loveYou} ✨", userName)
                                                        isGiftExpanded = false
                                                        showConfetti = true
                                                    },
                                                    containerColor = Color(0xFFFF4081),
                                                    contentColor = Color.White
                                                ) {
                                                    Icon(Icons.Default.Favorite, contentDescription = strings.loveYou)
                                                }
                                            }
                                        }
                                    }
                                    FloatingActionButton(
                                        onClick = { isGiftExpanded = !isGiftExpanded },
                                        containerColor = if (isGiftExpanded) Color.LightGray else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(
                                            if (isGiftExpanded) Icons.Default.Close else Icons.Default.Star,
                                            contentDescription = strings.loveExtraLabel
                                        )
                                    }
                                }
                            } else if (!isCompletedViewOpen && currentScreen != Screen.DailyConnection && currentScreen != Screen.Settings && currentScreen != Screen.Movies) {
                                FloatingActionButton(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = strings.add)
                                }
                            }
                        }
                    ) { paddingValues ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            if (isExpanded) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    header = {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFFF4081),
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                ) {
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.Main,
                                        onClick = { currentScreen = Screen.Main },
                                        icon = { Icon(Icons.Default.Home, contentDescription = t().start) },
                                        label = { Text(t().start) }
                                    )
                                    if (visibleCategories.contains("reminders")) {
                                        NavigationRailItem(
                                            selected = currentScreen == Screen.Second,
                                            onClick = { currentScreen = Screen.Second },
                                            icon = { Icon(Icons.Default.Notifications, contentDescription = t().reminders) },
                                            label = { Text(t().reminders) }
                                        )
                                    }
                                    if (visibleCategories.contains("dates")) {
                                        NavigationRailItem(
                                            selected = currentScreen == Screen.Third,
                                            onClick = { currentScreen = Screen.Third },
                                            icon = { Icon(Icons.Default.DateRange, contentDescription = t().dates) },
                                            label = { Text(t().dates) }
                                        )
                                    }
                                    if (visibleCategories.contains("market")) {
                                        NavigationRailItem(
                                            selected = currentScreen == Screen.Fourth,
                                            onClick = { currentScreen = Screen.Fourth },
                                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = t().market) },
                                            label = { Text(t().market) }
                                        )
                                    }
                                    if (visibleCategories.contains("bucket")) {
                                        NavigationRailItem(
                                            selected = currentScreen == Screen.BucketList,
                                            onClick = { currentScreen = Screen.BucketList },
                                            icon = { Icon(Icons.Default.Star, contentDescription = t().bucket) },
                                            label = { Text(t().bucket) }
                                        )
                                    }
                                    if (visibleCategories.contains("movies")) {
                                        NavigationRailItem(
                                            selected = currentScreen == Screen.Movies,
                                            onClick = { currentScreen = Screen.Movies },
                                            icon = { Icon(Icons.Default.Movie, contentDescription = t().movies) },
                                            label = { Text(t().movies) }
                                        )
                                    }
                                    if (visibleCategories.contains("daily")) {
                                        NavigationRailItem(
                                            selected = currentScreen == Screen.DailyConnection,
                                            onClick = { currentScreen = Screen.DailyConnection },
                                            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = t().daily) },
                                            label = { Text(t().daily) }
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.Settings,
                                        onClick = { currentScreen = Screen.Settings },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = t().settings) },
                                        label = { Text(t().settings) }
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f)
                            ) {
                                AppBackground {
                                    when (currentScreen) {
                                        Screen.Main -> LoveScreen(
                                            title = if (mainTitle.isNotBlank()) mainTitle else t().story,
                                            visibleCategories = visibleCategories,
                                            categoryOrder = categoryOrder,
                                            onOrderChange = { newOrder ->
                                                categoryOrder = newOrder
                                                sharedPrefs.edit().putString("category_order", newOrder.joinToString(",")).apply()
                                            },
                                            deviceId = deviceId,
                                            userName = userName,
                                            isExpanded = isExpanded,
                                            onNavigateToSecond = { currentScreen = Screen.Second },
                                            onNavigateToThird = { currentScreen = Screen.Third },
                                            onNavigateToFourth = { currentScreen = Screen.Fourth },
                                            onNavigateToSettings = { currentScreen = Screen.Settings },
                                            onNavigateToBucketList = { currentScreen = Screen.BucketList },
                                            onNavigateToMovies = { currentScreen = Screen.Movies },
                                            onNavigateToDaily = { currentScreen = Screen.DailyConnection },
                                            onTriggerConfetti = { showConfetti = true }
                                        )
                                        Screen.Second -> RemindersScreen(
                                            deviceId = deviceId,
                                            userName = userName,
                                            showAddDialog = showAddDialog,
                                            onDismissDialog = { showAddDialog = false },
                                            onCompletedViewToggled = { isCompletedViewOpen = it }
                                        )
                                        Screen.Third -> ImportantDatesScreen(
                                            deviceId = deviceId,
                                            userName = userName,
                                            showAddDialog = showAddDialog,
                                            onDismissDialog = { showAddDialog = false },
                                            onCompletedViewToggled = { isCompletedViewOpen = it }
                                        )
                                        Screen.Fourth -> ShoppingListScreen(
                                            deviceId = deviceId,
                                            userName = userName,
                                            showAddDialog = showAddDialog,
                                            onDismissDialog = { showAddDialog = false }
                                        )
                                        Screen.BucketList -> BucketListScreen(
                                            deviceId = deviceId,
                                            userName = userName,
                                            showAddDialog = showAddDialog,
                                            onDismissDialog = { showAddDialog = false },
                                            onCompletedViewToggled = { isCompletedViewOpen = it }
                                        )
                                        Screen.Movies -> MoviesListScreen(
                                            deviceId = deviceId,
                                            userName = userName,
                                            showAddDialog = showAddDialog,
                                            onDismissDialog = { showAddDialog = false },
                                            onAddClick = { showAddDialog = true }
                                        )
                                        Screen.DailyConnection -> DailyConnectionScreen(
                                            deviceId = deviceId,
                                            userName = userName
                                        )
                                        Screen.Settings -> SettingsScreen(
                                            currentTheme = themeMode,
                                            currentName = userName,
                                            currentDeviceId = deviceId,
                                            currentWidgetConfigs = widgetConfigs,
                                            isAutoRotateEnabled = autoRotateWidget,
                                            currentCurrency = localCurrency,
                                            currentMainTitle = mainTitle,
                                            currentLanguage = appLanguage,
                                            currentVisibleCategories = visibleCategories,
                                            onThemeChange = { themeMode = it },
                                            onNameChange = { newName ->
                                                val trimmedName = newName.trim()
                                                userName = trimmedName
                                                sharedPrefs.edit().putString("user_name", trimmedName).commit()
                                            },
                                            onMainTitleChange = { newTitle ->
                                                mainTitle = newTitle
                                                sharedPrefs.edit().putString("main_screen_title", newTitle).commit()
                                            },
                                            onLanguageChange = { lang ->
                                                appLanguage = lang
                                                sharedPrefs.edit().putString("app_language", lang).commit()
                                            },
                                            onVisibleCategoriesChange = { categories ->
                                                visibleCategories = categories
                                                sharedPrefs.edit().putString("visible_categories", categories.joinToString(",")).commit()
                                            },
                                            onWidgetConfigsChange = { configs ->
                                                widgetConfigs = configs
                                                sharedPrefs.edit().putString("widget_configs", configs.joinToString(",")).commit()
                                                scope.launch {
                                                    delay(200)
                                                    com.lexnicholls.lovecounter.widget.LoveWidget().updateAll(context.applicationContext)
                                                }
                                            },
                                            onAutoRotateChange = { enabled ->
                                                autoRotateWidget = enabled
                                                sharedPrefs.edit().putBoolean("widget_auto_rotate", enabled).commit()
                                                scope.launch {
                                                    delay(200)
                                                    com.lexnicholls.lovecounter.widget.LoveWidget().updateAll(context.applicationContext)
                                                }
                                            },
                                            onCurrencyChange = { currency ->
                                                localCurrency = currency
                                                sharedPrefs.edit().putString("local_currency", currency).commit()
                                            },
                                            onResetDeviceId = {
                                                val newId = java.util.UUID.randomUUID().toString()
                                                sharedPrefs.edit().putString("device_id", newId).apply()
                                                Toast.makeText(context, "ID reiniciado. Por favor, reinicia la app.", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (showConfetti) {
                        KonfettiView(
                            modifier = Modifier.fillMaxSize(),
                            parties = listOf(party)
                        )
                    }
                }
            }
        }
    }
}

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
    onResetDeviceId: () -> Unit
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
        val currentLangLabel = AppLanguage.values().find { it.code == currentLanguage }?.label ?: t().system

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

enum class Screen {
    Main, Second, Third, Fourth, Settings, BucketList, DailyConnection, Movies
}

enum class ThemeMode {
    Light, Dark, System
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveScreen(
    title: String,
    visibleCategories: Set<String>,
    categoryOrder: List<String>,
    onOrderChange: (List<String>) -> Unit,
    deviceId: String,
    userName: String,
    isExpanded: Boolean = false,
    onNavigateToSecond: () -> Unit,
    onNavigateToThird: () -> Unit,
    onNavigateToFourth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBucketList: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToDaily: () -> Unit,
    onTriggerConfetti: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val startDate = remember { LocalDateTime.of(2021, 1, 4, 0, 0) }
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    
    var partnerStatus by rememberSaveable { mutableStateOf("❓") }
    var myStatus by rememberSaveable { mutableStateOf("😊") }
    var showStatusDialog by rememberSaveable { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Listen for partner and my status based on NAMES instead of Device IDs
    DisposableEffect(userName) {
        if (userName.isBlank()) return@DisposableEffect onDispose {}
        
        Log.d("StatusSync", "Iniciando listener para usuario: $userName")
        val registration = db.collection("partner_status")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StatusSync", "Error en listener", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var foundMyStatus = false
                    var foundPartnerStatus = false
                    
                    snapshot.documents.forEach { doc ->
                        val status = doc.getString("emoji") ?: "❓"
                        val docId = doc.id
                        
                        if (docId == userName) {
                            myStatus = status
                            foundMyStatus = true
                        } else if (!docId.contains("-") && docId.length < 25) {
                            partnerStatus = status
                            foundPartnerStatus = true
                        }
                    }
                }
            }
        onDispose { registration.remove() }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            isRefreshing = true
            // Re-fetch logic or just a delay to show refresh worked
            db.collection("partner_status").get().addOnCompleteListener {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val period = remember(currentTime) { Period.between(startDate.toLocalDate(), currentTime.toLocalDate()) }
    val years by remember(period) { derivedStateOf { period.years } }
    val months by remember(period) { derivedStateOf { period.months } }
    val periodDays by remember(period) { derivedStateOf { period.days } }
    
    val duration = remember(currentTime) { Duration.between(startDate, currentTime) }
    val totalDays by remember(duration) { derivedStateOf { duration.toDays() } }
    val hours by remember(duration) { derivedStateOf { duration.toHours() % 24 } }
    val minutes by remember(duration) { derivedStateOf { duration.toMinutes() % 60 } }
    val seconds by remember(duration) { derivedStateOf { duration.seconds % 60 } }

    // Check for Milestones (e.g., every 100 days or round numbers)
    LaunchedEffect(totalDays) {
        if (totalDays > 0 && (totalDays % 100 == 0L || totalDays % 365 == 0L)) {
            onTriggerConfetti()
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text(t().howDoYouFeel) },
            text = {
                val emojis = listOf("😊", "🥰", "😍", "😴", "🤔", "😢", "😤", "🤢", "🤒")
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojis.take(5).forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 32.sp,
                                modifier = Modifier.clickable {
                                    val data = hashMapOf(
                                        "emoji" to emoji,
                                        "timestamp" to Timestamp.now(),
                                        "senderName" to userName,
                                        "userName" to userName,
                                        "senderId" to deviceId,
                                        "deviceId" to deviceId
                                    )
                                    db.collection("partner_status").document(userName).set(data)
                                    // También enviamos una notificación para que la pareja se entere del cambio de estado
                                    sendInterpretedNotification(context, "Estado", "Nuevo estado de $userName: $emoji", userName)
                                    showStatusDialog = false
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojis.drop(5).forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 32.sp,
                                modifier = Modifier.clickable {
                                    val data = hashMapOf(
                                        "emoji" to emoji,
                                        "timestamp" to Timestamp.now(),
                                        "senderName" to userName,
                                        "userName" to userName,
                                        "senderId" to deviceId,
                                        "deviceId" to deviceId
                                    )
                                    db.collection("partner_status").document(userName).set(data)
                                    // También enviamos una notificación para que la pareja se entere del cambio de estado
                                    sendInterpretedNotification(context, "Estado", "Nuevo estado de $userName: $emoji", userName)
                                    showStatusDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text(t().close) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = t().settings, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF4081)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Hero Card
            val strings = t()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val dateFormatter = remember(strings) {
                            java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", 
                                when(strings.settings) {
                                    "Settings" -> java.util.Locale.ENGLISH
                                    "Paramètres" -> java.util.Locale.FRENCH
                                    "Einstellungen" -> java.util.Locale.GERMAN
                                    "Configurações" -> java.util.Locale.forLanguageTag("pt")
                                    else -> java.util.Locale.forLanguageTag("es")
                                }
                            )
                        }
                        Text(
                            text = "${strings.since} ${startDate.format(dateFormatter)}",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "$years${strings.years} - $months${strings.months} - $periodDays${strings.days}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$hours ${strings.hoursShort} $minutes ${strings.minutesShort} $seconds ${strings.secondsShort}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF4081)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = strings.partner, fontSize = 12.sp, color = Color.Gray)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Gray.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = partnerStatus, 
                                fontSize = 32.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(text = strings.me, fontSize = 12.sp, color = Color.Gray)
                        Surface(
                            modifier = Modifier.clickable { showStatusDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = myStatus, 
                                fontSize = 32.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            val allTiles = remember(strings) {
                listOf(
                    TileData("reminders", strings.reminders, strings.remindersDesc, Icons.Default.Notifications, Color(0xFF7C4DFF), onNavigateToSecond),
                    TileData("dates", strings.dates, strings.datesDesc, Icons.Default.DateRange, Color(0xFFFF4081), onNavigateToThird),
                    TileData("market", strings.market, strings.marketDesc, Icons.Default.ShoppingCart, Color(0xFF4CAF50), onNavigateToFourth),
                    TileData("bucket", strings.bucket, strings.bucketDesc, Icons.Default.Star, Color(0xFFFF9800), onNavigateToBucketList),
                    TileData("movies", strings.movies, strings.moviesDesc, Icons.Default.Movie, Color(0xFFE91E63), onNavigateToMovies),
                    TileData("daily", strings.daily, strings.dailyDesc, Icons.Default.FavoriteBorder, Color(0xFF03A9F4), onNavigateToDaily)
                )
            }

            val visibleTiles = categoryOrder.mapNotNull { id ->
                if (visibleCategories.contains(id)) allTiles.find { it.id == id } else null
            }

            var isReorderMode by remember { mutableStateOf(false) }

            if (isReorderMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Reordenar categorías", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { isReorderMode = false }) {
                                Icon(Icons.Default.Check, contentDescription = "Listo", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        categoryOrder.forEachIndexed { index, id ->
                            val tile = allTiles.find { it.id == id }
                            if (tile != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(tile.icon, contentDescription = null, tint = tile.color, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(text = tile.title, modifier = Modifier.weight(1f))
                                    
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val newList = categoryOrder.toMutableList()
                                                val tmp = newList[index]
                                                newList[index] = newList[index-1]
                                                newList[index-1] = tmp
                                                onOrderChange(newList)
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            if (index < categoryOrder.size - 1) {
                                                val newList = categoryOrder.toMutableList()
                                                val tmp = newList[index]
                                                newList[index] = newList[index+1]
                                                newList[index+1] = tmp
                                                onOrderChange(newList)
                                            }
                                        },
                                        enabled = index < categoryOrder.size - 1
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Navigation Grid
            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val chunks = visibleTiles.chunked(2)
                    chunks.forEach { chunk ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            chunk.forEach { tile ->
                                Box(modifier = Modifier.weight(1f)) {
                                    DashboardTile(
                                        title = tile.title,
                                        subtitle = tile.subtitle,
                                        icon = tile.icon,
                                        color = tile.color,
                                        onClick = tile.onClick,
                                        onLongClick = { isReorderMode = true }
                                    )
                                }
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    visibleTiles.forEach { tile ->
                        DashboardTile(
                            title = tile.title,
                            subtitle = tile.subtitle,
                            icon = tile.icon,
                            color = tile.color,
                            onClick = tile.onClick,
                            onLongClick = { isReorderMode = true }
                        )
                    }
                }
            }
            
            // Espacio para evitar que el contenido quede bajo el botón flotante
            Spacer(modifier = Modifier.height(88.dp))
        }

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

data class TileData(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DashboardTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

private fun sendInterpretedNotification(context: Context, title: String, message: String, senderName: String) {
    val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val deviceId = sharedPrefs.getString("device_id", "") ?: ""
    val nameFromPrefs = sharedPrefs.getString("user_name", "") ?: ""
    
    Log.d("LoveFCM", "--- INICIO ENVÍO ---")
    Log.d("LoveFCM", "senderName recibido: '$senderName'")
    Log.d("LoveFCM", "nombre en prefs: '$nameFromPrefs'")
    Log.d("LoveFCM", "deviceId: '$deviceId'")
    
    if (senderName.isBlank() && nameFromPrefs.isBlank()) {
        Log.e("LoveFCM", "ERROR CRÍTICO: El nombre está vacío en todas partes!")
    }

    val finalSenderName = if (senderName.isNotBlank()) senderName else nameFromPrefs
    
    val db = FirebaseFirestore.getInstance()
    val notification = hashMapOf(
        "name" to title,
        "value" to message,
        "timestamp" to Timestamp.now(),
        "senderName" to finalSenderName,
        "userName" to finalSenderName,
        "senderId" to deviceId,
        "deviceId" to deviceId
    )
    // Use a hidden collection so it doesn't appear in the app lists
    db.collection("quick_messages").add(notification)
        .addOnSuccessListener {
            Toast.makeText(context, "Mensaje enviado ✨", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error al enviar", Toast.LENGTH_SHORT).show()
        }
}
