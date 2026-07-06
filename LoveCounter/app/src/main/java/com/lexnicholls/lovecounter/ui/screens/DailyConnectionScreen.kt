package com.lexnicholls.lovecounter.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.data.repository.QuestionsRepository
import com.lexnicholls.lovecounter.ui.theme.DailyColor
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun DailyConnectionScreen(
    deviceId: String,
    userName: String,
    sharedId: String
) {
    val context = LocalContext.current
    val strings = t()
    val db = FirebaseFirestore.getInstance()
    
    var skippedQuestion by remember(sharedId) { mutableStateOf<String?>(null) }
    val dailyQuestion = QuestionsRepository.getDailyQuestion(sharedId)
    val currentQuestion = skippedQuestion ?: dailyQuestion

    var completedDates by remember { mutableStateOf(setOf<String>()) }
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ISO_DATE)
    
    val relationRef = db.collection("relations").document(sharedId)
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    LaunchedEffect(sharedId) {
        if (sharedId.isBlank()) return@LaunchedEffect
        relationRef.collection("daily_completions")
            .whereGreaterThanOrEqualTo("__name__", weekDates.first().format(DateTimeFormatter.ISO_DATE))
            .whereLessThanOrEqualTo("__name__", weekDates.last().format(DateTimeFormatter.ISO_DATE))
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val completed = snapshot.documents.map { it.id }.toSet()
                    completedDates = completed
                }
            }
    }

    var showHistory by remember { mutableStateOf(false) }
    var historyQuestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text(strings.answeredQuestions) },
            text = {
                if (historyQuestions.isEmpty()) {
                    Text(strings.noQuestionsAnswered)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(historyQuestions) { (date, question) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = date, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = question, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text(strings.close) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.daily,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    relationRef.collection("daily_completions")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            historyQuestions = snapshot.documents.mapNotNull { doc ->
                                val question = doc.getString("question") ?: ""
                                if (question.isNotBlank()) {
                                    doc.id to question
                                } else null
                            }
                            showHistory = true
                        }
                }
            ) {
                Icon(Icons.Default.History, null, tint = LovePink)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- WEEKLY CALENDAR (GLASS STYLE) ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
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
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isToday) LovePink else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        when {
                            isCompleted -> {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            isToday -> {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(LovePink, CircleShape)
                                )
                            }
                            isFuture -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.HeartBroken,
                                    contentDescription = null,
                                    tint = Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.4f))

        // --- DECORATIVE ICON ---
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = LovePink.copy(alpha = 0.05f),
                modifier = Modifier.size(180.dp)
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = LovePink.copy(alpha = 0.8f),
                modifier = Modifier.size(72.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // --- QUESTION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(32.dp)) {
                Text(
                    text = currentQuestion,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // --- MAIN BUTTON ---
        val isAlreadyAnswered = completedDates.contains(todayStr)
        
        Button(
            onClick = {
                if (!isAlreadyAnswered) {
                    relationRef.collection("daily_completions").document(todayStr).set(mapOf(
                        "completed" to true,
                        "question" to currentQuestion,
                        "timestamp" to Timestamp.now()
                    ))
                    
                    val notification = hashMapOf(
                        "name" to strings.daily,
                        "value" to String.format(strings.userActionNotification, userName),
                        "timestamp" to Timestamp.now(),
                        "senderName" to userName,
                        "userName" to userName,
                        "senderId" to deviceId,
                        "deviceId" to deviceId,
                        "senderUid" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                        "relationId" to sharedId,
                        "targetTopic" to "relation_$sharedId"
                    )
                    relationRef.collection("quick_messages").add(notification)
                    Toast.makeText(context, strings.answered, Toast.LENGTH_SHORT).show()
                } else {
                    skippedQuestion = QuestionsRepository.getRandomQuestion(sharedId)
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAlreadyAnswered) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else LovePink,
                contentColor = if (isAlreadyAnswered) MaterialTheme.colorScheme.secondary else Color.White
            )
        ) {
            Icon(if (isAlreadyAnswered) Icons.Default.Refresh else Icons.Default.Check, null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isAlreadyAnswered) strings.seeAnotherQuestion else strings.answerQuestion,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = strings.talkAboutThis,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        )
    }
}
