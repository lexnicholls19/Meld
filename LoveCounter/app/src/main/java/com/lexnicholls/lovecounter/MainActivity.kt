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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.compose.*
import androidx.navigation.NavDestination.Companion.hierarchy
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
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
            val sharedPrefs = remember { context.getSharedPreferences("prefs", MODE_PRIVATE) }
            val deviceId = remember {
                val id = sharedPrefs.getString("device_id", null) ?: java.util.UUID.randomUUID().toString()
                sharedPrefs.edit().putString("device_id", id).apply()
                id
            }
            
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            var themeMode by rememberSaveable { 
                val saved = sharedPrefs.getString("app_theme", ThemeMode.System.name) ?: ThemeMode.System.name
                mutableStateOf(ThemeMode.valueOf(saved))
            }
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
            var fabMessage1 by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("fab_message_1", null))
            }
            var fabMessage2 by rememberSaveable {
                mutableStateOf(sharedPrefs.getString("fab_message_2", null))
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

            com.lexnicholls.lovecounter.ui.theme.MeldTheme(
                darkTheme = useDarkTheme,
                dynamicColor = false // Set to true if you want Android 12+ dynamic colors
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
                            if ((currentRoute == Screen.Main.name) && !isMainReorderMode) {
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
                                                        text = fabMessage1 ?: "${t().missYou} 💛",
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                SmallFloatingActionButton(
                                                    onClick = { 
                                                        val msg = fabMessage1 ?: "${strings.missYou} 💛"
                                                        sendInterpretedNotification(context, strings.loveExtraLabel, msg, userName)
                                                        isGiftExpanded = false
                                                    },
                                                    containerColor = TertiaryColor,
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
                                                        text = fabMessage2 ?: "${strings.loveYou} ✨",
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                SmallFloatingActionButton(
                                                    onClick = { 
                                                        val msg = fabMessage2 ?: "${strings.loveYou} ✨"
                                                        sendInterpretedNotification(context, strings.loveExtraLabel, msg, userName)
                                                        isGiftExpanded = false
                                                        showConfetti = true
                                                    },
                                                    containerColor = LovePink,
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
                            } else if (isMovieSelectionMode && currentRoute == Screen.Movies.name) {
                                FloatingActionButton(
                                    onClick = { showDeleteMoviesDialog = true },
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete)
                                }
                            } else if (!isMainReorderMode && 
                                !isCompletedViewOpen &&
                                currentRoute != Screen.DailyConnection.name && 
                                currentRoute != Screen.Settings.name && 
                                currentRoute != Screen.Login.name &&
                                currentRoute != Screen.Register.name &&
                                currentRoute != Screen.Welcome.name &&
                                currentRoute != Screen.Drawing.name
                            ) {
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
                                            tint = LovePink,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                ) {
                                    NavigationRailItem(
                                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Main.name } == true,
                                        onClick = { 
                                            navController.navigate(Screen.Main.name) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Home, contentDescription = t().start) },
                                        label = { Text(t().start) }
                                    )
                                    if (visibleCategories.contains("reminders")) {
                                        NavigationRailItem(
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Second.name } == true,
                                            onClick = { 
                                                navController.navigate(Screen.Second.name) {
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
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Third.name } == true,
                                            onClick = { 
                                                navController.navigate(Screen.Third.name) {
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
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Fourth.name } == true,
                                            onClick = { 
                                                navController.navigate(Screen.Fourth.name) {
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
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.BucketList.name } == true,
                                            onClick = { 
                                                navController.navigate(Screen.BucketList.name) {
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
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Movies.name } == true,
                                            onClick = { 
                                                navController.navigate(Screen.Movies.name) {
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
                                            selected = currentDestination?.hierarchy?.any { it.route == Screen.DailyConnection.name } == true,
                                            onClick = { 
                                                navController.navigate(Screen.DailyConnection.name) {
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
                                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.name } == true,
                                        onClick = { 
                                            navController.navigate(Screen.Settings.name) {
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
                                    val loveViewModel: LoveViewModel = hiltViewModel()
                                    val sharedId by loveViewModel.sharedId

                                    NavHost(
                                        navController = navController,
                                        startDestination = if (currentUser != null) Screen.Main.name else Screen.Login.name
                                    ) {
                                        composable(Screen.Login.name) {
                                            LoginScreen(
                                                onNavigateToRegister = { navController.navigate(Screen.Register.name) },
                                                onLoginSuccess = { 
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
                                                    navController.navigate(Screen.Welcome.name) {
                                                        popUpTo(Screen.Login.name) { inclusive = true }
                                                    }
                                                }
                                            )
                                        }
                                        composable(Screen.Welcome.name) {
                                            WelcomeScreen(
                                                onContinue = { name, title, categories, date, profileUri, currency, f1, f2 ->
                                                    userName = name
                                                    mainTitle = title
                                                    visibleCategories = categories
                                                    relationshipDate = date
                                                    localCurrency = currency
                                                    fabMessage1 = f1
                                                    fabMessage2 = f2
                                                    
                                                    loveViewModel.updateProfile(name)
                                                    
                                                    sharedPrefs.edit().apply {
                                                        putString("user_name", name)
                                                        putString("main_screen_title", title)
                                                        putString("visible_categories", categories.joinToString(","))
                                                        putString("local_currency", currency)
                                                        putString("fab_message_1", f1)
                                                        putString("fab_message_2", f2)
                                                        if (date != null) {
                                                            putLong("relationship_date", date)
                                                        }
                                                        if (profileUri != null) {
                                                            putString("profile_pic_uri", profileUri.toString())
                                                        }
                                                        
                                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                                        if (uid != null) {
                                                            putBoolean("first_time_$uid", false)
                                                        }
                                                        apply()
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
                                                    sharedPrefs.edit().putString("category_order", newOrder.joinToString(",")).apply()
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
                                                userName = userName
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
                                                currentCurrency = localCurrency,
                                                currentMainTitle = mainTitle,
                                                currentLanguage = appLanguage,
                                                currentVisibleCategories = visibleCategories,
                                                currentRelationshipDate = relationshipDate,
                                                onThemeChange = { mode -> 
                                                    themeMode = mode
                                                    sharedPrefs.edit().putString("app_theme", mode.name).apply()
                                                },
                                                onNameChange = { newName ->
                                                    val trimmedName = newName.trim()
                                                    userName = trimmedName
                                                    sharedPrefs.edit().putString("user_name", trimmedName).commit()
                                                    loveViewModel.updateProfile(trimmedName)
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
                                                onRelationshipDateChange = { date ->
                                                    relationshipDate = date
                                                    if (date != null) {
                                                        sharedPrefs.edit().putLong("relationship_date", date).commit()
                                                    } else {
                                                        sharedPrefs.edit().remove("relationship_date").commit()
                                                    }
                                                },
                                                onFabMessagesChange = { m1, m2 ->
                                                    fabMessage1 = m1
                                                    fabMessage2 = m2
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
                }
            }
        }
    }
}
