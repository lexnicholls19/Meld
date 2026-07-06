package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScreen(
    userId: String,
    userName: String,
    onBack: () -> Unit
) {
    val strings = t()
    val loveViewModel: LoveViewModel = hiltViewModel()
    val currentUserProfile by loveViewModel.currentUserProfile
    val members by loveViewModel.members
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var showConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text(
                    text = strings.wellness,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

        // My Wellness Section
        if (currentUserProfile?.trackWellness == true) {
            WellnessCard(
                user = currentUserProfile!!,
                isMe = true,
                onLogPeriod = {
                    loveViewModel.updateWellnessData(
                        Timestamp.now(),
                        currentUserProfile!!.cycleLength,
                        currentUserProfile!!.periodDuration
                    )
                },
                onConfig = { showConfigDialog = true }
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text(text = strings.wellnessDesc, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text(text = strings.trackPeriodQuestion, fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { loveViewModel.updateWellnessPreferences(true, false) }) {
                        Text(strings.apply)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Partner Wellness Section
        val sharedMembers = members.filter { it.uid != currentUserId && it.trackWellness && it.shareWellness }
        if (sharedMembers.isNotEmpty()) {
            Text(
                text = strings.partner.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LovePink,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            sharedMembers.forEach { partner ->
                WellnessCard(user = partner, isMe = false)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

    if (showConfigDialog && currentUserProfile != null) {
        var cycleLength by remember { mutableIntStateOf(currentUserProfile!!.cycleLength) }
        var periodDuration by remember { mutableIntStateOf(currentUserProfile!!.periodDuration) }

        LoveAlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = strings.settings,
            onConfirm = {
                loveViewModel.updateWellnessData(
                    currentUserProfile!!.lastPeriodStart,
                    cycleLength,
                    periodDuration
                )
                showConfigDialog = false
            }
        ) {
            Column {
                Text(text = "${strings.intensity}: $cycleLength ${strings.days.lowercase()}", fontWeight = FontWeight.Bold)
                Slider(
                    value = cycleLength.toFloat(),
                    onValueChange = { cycleLength = it.toInt() },
                    valueRange = 20f..45f,
                    steps = 25
                )
                Spacer(Modifier.height(16.dp))
                Text(text = "${strings.duration}: $periodDuration ${strings.days.lowercase()}", fontWeight = FontWeight.Bold)
                Slider(
                    value = periodDuration.toFloat(),
                    onValueChange = { periodDuration = it.toInt() },
                    valueRange = 1f..15f,
                    steps = 14
                )
            }
        }
    }
}

@Composable
fun WellnessCard(
    user: com.lexnicholls.lovecounter.viewmodel.User,
    isMe: Boolean,
    onLogPeriod: (() -> Unit)? = null,
    onConfig: (() -> Unit)? = null
) {
    val strings = t()
    val lastStart = user.lastPeriodStart?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
    val today = LocalDate.now()
    
    val cycleInfo = remember(user.lastPeriodStart, user.cycleLength, user.periodDuration) {
        if (lastStart == null) null
        else {
            val nextStart = lastStart.plusDays(user.cycleLength.toLong())
            val daysUntil = ChronoUnit.DAYS.between(today, nextStart)
            val dayOfCycle = ChronoUnit.DAYS.between(lastStart, today) % user.cycleLength + 1
            val isDuringPeriod = dayOfCycle <= user.periodDuration && ChronoUnit.DAYS.between(lastStart, today) >= 0 && ChronoUnit.DAYS.between(lastStart, today) < user.periodDuration
            
            Triple(daysUntil, dayOfCycle, isDuringPeriod)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cycleInfo?.third == true) LovePink.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (cycleInfo?.third == true) LovePink.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = LovePink.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.WaterDrop, null, tint = LovePink, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isMe) strings.me else user.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (isMe && onConfig != null) {
                    IconButton(onClick = onConfig) {
                        Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (cycleInfo != null) {
                val (daysUntil, dayOfCycle, isDuringPeriod) = cycleInfo
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDuringPeriod) strings.periodStatus else strings.nextPeriodIn,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = if (isDuringPeriod) "${strings.days} $dayOfCycle" else "$daysUntil ${strings.days.lowercase()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDuringPeriod) LovePink else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Simple progress circle for cycle
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                        CircularProgressIndicator(
                            progress = { (dayOfCycle.toFloat() / user.cycleLength.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = LovePink,
                            strokeWidth = 6.dp,
                            trackColor = LovePink.copy(alpha = 0.1f),
                        )
                        Text(text = "$dayOfCycle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(text = strings.noDataRecorded, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            if (isMe && onLogPeriod != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onLogPeriod,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LovePink)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.logPeriodStart)
                }
            }
        }
    }
}
