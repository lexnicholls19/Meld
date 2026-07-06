package com.lexnicholls.lovecounter.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.lexnicholls.lovecounter.ui.theme.ReminderColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    deviceId: String,
    userName: String,
    userId: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val strings = t()
    
    var pendingReminders by remember { mutableStateOf<List<ReminderItem>>(emptyList()) }
    var completedReminders by remember { mutableStateOf<List<ReminderItem>>(emptyList()) }
    var recurrentReminders by remember { mutableStateOf<List<ReminderItem>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }

    DisposableEffect(userId) {
        val collection = db.collection("users").document(userId).collection("reminders")
        val registration = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error listening to reminders", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allItems = snapshot.documents.mapNotNull { doc ->
                        ReminderItem(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            description = doc.getString("description") ?: "",
                            location = doc.getString("location") ?: "",
                            dueDate = doc.getTimestamp("dueDate"),
                            completed = doc.getBoolean("completed") ?: false,
                            addedBy = doc.getString("addedBy") ?: strings.someone,
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                            isRecurrent = doc.getBoolean("isRecurrent") ?: false,
                            resetDay = doc.getLong("resetDay")?.toInt() ?: 1,
                            lastResetMonth = doc.getLong("lastResetMonth")?.toInt() ?: -1,
                            lastResetYear = doc.getLong("lastResetYear")?.toInt() ?: -1
                        )
                    }
                    
                    // Automatic reset of recurrent items
                    val now = LocalDate.now()
                    val currentMonth = now.monthValue
                    val currentYear = now.year
                    val currentDay = now.dayOfMonth
                    val batch = db.batch()
                    var needsCommit = false

                    allItems.forEach { item ->
                        if (item.isRecurrent && item.completed) {
                            val lastMonth = item.lastResetMonth
                            val lastYear = item.lastResetYear
                            val shouldReset = (currentYear > lastYear || (currentYear == lastYear && currentMonth > lastMonth)) &&
                                    currentDay >= item.resetDay
                            if (shouldReset) {
                                val docRef = collection.document(item.id)
                                batch.update(docRef, mapOf(
                                    "completed" to false,
                                    "lastResetMonth" to currentMonth,
                                    "lastResetYear" to currentYear
                                ))
                                needsCommit = true
                            }
                        }
                    }
                    if (needsCommit) batch.commit()

                    fun sortReminders(list: List<ReminderItem>): List<ReminderItem> {
                        return list.sortedWith(
                            compareBy<ReminderItem> { it.dueDate == null }
                                .thenBy { it.dueDate?.seconds ?: Long.MAX_VALUE }
                                .thenByDescending { it.timestamp.seconds }
                        )
                    }

                    pendingReminders = sortReminders(allItems.filter { !it.completed && !it.isRecurrent })
                    completedReminders = sortReminders(allItems.filter { it.completed && !it.isRecurrent })
                    recurrentReminders = allItems.filter { it.isRecurrent }.sortedBy { it.resetDay }
                }
            }
        onDispose { registration.remove() }
    }

    LaunchedEffect(selectedTab) {
        onCompletedViewToggled(selectedTab == 2)
        selectedIds.clear()
    }

    // Add/Edit Logic (Visual tweaks only)
    var editingItem by remember { mutableStateOf<ReminderItem?>(null) }
    if (editingItem != null && !isSelectionMode) {
        var text by remember { mutableStateOf(editingItem!!.text) }
        var description by remember { mutableStateOf(editingItem!!.description) }
        var location by remember { mutableStateOf(editingItem!!.location) }
        var isRecurrent by remember { mutableStateOf(editingItem!!.isRecurrent) }
        var resetDay by remember { mutableIntStateOf(editingItem!!.resetDay) }
        
        val initialDateMillis = editingItem!!.dueDate?.let {
            it.toDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.confirm) } },
                dismissButton = { TextButton(onClick = { dateState.selectedDateMillis = null; showDatePicker = false }) { Text(strings.cancel) } }
            ) { DatePicker(state = dateState) }
        }

        LoveAlertDialog(
            onDismissRequest = { editingItem = null },
            title = strings.edit,
            onConfirm = {
                if (text.isNotBlank()) {
                    val updates = mutableMapOf<String, Any>("text" to text, "description" to description, "location" to location, "isRecurrent" to isRecurrent)
                    if (isRecurrent) {
                        updates["resetDay"] = resetDay
                        updates["dueDate"] = com.google.firebase.firestore.FieldValue.delete()
                    } else {
                        if (dateState.selectedDateMillis != null) updates["dueDate"] = Timestamp(Date(dateState.selectedDateMillis!!))
                        else updates["dueDate"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    db.collection("users").document(userId).collection("reminders").document(editingItem!!.id).update(updates)
                    editingItem = null
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.reminders)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = description, onValueChange = { description = it }, label = strings.description, isOptional = true)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = location, onValueChange = { location = it }, label = strings.location, isOptional = true)
                Spacer(Modifier.height(16.dp))
                
                if (isRecurrent) {
                    Text(text = strings.resetDay + ": $resetDay", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ReminderColor)
                    Slider(value = resetDay.toFloat(), onValueChange = { resetDay = it.roundToInt() }, valueRange = 1f..31f, steps = 30, modifier = Modifier.padding(horizontal = 16.dp))
                    TextButton(onClick = { isRecurrent = false }) { Text(strings.cancel + " " + strings.monthly, color = Color.Gray) }
                } else {
                    val dateDisplay = dateState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    } ?: strings.notSelected
                    OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, tint = ReminderColor)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(strings.dueDateOptional, fontSize = 12.sp, color = Color.Gray)
                                Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    if (selectedTab == 1) {
                        TextButton(onClick = { isRecurrent = true }) { Text(strings.recurrent) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var text by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        val dateState = rememberDatePickerState()
        var showDatePicker by remember { mutableStateOf(false) }
        var resetDay by remember { mutableIntStateOf(1) }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.confirm) } },
                dismissButton = { TextButton(onClick = { dateState.selectedDateMillis = null; showDatePicker = false }) { Text(strings.cancel) } }
            ) { DatePicker(state = dateState) }
        }

        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            title = if (selectedTab == 1) strings.recurrent else strings.add,
            onConfirm = {
                if (text.isNotBlank()) {
                    val item = mutableMapOf<String, Any>("text" to text, "description" to description, "location" to location, "completed" to false, "timestamp" to Timestamp.now(), "addedBy" to userName, "isRecurrent" to (selectedTab == 1))
                    if (selectedTab == 1) {
                        item["resetDay"] = resetDay
                        val now = LocalDate.now()
                        item["lastResetMonth"] = now.monthValue
                        item["lastResetYear"] = now.year
                    } else {
                        dateState.selectedDateMillis?.let { item["dueDate"] = Timestamp(Date(it)) }
                    }
                    db.collection("users").document(userId).collection("reminders").add(item).addOnSuccessListener { onDismissDialog() }
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.reminders)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = description, onValueChange = { description = it }, label = strings.description, isOptional = true)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = location, onValueChange = { location = it }, label = strings.location, isOptional = true)
                Spacer(Modifier.height(16.dp))

                if (selectedTab == 1) {
                    Text(text = strings.resetDay + ": $resetDay", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ReminderColor)
                    Slider(value = resetDay.toFloat(), onValueChange = { resetDay = it.roundToInt() }, valueRange = 1f..31f, steps = 30, modifier = Modifier.padding(horizontal = 16.dp))
                    Text(text = strings.autoReset + " (" + strings.monthly + ")", fontSize = 12.sp, color = Color.Gray)
                } else {
                    val dateDisplay = dateState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    } ?: strings.notSelected
                    OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, tint = ReminderColor)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(strings.dueDateOptional, fontSize = 12.sp, color = Color.Gray)
                                Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            
            if (isSelectionMode) {
                SelectionHeader(selectedCount = selectedIds.size, onCancel = { selectedIds.clear() }) {
                    val batch = db.batch()
                    selectedIds.forEach { id -> batch.delete(db.collection("users").document(userId).collection("reminders").document(id)) }
                    batch.commit().addOnSuccessListener { selectedIds.clear() }
                }
            } else {
                Text(text = strings.reminders, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.currentReminders) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.recurrent) })
                if (completedReminders.isNotEmpty()) {
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.completed) })
                } else if (selectedTab == 2) { selectedTab = 0 }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val rawList = when(selectedTab) {
                0 -> pendingReminders
                1 -> recurrentReminders
                else -> completedReminders
            }
            val currentList = remember(rawList, pendingDeleteId) {
                rawList.filter { it.id != pendingDeleteId }
            }

            if (currentList.isEmpty()) {
                EmptyState(
                    title = strings.noItemsYet,
                    subtitle = when (selectedTab) {
                        0 -> strings.remindersCurrentEmpty
                        1 -> strings.remindersRecurrentEmpty
                        else -> strings.noPendingItems
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hero Section for first urgent reminder
                    if (selectedTab == 0 && currentList.isNotEmpty() && currentList.first().dueDate != null) {
                        item(key = "hero") {
                            HeroReminderCard(item = currentList.first(), onClick = { editingItem = currentList.first() })
                        }
                    }

                    itemsIndexed(currentList, key = { _, item -> item.id }) { _, item ->
                        val isHero = selectedTab == 0 && item == currentList.first() && item.dueDate != null
                        if (!isHero) {
                            ReminderRowModern(
                                item = item,
                                isSelected = selectedIds.contains(item.id),
                                onToggle = {
                                    val updates = mutableMapOf<String, Any>("completed" to !item.completed)
                                    if (!item.completed && item.isRecurrent) {
                                        val now = LocalDate.now()
                                        updates["lastResetMonth"] = now.monthValue
                                        updates["lastResetYear"] = now.year
                                    }
                                    db.collection("users").document(userId).collection("reminders").document(item.id).update(updates)
                                },
                                onDelete = { 
                                    pendingDeleteId?.let { oldId ->
                                        db.collection("users").document(userId).collection("reminders").document(oldId).delete()
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
                    db.collection("users").document(userId).collection("reminders").document(id).delete()
                }
                pendingDeleteId = null
            }
        )
    }
}

@Composable
fun HeroReminderCard(item: ReminderItem, onClick: () -> Unit) {
    val strings = t()
    val date = item.dueDate?.let {
        Instant.ofEpochMilli(it.toDate().time).atZone(ZoneOffset.UTC).toLocalDate()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                modifier = Modifier.size(100.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondary, shape = CircleShape) {
                    Text(
                        text = date?.format(DateTimeFormatter.ofPattern("d MMMM")) ?: strings.pending.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = item.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                if (item.description.isNotBlank()) {
                    Text(text = item.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), maxLines = 2)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.ReminderRowModern(
    item: ReminderItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val strings = t()
    val context = LocalContext.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true }
            else if (it == SwipeToDismissBoxValue.StartToEnd) { onToggle(); false }
            else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled || dismissState.currentValue != SwipeToDismissBoxValue.Settled
            if (isSwiping) {
                val color = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> if (item.completed) Color(0xFFFFA500) else Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }
                val icon = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> if (item.completed) Icons.Default.Refresh else Icons.Default.Check
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    else -> null
                }
                Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(color), contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) {
                    if (icon != null) Icon(icon, contentDescription = null, tint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.White, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        },
        modifier = Modifier.animateItem()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = ReminderColor.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (item.isRecurrent) Icons.Default.Repeat else Icons.Default.Notifications, contentDescription = null, tint = ReminderColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.completed) Color.Gray else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else null
                    )
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            textDecoration = if (item.completed) TextDecoration.LineThrough else null
                        )
                    }
                }

                if (item.isRecurrent) {
                    Text(text = "${strings.resetDay} ${item.resetDay}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ReminderColor)
                } else if (item.dueDate != null) {
                    val date = Instant.ofEpochMilli(item.dueDate.toDate().time).atZone(ZoneOffset.UTC).toLocalDate()
                    Text(text = date.format(DateTimeFormatter.ofPattern("dd/MM")), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                if (item.location.isNotBlank()) {
                    IconButton(onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(item.location)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") })
                    }) { Icon(Icons.Default.Map, null, tint = ReminderColor, modifier = Modifier.size(20.dp)) }
                }
                
                if (isSelected) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Task, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text(text = title, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, color = Color.Gray.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

data class ReminderItem(
    val id: String,
    val text: String,
    val description: String = "",
    val location: String = "",
    val dueDate: Timestamp? = null,
    val completed: Boolean,
    val addedBy: String,
    val timestamp: Timestamp = Timestamp.now(),
    val isRecurrent: Boolean = false,
    val resetDay: Int = 1,
    val lastResetMonth: Int = -1,
    val lastResetYear: Int = -1
)
