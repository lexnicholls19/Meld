package com.lexnicholls.lovecounter.ui.screens

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
fun ImportantDatesScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var dates by remember { mutableStateOf<List<DateItem>>(emptyList()) }

    DisposableEffect(Unit) {
        val registration = db.collection("important_dates")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    dates = snapshot.documents.mapNotNull { doc ->
                        DateItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
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
                        "title" to text,
                        "timestamp" to Timestamp.now(),
                        "addedBy" to userName
                    )
                    db.collection("important_dates").add(item)
                    onDismissDialog()
                }
            }
        ) {
            LoveTextField(
                value = text,
                onValueChange = { text = it },
                label = t().dates,
                placeholder = t().datesDesc
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = t().dates, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dates) { date ->
                DateRow(date, onDelete = {
                    db.collection("important_dates").document(date.id).delete()
                })
            }
        }
    }
}

@Composable
fun DateRow(date: DateItem, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = date.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "${t().addedBy}: ${date.addedBy}", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

data class DateItem(
    val id: String,
    val title: String,
    val addedBy: String
)
