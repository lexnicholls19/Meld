package com.lexnicholls.lovecounter.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import com.lexnicholls.lovecounter.viewmodel.User
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WellnessCalendarScreen(
    onBack: () -> Unit
) {
    val strings = t()
    val loveViewModel: LoveViewModel = hiltViewModel()
    val currentUserProfile by loveViewModel.currentUserProfile
    val members by loveViewModel.members
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    val sharedMembers = members.filter { it.uid != currentUserId && it.trackWellness && it.shareWellness }
    
    val displayUser = remember(currentUserProfile, sharedMembers) {
        if (currentUserProfile?.trackWellness == true) currentUserProfile else sharedMembers.firstOrNull()
    }

    if (displayUser == null) {
        if (currentUserProfile == null || (currentUserProfile?.trackWellness == false && members.isEmpty())) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LovePink)
            }
            return
        }
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var isYearView by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Asegurar que al abrir la vista de mes, sea el mes actual o el seleccionado
    LaunchedEffect(Unit) {
        currentMonth = YearMonth.from(selectedDate)
    }

    LaunchedEffect(displayUser.uid) {
        loveViewModel.observeWellnessLogs(displayUser.uid)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) }
                Spacer(modifier = Modifier.weight(1f))
                
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !isYearView,
                        onClick = { isYearView = false },
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    ) { Text(strings.month) }
                    SegmentedButton(
                        selected = isYearView,
                        onClick = { isYearView = true },
                        shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                    ) { Text(strings.year) }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                // Retiramos el botón de configuración aquí (IconButton de Settings eliminado)
                Box(modifier = Modifier.size(48.dp)) // Espaciador para mantener simetría
            }

            Spacer(modifier = Modifier.height(24.dp))

        if (isYearView) {
            YearView(user = displayUser)
        } else {
            val logs by loveViewModel.wellnessLogs
            MonthViewWithPrediction(
                month = currentMonth, 
                user = displayUser,
                onMonthChange = { currentMonth = it },
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                wellnessLogs = logs
            )
        }
        }
    }
}

@Composable
fun MonthViewWithPrediction(
    month: YearMonth, 
    user: User, 
    onMonthChange: (YearMonth) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    wellnessLogs: Map<String, List<String>>
) {
    val strings = t()
    val lastStart = user.lastPeriodStart?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) { Icon(Icons.Default.ChevronLeft, null) }
            Text(text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) { Icon(Icons.Default.ChevronRight, null) }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val daysInMonth = month.lengthOfMonth()
        val firstDayOfMonth = month.atDay(1).dayOfWeek.value % 7
        val weekDays = strings.weekDays.split(",")

        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth()) {
            items(firstDayOfMonth) { Spacer(Modifier.fillMaxWidth()) }
            items(daysInMonth) { day ->
                val date = month.atDay(day + 1)
                val dayState = getDayState(date, lastStart, user)
                val isSelected = date == selectedDate

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .clickable { onDateSelected(date) }, 
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape))
                    }
                    
                    when (dayState) {
                        DayState.Period -> Box(modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape).background(LovePink))
                        DayState.PredictedPeriod -> Box(modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape).background(LovePink.copy(alpha = 0.1f)).border(1.dp, LovePink, CircleShape))
                        DayState.Ovulation -> Box(modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape).background(Color.Cyan.copy(alpha = 0.1f)).border(1.dp, Color.Cyan, CircleShape))
                        DayState.Fertile -> Box(modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape).border(1.dp, Color.Cyan.copy(alpha = 0.3f), CircleShape))
                        else -> {}
                    }
                    Text(
                        text = (day + 1).toString(), 
                        fontSize = 16.sp,
                        fontWeight = if (dayState != DayState.None || isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when (dayState) {
                            DayState.Period -> Color.White
                            DayState.PredictedPeriod -> LovePink
                            DayState.Ovulation, DayState.Fertile -> Color.Cyan
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Panel de detalles del día seleccionado
        DayDetailPanel(selectedDate, lastStart, user, wellnessLogs[selectedDate.toString()] ?: emptyList())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayDetailPanel(date: LocalDate, lastStart: LocalDate?, user: User, activities: List<String>) {
    val strings = t()
    val dayState = getDayState(date, lastStart, user)
    var selectedActivityLabel by remember(date) { mutableStateOf<String?>(null) }
    
    // Calcular información detallada
    val cycleLength = if (user.cycleLength > 0) user.cycleLength else 28
    val daysSinceStart = if (lastStart != null) ChronoUnit.DAYS.between(lastStart, date) else 0L
    val dayOfCycle = if (lastStart != null) ((daysSinceStart % cycleLength) + 1).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.getDefault())),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (lastStart != null) {
            Text(
                text = strings.cycleDay.format(dayOfCycle),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (dayState == DayState.Period || dayState == DayState.PredictedPeriod) LovePink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            val probability = when (dayState) {
                DayState.Ovulation -> strings.ovulationDay
                DayState.Fertile -> strings.fertileDay
                else -> strings.lowPregnancyProbability
            }
            
            Text(
                text = probability,
                fontSize = 14.sp,
                color = if (dayState == DayState.Ovulation || dayState == DayState.Fertile) Color.Cyan else Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (activities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = strings.registeredActivities, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Usamos FlowRow para que los iconos continúen debajo si no caben en una fila
                FlowRow(
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { selectedActivityLabel = null },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activities.forEach { id ->
                        val label = getActivityLabel(id, strings)
                        val isSelected = selectedActivityLabel == label
                        
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (id.contains("desire")) Color.Gray.copy(alpha = 0.1f) else LovePink.copy(alpha = 0.1f))
                                    .clickable { 
                                        selectedActivityLabel = if (isSelected) null else label 
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getActivityIcon(id), 
                                    null, 
                                    modifier = Modifier.size(22.dp), 
                                    tint = if (id.contains("desire")) Color.Gray else LovePink
                                )
                            }
                            
                            if (isSelected) {
                                Popup(
                                    alignment = Alignment.BottomCenter,
                                    offset = IntOffset(0, with(LocalDensity.current) { 48.dp.roundToPx() }),
                                    onDismissRequest = { selectedActivityLabel = null },
                                    properties = PopupProperties(dismissOnClickOutside = true)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shadowElevation = 4.dp
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Empuja el botón al fondo

            Button(
                onClick = { /* Navegar a edición */ },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LovePink),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(text = strings.editPeriodDates, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(text = strings.noDataRecorded, color = Color.Gray)
        }
    }
}

@Composable
fun YearView(user: User) {
    val currentYear = LocalDate.now().year
    val years = listOf(currentYear, currentYear + 1)
    val lastStart = remember(user.lastPeriodStart) {
        user.lastPeriodStart?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
    }

    // Usar LazyColumn para mejor rendimiento y control de scroll
    val lazyListState = rememberLazyListState()
    val currentMonthIdx = remember { LocalDate.now().monthValue - 1 }
    
    // Calculamos el índice de la fila: cada fila tiene 2 meses
    val targetRowIndex = remember { (currentMonthIdx / 2) }

    LaunchedEffect(Unit) {
        // Hacemos scroll directo a la fila que contiene el mes actual
        // +1 para saltar el encabezado del año si es necesario, o ajustar según items
        lazyListState.scrollToItem(targetRowIndex + 1) 
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        years.forEach { year ->
            item {
                Text(
                    text = year.toString(), 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 32.sp, 
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            }
            
            val monthRows = (0 until 6).toList()
            items(monthRows.size) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (col in 0 until 2) {
                        val monthIdx = row * 2 + col
                        val month = YearMonth.of(year, monthIdx + 1)
                        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase()
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = monthName, 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OptimizedMonthCanvas(month = month, lastStart = lastStart, user = user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptimizedMonthCanvas(month: YearMonth, lastStart: LocalDate?, user: User) {
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfMonth = month.atDay(1).dayOfWeek.value % 7
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    val dayStates = remember(month, lastStart, user) {
        (1..daysInMonth).map { day ->
            getDayState(month.atDay(day), lastStart, user)
        }
    }

    val density = LocalDensity.current
    val textPaint = remember(onSurfaceColor) {
        Paint().apply {
            color = onSurfaceColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 11.sp.toPx() }
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
    }
    
    val textPaintBold = remember(onSurfaceColor) {
        Paint().apply {
            color = onSurfaceColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 11.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.1f)) {
        val cellWidth = size.width / 7
        for (dayIdx in 0 until daysInMonth) {
            val cellIndex = dayIdx + firstDayOfMonth
            val row = cellIndex / 7
            val col = cellIndex % 7
            
            val centerX = col * cellWidth + cellWidth / 2
            val centerY = row * cellWidth + cellWidth / 2
            
            val dayState = dayStates[dayIdx]
            val radius = cellWidth * 0.42f
            
            if (dayState != DayState.None) {
                val color = when (dayState) {
                    DayState.Period -> LovePink
                    DayState.PredictedPeriod -> LovePink.copy(alpha = 0.6f)
                    DayState.Ovulation -> Color.Cyan
                    DayState.Fertile -> Color.Cyan.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
                
                if (dayState == DayState.Period) {
                    drawCircle(
                        color = color, 
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )
                } else {
                    drawCircle(
                        color = color, 
                        radius = radius, 
                        style = Stroke(width = 1.25.dp.toPx()),
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )
                }
            }
            
            val dayStr = (dayIdx + 1).toString()
            val paint = if (dayState != DayState.None) {
                if (dayState == DayState.Period) {
                    textPaintBold.apply { color = android.graphics.Color.WHITE }
                } else {
                    textPaintBold.apply { 
                        color = when(dayState) {
                            DayState.PredictedPeriod -> LovePink.toArgb()
                            DayState.Ovulation, DayState.Fertile -> Color.Cyan.toArgb()
                            else -> onSurfaceColor.toArgb()
                        }
                    }
                }
            } else {
                textPaint.apply { color = onSurfaceColor.copy(alpha = 0.8f).toArgb() }
            }
            
            drawIntoCanvas { canvas ->
                val fontMetrics = paint.fontMetrics
                val textY = centerY + (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
                canvas.nativeCanvas.drawText(dayStr, centerX, textY, paint)
            }
        }
    }
}

enum class DayState { None, Period, PredictedPeriod, Fertile, Ovulation }

fun getDayState(date: LocalDate, lastStart: LocalDate?, user: User): DayState {
    if (lastStart == null) return DayState.None
    
    val daysSince = ChronoUnit.DAYS.between(lastStart, date)
    val cycleLength = if (user.cycleLength > 0) user.cycleLength else 28
    val dayInCycle = ((daysSince % cycleLength) + cycleLength) % cycleLength
    
    val isFuture = date.isAfter(LocalDate.now())
    val isCurrentOrPast = !isFuture
    
    return when {
        dayInCycle < user.periodDuration -> {
            if (isCurrentOrPast && daysSince >= 0 && daysSince < user.periodDuration) DayState.Period 
            else DayState.PredictedPeriod
        }
        dayInCycle.toInt() == (cycleLength - 14) -> DayState.Ovulation
        // Ventana fértil: 6 días antes y 4 días después de la ovulación
        dayInCycle.toInt() in (cycleLength - 14 - 6)..(cycleLength - 14 + 4) -> DayState.Fertile
        else -> DayState.None
    }
}

fun getActivityLabel(id: String, strings: com.lexnicholls.lovecounter.util.Strings): String {
    return when(id) {
        "no_sex" -> strings.noSex
        "protected_sex" -> strings.protectedSex
        "unprotected_sex" -> strings.unprotectedSex
        "oral_sex" -> strings.oralSex
        "anal_sex" -> strings.analSex
        "masturbation" -> strings.masturbation
        "sensual_contact" -> strings.sensualContact
        "sex_toys" -> strings.sexToys
        "orgasm" -> strings.orgasm
        "high_desire" -> strings.highDesire
        "neutral_desire" -> strings.neutralDesire
        "low_desire" -> strings.lowDesire
        else -> ""
    }
}

