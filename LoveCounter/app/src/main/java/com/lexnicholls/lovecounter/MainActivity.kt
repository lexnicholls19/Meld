package com.lexnicholls.lovecounter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.app.NotificationCompat
import androidx.compose.animation.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.messaging.FirebaseMessaging
import androidx.glance.appwidget.updateAll
import com.lexnicholls.lovecounter.ui.navigation.Screen
import com.lexnicholls.lovecounter.ui.navigation.ThemeMode
import com.lexnicholls.lovecounter.ui.screens.*
import com.lexnicholls.lovecounter.ui.components.AppBackground
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.util.ProvideStrings
import com.lexnicholls.lovecounter.util.t
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import com.lexnicholls.lovecounter.ui.theme.*
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexnicholls.lovecounter.util.UpdateManager
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var updateManager: UpdateManager

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
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

    private fun showSystemNotification(context: Context, title: String, message: String) {
        val channelId = "quick_actions_channel"
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            "Acciones Rápidas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de acciones rápidas de tu pareja"
        }
        notificationManager?.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_heart)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        try {
            notificationManager?.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e("Notification", "Error showing notification: ${e.message}")
        }
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        askNotificationPermission()
        
        // Limpieza de tópico global antiguo para evitar spam
        FirebaseMessaging.getInstance().unsubscribeFromTopic("recordatorios")

        setContent {
            val loveViewModel: LoveViewModel = hiltViewModel()
            val sharedId by loveViewModel.sharedId
            val incomingMessage by loveViewModel.incomingMessage

            var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
            var changelogToShow by remember { mutableStateOf<String?>(null) }
            var showWhatsNew by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                // Check if an update was just installed
                changelogToShow = updateManager.getNewVersionChangelog()
                if (changelogToShow != null) {
                    updateManager.markUpdateAsSeen()
                }

                // Check for new updates
                updateManager.checkForUpdates { info ->
                    if (info.isUpdateAvailable) {
                        updateInfo = info
                    }
                }
            }

            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("prefs", MODE_PRIVATE) }
            val deviceId = remember {
                val id = sharedPrefs.getString("device_id", null) ?: java.util.UUID.randomUUID().toString()
                if (!sharedPrefs.contains("device_id")) {
                    sharedPrefs.edit { putString("device_id", id) }
                }
                id
            }
            
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            var themeMode by rememberSaveable { 
                val saved = sharedPrefs.getString("app_theme", ThemeMode.System.name) ?: ThemeMode.System.name
                mutableStateOf(ThemeMode.valueOf(saved))
            }

            val useDarkTheme = when (themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }

            var userName by rememberSaveable { mutableStateOf(sharedPrefs.getString("user_name", "") ?: "") }
            var bgColor1 by rememberSaveable { mutableStateOf(sharedPrefs.getString("bg_color_1", "#EBE3FF") ?: "#EBE3FF") }
            var bgColor2 by rememberSaveable { mutableStateOf(sharedPrefs.getString("bg_color_2", "#FFD9E2") ?: "#FFD9E2") }
            var widgetConfigs by rememberSaveable { 
                val saved = sharedPrefs.getString("widget_configs", "Timer") ?: "Timer"
                mutableStateOf(saved.split(",").filter { it.isNotBlank() }.toSet())
            }
            var autoRotateWidget by rememberSaveable {
                mutableStateOf(sharedPrefs.getBoolean("widget_auto_rotate", false))
            }
            var autoRotateInterval by rememberSaveable {
                mutableStateOf(sharedPrefs.getInt("widget_rotate_interval", 60))
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
            var fabMessage1 by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("fab_message_1", null))
            }
            var fabMessage2 by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("fab_message_2", null))
            }
            var fabIcon1 by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("fab_icon_1", "") ?: "")
            }
            var fabIcon2 by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("fab_icon_2", "") ?: "")
            }
            var relationshipDate by rememberSaveable {
                val saved = sharedPrefs.getLong("relationship_date", -1L)
                mutableStateOf(if (saved == -1L) null else saved)
            }
            var visibleCategories by rememberSaveable {
                val default = "reminders,dates,market,bucket,drawing,daily,movies"
                val saved = sharedPrefs.getString("visible_categories", default) ?: default
                mutableStateOf(saved.split(",").filter { it.isNotBlank() }.toSet())
            }
            var categoryOrder by rememberSaveable {
                val allPossible = listOf("reminders", "dates", "market", "bucket", "drawing", "daily", "movies")
                val saved = sharedPrefs.getString("category_order", null)
                val currentList = if (saved == null) {
                    allPossible
                } else {
                    val list = saved.split(",").filter { it.isNotBlank() }.toMutableList()
                    // Migración: Agregar nuevas categorías que no estén en la lista guardada
                    allPossible.forEach { cat ->
                        if (!list.contains(cat)) list.add(cat)
                    }
                    list
                }
                mutableStateOf(currentList)
            }

            LaunchedEffect(sharedId) {
                if (sharedId != null) {
                    val id = sharedId!!
                    val prefix = "rel_${id}_"
                    
                    fun getString(key: String, default: String?): String? {
                        val prefixedValue = sharedPrefs.getString(prefix + key, null)
                        if (prefixedValue != null) return prefixedValue
                        val oldValue = sharedPrefs.getString(key, null)
                        if (oldValue != null) {
                            sharedPrefs.edit { putString(prefix + key, oldValue) }
                            return oldValue
                        }
                        return default
                    }
                    
                    fun getBoolean(key: String, default: Boolean): Boolean {
                        if (sharedPrefs.contains(prefix + key)) return sharedPrefs.getBoolean(prefix + key, default)
                        if (sharedPrefs.contains(key)) {
                            val old = sharedPrefs.getBoolean(key, default)
                            sharedPrefs.edit { putBoolean(prefix + key, old) }
                            return old
                        }
                        return default
                    }

                    fun getInt(key: String, default: Int): Int {
                        if (sharedPrefs.contains(prefix + key)) return sharedPrefs.getInt(prefix + key, default)
                        if (sharedPrefs.contains(key)) {
                            val old = sharedPrefs.getInt(key, default)
                            sharedPrefs.edit { putInt(prefix + key, old) }
                            return old
                        }
                        return default
                    }

                    mainTitle = getString("main_screen_title", "") ?: ""
                    userName = getString("user_name", "") ?: ""
                    bgColor1 = getString("bg_color_1", if (useDarkTheme) "#0F172A" else "#EBE3FF") ?: "#EBE3FF"
                    bgColor2 = getString("bg_color_2", if (useDarkTheme) "#4C0519" else "#FFD9E2") ?: "#FFD9E2"
                    fabMessage1 = getString("fab_message_1", null)
                    fabMessage2 = getString("fab_message_2", null)
                    fabIcon1 = getString("fab_icon_1", "") ?: ""
                    fabIcon2 = getString("fab_icon_2", "") ?: ""
                    localCurrency = getString("local_currency", "COP") ?: "COP"
                    
                    widgetConfigs = (getString("widget_configs", "Timer") ?: "Timer")
                        .split(",").filter { it.isNotBlank() }.toSet()
                    autoRotateWidget = getBoolean("widget_auto_rotate", false)
                    autoRotateInterval = getInt("widget_rotate_interval", 60)
                    
                    val defaultCategories = "reminders,dates,market,bucket,drawing,daily,movies"
                    visibleCategories = (getString("visible_categories", defaultCategories) ?: defaultCategories)
                        .split(",").filter { it.isNotBlank() }.toSet()
                    
                    categoryOrder = (getString("category_order", null) ?: defaultCategories)
                        .split(",").filter { it.isNotBlank() }.toList()

                    val sDate = sharedPrefs.getLong(prefix + "relationship_date", -1L)
                    relationshipDate = if (sDate != -1L) sDate else {
                        val oldDate = sharedPrefs.getLong("relationship_date", -1L)
                        if (oldDate != -1L) {
                            sharedPrefs.edit { putLong(prefix + "relationship_date", oldDate) }
                            oldDate
                        } else null
                    }
                } else {
                    // Valores por defecto globales cuando no hay relación activa
                    mainTitle = sharedPrefs.getString("main_screen_title", "") ?: ""
                    userName = sharedPrefs.getString("user_name", "") ?: ""
                    fabMessage1 = sharedPrefs.getString("fab_message_1", null)
                    fabMessage2 = sharedPrefs.getString("fab_message_2", null)
                    fabIcon1 = sharedPrefs.getString("fab_icon_1", "") ?: ""
                    fabIcon2 = sharedPrefs.getString("fab_icon_2", "") ?: ""
                    localCurrency = sharedPrefs.getString("local_currency", "COP") ?: "COP"
                    val defaultCategories = "reminders,dates,market,bucket,drawing,daily,movies"
                    visibleCategories = (sharedPrefs.getString("visible_categories", defaultCategories) ?: defaultCategories)
                        .split(",").filter { it.isNotBlank() }.toSet()
                    categoryOrder = (sharedPrefs.getString("category_order", defaultCategories) ?: defaultCategories)
                        .split(",").filter { it.isNotBlank() }.toList()
                    val sDate = sharedPrefs.getLong("relationship_date", -1L)
                    relationshipDate = if (sDate == -1L) null else sDate
                }
            }

            // Confetti State
            var showConfetti by remember { mutableStateOf(false) }

            LaunchedEffect(incomingMessage) {
                incomingMessage?.let { (title, message) ->
                    Toast.makeText(context, "$title: $message", Toast.LENGTH_LONG).show()
                    showSystemNotification(context, title, message)
                    showConfetti = true
                    loveViewModel.clearIncomingMessage()
                }
            }

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

            MeldTheme(
                darkTheme = useDarkTheme,
                dynamicColor = false, // Set to true if you want Android 12+ dynamic colors
                bgColors = try { 
                    val c1 = android.graphics.Color.parseColor(bgColor1)
                    val c2 = android.graphics.Color.parseColor(bgColor2)
                    Color(c1) to Color(c2)
                } catch(_: Exception) { null }
            ) {
                ProvideStrings(appLanguage) {
                    val strings = t()
                    var showAddDialog by rememberSaveable { mutableStateOf(false) }
                    var showDeleteMoviesDialog by rememberSaveable { mutableStateOf(false) }
                    var isMovieSelectionMode by remember { mutableStateOf(false) }
                    var isCompletedViewOpen by rememberSaveable { mutableStateOf(false) }
                    var showExitDialog by rememberSaveable { mutableStateOf(false) }
                    var isGiftExpanded by rememberSaveable { mutableStateOf(false) }
                    var isMainReorderMode by rememberSaveable { mutableStateOf(false) }

                    // Reset completed view state when changing screens
                    LaunchedEffect(currentDestination?.route) {
                        isCompletedViewOpen = false
                        isGiftExpanded = false
                        isMainReorderMode = false
                        isMovieSelectionMode = false
                        showDeleteMoviesDialog = false
                    }

                    // Handle System Back Button
                    BackHandler(enabled = true) {
                        val currentRoute = currentDestination?.route
                        when (currentRoute) {
                            Screen.Login.name, Screen.Main.name -> showExitDialog = true
                            Screen.Register.name -> navController.popBackStack()
                            else -> navController.navigate(Screen.Main.name) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                    if (showExitDialog) {
                        LoveAlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = strings.exit,
                            confirmButtonText = strings.yes,
                            dismissButtonText = strings.cancel,
                            onConfirm = { finish() }
                        ) {
                            Text(strings.exitConfirm)
                        }
                    }

                    Scaffold(
                        floatingActionButton = {
                            val currentRoute = currentDestination?.route
                            val hasQuickActions = !fabMessage1.isNullOrBlank() || !fabMessage2.isNullOrBlank()
                            
                            AnimatedContent(
                                targetState = currentRoute,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                        .togetherWith(fadeOut(animationSpec = tween(90)))
                                },
                                label = "FABTransition"
                            ) { targetRoute ->
                                if ((targetRoute == Screen.Main.name) && !isMainReorderMode && hasQuickActions) {
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
                                                if (!fabMessage1.isNullOrBlank()) {
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
                                                                text = fabMessage1!!,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        SmallFloatingActionButton(
                                                            onClick = { 
                                                                sendInterpretedNotification(context, fabIcon1, fabMessage1!!, userName)
                                                                isGiftExpanded = false
                                                            },
                                                            containerColor = TertiaryColor,
                                                            contentColor = Color.White
                                                        ) {
                                                            Text(fabIcon1, fontSize = 18.sp)
                                                        }
                                                    }
                                                }

                                                if (!fabMessage2.isNullOrBlank()) {
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
                                                                text = fabMessage2!!,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        SmallFloatingActionButton(
                                                            onClick = { 
                                                                sendInterpretedNotification(context, fabIcon2, fabMessage2!!, userName)
                                                                isGiftExpanded = false
                                                                showConfetti = true
                                                            },
                                                            containerColor = LovePink,
                                                            contentColor = Color.White
                                                        ) {
                                                            Text(fabIcon2, fontSize = 18.sp)
                                                        }
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
                                } else if (isMovieSelectionMode && targetRoute == Screen.Movies.name) {
                                    FloatingActionButton(
                                        onClick = { showDeleteMoviesDialog = true },
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                    }
                                } else if (!isMainReorderMode && 
                                    !isCompletedViewOpen &&
                                    targetRoute != Screen.Main.name &&
                                    targetRoute != Screen.DailyConnection.name && 
                                    targetRoute != Screen.Settings.name && 
                                    targetRoute != Screen.Login.name && 
                                    targetRoute != Screen.Register.name && 
                                    targetRoute != Screen.Welcome.name && 
                                    targetRoute != Screen.Drawing.name && 
                                    targetRoute?.startsWith(Screen.MovieDetail.name) != true
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        if (targetRoute == Screen.Movies.name) {
                                            val cinemaViewModel: com.lexnicholls.lovecounter.viewmodel.CinemaViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
                                            var diceRotation by remember { mutableStateOf(0f) }
                                            val animatedRotation by animateFloatAsState(
                                                targetValue = diceRotation,
                                                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                                                label = "DiceRoll"
                                            )

                                            SmallFloatingActionButton(
                                                onClick = { 
                                                    diceRotation += 360f
                                                    cinemaViewModel.triggerRandom() 
                                                },
                                                modifier = Modifier.graphicsLayer(rotationZ = animatedRotation),
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Icon(Icons.Default.Casino, contentDescription = "Random")
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        FloatingActionButton(onClick = { showAddDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = strings.add)
                                        }
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        val currentRoute = currentDestination?.route
                        val authScreens = listOf(Screen.Login.name, Screen.Register.name, Screen.Welcome.name)
                        val shouldShowRail = isExpanded && currentRoute != null && currentRoute !in authScreens

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            if (shouldShowRail) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    header = {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = LovePink,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                ) {
                                    NavigationRailItem(
                                        selected = currentRoute == Screen.Main.name,
                                        onClick = { 
                                            navController.navigate(Screen.Main.name) {
                                                popUpTo(Screen.Main.name) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Home, contentDescription = t().start) },
                                        label = { Text(t().start) }
                                    )
                                    if (visibleCategories.contains("reminders")) {
                                        NavigationRailItem(
                                            selected = currentRoute == Screen.Second.name,
                                            onClick = { 
                                                navController.navigate(Screen.Second.name) {
                                                    popUpTo(Screen.Main.name) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Notifications, contentDescription = t().reminders) },
                                            label = { Text(t().reminders) }
                                        )
                                    }
                                    if (visibleCategories.contains("dates")) {
                                        NavigationRailItem(
                                            selected = currentRoute == Screen.Third.name,
                                            onClick = { 
                                                navController.navigate(Screen.Third.name) {
                                                    popUpTo(Screen.Main.name) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.DateRange, contentDescription = t().dates) },
                                            label = { Text(t().dates) }
                                        )
                                    }
                                    if (visibleCategories.contains("market")) {
                                        NavigationRailItem(
                                            selected = currentRoute == Screen.Fourth.name,
                                            onClick = { 
                                                navController.navigate(Screen.Fourth.name) {
                                                    popUpTo(Screen.Main.name) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = t().market) },
                                            label = { Text(t().market) }
                                        )
                                    }
                                    if (visibleCategories.contains("bucket")) {
                                        NavigationRailItem(
                                            selected = currentRoute == Screen.BucketList.name,
                                            onClick = { 
                                                navController.navigate(Screen.BucketList.name) {
                                                    popUpTo(Screen.Main.name) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Star, contentDescription = t().bucket) },
                                            label = { Text(t().bucket) }
                                        )
                                    }
                                    if (visibleCategories.contains("movies")) {
                                        NavigationRailItem(
                                            selected = currentRoute == Screen.Movies.name,
                                            onClick = { 
                                                navController.navigate(Screen.Movies.name) {
                                                    popUpTo(Screen.Main.name) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Movie, contentDescription = t().movies) },
                                            label = { Text(t().movies) }
                                        )
                                    }
                                    if (visibleCategories.contains("daily")) {
                                        NavigationRailItem(
                                            selected = currentRoute == Screen.DailyConnection.name,
                                            onClick = { 
                                                navController.navigate(Screen.DailyConnection.name) {
                                                    popUpTo(Screen.Main.name) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = t().daily) },
                                            label = { Text(t().daily) }
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    NavigationRailItem(
                                        selected = currentRoute == Screen.Settings.name,
                                        onClick = { 
                                            navController.navigate(Screen.Settings.name) {
                                                popUpTo(Screen.Main.name) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = t().settings) },
                                        label = { Text(t().settings) }
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f)
                            ) {
                                AppBackground {
                                    val currentUser = remember { FirebaseAuth.getInstance().currentUser }

                                    // Gestión dinámica de suscripciones a Tópicos (Todas las relaciones)
                                    val relationIds by loveViewModel.relationIds
                                    val subscribedTopics = remember { mutableStateListOf<String>() }

                                    LaunchedEffect(relationIds) {
                                        val currentTopics = relationIds.map { "relation_$it" }.toSet()
                                        
                                        // Suscribir a nuevos
                                        currentTopics.forEach { topic ->
                                            if (!subscribedTopics.contains(topic)) {
                                                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                                    .addOnSuccessListener { 
                                                        Log.d("FCM", "Subscribed to $topic")
                                                        subscribedTopics.add(topic) 
                                                    }
                                            }
                                        }

                                        // Desuscribir de los que ya no están
                                        val toUnsubscribe = subscribedTopics.filter { !currentTopics.contains(it) }
                                        toUnsubscribe.forEach { topic ->
                                            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                                                .addOnSuccessListener { 
                                                    Log.d("FCM", "Unsubscribed from $topic")
                                                    subscribedTopics.remove(topic) 
                                                }
                                        }
                                    }

                                    NavHost(
                                        navController = navController,
                                        startDestination = if (currentUser != null) Screen.Main.name else Screen.Login.name
                                    ) {
                                        composable(Screen.Login.name) {
                                            val hasAccountOnDevice = sharedPrefs.getBoolean("has_account_on_device", false)
                                            LoginScreen(
                                                hasAccountOnDevice = hasAccountOnDevice,
                                                onNavigateToRegister = { navController.navigate(Screen.Register.name) },
                                                onLoginSuccess = { 
                                                    sharedPrefs.edit { putBoolean("has_account_on_device", true) }
                                                    val isFirst = sharedPrefs.getBoolean("first_time_${FirebaseAuth.getInstance().currentUser?.uid}", true)
                                                    if (isFirst) {
                                                        navController.navigate(Screen.Welcome.name) {
                                                            popUpTo(Screen.Login.name) { inclusive = true }
                                                        }
                                                    } else {
                                                        navController.navigate(Screen.Main.name) {
                                                            popUpTo(Screen.Login.name) { inclusive = true }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        composable(Screen.Register.name) {
                                            RegisterScreen(
                                                onNavigateToLogin = { navController.popBackStack() },
                                                onRegisterSuccess = { 
                                                    sharedPrefs.edit { putBoolean("has_account_on_device", true) }
                                                    navController.navigate(Screen.Welcome.name) {
                                                        popUpTo(Screen.Login.name) { inclusive = true }
                                                    }
                                                }
                                            )
                                        }
                                        composable(Screen.Welcome.name) {
                                            WelcomeScreen(
                                                onContinue = { name, title, categories, date, profileUri, currency, f1, f2, f1i, f2i ->
                                                    userName = name
                                                    mainTitle = title
                                                    visibleCategories = categories
                                                    relationshipDate = date
                                                    localCurrency = currency
                                                    fabMessage1 = f1
                                                    fabMessage2 = f2
                                                    fabIcon1 = f1i
                                                    fabIcon2 = f2i
                                                    
                                                    loveViewModel.updateProfile(name)
                                                    
                                                    val prefix = if (sharedId != null) "rel_${sharedId}_" else ""
                                                    sharedPrefs.edit {
                                                        putString("${prefix}user_name", name)
                                                        putString("${prefix}main_screen_title", title)
                                                        putString("${prefix}visible_categories", categories.joinToString(","))
                                                        putString("${prefix}local_currency", currency)
                                                        putString("${prefix}fab_message_1", f1)
                                                        putString("${prefix}fab_message_2", f2)
                                                        putString("${prefix}fab_icon_1", f1i)
                                                        putString("${prefix}fab_icon_2", f2i)
                                                        if (date != null) {
                                                            putLong("${prefix}relationship_date", date)
                                                        }
                                                        if (profileUri != null) {
                                                            putString("profile_pic_uri", profileUri.toString())
                                                        }
                                                        
                                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                                        if (uid != null) {
                                                            putBoolean("first_time_$uid", false)
                                                            putBoolean("has_account_on_device", true)
                                                        }
                                                    }

                                                    navController.navigate(Screen.Main.name) {
                                                        popUpTo(Screen.Welcome.name) { inclusive = true }
                                                    }
                                                }
                                            )
                                        }
                                        composable(Screen.Main.name) {
                                            LoveScreen(
                                                title = mainTitle,
                                                visibleCategories = visibleCategories,
                                                categoryOrder = categoryOrder,
                                                onOrderChange = { newOrder ->
                                                    categoryOrder = newOrder
                                                    val key = if (sharedId != null) "rel_${sharedId}_category_order" else "category_order"
                                                    sharedPrefs.edit { putString(key, newOrder.joinToString(",")) }
                                                },
                                                deviceId = deviceId,
                                                userName = userName,
                                                relationshipDate = relationshipDate,
                                                isReorderMode = isMainReorderMode,
                                                onReorderModeChange = { isMainReorderMode = it },
                                                onNavigateToSecond = { navController.navigate(Screen.Second.name) },
                                                onNavigateToThird = { navController.navigate(Screen.Third.name) },
                                                onNavigateToFourth = { navController.navigate(Screen.Fourth.name) },
                                                onNavigateToSettings = { navController.navigate(Screen.Settings.name) },
                                                onNavigateToBucketList = { navController.navigate(Screen.BucketList.name) },
                                                onNavigateToMovies = { navController.navigate(Screen.Movies.name) },
                                                onNavigateToDaily = { navController.navigate(Screen.DailyConnection.name) },
                                                onNavigateToDrawing = { navController.navigate(Screen.Drawing.name) },
                                                onTriggerConfetti = { showConfetti = true }
                                            )
                                        }
                                        composable(Screen.Drawing.name) {
                                            DrawingScreen(
                                                userId = sharedId ?: "",
                                                userName = userName,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                        composable(Screen.Second.name) {
                                            RemindersScreen(
                                                deviceId = deviceId,
                                                userName = userName,
                                                showAddDialog = showAddDialog,
                                                onDismissDialog = { showAddDialog = false },
                                                onCompletedViewToggled = { isCompletedViewOpen = it },
                                                userId = sharedId ?: ""
                                            )
                                        }
                                        composable(Screen.Third.name) {
                                            ImportantDatesScreen(
                                                deviceId = deviceId,
                                                userName = userName,
                                                showAddDialog = showAddDialog,
                                                onDismissDialog = { showAddDialog = false },
                                                onCompletedViewToggled = { isCompletedViewOpen = it },
                                                userId = sharedId ?: ""
                                            )
                                        }
                                        composable(Screen.Fourth.name) {
                                            ShoppingListScreen(
                                                deviceId = deviceId,
                                                userName = userName,
                                                showAddDialog = showAddDialog,
                                                onDismissDialog = { showAddDialog = false },
                                                userId = sharedId ?: ""
                                            )
                                        }
                                        composable(Screen.BucketList.name) {
                                            BucketListScreen(
                                                deviceId = deviceId,
                                                userName = userName,
                                                showAddDialog = showAddDialog,
                                                onDismissDialog = { showAddDialog = false },
                                                onCompletedViewToggled = { isCompletedViewOpen = it },
                                                userId = sharedId ?: ""
                                            )
                                        }
                                        composable(Screen.Movies.name) {
                                            MoviesListScreen(
                                                userName = userName,
                                                showAddDialog = showAddDialog,
                                                showDeleteDialog = showDeleteMoviesDialog,
                                                onDismissDialog = { showAddDialog = false },
                                                onDismissDeleteDialog = { showDeleteMoviesDialog = false },
                                                onMovieClick = { id, type -> 
                                                    navController.navigate("${Screen.MovieDetail.name}/$id/$type")
                                                },
                                                onSelectionChange = { isMovieSelectionMode = it },
                                                userId = sharedId ?: ""
                                            )
                                        }
                                        composable(
                                            route = "${Screen.MovieDetail.name}/{movieId}/{type}",
                                            arguments = listOf(
                                                navArgument("movieId") { type = NavType.StringType },
                                                navArgument("type") { type = NavType.StringType }
                                            )
                                        ) { backStackEntry ->
                                            val mId = backStackEntry.arguments?.getString("movieId") ?: ""
                                            val mType = backStackEntry.arguments?.getString("type") ?: "movie"
                                            MovieDetailScreen(
                                                userId = sharedId ?: "",
                                                movieId = mId,
                                                mediaType = mType,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                        composable(Screen.DailyConnection.name) {
                                            DailyConnectionScreen(
                                                deviceId = deviceId,
                                                userName = userName,
                                                sharedId = sharedId ?: ""
                                            )
                                        }
                                        composable(Screen.Settings.name) {
                                            val loveViewModel: LoveViewModel = hiltViewModel()
                                            
                                            val syncStatus by loveViewModel.syncStatus
                                            LaunchedEffect(syncStatus) {
                                                syncStatus?.let {
                                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                                    loveViewModel.clearSyncStatus()
                                                }
                                            }

                                            SettingsScreen(
                                                currentTheme = themeMode,
                                                currentName = userName,
                                                currentWidgetConfigs = widgetConfigs,
                                                isAutoRotateEnabled = autoRotateWidget,
                                                currentAutoRotateInterval = autoRotateInterval,
                                                currentCurrency = localCurrency,
                                                currentMainTitle = mainTitle,
                                                currentLanguage = appLanguage,
                                                currentVisibleCategories = visibleCategories,
                                                currentRelationshipDate = relationshipDate,
                                                currentBgColor1 = bgColor1,
                                                currentBgColor2 = bgColor2,
                                                onThemeChange = { mode -> 
                                                    themeMode = mode
                                                    sharedPrefs.edit { putString("app_theme", mode.name) }
                                                },
                                                onNameChange = { newName ->
                                                    val trimmedName = newName.trim()
                                                    userName = trimmedName
                                                    val prefix = if (sharedId != null) "rel_${sharedId}_" else ""
                                                    sharedPrefs.edit { putString("${prefix}user_name", trimmedName) }
                                                    loveViewModel.updateProfile(trimmedName)
                                                },
                                                onMainTitleChange = { newTitle ->
                                                    mainTitle = newTitle
                                                    val key = if (sharedId != null) "rel_${sharedId}_main_screen_title" else "main_screen_title"
                                                    sharedPrefs.edit { putString(key, newTitle) }
                                                },
                                                onLanguageChange = { lang ->
                                                    appLanguage = lang
                                                    sharedPrefs.edit { putString("app_language", lang) }
                                                },
                                                onVisibleCategoriesChange = { categories ->
                                                    visibleCategories = categories
                                                    val key = if (sharedId != null) "rel_${sharedId}_visible_categories" else "visible_categories"
                                                    sharedPrefs.edit { putString(key, categories.joinToString(",")) }
                                                },
                                                onRelationshipDateChange = { date ->
                                                    relationshipDate = date
                                                    val key = if (sharedId != null) "rel_${sharedId}_relationship_date" else "relationship_date"
                                                    if (date != null) {
                                                        sharedPrefs.edit { putLong(key, date) }
                                                    } else {
                                                        sharedPrefs.edit { remove(key) }
                                                    }
                                                },
                                                onFabMessagesChange = { m1, m2, i1, i2 ->
                                                    fabMessage1 = m1
                                                    fabMessage2 = m2
                                                    fabIcon1 = i1
                                                    fabIcon2 = i2
                                                    val prefix = if (sharedId != null) "rel_${sharedId}_" else ""
                                                    sharedPrefs.edit {
                                                        putString("${prefix}fab_message_1", m1)
                                                        putString("${prefix}fab_message_2", m2)
                                                        putString("${prefix}fab_icon_1", i1)
                                                        putString("${prefix}fab_icon_2", i2)
                                                    }
                                                },
                                                onBackgroundColorsChange = { c1, c2 ->
                                                    bgColor1 = c1
                                                    bgColor2 = c2
                                                    val prefix = if (sharedId != null) "rel_${sharedId}_" else ""
                                                    sharedPrefs.edit {
                                                        putString("${prefix}bg_color_1", c1)
                                                        putString("${prefix}bg_color_2", c2)
                                                    }
                                                },
                                                onWidgetConfigsChange = { configs ->
                                                    widgetConfigs = configs
                                                    val key = if (sharedId != null) "rel_${sharedId}_widget_configs" else "widget_configs"
                                                    sharedPrefs.edit { putString(key, configs.joinToString(",")) }
                                                    scope.launch {
                                                        delay(300)
                                                        // Use updateAll to broadcast to all instances
                                                        com.lexnicholls.lovecounter.widget.LoveWidget().updateAll(context.applicationContext)
                                                    }
                                                },
                                                onAutoRotateChange = { enabled ->
                                                    autoRotateWidget = enabled
                                                    val key = if (sharedId != null) "rel_${sharedId}_widget_auto_rotate" else "widget_auto_rotate"
                                                    sharedPrefs.edit { putBoolean(key, enabled) }
                                                    scope.launch {
                                                        delay(200)
                                                        com.lexnicholls.lovecounter.widget.LoveWidget().updateAll(context.applicationContext)
                                                    }
                                                },
                                                onAutoRotateIntervalChange = { interval ->
                                                    autoRotateInterval = interval
                                                    val key = if (sharedId != null) "rel_${sharedId}_widget_rotate_interval" else "widget_rotate_interval"
                                                    sharedPrefs.edit { putInt(key, interval) }
                                                    scope.launch {
                                                        delay(200)
                                                        com.lexnicholls.lovecounter.widget.LoveWidget().updateAll(context.applicationContext)
                                                    }
                                                },
                                                onCurrencyChange = { currency ->
                                                    localCurrency = currency
                                                    val key = if (sharedId != null) "rel_${sharedId}_local_currency" else "local_currency"
                                                    sharedPrefs.edit { putString(key, currency) }
                                                },
                                                onLogout = {
                                                    navController.navigate(Screen.Login.name) {
                                                        popUpTo(0) { inclusive = true }
                                                    }
                                                },
                                                onSyncQuestions = {
                                                    loveViewModel.syncQuestionsToFirebase()
                                                }
                                            )
                                        }
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

                    // OTA Update Dialog
                    updateInfo?.let { info ->
                        LoveAlertDialog(
                            onDismissRequest = { updateInfo = null },
                            title = strings.updateAvailable,
                            confirmButtonText = strings.download,
                            onConfirm = {
                                updateManager.savePendingChangelog(info.changelog)
                                updateManager.downloadAndInstall(info.downloadUrl, info.latestVersionName)
                                updateInfo = null
                            }
                        ) {
                            Column {
                                Text("Meld v${info.latestVersionName}", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(info.changelog)
                            }
                        }
                    }

                    // Post-Update Dialog
                    changelogToShow?.let { changelog ->
                        LoveAlertDialog(
                            onDismissRequest = { changelogToShow = null },
                            title = strings.updateInstalled,
                            confirmButtonText = strings.close,
                            dismissButtonText = strings.whatsNew,
                            onConfirm = { changelogToShow = null },
                            onDismiss = { showWhatsNew = changelog }
                        ) {
                            Text(strings.welcomeBack)
                        }
                    }

                    // What's New View
                    showWhatsNew?.let { changelog ->
                        LoveAlertDialog(
                            onDismissRequest = { showWhatsNew = null },
                            title = strings.whatsNew,
                            confirmButtonText = strings.close,
                            onConfirm = { showWhatsNew = null },
                            showDismissButton = false
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(changelog)
                            }
                        }
                    }
                }
            }
        }
    }
}
