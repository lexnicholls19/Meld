package com.lexnicholls.lovecounter.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import com.lexnicholls.lovecounter.viewmodel.User
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScreen(
    userId: String,
    userName: String,
    onBack: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val strings = t()
    val loveViewModel: LoveViewModel = hiltViewModel()
    val currentUserProfile by loveViewModel.currentUserProfile
    val members by loveViewModel.members
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var showConfigDialog by remember { mutableStateOf(false) }
    var showSexPopup by remember { mutableStateOf(false) }
    var showPeriodLogger by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val showMyCycle = currentUserProfile?.trackWellness == true
    val sharedMembers = members.filter { it.uid != currentUserId && it.trackWellness && it.shareWellness }
    val showPartnerCycle = sharedMembers.isNotEmpty()

    val tabs = remember(showMyCycle, showPartnerCycle) {
        val list = mutableListOf<String>()
        if (showMyCycle || (!showMyCycle && !showPartnerCycle)) {
            list.add(strings.me)
        }
        if (showPartnerCycle) {
            list.add(strings.partner)
        }
        list
    }

    var selectedTabIndex by remember(tabs.size) { mutableIntStateOf(0) }
    val currentTab = tabs.getOrNull(selectedTabIndex)
    val isMe = currentTab == strings.me
    
    val displayUser = remember(isMe, currentUserProfile, sharedMembers) {
        if (isMe) currentUserProfile else sharedMembers.firstOrNull()
    }

    LaunchedEffect(displayUser?.uid) {
        displayUser?.uid?.let { loveViewModel.observeWellnessLogs(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(LovePink.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)
        )
    )) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header & Tabs
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.wellness,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (showMyCycle || showPartnerCycle) {
                        IconButton(onClick = onNavigateToCalendar) {
                            Icon(Icons.Default.CalendarMonth, null, tint = LovePink)
                        }
                    }
                }

                if (tabs.size > 1) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabIndex == index) LovePink else Color.Gray
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content
            if (isMe) {
                if (currentUserProfile == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LovePink)
                    }
                } else if (showMyCycle) {
                    val logs by loveViewModel.wellnessLogs
                    FloStyleMainView(
                        user = currentUserProfile!!,
                        isReadOnly = false,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        wellnessLogs = logs,
                        onLogPeriod = { showPeriodLogger = true },
                        onLogSex = { showSexPopup = true }
                    )
                } else {
                    WellnessIntroView(strings) { loveViewModel.updateWellnessPreferences(true, false) }
                }
            } else {
                val partner = sharedMembers.firstOrNull()
                if (partner != null) {
                    val logs by loveViewModel.wellnessLogs
                    FloStyleMainView(
                        user = partner,
                        isReadOnly = true,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        wellnessLogs = logs,
                        onLogPeriod = {},
                        onLogSex = {}
                    )
                }
            }
        }
    }

    // Popups
    if (showSexPopup) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val logs by loveViewModel.wellnessLogs
        val dateKey = selectedDate.toString()
        val currentActivities = logs[dateKey] ?: emptyList()
        
        ModalBottomSheet(
            onDismissRequest = { showSexPopup = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SexLogBottomSheet(
                user = currentUserProfile!!,
                selectedDate = selectedDate,
                initialActivities = currentActivities,
                onDismiss = { showSexPopup = false },
                onSave = { activities ->
                    loveViewModel.updateWellnessLog(dateKey, activities)
                }
            )
        }
    }

    if (showPeriodLogger) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPeriodLogger = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            PeriodLoggerFullView(
                user = currentUserProfile!!,
                onDismiss = { showPeriodLogger = false },
                onSave = { date ->
                    loveViewModel.updateWellnessData(
                        Timestamp(date),
                        currentUserProfile!!.cycleLength,
                        currentUserProfile!!.periodDuration
                    )
                    showPeriodLogger = false
                }
            )
        }
    }
}

@Composable
fun FloStyleMainView(
    user: User,
    isReadOnly: Boolean,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    wellnessLogs: Map<String, List<String>>,
    onLogPeriod: () -> Unit,
    onLogSex: () -> Unit
) {
    val strings = t()
    val lastStart = user.lastPeriodStart?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
    
    val cycleInfo = remember(user.lastPeriodStart, user.cycleLength, user.periodDuration, selectedDate) {
        if (lastStart == null) null
        else {
            val nextStart = lastStart.plusDays(user.cycleLength.toLong())
            val daysUntil = ChronoUnit.DAYS.between(selectedDate, nextStart)
            
            // Sincronizar cálculo de ovulación con WeeklyStrip
            val ovulationDate = lastStart.plusDays((user.cycleLength - 14).toLong())
            val isOvulation = selectedDate == ovulationDate
            val isFertile = ChronoUnit.DAYS.between(ovulationDate, selectedDate) in -6..4
            
            val totalDaysFromStart = ChronoUnit.DAYS.between(lastStart, selectedDate)
            val dayOfCycle = (totalDaysFromStart % user.cycleLength + 1).toInt()
            val isDuringPeriod = dayOfCycle in 1..user.periodDuration
            
            val predictionText = when {
                isDuringPeriod -> strings.periodStatus
                else -> strings.nextPeriodIn
            }
            
            val mainValue = when {
                isDuringPeriod -> "D $dayOfCycle"
                else -> "$daysUntil ${strings.days.lowercase()}"
            }
            
            val probabilityText = when {
                isOvulation -> strings.ovulationDay
                isFertile -> strings.fertileDay
                else -> strings.lowPregnancyProbability
            }

            Quadruple(predictionText, mainValue, probabilityText, dayOfCycle)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current Date Label
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.getDefault())),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Weekly Strip (Interactive & Swipeable)
        WeeklyStrip(
            selectedDate = selectedDate, 
            onDateSelected = onDateSelected, 
            lastStart = lastStart, 
            user = user,
            wellnessLogs = wellnessLogs
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Center Status Circle
        val circleColor = when {
            cycleInfo?.third == strings.ovulationDay || cycleInfo?.third == strings.fertileDay -> Color.Cyan
            cycleInfo?.first == strings.periodStatus -> LovePink
            else -> Color.Gray // Gris para días fuera de periodo/fertilidad
        }

        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(circleColor.copy(alpha = 0.05f), circleColor.copy(alpha = 0.2f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                if (cycleInfo != null) {
                    Text(text = cycleInfo.first, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    if (cycleInfo.second.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cycleInfo.second,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = cycleInfo.third, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                } else {
                    Text(text = strings.noDataRecorded, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Action Buttons
        if (!isReadOnly) {
            val isFuture = selectedDate.isAfter(LocalDate.now())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                WellnessActionButton(icon = Icons.Default.WaterDrop, label = strings.logPeriodStart, color = LovePink, onClick = onLogPeriod)
                if (!isFuture) {
                    WellnessActionButton(icon = Icons.Default.Favorite, label = strings.sex, color = Color.White, tint = Color.Gray, onClick = onLogSex)
                }
            }
        } else {
            // Partner View: Show partner name
            Surface(
                modifier = Modifier.padding(bottom = 80.dp),
                color = LovePink.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(LovePink), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.WaterDrop, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LovePink)
                }
            }
        }
    }
}

@Composable
fun WeeklyStrip(
    selectedDate: LocalDate, 
    onDateSelected: (LocalDate) -> Unit, 
    lastStart: LocalDate?, 
    user: User,
    wellnessLogs: Map<String, List<String>>
) {
    // Generar días solo una vez y recordar
    val days = remember {
        val today = LocalDate.now()
        (-60..60).map { today.plusDays(it.toLong()) }
    }
    
    // Centrar en el día de hoy al inicio de forma eficiente
    val todayIndex = remember { days.indexOf(LocalDate.now()) }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = (todayIndex - 3).coerceAtLeast(0)
    )

    androidx.compose.foundation.lazy.LazyRow(
        state = lazyListState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(days, key = { it.toString() }) { date ->
            val isSelected = date == selectedDate
            val isToday = remember(date) { date == LocalDate.now() }
            val dayOfWeek = remember(date) { date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
            
            // Usar derivedStateOf para cálculos complejos dentro de items si es necesario, 
            // aunque aquí el cálculo es simple.
            val isPeriod = remember(lastStart, date, user.periodDuration) {
                lastStart != null && ChronoUnit.DAYS.between(lastStart, date) in 0 until user.periodDuration.toLong()
            }
            val ovulationDay = remember(lastStart, user.cycleLength) {
                lastStart?.plusDays((user.cycleLength - 14).toLong())
            }
            val isOvulation = remember(date, ovulationDay) { date == ovulationDay }
            val isFertile = remember(date, ovulationDay) {
                date != null && ovulationDay != null && 
                ChronoUnit.DAYS.between(ovulationDay, date) in -6..4
            }
            
            val activities = wellnessLogs[date.toString()] ?: emptyList()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = if (isToday) "HOY" else dayOfWeek,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        if (isOvulation || isFertile) Color.Cyan else LovePink
                    } else if (isToday) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isOvulation -> Color.Cyan
                                isSelected && (isOvulation || isFertile) -> Color.Cyan.copy(alpha = 0.1f)
                                isSelected -> LovePink.copy(alpha = 0.1f)
                                isToday -> Color.LightGray.copy(alpha = 0.4f)
                                else -> Color.Transparent
                            }
                        )
                        .then(
                            if (isPeriod) Modifier.border(1.dp, LovePink.copy(alpha = 0.5f), CircleShape)
                            else if (isOvulation) Modifier
                            else if (isFertile) Modifier.border(1.dp, Color.Cyan.copy(alpha = 0.5f), CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isOvulation -> Color.White
                            isSelected -> if (isFertile) Color.Cyan else LovePink
                            isPeriod -> LovePink
                            isFertile -> Color.Cyan
                            else -> if (isToday) MaterialTheme.colorScheme.onSurface else Color.Gray
                        }
                    )
                }
                
                if (activities.isNotEmpty()) {
                    DailyActivityIcons(activities)
                }
            }
        }
    }
}

@Composable
fun DailyActivityIcons(activities: List<String>) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val maxIcons = 2
        activities.take(maxIcons).forEach { id ->
            Icon(
                getActivityIcon(id), 
                null, 
                modifier = Modifier.size(10.dp), 
                tint = if (id.contains("desire")) Color.Gray else LovePink
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        if (activities.size > maxIcons) {
            Text(text = "+${activities.size - maxIcons}", fontSize = 8.sp, color = Color.Gray)
        }
    }
}

fun getActivityIcon(id: String): ImageVector {
    return when(id) {
        "no_sex" -> Icons.Default.Block
        "protected_sex" -> Icons.Default.Lock
        "unprotected_sex" -> Icons.Default.LockOpen
        "oral_sex" -> Icons.Default.Face
        "anal_sex" -> Icons.Default.Circle
        "masturbation" -> Icons.Default.TouchApp
        "sensual_contact" -> Icons.Default.Favorite
        "sex_toys" -> Icons.Default.CellTower
        "orgasm" -> Icons.Default.AutoAwesome
        "high_desire" -> Icons.Default.Favorite
        "neutral_desire", "low_desire" -> Icons.Default.FavoriteBorder
        else -> Icons.Default.Add
    }
}

@Composable
fun WellnessActionButton(icon: ImageVector, label: String, color: Color, tint: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = tint,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(icon, null)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun WellnessIntroView(strings: com.lexnicholls.lovecounter.util.Strings, onEnable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text(text = strings.wellnessDesc, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text = strings.trackPeriodQuestion, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onEnable, shape = RoundedCornerShape(16.dp)) {
            Text(strings.apply)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SexLogBottomSheet(
    user: User, 
    selectedDate: LocalDate,
    initialActivities: List<String>,
    onDismiss: () -> Unit, 
    onSave: (List<String>) -> Unit
) {
    val strings = t()
    val lastStart = user.lastPeriodStart?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
    val dayOfCycle = if (lastStart != null) (ChronoUnit.DAYS.between(lastStart, selectedDate) % user.cycleLength + 1).toInt() else 0

    val options = listOf(
        "no_sex" to (strings.noSex to Icons.Default.Block),
        "protected_sex" to (strings.protectedSex to Icons.Default.Lock),
        "unprotected_sex" to (strings.unprotectedSex to Icons.Default.LockOpen),
        "oral_sex" to (strings.oralSex to Icons.Default.Face),
        "anal_sex" to (strings.analSex to Icons.Default.Circle),
        "masturbation" to (strings.masturbation to Icons.Default.TouchApp),
        "sensual_contact" to (strings.sensualContact to Icons.Default.Favorite),
        "sex_toys" to (strings.sexToys to Icons.Default.CellTower),
        "orgasm" to (strings.orgasm to Icons.Default.AutoAwesome),
        "high_desire" to (strings.highDesire to Icons.Default.Favorite),
        "neutral_desire" to (strings.neutralDesire to Icons.Default.FavoriteBorder),
        "low_desire" to (strings.lowDesire to Icons.Default.FavoriteBorder)
    )

    var selectedSet by remember { mutableStateOf(initialActivities.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val titleText = when {
            selectedDate == LocalDate.now() -> strings.today
            selectedDate == LocalDate.now().minusDays(1) -> "Ayer"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.getDefault()))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null)
            }
            Text(text = titleText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(48.dp)) // Spacer for symmetry
        }

        if (dayOfCycle > 0) {
            Text(
                text = strings.cycleDay.format(dayOfCycle), 
                fontSize = 14.sp, 
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = strings.sexAndDesire, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { (id, pair) ->
                val (label, icon) = pair
                val isSelected = selectedSet.contains(id)
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            selectedSet = if (isSelected) selectedSet - id else selectedSet + id
                        }
                        .background(if (isSelected) LovePink.copy(alpha = 0.05f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) LovePink else Color.LightGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, modifier = Modifier.size(18.dp), tint = LovePink)
                        Spacer(Modifier.width(8.dp))
                        Text(text = label, fontSize = 13.sp)
                    }
                    
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(LovePink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onSave(selectedSet.toList()); onDismiss() }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = ButtonDefaults.buttonColors(containerColor = LovePink),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Registrar", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PeriodLoggerFullView(user: User, onDismiss: () -> Unit, onSave: (Date) -> Unit) {
    val strings = t()
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Asegura que los botones no queden detrás de la barra de sistema
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()) // Permite scroll si la pantalla es muy pequeña
    ) {
        Text(
            text = strings.logPeriodStart,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = strings.selectDate,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, null)
            }
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Grid
        val daysInMonth = selectedMonth.lengthOfMonth()
        val firstDayOfMonth = selectedMonth.atDay(1).dayOfWeek.value % 7
        val weekDays = strings.weekDays.split(",")

        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(240.dp), // Reducido un poco para dar más aire
            userScrollEnabled = false // El scroll lo maneja el Column padre
        ) {
            items(firstDayOfMonth) { Spacer(Modifier.fillMaxWidth()) }
            items(daysInMonth) { day ->
                val date = selectedMonth.atDay(day + 1)
                val isSelected = date == selectedDate
                val isPrediction = lastPredictedPeriod(user, date)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .clickable { selectedDate = date }
                        .background(
                            when {
                                isSelected -> LovePink
                                isPrediction -> LovePink.copy(alpha = 0.2f)
                                else -> Color.Transparent
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (day + 1).toString(),
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.cancel, color = LovePink, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    val date = Date.from(selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                    onSave(date)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LovePink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.save, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun lastPredictedPeriod(user: User, date: LocalDate): Boolean {
    val lastStart = user.lastPeriodStart?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate() ?: return false
    val daysSince = ChronoUnit.DAYS.between(lastStart, date)
    val cycleLength = if (user.cycleLength > 0) user.cycleLength else 28
    val dayInCycle = ((daysSince % cycleLength) + cycleLength) % cycleLength
    return dayInCycle < user.periodDuration
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
