package com.lexnicholls.lovecounter

import androidx.activity.compose.BackHandler
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.net.Uri
import androidx.compose.ui.draw.scale

// Data Class
data class ImportantDate(
    val id: String = "",
    val name: String = "",
    val type: String = "date",
    val value: String = "",
    val date: String = "",
    val location: String = "",
    val description: String = "",
    val parentId: String = "", // Para sub-items
    @get:PropertyName("isCompleted")
    val isCompleted: Boolean = false,
    val senderId: String = "",
    val price: String = "",
    val currency: String = "",
    val imagePath: String = "",
    val createdBy: String = ""
)

// Interface for Reminder Actions
interface ReminderActions {
    fun onAdd(name: String, type: String, value: String, date: String = "", location: String = "", description: String = "", parentId: String = "")
    fun onUpdate(item: ImportantDate, name: String, type: String, value: String, date: String = "", location: String = "", description: String = "", parentId: String = "")
    fun onToggleComplete(item: ImportantDate)
    fun onDelete(item: ImportantDate)
    fun onRefresh()
}

@Composable
fun AppBackground(
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    val gradient = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFBFF),
                Color(0xFFFFF0F3)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1C1E),
                Color(0xFF2C1619)
            )
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseReminderScreen(
    title: String,
    collectionName: String,
    deviceId: String,
    userName: String = "",
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit = {},
    enableCompletedTab: Boolean = true,
    enableEdit: Boolean = true,
    enableCompleteSwipe: Boolean = true,
    showCountdown: Boolean = false,
    useSimplifiedDialog: Boolean = false,
    enableDescription: Boolean = false,
    headerContent: @Composable (List<ImportantDate>) -> Unit = {}
) {
    val context = LocalContext.current
    val strings = t()
    
    val db = remember {
        ProviderInstaller.installIfNeeded(context)
        FirebaseFirestore.getInstance()
    }
    
    var datesList by remember { mutableStateOf<List<ImportantDate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCompletedScreen by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Notify when completed screen state changes
    LaunchedEffect(showCompletedScreen) {
        onCompletedViewToggled(showCompletedScreen)
    }

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
    
    var itemToComplete by remember { mutableStateOf<ImportantDate?>(null) }
    var itemToDelete by remember { mutableStateOf<ImportantDate?>(null) }
    var itemToEdit by remember { mutableStateOf<ImportantDate?>(null) }
    var targetParentIdForNewItem by remember { mutableStateOf<String?>(null) }

    // Implementation of the Interface logic
    val actions = remember(refreshTrigger) {
        object : ReminderActions {
            override fun onAdd(name: String, type: String, value: String, date: String, location: String, description: String, parentId: String) {
                val newEntry = hashMapOf(
                    "name" to name,
                    "type" to type,
                    "value" to value,
                    "date" to date,
                    "location" to location,
                    "description" to description,
                    "parentId" to parentId,
                    "isCompleted" to false,
                    "senderId" to deviceId,
                    "deviceId" to deviceId,
                    "senderName" to userName,
                    "userName" to userName
                )
                db.collection(collectionName).add(newEntry)
                    .addOnSuccessListener {
                        Toast.makeText(context, "¡Añadido! ✨", Toast.LENGTH_SHORT).show()
                        onDismissDialog()
                        onRefresh()
                    }
            }

            override fun onUpdate(item: ImportantDate, name: String, type: String, value: String, date: String, location: String, description: String, parentId: String) {
                val updates = hashMapOf<String, Any>(
                    "name" to name,
                    "type" to type,
                    "value" to value,
                    "date" to date,
                    "location" to location,
                    "description" to description,
                    "parentId" to parentId,
                    "senderId" to deviceId,
                    "deviceId" to deviceId,
                    "senderName" to userName,
                    "userName" to userName
                )
                db.collection(collectionName).document(item.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Actualizado ✅", Toast.LENGTH_SHORT).show()
                        itemToEdit = null
                        onRefresh()
                    }
            }

            override fun onToggleComplete(item: ImportantDate) {
                db.collection(collectionName).document(item.id)
                    .update(
                        "isCompleted", !item.isCompleted,
                        "senderId", deviceId,
                        "deviceId", deviceId,
                        "senderName", userName,
                        "userName", userName
                    )
                    .addOnSuccessListener {
                        val msg = if (!item.isCompleted) "${strings.completed}! ✅" else strings.restore
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onRefresh()
                    }
            }

            override fun onDelete(item: ImportantDate) {
                db.collection(collectionName).document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "${strings.delete} 🗑️", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    }
            }

            override fun onRefresh() {
                refreshTrigger++
            }
        }
    }

    // Fetch dates (Real-time Listener)
    DisposableEffect(refreshTrigger, collectionName) {
        isLoading = true
        val registration = db.collection(collectionName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    datesList = snapshot.documents.mapNotNull { doc ->
                        val isCompletedValue = doc.getBoolean("isCompleted") ?: false
                        doc.toObject(ImportantDate::class.java)?.copy(
                            id = doc.id,
                            isCompleted = isCompletedValue
                        )
                    }.sortedBy { it.isCompleted }
                }
                isLoading = false
            }
        onDispose { registration.remove() }
    }

    // Handle System Back Button for internal navigation (Completed View)
    BackHandler(enabled = showCompletedScreen) {
        showCompletedScreen = false
    }

    if (showCompletedScreen) {
        CompletedView(
            title = strings.completed,
            dates = datesList.filter { it.isCompleted },
            onBack = { showCompletedScreen = false },
            onToggleComplete = { actions.onToggleComplete(it) }
        )
        return
    }

    if (showAddDialog || targetParentIdForNewItem != null) {
        AddDateDialog(
            isSimplified = useSimplifiedDialog,
            enableDescription = enableDescription,
            enableParentSelection = (collectionName == "bucket_list"),
            availableParents = datesList.filter { it.parentId.isEmpty() },
            collectionName = collectionName,
            preSelectedParentId = targetParentIdForNewItem ?: "",
            onDismiss = { 
                onDismissDialog()
                targetParentIdForNewItem = null
            },
            onConfirm = { n, t, v, dt, l, d, p -> 
                actions.onAdd(n, t, v, dt, l, d, p)
                targetParentIdForNewItem = null
            }
        )
    }

    itemToEdit?.let { item ->
        AddDateDialog(
            initialItem = item,
            isSimplified = useSimplifiedDialog,
            enableDescription = enableDescription,
            enableParentSelection = (collectionName == "bucket_list"),
            availableParents = datesList.filter { it.parentId.isEmpty() && it.id != item.id },
            readOnlyFirst = (collectionName == "bucket_list" && item.parentId.isNotEmpty()),
            collectionName = collectionName,
            onDismiss = { itemToEdit = null },
            onConfirm = { n, t, v, dt, l, d, p -> actions.onUpdate(item, n, t, v, dt, l, d, p) }
        )
    }

    // Confirmations
    itemToComplete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToComplete = null },
            title = { Text(strings.confirm) },
            text = { 
                Text(
                    if (item.isCompleted) "¿Deseas devolver '${item.name}' a un estado no completado?"
                    else "¿Deseas marcar '${item.name}' como completado?"
                ) 
            },
            confirmButton = {
                Button(onClick = {
                    actions.onToggleComplete(item)
                    itemToComplete = null
                }) { Text(strings.yes) }
            },
            dismissButton = {
                TextButton(onClick = { itemToComplete = null }) { Text(strings.cancel) }
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(strings.delete) },
            text = { Text("${strings.deleteConfirm} '${item.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        actions.onDelete(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text(strings.cancel) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        AppBackground {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title, 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (enableCompletedTab) {
                        TextButton(
                            onClick = { showCompletedScreen = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(strings.completed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                headerContent(datesList)

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val pending = if (enableCompletedTab) datesList.filter { !it.isCompleted } else datesList
                        if (pending.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noPendingItems, color = Color.Gray)
                            }
                        } else {
                            if (collectionName == "bucket_list") {
                                // Optimización: Recordar la agrupación para evitar recalcular en cada recomposición
                                val groupedData = remember(pending) {
                                    val parents = pending.filter { it.parentId.isEmpty() }
                                    val childrenMap = pending.filter { it.parentId.isNotEmpty() }.groupBy { it.parentId }
                                    parents to childrenMap
                                }
                                val (parents, childrenMap) = groupedData
                                var expandedParentId by remember { mutableStateOf<String?>(null) }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    parents.forEach { parent ->
                                        item(key = "header_${parent.id}") {
                                            BucketCategoryHeader(
                                                parent = parent,
                                                isExpanded = expandedParentId == parent.id,
                                                onToggleExpand = {
                                                    expandedParentId = if (expandedParentId == parent.id) null else parent.id
                                                },
                                                childrenCount = childrenMap[parent.id]?.size ?: 0,
                                                completedChildrenCount = childrenMap[parent.id]?.count { it.isCompleted } ?: 0,
                                                onEdit = { itemToEdit = parent },
                                                onDelete = { itemToDelete = parent },
                                                onAddItem = { targetParentIdForNewItem = parent.id }
                                            )
                                        }

                                        if (expandedParentId == parent.id) {
                                            val children = childrenMap[parent.id] ?: emptyList()
                                            if (children.isEmpty()) {
                                                item(key = "empty_${parent.id}") {
                                                    Text(
                                                        text = "Sin ítems aún",
                                                        fontSize = 14.sp,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                                                    )
                                                }
                                            } else {
                                                items(children, key = { it.id }) { child ->
                                                    Box(modifier = Modifier.padding(start = 24.dp)) {
                                                        SwipeToCompleteWrapper(
                                                            onSwipeRight = { actions.onToggleComplete(child) },
                                                            onSwipeLeft = { actions.onDelete(child) },
                                                            isCompleted = child.isCompleted,
                                                            content = {
                                                                ReminderCard(
                                                                    item = child,
                                                                    showIcon = false,
                                                                    onToggleComplete = { actions.onToggleComplete(child) },
                                                                    onClick = { itemToEdit = child },
                                                                    collectionName = "bucket_list"
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(pending, key = { it.id }) { item ->
                                        SwipeToCompleteWrapper(
                                            onSwipeRight = { if (enableCompleteSwipe) itemToComplete = item },
                                            onSwipeLeft = { itemToDelete = item },
                                            enableRightSwipe = enableCompleteSwipe,
                                            isCompleted = item.isCompleted,
                                            content = {
                                                ReminderCard(
                                                    item = item,
                                                    showIcon = false,
                                                    onToggleComplete = {},
                                                    onClick = { if (enableEdit) itemToEdit = item },
                                                    showCountdown = showCountdown,
                                                    collectionName = collectionName
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun CompletedView(
    title: String,
    dates: List<ImportantDate>,
    onBack: () -> Unit,
    onToggleComplete: (ImportantDate) -> Unit
) {
    val strings = t()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← ${strings.back}") }
            Spacer(Modifier.weight(1f))
            Text(text = title, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(80.dp))
        }
        Spacer(Modifier.height(20.dp))
        if (dates.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.noItemsYet)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(dates) { item ->
                    ReminderCard(
                        item = item,
                        onToggleComplete = { onToggleComplete(item) },
                        onClick = { /* Could support editing completed too if wanted */ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToCompleteWrapper(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    enableRightSwipe: Boolean = true,
    isCompleted: Boolean = false,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (enableRightSwipe) {
                        onSwipeRight()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeLeft()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp)),
        enableDismissFromStartToEnd = enableRightSwipe,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (isCompleted) Color(0xFFFF9800) else Color(0xFF4CAF50)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (isCompleted) Icons.Default.Refresh else Icons.Default.Check
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) {
        content()
    }
}

private fun calculateDaysUntil(dateString: String): Long? {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val birthDate = LocalDate.parse(dateString, formatter)
        val today = LocalDate.now()
        
        var nextDate = birthDate.withYear(today.year)
        
        if (nextDate.isBefore(today)) {
            nextDate = nextDate.plusYears(1)
        }
        
        ChronoUnit.DAYS.between(today, nextDate)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ReminderCard(
    item: ImportantDate,
    showIcon: Boolean = true,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit = {},
    showCountdown: Boolean = false,
    collectionName: String = ""
) {
    val strings = t()
    val context = LocalContext.current
    val indicatorColor = when {
        collectionName == "reminders" -> Color(0xFF7C4DFF) // Morado
        collectionName == "important_dates" -> Color(0xFFFF4081) // Rosado
        collectionName == "shopping_list" || item.type == "shopping_list" -> Color(0xFF4CAF50) // Verde
        collectionName == "bucket_list" -> Color(0xFFFFC107) // Amarillo (Amber)
        item.type == "date" -> Color(0xFFFF4081)
        item.type == "location" -> Color(0xFF4CAF50)
        else -> Color(0xFF7C4DFF)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCompleted) 1.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Indicator Strip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(if (item.isCompleted) indicatorColor.copy(alpha = 0.5f) else indicatorColor)
            )
            
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (item.date.isNotBlank()) {
                        InfoRow(text = "📅 ${item.date}", isCompleted = item.isCompleted)
                    }
                    if (item.location.isNotBlank()) {
                        InfoRow(text = "📍 ${item.location}", isCompleted = item.isCompleted)
                    }
                    if (item.value.isNotBlank()) {
                        val legacyText = when (item.type) {
                            "date" -> if (item.date.isBlank()) "📅 ${item.value}" else null
                            "location" -> if (item.location.isBlank()) "📍 ${item.value}" else null
                            "text", "multi" -> item.value
                            else -> if (collectionName == "important_dates" && item.date.isBlank()) "📅 ${item.value}" else item.value
                        }
                        legacyText?.let { InfoRow(text = it, isCompleted = item.isCompleted) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = strings.completed,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.padding(end = 8.dp).size(24.dp)
                        )
                    }

                    if (item.location.isNotBlank()) {
                        IconButton(onClick = {
                            try {
                                val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(item.location)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, strings.mapError, Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = strings.viewOnMap,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (showCountdown) {
                    val strings = t()
                    val dateToCalculate = if (item.date.isNotBlank()) item.date 
                                          else if (item.type == "date" || collectionName == "important_dates") item.value 
                                          else ""
                    
                    val daysLeft = if (dateToCalculate.isNotBlank()) calculateDaysUntil(dateToCalculate) else null

                    if (daysLeft != null) {
                        Surface(
                            color = if (daysLeft == 0L) Color(0xFFFF4081) else indicatorColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    daysLeft == 0L -> strings.today
                                    daysLeft == 1L -> strings.tomorrow
                                    else -> strings.daysLeftShort.format(daysLeft)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (daysLeft == 0L) Color.White else indicatorColor
                            )
                        }
                    }
                }

                if (showIcon) {
                    IconButton(onClick = onToggleComplete) {
                        Icon(
                            imageVector = if (item.isCompleted) Icons.Default.Refresh else Icons.Default.CheckCircle,
                            contentDescription = "Completar",
                            tint = if (item.isCompleted) Color.Gray else Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(text: String, isCompleted: Boolean) {
    Text(
        text = text,
        fontSize = 15.sp,
        color = Color.Gray,
        style = MaterialTheme.typography.bodyMedium,
        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
    )
}

@Composable
fun BucketCategoryHeader(
    parent: ImportantDate,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    childrenCount: Int,
    completedChildrenCount: Int,
    onEdit: (ImportantDate) -> Unit,
    onDelete: (ImportantDate) -> Unit,
    onAddItem: (ImportantDate) -> Unit = {}
) {
    val strings = t()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = parent.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (childrenCount > 0) {
                Text(
                    text = "$completedChildrenCount/$childrenCount",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("${strings.add} a esta categoría") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuExpanded = false
                            onAddItem(parent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.edit) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuExpanded = false
                            onEdit(parent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuExpanded = false
                            onDelete(parent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDateDialog(
    initialItem: ImportantDate? = null,
    isSimplified: Boolean = false,
    enableDescription: Boolean = false,
    enableParentSelection: Boolean = false,
    availableParents: List<ImportantDate> = emptyList(),
    preSelectedParentId: String = "",
    readOnlyFirst: Boolean = false,
    collectionName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, value: String, date: String, location: String, description: String, parentId: String) -> Unit
) {
    val strings = t()
    var isEditMode by remember { mutableStateOf(!readOnlyFirst) }
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var selectedParentId by remember { mutableStateOf(initialItem?.parentId ?: preSelectedParentId) }
    
    // Si estamos añadiendo uno nuevo y habilitamos parent selection
    var isChildMode by remember { mutableStateOf(initialItem?.parentId?.isNotEmpty() == true || preSelectedParentId.isNotEmpty()) }

    var textValue by remember { mutableStateOf(initialItem?.value ?: "") }
    var locationValue by remember { mutableStateOf(initialItem?.location ?: "") }
    var selectedDateMillis by remember { 
        mutableStateOf(
            run {
                val dateStr = if (initialItem?.date?.isNotBlank() == true) {
                    initialItem.date
                } else if (initialItem?.value?.isNotBlank() == true && (initialItem.type == "date" || collectionName == "important_dates")) {
                    initialItem.value
                } else if (initialItem == null && isSimplified) {
                    java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } else {
                    null
                }

                dateStr?.let {
                    try {
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        java.time.LocalDate.parse(it, formatter)
                            .atStartOfDay(ZoneId.of("UTC"))
                            .toInstant()
                            .toEpochMilli()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        ) 
    }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )
    var showDatePicker by remember { mutableStateOf(false) }

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

    if (showDatePicker) {
        val initialSelection = remember { datePickerState.selectedDateMillis }
        LaunchedEffect(datePickerState.selectedDateMillis) {
            if (datePickerState.selectedDateMillis != null && datePickerState.selectedDateMillis != initialSelection) {
                selectedDateMillis = datePickerState.selectedDateMillis
                showDatePicker = false
            }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialItem == null) {
                        when (collectionName) {
                            "reminders" -> "Añadir nuevo recordatorio"
                            "important_dates" -> "Añadir fecha importante"
                            "bucket_list" -> if (enableParentSelection && !isChildMode) "Añadir categoría" else "Añadir nuevo ítem"
                            else -> "Añadir nuevo"
                        }
                    } else if (!isEditMode) {
                        "Detalles"
                    } else {
                        "Editar registro"
                    },
                    modifier = Modifier.weight(1f)
                )
                
                if (initialItem == null && enableParentSelection) {
                    // Switch/Slider para Padre/Hijo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            "P", 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (!isChildMode) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Switch(
                            checked = isChildMode,
                            onCheckedChange = { isChildMode = it },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Text(
                            "H", 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (isChildMode) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                if (initialItem != null && !isEditMode) {
                    IconButton(onClick = { isEditMode = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        text = {
            Column {
                val isActuallyParent = initialItem != null && initialItem.parentId.isEmpty() && collectionName == "bucket_list"
                
                if ((initialItem == null && enableParentSelection && !isChildMode) || isActuallyParent) {
                    // MODO PADRE (Creando o Editando categoría)
                    if (initialItem == null) {
                        Text("Creando nueva categoría contenedora", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                    }
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.category) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        readOnly = !isEditMode
                    )
                } else {
                    // MODO HIJO O EDICIÓN NORMAL
                    if (enableParentSelection && initialItem == null) {
                        var parentExpanded by remember { mutableStateOf(false) }
                        val selectedParentName = availableParents.find { it.id == selectedParentId }?.name ?: strings.category
                        
                        Text(strings.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            OutlinedButton(
                                onClick = { if (isEditMode) parentExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditMode,
                                border = if (isEditMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                colors = if (!isEditMode) ButtonDefaults.textButtonColors() else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(selectedParentName)
                            }
                            DropdownMenu(
                                expanded = parentExpanded,
                                onDismissRequest = { parentExpanded = false }
                            ) {
                                availableParents.forEach { parent ->
                                    DropdownMenuItem(
                                        text = { Text(parent.name) },
                                        onClick = {
                                            selectedParentId = parent.id
                                            parentExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isSimplified) strings.mainTitle else strings.userName) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = !isEditMode,
                        colors = fieldColors
                    )

                    if (enableDescription) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = description,
                            onValueChange = { if (it.length <= 255) description = it },
                            label = { Text(strings.details) },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = !isEditMode,
                            colors = fieldColors,
                            supportingText = {
                                if (isEditMode) {
                                    Text(
                                        text = "${255 - description.length} ${strings.charactersLeft}",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )
                    }
                    
                    if (!isSimplified) {
                        // TEXT OPTION
                        if (isEditMode || textValue.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                label = { Text(strings.product) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = !isEditMode,
                                colors = fieldColors
                            )
                        }

                        // LOCATION OPTION
                        if (isEditMode || locationValue.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextField(
                                value = locationValue,
                                onValueChange = { locationValue = it },
                                label = { Text(strings.location) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ej: Torre Eiffel, París") },
                                readOnly = !isEditMode,
                                colors = fieldColors
                            )
                        }

                        // DATE OPTION
                        if (isEditMode || selectedDateMillis != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val dateDisplay = selectedDateMillis?.let {
                                val date = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            } ?: strings.selectDate

                            OutlinedButton(
                                onClick = { if (isEditMode) showDatePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEditMode || selectedDateMillis != null,
                                border = if (isEditMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                colors = if (!isEditMode) ButtonDefaults.textButtonColors() else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(dateDisplay)
                            }
                        }
                    } else {
                        // SIMPLIFIED (Mainly for ImportantDatesScreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        val dateDisplay = selectedDateMillis?.let {
                            val date = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        } ?: strings.selectDate

                        OutlinedButton(
                            onClick = { if (isEditMode) showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isEditMode || selectedDateMillis != null,
                            border = if (isEditMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                            colors = if (!isEditMode) ButtonDefaults.textButtonColors() else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(dateDisplay)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditMode) {
                Button(
                    onClick = {
                        val finalDate = selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        } ?: ""

                        if (name.isNotBlank()) {
                            onConfirm(
                                name, 
                                if (isSimplified) "date" else "multi", 
                                textValue, 
                                finalDate,
                                locationValue,
                                description,
                                if (isChildMode) selectedParentId else ""
                            )
                        }
                    }
                ) {
                    Text(if (initialItem == null) "Añadir" else "Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEditMode && initialItem != null) "Cancelar" else "Cerrar")
            }
        }
    )
}
