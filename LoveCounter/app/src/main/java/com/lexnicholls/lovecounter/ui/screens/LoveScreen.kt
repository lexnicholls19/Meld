package com.lexnicholls.lovecounter.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.ui.theme.*
import java.util.Locale
import java.time.Duration
import java.time.Period
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveScreen(
    title: String,
    visibleCategories: Set<String>,
    categoryOrder: List<String>,
    onOrderChange: (List<String>) -> Unit,
    deviceId: String,
    userName: String,
    onNavigateToSecond: () -> Unit,
    onNavigateToThird: () -> Unit,
    onNavigateToFourth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBucketList: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToDaily: () -> Unit,
    onTriggerConfetti: () -> Unit,
    viewModel: LoveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val startDate = viewModel.startDate
    val currentTime by viewModel.currentTime
    
    DisposableEffect(userName) {
        viewModel.listenToStatuses(userName)
        onDispose { }
    }

    val period = remember(currentTime) { Period.between(startDate.toLocalDate(), currentTime.toLocalDate()) }
    val years = period.years
    val months = period.months
    val periodDays = period.days
    
    val duration = remember(currentTime) { Duration.between(startDate, currentTime) }
    val totalDays = duration.toDays()

    var isReorderMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(totalDays) {
        if (totalDays > 0 && (totalDays % 100 == 0L || totalDays % 365 == 0L)) {
            onTriggerConfetti()
        }
    }

    val strings = t()
    val allTiles = remember(strings) {
        listOf(
            TileData("reminders", strings.reminders, strings.remindersDesc, Icons.Default.Notifications, ReminderColor, onNavigateToSecond),
            TileData("dates", strings.dates, strings.datesDesc, Icons.Default.DateRange, DatesColor, onNavigateToThird),
            TileData("market", strings.market, strings.marketDesc, Icons.Default.ShoppingCart, MarketColor, onNavigateToFourth),
            TileData("bucket", strings.bucket, strings.bucketDesc, Icons.Default.Star, BucketColor, onNavigateToBucketList),
            TileData("movies", strings.movies, strings.moviesDesc, Icons.Default.Movie, MoviesColor, onNavigateToMovies),
            TileData("daily", strings.daily, strings.dailyDesc, Icons.Default.FavoriteBorder, DailyColor, onNavigateToDaily)
        )
    }

    val visibleTilesOrder = remember(categoryOrder, visibleCategories) {
        categoryOrder.filter { visibleCategories.contains(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = LovePink) },
                actions = {
                    if (isReorderMode) {
                        IconButton(onClick = { isReorderMode = false }) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Timer Hero Card
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
                        }
                    }
                }
            }

            item {
                if (isReorderMode) {
                    Text(
                        text = "Modo edición: Arrastra las tarjetas para reordenar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            itemsIndexed(visibleTilesOrder, key = { _, id -> id }) { index, id ->
                val tile = allTiles.find { it.id == id }
                if (tile != null) {
                    val currentIndex by rememberUpdatedState(index)
                    val currentOrder by rememberUpdatedState(visibleTilesOrder)
                    val fullOrder by rememberUpdatedState(categoryOrder)

                    var dragAmountY by remember { mutableStateOf(0f) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .offset { IntOffset(0, dragAmountY.roundToInt()) }
                            .pointerInput(Unit) {
                                if (isReorderMode) {
                                    detectDragGestures(
                                        onDragStart = { dragAmountY = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragAmountY += dragAmount.y
                                            
                                            // Adjusted threshold for better precision and speed control
                                            // A larger value (like 250f) requires more finger movement per swap
                                            val threshold = 250f 

                                            if (dragAmountY > threshold && currentIndex < currentOrder.size - 1) {
                                                val newList = fullOrder.toMutableList()
                                                val currentId = currentOrder[currentIndex]
                                                val targetId = currentOrder[currentIndex + 1]
                                                
                                                val currentPos = newList.indexOf(currentId)
                                                val targetPos = newList.indexOf(targetId)
                                                
                                                newList[currentPos] = targetId
                                                newList[targetPos] = currentId
                                                
                                                onOrderChange(newList)
                                                dragAmountY -= threshold 
                                            } else if (dragAmountY < -threshold && currentIndex > 0) {
                                                val newList = fullOrder.toMutableList()
                                                val currentId = currentOrder[currentIndex]
                                                val targetId = currentOrder[currentIndex - 1]
                                                
                                                val currentPos = newList.indexOf(currentId)
                                                val targetPos = newList.indexOf(targetId)
                                                
                                                newList[currentPos] = targetId
                                                newList[targetPos] = currentId
                                                
                                                onOrderChange(newList)
                                                dragAmountY += threshold
                                            }
                                        },
                                        onDragEnd = { dragAmountY = 0f },
                                        onDragCancel = { dragAmountY = 0f }
                                    )
                                }
                            }
                    ) {
                        DashboardTile(
                            title = tile.title,
                            subtitle = tile.subtitle,
                            icon = tile.icon,
                            color = tile.color,
                            onClick = { if (!isReorderMode) tile.onClick() },
                            onLongClick = { isReorderMode = true },
                            isReorderMode = isReorderMode
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
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
    onLongClick: () -> Unit = {},
    isReorderMode: Boolean = false
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReorderMode) {
                Icon(
                    Icons.Default.DragHandle, 
                    contentDescription = null, 
                    tint = Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = 12.dp)
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
}

fun sendInterpretedNotification(context: Context, title: String, message: String, senderName: String) {
    val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val deviceId = sharedPrefs.getString("device_id", "") ?: ""
    val nameFromPrefs = sharedPrefs.getString("user_name", "") ?: ""
    
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
    db.collection("quick_messages").add(notification)
        .addOnSuccessListener {
            Toast.makeText(context, "Mensaje enviado ✨", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error al enviar", Toast.LENGTH_SHORT).show()
        }
}
