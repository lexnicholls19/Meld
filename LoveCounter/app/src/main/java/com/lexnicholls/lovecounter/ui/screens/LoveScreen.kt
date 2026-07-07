package com.lexnicholls.lovecounter.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.util.getStringsForLanguage
import com.lexnicholls.lovecounter.ui.theme.*
import java.util.Locale
import java.time.Duration
import java.time.Period
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt

enum class DashboardDisplayMode {
    FULL, COMPACT, ICONS
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
    relationshipDate: Long?,
    isReorderMode: Boolean,
    onReorderModeChange: (Boolean) -> Unit,
    onNavigateToSecond: () -> Unit,
    onNavigateToThird: () -> Unit,
    onNavigateToFourth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBucketList: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToDaily: () -> Unit,
    onNavigateToDrawing: () -> Unit,
    onNavigateToWellness: () -> Unit,
    onTriggerConfetti: () -> Unit,
    viewModel: LoveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = t()
    val sharedPrefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
    
    var displayMode by rememberSaveable { 
        mutableStateOf(
            DashboardDisplayMode.valueOf(sharedPrefs.getString("dashboard_display_mode", DashboardDisplayMode.FULL.name) ?: DashboardDisplayMode.FULL.name)
        )
    }

    LaunchedEffect(displayMode) {
        sharedPrefs.edit().putString("dashboard_display_mode", displayMode.name).apply()
    }

    val startDate = remember(relationshipDate) {
        relationshipDate?.let {
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
        }
    }
    
    val currentTime by viewModel.currentTime
    
    // Optimizamos los cálculos de periodo para que no bloqueen el hilo UI
    val periodInfo by remember(currentTime.toLocalDate(), startDate) {
        derivedStateOf {
            startDate?.let { 
                val p = java.time.Period.between(it.toLocalDate(), currentTime.toLocalDate())
                Triple(p.years, p.months, p.days)
            }
        }
    }
    
    val totalDays by remember(currentTime.toLocalDate(), startDate) {
        derivedStateOf {
            startDate?.let { java.time.Duration.between(it, currentTime).toDays() } ?: -1L
        }
    }

    DisposableEffect(userName) {
        viewModel.listenToStatuses(userName)
        onDispose { }
    }

    var draggingItemId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(totalDays) {
        if (totalDays > 0 && (totalDays % 100 == 0L || totalDays % 365 == 0L)) {
            onTriggerConfetti()
        }
    }

    val allTiles = remember(strings, onNavigateToSecond, onNavigateToThird, onNavigateToFourth, onNavigateToSettings, onNavigateToBucketList, onNavigateToMovies, onNavigateToDaily, onNavigateToDrawing, onNavigateToWellness) {
        listOf(
            TileData("reminders", strings.reminders, strings.remindersDesc, Icons.Default.Notifications, ReminderColor, onNavigateToSecond),
            TileData("dates", strings.dates, strings.datesDesc, Icons.Default.DateRange, DatesColor, onNavigateToThird),
            TileData("market", strings.market, strings.marketDesc, Icons.Default.ShoppingCart, MarketColor, onNavigateToFourth),
            TileData("bucket", strings.bucket, strings.bucketDesc, Icons.Default.Star, BucketColor, onNavigateToBucketList),
            TileData("drawing", strings.drawing, strings.drawingDesc, Icons.Default.Brush, LovePink, onNavigateToDrawing),
            TileData("movies", strings.movies, strings.moviesDesc, Icons.Default.Movie, MoviesColor, onNavigateToMovies),
            TileData("daily", strings.daily, strings.dailyDesc, Icons.Default.FavoriteBorder, DailyColor, onNavigateToDaily),
            TileData("wellness", strings.wellness, strings.wellnessDesc, Icons.Default.Favorite, WellnessColor, onNavigateToWellness)
        )
    }

    val members by viewModel.members
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val currentUserProfile by viewModel.currentUserProfile
    
    val actualVisibleCategories = remember(visibleCategories, members, currentUserProfile, currentUserId) {
        val filtered = visibleCategories.toMutableSet()
        val iTrack = currentUserProfile?.trackWellness == true
        val someoneElseShares = members.any { it.uid != currentUserId && it.trackWellness && it.shareWellness }

        if (iTrack || someoneElseShares) {
            filtered.add("wellness")
        } else if (currentUserProfile != null) {
            // Solo la ocultamos si estamos seguros de que NADIE rastrea ni comparte
            filtered.remove("wellness")
        }
        filtered
    }

    val visibleTilesOrder = remember(categoryOrder, actualVisibleCategories) {
        categoryOrder.filter { actualVisibleCategories.contains(it) }
    }

    val customColors = LocalCustomColors.current
    val isDark = LocalIsDark.current
    
    val topBarColor = remember(customColors, isDark) {
        if (customColors != null) {
            customColors.first
        } else if (isDark) {
            Color(0xFF0F172A) // El primer color del gradiente oscuro por defecto
        } else {
            Color(0xFFEBE3FF) // El primer color del gradiente claro por defecto
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    AnimatedContent(targetState = isReorderMode, label = "TitleTransition") { reordering ->
                        Text(
                            text = if (reordering) strings.reorderCategories else title, 
                            fontWeight = FontWeight.Bold, 
                            color = LovePink,
                            fontSize = if (reordering) 20.sp else 24.sp
                        )
                    }
                },
                actions = {
                    if (isReorderMode) {
                        IconButton(onClick = { onReorderModeChange(false) }) {
                            Icon(Icons.Default.Check, contentDescription = strings.save, tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = Color.Gray)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyVerticalGrid(
            columns = when(displayMode) {
                DashboardDisplayMode.FULL -> GridCells.Fixed(1)
                DashboardDisplayMode.COMPACT -> GridCells.Fixed(2)
                DashboardDisplayMode.ICONS -> GridCells.Fixed(3)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = !isReorderMode
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "hero_card") {
                AnimatedVisibility(
                    visible = !isReorderMode && startDate != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                                "Settings" -> Locale.ENGLISH
                                                "Paramètres" -> Locale.FRENCH
                                                "Einstellungen" -> Locale.GERMAN
                                                "Configurações" -> Locale.forLanguageTag("pt")
                                                else -> Locale.forLanguageTag("es")
                                            }
                                        )
                                    }
                                    startDate?.let {
                                        Text(
                                            text = "${strings.since} ${it.format(dateFormatter)}",
                                            fontSize = 16.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = periodInfo?.let { (y, m, d) ->
                                            val yLabel = when(strings.settings) {
                                                "Settings" -> "Y"
                                                "Configuración" -> "Y"
                                                else -> strings.years
                                            }
                                            val mLabel = when(strings.settings) {
                                                "Settings" -> "M"
                                                "Configuración" -> "M"
                                                else -> strings.months
                                            }
                                            val dLabel = when(strings.settings) {
                                                "Settings" -> "D"
                                                "Configuración" -> "D"
                                                else -> strings.days
                                            }
                                            "$y$yLabel - $m$mLabel - $d$dLabel"
                                        } ?: "...",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            if (isReorderMode) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "display_mode_selector") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val modes = listOf(
                            DashboardDisplayMode.FULL to Icons.Default.TableRows,
                            DashboardDisplayMode.COMPACT to Icons.Default.GridView,
                            DashboardDisplayMode.ICONS to Icons.Default.Apps
                        )
                        
                        modes.forEach { (mode, icon) ->
                            val isSelected = displayMode == mode
                            IconButton(
                                onClick = { displayMode = mode },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isSelected) LovePink.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (isSelected) LovePink else Color.Gray
                                )
                            ) {
                                Icon(icon, contentDescription = null)
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "drag_instructions") {
                    Text(
                        text = strings.dragToReorder,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            itemsIndexed(visibleTilesOrder, key = { _, id -> id }) { index, id ->
                val tile = allTiles.find { it.id == id }
                if (tile != null) {
                    val currentIndex by rememberUpdatedState(index)
                    val currentOrder by rememberUpdatedState(visibleTilesOrder)
                    val fullOrder by rememberUpdatedState(categoryOrder)

                    var dragAmountY by remember { mutableStateOf(0f) }
                    val density = LocalDensity.current
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { if (draggingItemId != id) it.animateItem() else it }
                            .graphicsLayer {
                                translationY = dragAmountY
                            }
                    ) {
                        val currentDragHandleModifier = if (isReorderMode) {
                            Modifier.pointerInput(id) {
                                detectDragGestures(
                                    onDragStart = { 
                                        dragAmountY = 0f 
                                        draggingItemId = id
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAmountY += dragAmount.y
                                        
                                        val thresholdPx = with(density) { 
                                            when(displayMode) {
                                                DashboardDisplayMode.FULL -> 100.dp
                                                DashboardDisplayMode.COMPACT -> 80.dp
                                                DashboardDisplayMode.ICONS -> 60.dp
                                            }.toPx()
                                        }
                                        
                                        if (dragAmountY > thresholdPx && currentIndex < currentOrder.size - 1) {
                                            val newList = fullOrder.toMutableList()
                                            val currentId = currentOrder[currentIndex]
                                            val targetId = currentOrder[currentIndex + 1]
                                            val cp = newList.indexOf(currentId)
                                            val tp = newList.indexOf(targetId)
                                            
                                            newList[cp] = targetId
                                            newList[tp] = currentId
                                            onOrderChange(newList)
                                            dragAmountY -= thresholdPx
                                        } else if (dragAmountY < -thresholdPx && currentIndex > 0) {
                                            val newList = fullOrder.toMutableList()
                                            val currentId = currentOrder[currentIndex]
                                            val targetId = currentOrder[currentIndex - 1]
                                            val cp = newList.indexOf(currentId)
                                            val tp = newList.indexOf(targetId)
                                            
                                            newList[cp] = targetId
                                            newList[tp] = currentId
                                            onOrderChange(newList)
                                            dragAmountY += thresholdPx
                                        }
                                    },
                                    onDragEnd = { 
                                        dragAmountY = 0f 
                                        draggingItemId = null
                                    },
                                    onDragCancel = { 
                                        dragAmountY = 0f 
                                        draggingItemId = null
                                    }
                                )
                            }
                        } else Modifier

                        DashboardTile(
                            title = tile.title,
                            subtitle = tile.subtitle,
                            icon = tile.icon,
                            color = tile.color,
                            onClick = { if (!isReorderMode) tile.onClick() },
                            onLongClick = { if (!isReorderMode) onReorderModeChange(true) },
                            isReorderMode = isReorderMode,
                            displayMode = displayMode,
                            dragHandleModifier = currentDragHandleModifier
                        )
                    }
                }
            }

            val showSummary = !isReorderMode && (displayMode == DashboardDisplayMode.ICONS || visibleTilesOrder.size <= 4)
            if (showSummary) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "summary_section") {
                    SummarySection(userId = viewModel.sharedId.value ?: "")
                }
            }

        }
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
    isReorderMode: Boolean = false,
    displayMode: DashboardDisplayMode = DashboardDisplayMode.FULL,
    dragHandleModifier: Modifier = Modifier,
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
        colors = CardDefaults.cardColors(
            containerColor = if (isReorderMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isReorderMode) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        when (displayMode) {
            DashboardDisplayMode.FULL -> {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isReorderMode) {
                        Icon(
                            Icons.Default.DragHandle, 
                            contentDescription = null, 
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.padding(end = 12.dp).then(dragHandleModifier)
                        )
                    }

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
                    if (!isReorderMode) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
                    }
                }
            }
            DashboardDisplayMode.COMPACT -> {
                Box(contentAlignment = Alignment.TopEnd) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = color.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = title, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isReorderMode) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.padding(8.dp).size(16.dp).then(dragHandleModifier)
                        )
                    }
                }
            }
            DashboardDisplayMode.ICONS -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
                        }
                    }
                    if (isReorderMode) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(14.dp).then(dragHandleModifier)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummarySection(userId: String) {
    if (userId.isBlank()) return
    
    val strings = t()
    val db = remember { FirebaseFirestore.getInstance() }
    var nearItems by remember { mutableStateOf<List<SummaryItem>>(emptyList()) }

    LaunchedEffect(userId) {
        // Fetch Reminders
        val remindersTask = db.collection("users").document(userId).collection("reminders")
            .whereEqualTo("completed", false)
            .get()
        
        // Fetch Important Dates
        val datesTask = db.collection("users").document(userId).collection("important_dates")
            .get()

        try {
            val remindersSnap = remindersTask.await()
            val datesSnap = datesTask.await()

            val items = mutableListOf<SummaryItem>()

            remindersSnap.documents.forEach { doc: DocumentSnapshot ->
                val dueDate = doc.getTimestamp("dueDate")
                if (dueDate != null) {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.now(),
                        java.time.Instant.ofEpochMilli(dueDate.toDate().time).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    )
                    if (days in 0..7) {
                        items.add(SummaryItem(
                            title = doc.getString("text") ?: "",
                            daysRemaining = days,
                            type = strings.reminders,
                            color = ReminderColor
                        ))
                    }
                }
            }

            datesSnap.documents.forEach { doc: DocumentSnapshot ->
                val date = doc.getTimestamp("date")
                if (date != null) {
                    val days = calculateDaysRemainingSummary(date)
                    if (days in 0..30) {
                        items.add(SummaryItem(
                            title = doc.getString("title") ?: "",
                            daysRemaining = days,
                            type = strings.dates,
                            color = DatesColor
                        ))
                    }
                }
            }

            nearItems = items.sortedBy { it.daysRemaining }.take(3)
        } catch (e: Exception) {
            Log.e("Summary", "Error fetching summary items", e)
        }
    }

    if (nearItems.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = strings.comingSoon.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LovePink,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    nearItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(item.color, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.type,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            Text(
                                text = if (item.daysRemaining == 0L) strings.today else strings.daysLeftShort.format(item.daysRemaining),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = item.color
                            )
                        }
                        if (index < nearItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SummaryItem(
    val title: String,
    val daysRemaining: Long,
    val type: String,
    val color: Color
)

fun calculateDaysRemainingSummary(dateTimestamp: Timestamp): Long {
    val today = java.time.LocalDate.now()
    val originalDate = java.time.Instant.ofEpochMilli(dateTimestamp.toDate().time)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()

    var nextOccurrence = originalDate.withYear(today.year)
    if (nextOccurrence.isBefore(today)) {
        nextOccurrence = nextOccurrence.plusYears(1)
    }

    return java.time.temporal.ChronoUnit.DAYS.between(today, nextOccurrence)
}

fun sendInterpretedNotification(context: Context, title: String, message: String, senderName: String) {
    val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val deviceId = sharedPrefs.getString("device_id", "") ?: ""
    val nameFromPrefs = sharedPrefs.getString("user_name", "") ?: ""
    val langCode = sharedPrefs.getString("app_language", "system") ?: "system"
    val strings = getStringsForLanguage(langCode)
    
    val finalSenderName = if (senderName.isNotBlank()) senderName else nameFromPrefs
    
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserUid = auth.currentUser?.uid ?: ""
    
    // Obtenemos el sharedId (relationId) de los perfiles de usuario si es necesario, 
    // pero para simplicidad y rapidez lo incluimos en el payload si el backend lo requiere para el topic.
    val notification = hashMapOf(
        "name" to title,
        "value" to message,
        "timestamp" to Timestamp.now(),
        "senderName" to finalSenderName,
        "userName" to finalSenderName,
        "senderId" to deviceId,
        "deviceId" to deviceId,
        "senderUid" to currentUserUid,
        "targetTopic" to "relation_" // El backend debería completar esto o podemos intentar obtenerlo aquí
    )
    
    // Intentamos obtener el relationId del usuario actual para enviarlo
    db.collection("users").document(currentUserUid).get()
        .addOnSuccessListener { snapshot ->
            val relationId = snapshot.getString("relationId")
            val pId = snapshot.getString("partnerId")
            
            val finalSharedId = relationId ?: if (pId != null) {
                listOf(currentUserUid, pId).sorted().joinToString("_")
            } else {
                currentUserUid
            }
            
            notification["relationId"] = finalSharedId
            notification["targetTopic"] = "relation_$finalSharedId"

            Log.d("Notification", "Enviando mensaje rápido a relación: $finalSharedId")
            db.collection("relations").document(finalSharedId).collection("quick_messages").add(notification)
                .addOnSuccessListener {
                    Log.d("Notification", "Mensaje enviado con éxito")
                    Toast.makeText(context, strings.quickMessageSent, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Notification", "Error al enviar mensaje: ${e.message}")
                    Toast.makeText(context, strings.drawingError, Toast.LENGTH_SHORT).show()
                }
        }
}
