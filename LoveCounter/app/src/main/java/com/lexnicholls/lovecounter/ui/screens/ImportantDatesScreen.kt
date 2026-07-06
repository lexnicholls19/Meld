package com.lexnicholls.lovecounter.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.ui.components.LoveUndoSnackbar
import com.lexnicholls.lovecounter.ui.theme.DatesColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImportantDatesScreen(
    deviceId: String,
    userName: String,
    userId: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val strings = t()
    
    var dates by remember { mutableStateOf<List<DateItem>>(emptyList()) }
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(userId) {
        val collection = db.collection("users").document(userId).collection("important_dates")
        val registration = collection
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    dates = snapshot.documents.mapNotNull { doc ->
                        DateItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = doc.getTimestamp("date") ?: Timestamp.now(),
                            addedBy = doc.getString("addedBy") ?: strings.someone
                        )
                    }
                }
            }
        onDispose { registration.remove() }
    }

    // Logic for Add/Edit remains the same but with modern UI tweaks...
    var editingItem by remember { mutableStateOf<DateItem?>(null) }
    if (editingItem != null && !isSelectionMode) {
        var title by remember { mutableStateOf(editingItem!!.title) }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = editingItem!!.date.toDate().time)
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.confirm) } },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) } }
            ) { DatePicker(state = dateState) }
        }

        LoveAlertDialog(
            onDismissRequest = { editingItem = null },
            title = strings.edit,
            onConfirm = {
                if (title.isNotBlank() && dateState.selectedDateMillis != null) {
                    db.collection("users").document(userId).collection("important_dates")
                        .document(editingItem!!.id)
                        .update(mapOf("title" to title, "date" to Timestamp(Date(dateState.selectedDateMillis!!))))
                    editingItem = null
                }
            }
        ) {
            Column {
                LoveTextField(value = title, onValueChange = { title = it }, label = strings.movieTitle)
                Spacer(Modifier.height(16.dp))
                val dateDisplay = dateState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: strings.selectDate
                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, tint = DatesColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(strings.selectDate, fontSize = 12.sp, color = Color.Gray)
                            Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        val dateState = rememberDatePickerState()
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.confirm) } },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) } }
            ) { DatePicker(state = dateState) }
        }

        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            title = strings.add,
            onConfirm = {
                if (title.isNotBlank() && dateState.selectedDateMillis != null) {
                    val item = hashMapOf("title" to title, "date" to Timestamp(Date(dateState.selectedDateMillis!!)), "timestamp" to Timestamp.now(), "addedBy" to userName)
                    db.collection("users").document(userId).collection("important_dates").add(item)
                        .addOnSuccessListener { onDismissDialog() }
                }
            }
        ) {
            Column {
                LoveTextField(value = title, onValueChange = { title = it }, label = strings.movieTitle)
                Spacer(Modifier.height(16.dp))
                val dateDisplay = dateState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: strings.selectDate
                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, tint = DatesColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(strings.selectDate, fontSize = 12.sp, color = Color.Gray)
                            Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isSelectionMode) {
                SelectionHeader(selectedCount = selectedIds.size, onCancel = { selectedIds.clear() }) {
                    val batch = db.batch()
                    selectedIds.forEach { id -> batch.delete(db.collection("users").document(userId).collection("important_dates").document(id)) }
                    batch.commit().addOnSuccessListener { selectedIds.clear() }
                }
            } else {
                Text(text = strings.dates, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            }

            val filteredDates = remember(dates, pendingDeleteId) {
                dates.filter { it.id != pendingDeleteId }
            }
            val sortedDates = remember(filteredDates) { filteredDates.sortedBy { calculateDaysRemaining(it.date) } }

            if (sortedDates.isEmpty()) {
                EmptyDatesState(strings.noItemsYet, strings.importantDatesEmpty)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Section: El más próximo
                    val nextEvent = sortedDates.first()
                    item(key = "hero") {
                        HeroDateCard(item = nextEvent, onClick = { editingItem = nextEvent })
                    }

                    // Grupos
                    val thisMonth = sortedDates.filter { calculateDaysRemaining(it.date) <= 31 && it != nextEvent }
                    val later = sortedDates.filter { calculateDaysRemaining(it.date) > 31 }

                    if (thisMonth.isNotEmpty()) {
                        item { SectionHeader(strings.today.replace("!", "")) }
                        items(thisMonth, key = { it.id }) { item ->
                            DateRowModern(
                                item = item,
                                isSelected = selectedIds.contains(item.id),
                                onDelete = { 
                                    pendingDeleteId?.let { oldId ->
                                        db.collection("users").document(userId).collection("important_dates").document(oldId).delete()
                                    }
                                    pendingDeleteId = item.id 
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        if (selectedIds.contains(item.id)) selectedIds.remove(item.id) else selectedIds.add(item.id)
                                    } else editingItem = item
                                },
                                onLongClick = { if (!isSelectionMode) selectedIds.add(item.id) }
                            )
                        }
                    }

                    if (later.isNotEmpty()) {
                        item { SectionHeader(strings.comingSoon) }
                        items(later, key = { it.id }) { item ->
                            DateRowModern(
                                item = item,
                                isSelected = selectedIds.contains(item.id),
                                onDelete = { 
                                    pendingDeleteId?.let { oldId ->
                                        db.collection("users").document(userId).collection("important_dates").document(oldId).delete()
                                    }
                                    pendingDeleteId = item.id 
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        if (selectedIds.contains(item.id)) selectedIds.remove(item.id) else selectedIds.add(item.id)
                                    } else editingItem = item
                                },
                                onLongClick = { if (!isSelectionMode) selectedIds.add(item.id) }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        LoveUndoSnackbar(
            show = pendingDeleteId != null,
            onUndo = { pendingDeleteId = null },
            onDismiss = {
                pendingDeleteId?.let { id ->
                    db.collection("users").document(userId).collection("important_dates").document(id).delete()
                }
                pendingDeleteId = null
            }
        )
    }
}

@Composable
fun HeroDateCard(item: DateItem, onClick: () -> Unit) {
    val strings = t()
    val days = calculateDaysRemaining(item.date)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Fondo decorativo sutil
            Icon(
                Icons.Default.Celebration,
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp),
                tint = DatesColor.copy(alpha = 0.05f)
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Surface(
                    color = DatesColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DatesColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (days == 0L) strings.today.uppercase() else strings.daysLeftShort.format(days).uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DatesColor
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = item.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Instant.ofEpochMilli(item.date.toDate().time).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("d MMMM")),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.DateRowModern(
    item: DateItem,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val days = calculateDaysRemaining(item.date)
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Solo mostramos el fondo si el estado no está en reposo
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled || dismissState.currentValue != SwipeToDismissBoxValue.Settled
            if (isSwiping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 20.dp)
                    )
                }
            }
        },
        modifier = Modifier.animateItem()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono circular estilizado
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = DatesColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (days <= 7) Icons.Default.AutoAwesome else Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = DatesColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = Instant.ofEpochMilli(item.date.toDate().time).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (days == 0L) "🎉" else "${days}d",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (days <= 7) DatesColor else Color.Gray
                    )
                    if (days > 0) {
                        Text(text = "faltan", fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.6f))
                    }
                }
                
                if (isSelected) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SelectionHeader(selectedCount: Int, onCancel: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null) }
            Text(text = "$selectedCount seleccionados", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
    }
}

@Composable
fun EmptyDatesState(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.EventBusy, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text(text = title, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, color = Color.Gray.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

fun calculateDaysRemaining(dateTimestamp: Timestamp): Long {
    val today = LocalDate.now()
    val originalDate = Instant.ofEpochMilli(dateTimestamp.toDate().time).atZone(ZoneOffset.UTC).toLocalDate()
    var nextOccurrence = originalDate.withYear(today.year)
    if (nextOccurrence.isBefore(today)) nextOccurrence = nextOccurrence.plusYears(1)
    return ChronoUnit.DAYS.between(today, nextOccurrence)
}

data class DateItem(
    val id: String,
    val title: String,
    val date: Timestamp,
    val addedBy: String
)
