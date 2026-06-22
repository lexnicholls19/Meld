package com.lexnicholls.lovecounter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.data.repository.QuestionsRepository
import com.lexnicholls.lovecounter.util.t
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun DailyConnectionScreen(
    deviceId: String,
    userName: String
) {
    val context = LocalContext.current
    val strings = t()
    val db = FirebaseFirestore.getInstance()
    
    var currentQuestion by remember { mutableStateOf(QuestionsRepository.getDailyQuestion()) }
    var completedDates by remember { mutableStateOf(setOf<String>()) }
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ISO_DATE)
    
    // Obtener las fechas de la semana actual (Lunes a Domingo)
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    // Cargar estados de completado de Firebase
    LaunchedEffect(Unit) {
        db.collection("daily_completions")
            .whereGreaterThanOrEqualTo("__name__", weekDates.first().format(DateTimeFormatter.ISO_DATE))
            .whereLessThanOrEqualTo("__name__", weekDates.last().format(DateTimeFormatter.ISO_DATE))
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val completed = snapshot.documents.map { it.id }.toSet()
                    completedDates = completed
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calendario Semanal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val dayLetters = strings.weekDays.split(",")
            weekDates.forEachIndexed { index, date ->
                val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                val isCompleted = completedDates.contains(dateStr)
                val isFuture = date.isAfter(today)
                val isToday = date.isEqual(today)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayLetters[index],
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) MaterialTheme.colorScheme.primary else Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    when {
                        isCompleted -> {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        isToday -> {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                        }
                        isFuture -> {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                        else -> { // Pasado no completado
                            Icon(
                                imageVector = Icons.Default.HeartBroken,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Corazón central y título
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color(0xFFD1C4E9), // Púrpura claro como en la imagen
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = strings.daily,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Tarjeta de la pregunta
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C3E) // Fondo oscuro de la tarjeta
            )
        ) {
            Text(
                text = currentQuestion,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.padding(24.dp),
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Botón principal
        val isAlreadyAnswered = completedDates.contains(todayStr)
        
        Button(
            onClick = {
                if (!isAlreadyAnswered) {
                    // Marcar como contestada en Firebase
                    db.collection("daily_completions").document(todayStr).set(mapOf("completed" to true))
                    
                    // Enviar notificación a la pareja
                    val notification = hashMapOf(
                        "name" to strings.daily,
                        "value" to String.format(strings.userActionNotification, userName),
                        "timestamp" to Timestamp.now(),
                        "senderName" to userName,
                        "userName" to userName,
                        "senderId" to deviceId,
                        "deviceId" to deviceId
                    )
                    db.collection("quick_messages").add(notification)
                    
                    Toast.makeText(context, strings.answered, Toast.LENGTH_SHORT).show()
                } else {
                    currentQuestion = QuestionsRepository.getRandomQuestion()
                }
            },
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAlreadyAnswered) Color.Gray else Color(0xFFE91E63) // Rosa vibrante
            )
        ) {
            Text(
                text = if (isAlreadyAnswered) strings.seeAnotherQuestion else strings.answerQuestion,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = strings.talkAboutThis,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}
