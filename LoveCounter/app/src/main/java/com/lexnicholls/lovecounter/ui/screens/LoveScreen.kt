package com.lexnicholls.lovecounter.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.theme.*
import java.util.Locale
import java.time.Duration
import java.time.Period

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
    onTriggerConfetti: () -> Unit,
    viewModel: LoveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val startDate = viewModel.startDate
    val currentTime by viewModel.currentTime
    
    val partnerStatus by viewModel.partnerStatus
    val myStatus by viewModel.myStatus
    var showStatusDialog by rememberSaveable { mutableStateOf(false) }

    val isRefreshing by viewModel.isRefreshing

    // El ViewModel se encarga de escuchar los cambios
    DisposableEffect(userName) {
        viewModel.listenToStatuses(userName)
        onDispose { }
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
        LoveAlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = t().howDoYouFeel,
            showDismissButton = false,
            onConfirm = { showStatusDialog = false },
            confirmButtonText = t().close
        ) {
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
                                viewModel.updateStatus(userName, deviceId, emoji)
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
                                viewModel.updateStatus(userName, deviceId, emoji)
                                // También enviamos una notificación para que la pareja se entere del cambio de estado
                                sendInterpretedNotification(context, "Estado", "Nuevo estado de $userName: $emoji", userName)
                                showStatusDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
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
                color = LovePink
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
                        Text(
                            text = "$hours ${strings.hoursShort} $minutes ${strings.minutesShort} $seconds ${strings.secondsShort}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = LovePink
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
                    TileData("reminders", strings.reminders, strings.remindersDesc, Icons.Default.Notifications, ReminderColor, onNavigateToSecond),
                    TileData("dates", strings.dates, strings.datesDesc, Icons.Default.DateRange, DatesColor, onNavigateToThird),
                    TileData("market", strings.market, strings.marketDesc, Icons.Default.ShoppingCart, MarketColor, onNavigateToFourth),
                    TileData("bucket", strings.bucket, strings.bucketDesc, Icons.Default.Star, BucketColor, onNavigateToBucketList),
                    TileData("movies", strings.movies, strings.moviesDesc, Icons.Default.Movie, MoviesColor, onNavigateToMovies),
                    TileData("daily", strings.daily, strings.dailyDesc, Icons.Default.FavoriteBorder, DailyColor, onNavigateToDaily)
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

fun sendInterpretedNotification(context: Context, title: String, message: String, senderName: String) {
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
