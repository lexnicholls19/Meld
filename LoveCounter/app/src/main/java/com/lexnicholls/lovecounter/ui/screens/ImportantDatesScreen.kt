package com.lexnicholls.lovecounter.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
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
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: "global"
    
    var dates by remember { mutableStateOf<List<DateItem>>(emptyList()) }

    // Multi-selection state
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }

    DisposableEffect(userId) {
        val collection = db.collection("users").document(userId).collection("important_dates")
        val registration = collection
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error listening to important dates", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    dates = snapshot.documents.mapNotNull { doc ->
                        DateItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = doc.getTimestamp("date") ?: Timestamp.now(),
                            addedBy = doc.getString("addedBy") ?: "Alguien"
                        )
                    }
                }
            }
        onDispose { registration.remove() }
    }

    // Edit Dialog
    var editingItem by remember { mutableStateOf<DateItem?>(null) }
    if (editingItem != null && !isSelectionMode) {
        var title by remember { mutableStateOf(editingItem!!.title) }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = editingItem!!.date.toDate().time
        )
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(t().confirm) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(t().cancel) }
                }
            ) {
                DatePicker(state = dateState)
            }
        }

        LoveAlertDialog(
            onDismissRequest = { editingItem = null },
            title = t().edit,
            onConfirm = {
                if (title.isNotBlank() && dateState.selectedDateMillis != null) {
                    db.collection("users").document(userId).collection("important_dates")
                        .document(editingItem!!.id)
                        .update(mapOf(
                            "title" to title,
                            "date" to Timestamp(Date(dateState.selectedDateMillis!!))
                        ))
                    editingItem = null
                }
            }
        ) {
            Column {
                LoveTextField(value = title, onValueChange = { title = it }, label = t().movieTitle)
                Spacer(Modifier.height(16.dp))
                
                val dateDisplay = dateState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: t().selectDate

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, null, tint = DatesColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(t().selectDate, fontSize = 12.sp, color = Color.Gray)
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
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(t().confirm) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(t().cancel) }
                }
            ) {
                DatePicker(state = dateState)
            }
        }

        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            title = t().add,
            onConfirm = {
                if (title.isNotBlank() && dateState.selectedDateMillis != null) {
                    val item = hashMapOf(
                        "title" to title,
                        "date" to Timestamp(Date(dateState.selectedDateMillis!!)),
                        "timestamp" to Timestamp.now(),
                        "addedBy" to userName
                    )
                    db.collection("users").document(userId).collection("important_dates").add(item)
                        .addOnSuccessListener { onDismissDialog() }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        ) {
            Column {
                LoveTextField(value = title, onValueChange = { title = it }, label = t().movieTitle)
                Spacer(Modifier.height(16.dp))

                val dateDisplay = dateState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: t().selectDate

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, null, tint = DatesColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(t().selectDate, fontSize = 12.sp, color = Color.Gray)
                            Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isSelectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedIds.clear() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                    Text(text = "${selectedIds.size} seleccionados", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = {
                        val batch = db.batch()
                        selectedIds.forEach { id ->
                            val docRef = db.collection("users").document(userId).collection("important_dates").document(id)
                            batch.delete(docRef)
                        }
                        batch.commit()
                        selectedIds.clear()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red)
                    }
                }
            }
        } else {
            Text(text = t().dates, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dates, key = { it.id }) { item ->
                val isSelected = selectedIds.contains(item.id)
                DateRow(
                    item = item,
                    isSelected = isSelected,
                    onDelete = {
                        db.collection("users").document(userId).collection("important_dates")
                            .document(item.id).delete()
                    },
                    onClick = {
                        if (isSelectionMode) {
                            if (isSelected) selectedIds.remove(item.id) else selectedIds.add(item.id)
                        } else {
                            editingItem = item
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) selectedIds.add(item.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DateRow(
    item: DateItem,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF44336),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2E)
            ),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador lateral (Tooltip color)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(DatesColor)
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            val date = Instant.ofEpochMilli(item.date.toDate().time)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Countdown of days remaining
                    val today = LocalDate.now()
                    val originalDate = Instant.ofEpochMilli(item.date.toDate().time)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    
                    // Logic to find the NEXT occurrence of the date (anniversary/birthday)
                    var nextOccurrence = originalDate.withYear(today.year)
                    if (nextOccurrence.isBefore(today)) {
                        nextOccurrence = nextOccurrence.plusYears(1)
                    }
                    
                    val daysRemaining = ChronoUnit.DAYS.between(today, nextOccurrence)
                    
                    Surface(
                        color = DatesColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "faltan $daysRemaining d",
                            color = DatesColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

data class DateItem(
    val id: String,
    val title: String,
    val date: Timestamp,
    val addedBy: String
)
