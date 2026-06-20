package com.lexnicholls.lovecounter

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.*
import coil.compose.AsyncImage
import retrofit2.HttpException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesListScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current
    val db = remember {
        ProviderInstaller.installIfNeeded(context)
        FirebaseFirestore.getInstance()
    }
    var itemsList by remember { mutableStateOf<List<ImportantDate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    val strings = t()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = listOf(strings.series, strings.films)
    var showSeenOnly by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            refreshTrigger++
        }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            pullToRefreshState.endRefresh()
        }
    }

    var itemToDelete by remember { mutableStateOf<ImportantDate?>(null) }
    var itemToShowDetails by remember { mutableStateOf<ImportantDate?>(null) }
    var columnCount by remember { mutableIntStateOf(3) }
    var showColumnMenu by remember { mutableStateOf(false) }

    var isPickingRandom by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPickingRandom) {
        if (isPickingRandom) {
            rotation.animateTo(
                targetValue = 720f,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
            val currentCategory = categories[selectedTabIndex]
            val filteredItems = itemsList.filter { it.type == currentCategory && !it.isCompleted }
            if (filteredItems.isNotEmpty()) {
                itemToShowDetails = filteredItems.random()
            } else {
                Toast.makeText(context, strings.noPendingItems, Toast.LENGTH_SHORT).show()
            }
            isPickingRandom = false
            rotation.snapTo(0f)
        }
    }

    // Deletion Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(strings.delete) },
            text = { Text("${strings.deleteConfirm} '${item.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("movies_list").document(item.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "${strings.delete} 🗑️", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Fetch items from Firestore (Real-time Sync)
    DisposableEffect(refreshTrigger) {
        isLoading = true
        val registration = db.collection("movies_list")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    itemsList = snapshot.documents.mapNotNull { doc ->
                        val isCompletedValue = doc.getBoolean("isCompleted") ?: false
                        doc.toObject(ImportantDate::class.java)?.copy(
                            id = doc.id,
                            isCompleted = isCompletedValue
                        )
                    }
                }
                isLoading = false
            }
        onDispose { registration.remove() }
    }

    // Handle System Back Button for internal navigation
    BackHandler(enabled = showSeenOnly) {
        showSeenOnly = false
    }

    if (showAddDialog || itemToShowDetails != null) {
        val currentCategory = categories[selectedTabIndex]
        AddMovieItemDialog(
            initialItem = itemToShowDetails,
            currentCategory = currentCategory,
            onDismiss = {
                onDismissDialog()
                itemToShowDetails = null
            },
            onConfirm = { name, category, details, imagePath ->
                val data = hashMapOf(
                    "name" to name,
                    "type" to category,
                    "value" to details,
                    "imagePath" to imagePath,
                    "createdBy" to (itemToShowDetails?.createdBy ?: userName),
                    "userName" to userName,
                    "isCompleted" to (itemToShowDetails?.isCompleted ?: false),
                    "senderId" to deviceId,
                    "deviceId" to deviceId
                )
                
                if (itemToShowDetails == null) {
                    db.collection("movies_list").add(data)
                        .addOnSuccessListener {
                            Toast.makeText(context, "¡Añadido! 🎬", Toast.LENGTH_SHORT).show()
                            refreshTrigger++
                            onDismissDialog()
                        }
                }
            },
            onDelete = { item ->
                itemToDelete = item
            },
            onToggleSeen = { item ->
                db.collection("movies_list").document(item.id)
                    .update(
                        "isCompleted", !item.isCompleted,
                        "senderId", deviceId,
                        "deviceId", deviceId,
                        "userName", userName,
                        "senderName", userName
                    )
                    .addOnSuccessListener { 
                        refreshTrigger++
                        itemToShowDetails = null
                    }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        AppBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showSeenOnly) "${strings.movies} (${strings.completed})" else strings.movies, 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        Box {
                            IconButton(onClick = { showColumnMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Columnas",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showColumnMenu,
                                onDismissRequest = { showColumnMenu = false }
                            ) {
                                (2..10).forEach { count ->
                                    DropdownMenuItem(
                                        text = { Text("$count Columnas") },
                                        onClick = {
                                            columnCount = count
                                            showColumnMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showSeenOnly = !showSeenOnly }) {
                            Icon(
                                imageVector = if (showSeenOnly) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = strings.completed,
                                tint = if (showSeenOnly) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    categories.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val currentCategory = categories[selectedTabIndex]
                        val filteredItems = itemsList.filter { it.type == currentCategory && it.isCompleted == showSeenOnly }

                        if (filteredItems.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${strings.noPendingItems} ($currentCategory)", color = Color.Gray)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnCount),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    MovieItemCard(
                                        item = item,
                                        onClick = { itemToShowDetails = item },
                                        onLongClick = { itemToDelete = item }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val currentCategory = categories[selectedTabIndex]
            val hasItems = itemsList.any { it.type == currentCategory && !it.isCompleted }

            if (!showSeenOnly && hasItems) {
                FloatingActionButton(
                    onClick = { 
                        if (!isPickingRandom) {
                            isPickingRandom = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Random picker",
                        modifier = Modifier.graphicsLayer(rotationZ = rotation.value)
                    )
                }
            }

            FloatingActionButton(
                onClick = onAddClick
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MovieItemCard(
    item: ImportantDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val initials = item.name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.imagePath.isNotBlank()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500${item.imagePath}",
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Title Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieItemDialog(
    initialItem: ImportantDate? = null,
    currentCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    onDelete: (ImportantDate) -> Unit = {},
    onToggleSeen: (ImportantDate) -> Unit = {}
) {
    val strings = t()
    var isEditMode by remember { mutableStateOf(initialItem == null) }
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var details by remember { mutableStateOf(initialItem?.value ?: "") }
    var imagePath by remember { mutableStateOf(initialItem?.imagePath ?: "") }

    // Search logic
    var suggestions by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var skipNextSearch by remember { mutableStateOf(false) }

    LaunchedEffect(name) {
        if (skipNextSearch) {
            skipNextSearch = false
            return@LaunchedEffect
        }
        if (isEditMode && name.length >= 3 && initialItem == null) {
            isSearching = true
            searchError = null
            kotlinx.coroutines.delay(500) // Debounce
            try {
                val response = MovieClient.api.searchMulti(name.trim())
                suggestions = response.results.take(5)
                if (suggestions.isEmpty()) {
                    searchError = "No se encontraron resultados"
                }
            } catch (e: HttpException) {
                suggestions = emptyList()
                searchError = if (e.code() == 401) {
                    "Error 401: API Key inválida o vencida. Por favor verifica MovieService.kt"
                } else {
                    "Error en el servidor: ${e.code()}"
                }
                android.util.Log.e("MoviesListScreen", "HTTP Error ${e.code()}", e)
            } catch (e: Exception) {
                suggestions = emptyList()
                searchError = "Error en la búsqueda: ${e.message}"
                android.util.Log.e("MoviesListScreen", "Search error", e)
            } finally {
                isSearching = false
            }
        } else {
            suggestions = emptyList()
            searchError = null
        }
    }

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = if (isEditMode) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        unfocusedContainerColor = if (isEditMode) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        focusedIndicatorColor = if (isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
        unfocusedIndicatorColor = if (isEditMode) MaterialTheme.colorScheme.outline else Color.Transparent,
        disabledContainerColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = if (isEditMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        disabledLabelColor = MaterialTheme.colorScheme.primary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dialogTitle = if (initialItem == null) "${strings.add} a $currentCategory" 
                                 else strings.details 
                Text(dialogTitle)
                if (initialItem != null) {
                    IconButton(onClick = { 
                        onDelete(initialItem)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column {
                if (!isEditMode && imagePath.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w500$imagePath",
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Box {
                    Column {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(strings.movieTitle) },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = !isEditMode,
                            colors = fieldColors,
                            trailingIcon = {
                                if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        )

                        if (isEditMode && (suggestions.isNotEmpty() || searchError != null)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column {
                                    if (searchError != null) {
                                        Text(
                                            text = searchError!!,
                                            modifier = Modifier.padding(16.dp),
                                            color = if (searchError!!.contains("Error")) MaterialTheme.colorScheme.error else Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    suggestions.forEach { movie ->
                                        DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(movie.displayTitle, fontWeight = FontWeight.Bold)
                                                    val year = (movie.release_date ?: movie.first_air_date ?: "").take(4)
                                                    if (year.isNotEmpty()) Text(year, fontSize = 12.sp, color = Color.Gray)
                                                }
                                            },
                                            onClick = {
                                                skipNextSearch = true
                                                name = movie.displayTitle
                                                // Automatic synopsis update behind the scenes
                                                details = movie.overview ?: ""
                                                imagePath = movie.poster_path ?: ""
                                                suggestions = emptyList()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isEditMode && details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = details,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (initialItem != null && !isEditMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onToggleSeen(initialItem) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (initialItem.isCompleted) Color.Gray else Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            if (initialItem.isCompleted) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (initialItem.isCompleted) strings.restore else strings.seen)
                    }
                }
            }
        },
        confirmButton = {
            if (isEditMode) {
                Button(onClick = { if (name.isNotBlank()) onConfirm(name, currentCategory, details, imagePath) }) {
                    Text(strings.add)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text(if (initialItem == null) strings.cancel else strings.close)
            }
        }
    )
}

