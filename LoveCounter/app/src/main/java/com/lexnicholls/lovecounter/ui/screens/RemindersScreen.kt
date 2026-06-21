package com.lexnicholls.lovecounter.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import com.lexnicholls.lovecounter.ui.theme.ReminderColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

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
    
    var pendingReminders by remember { mutableStateOf<List<ReminderItem>>(emptyList()) }
    var completedReminders by remember { mutableStateOf<List<ReminderItem>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Multi-selection state
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }

    // Escuchar TODOS los recordatorios del usuario
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
                            addedBy = doc.getString("addedBy") ?: "Alguien"
                        )
                    }
                    pendingReminders = allItems.filter { !it.completed }
                    completedReminders = allItems.filter { it.completed }
                }
            }
        onDispose { registration.remove() }
    }

    // Informar al padre
    LaunchedEffect(selectedTab) {
        onCompletedViewToggled(selectedTab == 1)
        selectedIds.clear() // Limpiar selección al cambiar de pestaña
    }

    // Diálogo para Editar
    var editingItem by remember { mutableStateOf<ReminderItem?>(null) }
    if (editingItem != null && !isSelectionMode) {
        var text by remember { mutableStateOf(editingItem!!.text) }
        var description by remember { mutableStateOf(editingItem!!.description) }
        var location by remember { mutableStateOf(editingItem!!.location) }
        
        val initialDateMillis = editingItem!!.dueDate?.let {
            it.toDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(t().confirm) }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        dateState.selectedDateMillis = null
                        showDatePicker = false 
                    }) { Text(t().cancel) }
                }
            ) {
                DatePicker(state = dateState)
            }
        }

        LoveAlertDialog(
            onDismissRequest = { editingItem = null },
            title = t().edit,
            onConfirm = {
                if (text.isNotBlank()) {
                    val updates = mutableMapOf<String, Any>(
                        "text" to text,
                        "description" to description,
                        "location" to location
                    )
                    
                    if (dateState.selectedDateMillis != null) {
                        updates["dueDate"] = Timestamp(Date(dateState.selectedDateMillis!!))
                    } else {
                        updates["dueDate"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    
                    db.collection("users").document(userId).collection("reminders")
                        .document(editingItem!!.id)
                        .update(updates)
                    editingItem = null
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LoveTextField(value = text, onValueChange = { text = it }, label = t().reminders)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = description, onValueChange = { description = it }, label = "Descripción", isOptional = true)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = location, onValueChange = { location = it }, label = "Ubicación", isOptional = true)
                Spacer(Modifier.height(16.dp))
                
                val dateDisplay = dateState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: "No seleccionada"
                
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, null, tint = Color.Gray)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Fecha límite (Opcional)", fontSize = 12.sp, color = Color.Gray)
                            Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
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

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(t().confirm) }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        dateState.selectedDateMillis = null
                        showDatePicker = false 
                    }) { Text(t().cancel) }
                }
            ) {
                DatePicker(state = dateState)
            }
        }

        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            title = t().add,
            onConfirm = {
                if (text.isNotBlank()) {
                    val item = mutableMapOf<String, Any>(
                        "text" to text,
                        "description" to description,
                        "location" to location,
                        "completed" to false,
                        "timestamp" to Timestamp.now(),
                        "addedBy" to userName
                    )
                    dateState.selectedDateMillis?.let {
                        item["dueDate"] = Timestamp(Date(it))
                    }

                    db.collection("users").document(userId).collection("reminders").add(item)
                        .addOnSuccessListener { onDismissDialog() }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LoveTextField(value = text, onValueChange = { text = it }, label = t().reminders)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = description, onValueChange = { description = it }, label = "Descripción", isOptional = true)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = location, onValueChange = { location = it }, label = "Ubicación", isOptional = true)
                Spacer(Modifier.height(16.dp))

                val dateDisplay = dateState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: "No seleccionada"

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, null, tint = Color.Gray)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Fecha límite (Opcional)", fontSize = 12.sp, color = Color.Gray)
                            Text(dateDisplay, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Barra Superior Dinámica (Título o Selección)
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
                            val docRef = db.collection("users").document(userId).collection("reminders").document(id)
                            batch.update(docRef, "completed", selectedTab == 0)
                        }
                        batch.commit()
                        selectedIds.clear()
                    }) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Check else Icons.Default.Refresh,
                            contentDescription = "Acción Masiva",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        val batch = db.batch()
                        selectedIds.forEach { id ->
                            val docRef = db.collection("users").document(userId).collection("reminders").document(id)
                            batch.delete(docRef)
                        }
                        batch.commit()
                        selectedIds.clear()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar Seleccionados", tint = Color.Red)
                    }
                }
            }
        } else {
            Text(text = t().reminders, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        }

        // Pestañas (Tabs)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Vigentes") }
            )
            if (completedReminders.isNotEmpty()) {
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(t().completed) }
                )
            } else if (selectedTab == 1) {
                selectedTab = 0
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentList = if (selectedTab == 0) pendingReminders else completedReminders

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(currentList, key = { _, item -> item.id }) { _, item ->
                val isSelected = selectedIds.contains(item.id)

                ReminderRow(
                    item = item,
                    isSelected = isSelected,
                    onToggle = {
                        db.collection("users").document(userId).collection("reminders")
                            .document(item.id).update("completed", !item.completed)
                    },
                    onDelete = {
                        db.collection("users").document(userId).collection("reminders")
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
fun ReminderRow(
    item: ReminderItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else if (it == SwipeToDismissBoxValue.StartToEnd) {
                onToggle()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> if (item.completed) Color(0xFFFFA500) else Color(0xFF4CAF50)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                else -> Color.Transparent
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> if (item.completed) Icons.Default.Refresh else Icons.Default.Check
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = color,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    if (icon != null) Icon(icon, contentDescription = null, tint = Color.White)
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
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
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
                        .width(4.dp)
                        .background(ReminderColor)
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.text, 
                            fontSize = 17.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (item.description.isNotBlank()) {
                            Text(text = item.description, fontSize = 14.sp, color = Color.Gray, maxLines = 2)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text(text = "${t().addedBy}: ${item.addedBy}", fontSize = 11.sp, color = Color.Gray)
                            
                            if (item.dueDate != null) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Event, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Spacer(Modifier.width(2.dp))
                                // Mostramos la fecha usando UTC para evitar desfases locales al leer
                                val date = Instant.ofEpochMilli(item.dueDate.toDate().time)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                Text(text = date.format(DateTimeFormatter.ofPattern("dd/MM")), fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    if (item.location.isNotBlank()) {
                        IconButton(onClick = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(item.location)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Ver en mapa", tint = Color.Red)
                        }
                    }

                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
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
    val addedBy: String
)
