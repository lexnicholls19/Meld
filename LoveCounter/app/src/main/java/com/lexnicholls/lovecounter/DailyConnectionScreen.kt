package com.lexnicholls.lovecounter

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun DailyConnectionScreen(deviceId: String = "", userName: String = "") {
    val strings = t()
    val db = remember { FirebaseFirestore.getInstance() }
    val today = LocalDate.now()
    val dateString = today.format(DateTimeFormatter.ISO_DATE)
    
    var dailyQuestion by rememberSaveable { mutableStateOf("...") }
    
    LaunchedEffect(strings) {
        dailyQuestion = strings.loading
    }
    var isAnsweredToday by rememberSaveable { mutableStateOf(false) }
    var weeklyStatus by remember { mutableStateOf<List<Pair<LocalDate, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // Real-time listener for today's status
    DisposableEffect(refreshKey) {
        val registration = db.collection("daily_questions").document(dateString)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    isAnsweredToday = snapshot.getBoolean("isAnswered") ?: false
                }
            }
        onDispose { registration.remove() }
    }

    LaunchedEffect(refreshKey) {
        try {
            // 1. Fetch Weekly Status (Solo para el streak)
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }
            
            val weekSnapshots = db.collection("daily_questions")
                .whereGreaterThanOrEqualTo("__name__", weekDates.first().format(DateTimeFormatter.ISO_DATE))
                .whereLessThanOrEqualTo("__name__", weekDates.last().format(DateTimeFormatter.ISO_DATE))
                .get().await()
            
            val statusMap = weekSnapshots.documents.associate { 
                it.id to (it.getBoolean("isAnswered") ?: false)
            }
            
            weeklyStatus = weekDates.map { date ->
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val answered = statusMap[dateStr] ?: false
                val emoji = when {
                    answered -> "🔥"
                    date.isBefore(today) -> "💔"
                    else -> "⚪"
                }
                date to emoji
            }

            // 2. Lógica Local de Pregunta (Basada en el día del año para sincronización)
            val allQuestions = QuestionsRepository.getAllQuestions()
            val dayOfYear = today.dayOfYear
            val questionIndex = (dayOfYear - 1) % allQuestions.size
            
            // Comprobación de 31 de Diciembre
            val isLastDayOfYear = today.monthValue == 12 && today.dayOfMonth == 31
            dailyQuestion = if (isLastDayOfYear) {
                "[💭] Al llegar a la pregunta 365 de este recorrido, y mirando hacia adelante, ¿qué promesa importante te gustaría hacerme para todo el año que tenemos por delante juntos?"
            } else {
                allQuestions[questionIndex]
            }

            // 3. Comprobar si ya fue contestada hoy en Firebase (Snapshot se encarga del real-time)
            try {
                val qDoc = db.collection("daily_questions").document(dateString).get().await()
                if (!qDoc.exists()) {
                    // Inicializar el registro de respuesta para hoy si no existe
                    db.collection("daily_questions").document(dateString).set(
                        hashMapOf("isAnswered" to false)
                    )
                }
            } catch (e: Exception) {
                Log.e("DailyConnection", "Error inicializando documento diario", e)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DailyConnection", "Critical error in screen logic", e)
        } finally {
            isLoading = false
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Centrado vertical para tablets
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Weekly Streak View
            WeeklyStreakHeader(weeklyStatus)

            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Pregunta del Día",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dailyQuestion,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (isAnsweredToday) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.answered, color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                if (!isAnsweredToday) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            db.collection("daily_questions").document(dateString)
                                .update(
                                    "isAnswered", true,
                                    "senderId", deviceId
                                )
                                .addOnSuccessListener {
                                    isAnsweredToday = true
                                    refreshKey++ // Actualiza el streak
                                    
                                    // Notificar a la pareja
                                    val notification = hashMapOf(
                                        "name" to strings.daily,
                                        "value" to "¡$userName! 🔥",
                                        "timestamp" to com.google.firebase.Timestamp.now(),
                                        "senderName" to userName,
                                        "userName" to userName,
                                        "senderId" to deviceId,
                                        "deviceId" to deviceId
                                    )
                                    db.collection("quick_messages").add(notification)
                                }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(strings.answerQuestion, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = strings.talkAboutThis,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WeeklyStreakHeader(status: List<Pair<LocalDate, String>>) {
    val strings = t()
    val daysInitials = if (strings.weekDays.contains(",")) {
        strings.weekDays.split(",").map { it.trim() }
    } else {
        listOf("L", "M", "M", "J", "V", "S", "D")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            status.forEachIndexed { index, pair ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = daysInitials[index],
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pair.first == LocalDate.now()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Text(
                        text = pair.first.dayOfMonth.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (pair.first == LocalDate.now()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = pair.second,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
