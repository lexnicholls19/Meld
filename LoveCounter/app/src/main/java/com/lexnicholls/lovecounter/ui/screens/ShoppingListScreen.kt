package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun ShoppingListScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: "global"
    var items by remember { mutableStateOf<List<ShoppingItem>>(emptyList()) }

    DisposableEffect(userId) {
        val registration = db.collection("users").document(userId).collection("shopping_list")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    items = snapshot.documents.mapNotNull { doc ->
                        ShoppingItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            bought = doc.getBoolean("bought") ?: false,
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
                        "name" to text,
                        "bought" to false,
                        "timestamp" to Timestamp.now(),
                        "addedBy" to userName
                    )
                    db.collection("users").document(userId).collection("shopping_list").add(item)
                    onDismissDialog()
                }
            }
        ) {
            LoveTextField(
                value = text,
                onValueChange = { text = it },
                label = t().product,
                placeholder = t().productPlaceholder
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = t().market, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                ShoppingRow(item, onToggle = {
                    db.collection("users").document(userId).collection("shopping_list").document(item.id).update("bought", !item.bought)
                }, onDelete = {
                    db.collection("users").document(userId).collection("shopping_list").document(item.id).delete()
                })
            }
        }
    }
}

@Composable
fun ShoppingRow(item: ShoppingItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.bought, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(text = item.name, fontSize = 16.sp)
                Text(text = "${t().addedBy}: ${item.addedBy}", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

data class ShoppingItem(
    val id: String,
    val name: String,
    val bought: Boolean,
    val addedBy: String
)
