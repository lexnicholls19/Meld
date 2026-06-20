package com.lexnicholls.lovecounter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField

@Composable
fun RemindersScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var reminders by remember { mutableStateOf<List<ReminderItem>>(emptyList()) }
    var isCompletedView by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCompletedView) {
        onCompletedViewToggled(isCompletedView)
    }

    DisposableEffect(isCompletedView) {
        val collection = db.collection("reminders")
        val query = collection
            .whereEqualTo("completed", isCompletedView)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                reminders = snapshot.documents.mapNotNull { doc ->
                    ReminderItem(
                        id = doc.id,
                        text = doc.getString("text") ?: "",
                        completed = doc.getBoolean("completed") ?: false,
                        addedBy = doc.getString("addedBy") ?: "Alguien"
                    )
                }
            }
        }
        onDispose { registration.remove() }
    }

    if (showAddDialog) {
        var text by remember { mutableStateOf("") }
        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            title = t().add,
            onConfirm = {
                if (text.isNotBlank()) {
                    val item = hashMapOf(
                        "text" to text,
                        "completed" to false,
                        "timestamp" to Timestamp.now(),
                        "addedBy" to userName
                    )
                    db.collection("reminders").add(item)
                    onDismissDialog()
                }
            }
        ) {
            LoveTextField(
                value = text,
                onValueChange = { text = it },
                label = t().product,
                placeholder = t().remindersDesc
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if(isCompletedView) t().completed else t().reminders, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { isCompletedView = !isCompletedView }) {
                Icon(
                    if (isCompletedView) Icons.Default.List else Icons.Default.DoneAll,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reminders) { item ->
                ReminderRow(item, onToggle = {
                    db.collection("reminders").document(item.id).update("completed", !item.completed)
                }, onDelete = {
                    db.collection("reminders").document(item.id).delete()
                })
            }
        }
    }
}

@Composable
fun ReminderRow(item: ReminderItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.completed, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(text = item.text, fontSize = 16.sp)
                Text(text = "${t().addedBy}: ${item.addedBy}", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

data class ReminderItem(
    val id: String,
    val text: String,
    val completed: Boolean,
    val addedBy: String
)
