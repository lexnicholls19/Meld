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
fun MoviesListScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onAddClick: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: "global"
    var movies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }

    DisposableEffect(userId) {
        val registration = db.collection("users").document(userId).collection("movies")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    movies = snapshot.documents.mapNotNull { doc ->
                        MovieItem(
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
                    db.collection("users").document(userId).collection("movies").add(item)
                    onDismissDialog()
                }
            }
        ) {
            LoveTextField(
                value = text,
                onValueChange = { text = it },
                label = t().movieTitle,
                placeholder = t().moviesDesc
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = t().movies, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Construction,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Próximamente",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Text(
                    text = "Estamos trabajando en esta sección ✨",
                    fontSize = 14.sp,
                    color = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // LazyColumn oculto temporalmente
        /*
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(movies) { movie ->
                MovieRow(movie, onDelete = {
                    db.collection("users").document(userId).collection("movies").document(movie.id).delete()
                })
            }
        }
        */
    }
}

@Composable
fun MovieRow(movie: MovieItem, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = movie.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "${t().addedBy}: ${movie.addedBy}", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

data class MovieItem(
    val id: String,
    val title: String,
    val addedBy: String
)
