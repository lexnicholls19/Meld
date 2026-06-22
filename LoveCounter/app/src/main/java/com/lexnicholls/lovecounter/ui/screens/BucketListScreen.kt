package com.lexnicholls.lovecounter.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.ui.theme.BucketColor
import com.lexnicholls.lovecounter.util.t
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BucketListScreen(
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
    
    var categories by remember { mutableStateOf<List<BucketCategory>>(emptyList()) }
    var expandedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Key: CategoryId, Value: Pair(CompletedCount, TotalCount)
    val categoryStats = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    
    val totalItems = categoryStats.values.sumOf { it.second }
    val completedItems = categoryStats.values.sumOf { it.first }
    val progress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f

    DisposableEffect(userId) {
        val registration = db.collection("users").document(userId).collection("bucket_list")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error listening to bucket categories", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val newCategories = snapshot.documents.map { doc ->
                        BucketCategory(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            addedBy = doc.getString("addedBy") ?: strings.someone
                        )
                    }
                    // Limpiar stats de categorías eliminadas
                    val currentIds = newCategories.map { it.id }.toSet()
                    categoryStats.keys.retainAll(currentIds)
                    
                    categories = newCategories
                }
            }
        onDispose { registration.remove() }
    }

    LaunchedEffect(Unit) {
        onCompletedViewToggled(false)
    }

    if (showAddDialog) {
        var isSubItemMode by remember { mutableStateOf(false) }
        var categoryTitle by remember { mutableStateOf("") }
        var selectedParentId by remember { mutableStateOf("") }
        var selectedParentTitle by remember { mutableStateOf(strings.selectCategory) }
        var itemTitle by remember { mutableStateOf("") }
        var itemDesc by remember { mutableStateOf("") }
        var itemLocation by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            titleContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.addNew,
                        color = com.lexnicholls.lovecounter.ui.theme.LovePink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("P", fontSize = 12.sp, color = if (!isSubItemMode) MaterialTheme.colorScheme.onSurface else Color.Gray)
                        Switch(
                            checked = isSubItemMode,
                            onCheckedChange = { isSubItemMode = it },
                            modifier = Modifier.padding(horizontal = 4.dp).scale(0.7f)
                        )
                        Text("H", fontSize = 12.sp, color = if (isSubItemMode) MaterialTheme.colorScheme.onSurface else Color.Gray)
                    }
                }
            },
            onConfirm = {
                if (!isSubItemMode) {
                    if (categoryTitle.isNotBlank()) {
                        val category = hashMapOf(
                            "title" to categoryTitle,
                            "timestamp" to Timestamp.now(),
                            "addedBy" to userName
                        )
                        db.collection("users").document(userId).collection("bucket_list").add(category)
                            .addOnSuccessListener { onDismissDialog() }
                    }
                } else {
                    if (selectedParentId.isNotBlank() && itemTitle.isNotBlank()) {
                        val subItem = hashMapOf(
                            "title" to itemTitle,
                            "description" to itemDesc,
                            "location" to itemLocation,
                            "completed" to false,
                            "timestamp" to Timestamp.now(),
                            "addedBy" to userName
                        )
                        db.collection("users").document(userId).collection("bucket_list")
                            .document(selectedParentId).collection("items").add(subItem)
                            .addOnSuccessListener { onDismissDialog() }
                    } else if (selectedParentId.isBlank()) {
                        Toast.makeText(context, strings.selectParentError, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!isSubItemMode) {
                    LoveTextField(value = categoryTitle, onValueChange = { categoryTitle = it }, label = strings.categoryName)
                } else {
                    Text(strings.parentCategory, fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = selectedParentTitle,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                color = if (selectedParentId.isEmpty()) Color.Gray else Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.title) },
                                    onClick = {
                                        selectedParentId = category.id
                                        selectedParentTitle = category.title
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LoveTextField(value = itemTitle, onValueChange = { itemTitle = it }, label = strings.name)
                    Spacer(Modifier.height(12.dp))
                    LoveTextField(value = itemDesc, onValueChange = { itemDesc = it }, label = strings.description)
                    Spacer(Modifier.height(12.dp))
                    LoveTextField(value = itemLocation, onValueChange = { itemLocation = it }, label = strings.location, isOptional = true)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = strings.bucket, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = strings.adventureProgress, fontSize = 12.sp, color = Color.Gray)
            Text(text = "$completedItems/$totalItems", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = BucketColor,
            trackColor = Color.Gray.copy(alpha = 0.2f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryItem(
                    category = category,
                    userId = userId,
                    db = db,
                    userName = userName,
                    isExpanded = expandedCategoryId == category.id,
                    onExpandedChange = { expanded ->
                        expandedCategoryId = if (expanded) category.id else null
                    },
                    onStatsChange = { completed, total ->
                        categoryStats[category.id] = Pair(completed, total)
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: BucketCategory,
    userId: String,
    db: FirebaseFirestore,
    userName: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStatsChange: (Int, Int) -> Unit
) {
    val strings = t()
    var subItems by remember { mutableStateOf<List<BucketSubItem>>(emptyList()) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var editingSubItem by remember { mutableStateOf<BucketSubItem?>(null) }

    val completedCount = subItems.count { it.completed }
    val totalCount = subItems.size

    LaunchedEffect(completedCount, totalCount) {
        onStatsChange(completedCount, totalCount)
    }

    DisposableEffect(category.id) {
        val registration = db.collection("users").document(userId)
            .collection("bucket_list").document(category.id)
            .collection("items")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val itemsList = snapshot.documents.map { doc ->
                        BucketSubItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            completed = doc.getBoolean("completed") ?: false,
                            location = doc.getString("location") ?: "",
                            addedBy = doc.getString("addedBy") ?: strings.someone
                        )
                    }
                    subItems = itemsList
                    category.items = itemsList
                }
            }
        onDispose { registration.remove() }
    }

    if (showAddTaskDialog) {
        var text by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        LoveAlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = strings.addCategory + " " + category.title,
            onConfirm = {
                if (text.isNotBlank()) {
                    val item = hashMapOf(
                        "title" to text,
                        "description" to description,
                        "location" to location,
                        "completed" to false,
                        "timestamp" to Timestamp.now(),
                        "addedBy" to userName
                    )
                    db.collection("users").document(userId)
                        .collection("bucket_list").document(category.id)
                        .collection("items").add(item)
                    showAddTaskDialog = false
                }
            }
        ) {
            Column {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.name)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = description, onValueChange = { description = it }, label = strings.description)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = location, onValueChange = { location = it }, label = strings.location, isOptional = true)
            }
        }
    }

    if (showEditCategoryDialog) {
        var text by remember { mutableStateOf(category.title) }
        LoveAlertDialog(
            onDismissRequest = { showEditCategoryDialog = false },
            title = strings.editAdventure,
            onConfirm = {
                if (text.isNotBlank()) {
                    db.collection("users").document(userId).collection("bucket_list").document(category.id)
                        .update("title", text)
                    showEditCategoryDialog = false
                }
            }
        ) {
            LoveTextField(value = text, onValueChange = { text = it }, label = strings.categoryName)
        }
    }

    if (showDeleteCategoryDialog) {
        LoveAlertDialog(
            onDismissRequest = { showDeleteCategoryDialog = false },
            title = strings.deleteAdventure,
            confirmButtonText = strings.delete,
            onConfirm = {
                db.collection("users").document(userId).collection("bucket_list").document(category.id).delete()
                showDeleteCategoryDialog = false
            }
        ) {
            Text(strings.deleteAdventureDesc.format(category.title))
        }
    }

    if (editingSubItem != null) {
        var text by remember { mutableStateOf(editingSubItem!!.title) }
        var description by remember { mutableStateOf(editingSubItem!!.description) }
        var location by remember { mutableStateOf(editingSubItem!!.location) }
        
        LoveAlertDialog(
            onDismissRequest = { editingSubItem = null },
            title = strings.edit,
            onConfirm = {
                if (text.isNotBlank()) {
                    db.collection("users").document(userId)
                        .collection("bucket_list").document(category.id)
                        .collection("items").document(editingSubItem!!.id)
                        .update(mapOf(
                            "title" to text,
                            "description" to description,
                            "location" to location
                        ))
                    editingSubItem = null
                }
            }
        ) {
            Column {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.name)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = description, onValueChange = { description = it }, label = strings.description)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = location, onValueChange = { location = it }, label = strings.location, isOptional = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(BucketColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!isExpanded) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = category.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    if (totalCount > 0) {
                        Text(text = "$completedCount/$totalCount", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 12.dp))
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.addToThisCategory) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = { 
                                    menuExpanded = false
                                    showAddTaskDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.edit) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { 
                                    menuExpanded = false
                                    showEditCategoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.delete) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = { 
                                    menuExpanded = false
                                    showDeleteCategoryDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn() + slideInVertically { -it / 2 },
            exit = shrinkVertically() + fadeOut() + slideOutVertically { -it / 2 }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .offset(y = (-8).dp),
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 8.dp, bottom = 16.dp, top = 16.dp)
                ) {
                    subItems.forEach { subItem ->
                        SubItemRow(
                            subItem = subItem,
                            onToggle = {
                                db.collection("users").document(userId)
                                    .collection("bucket_list").document(category.id)
                                    .collection("items").document(subItem.id)
                                    .update("completed", !subItem.completed)
                            },
                            onDelete = {
                                db.collection("users").document(userId)
                                    .collection("bucket_list").document(category.id)
                                    .collection("items").document(subItem.id).delete()
                            },
                            onClick = { editingSubItem = subItem }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubItemRow(
    subItem: BucketSubItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = t()
    
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "subitem_swipe")
    val threshold = 250f

    LaunchedEffect(subItem.completed) {
        offsetX = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX = (offsetX + delta).coerceIn(-500f, 500f)
                },
                onDragStopped = {
                    if (offsetX > threshold) {
                        onToggle()
                    } else if (offsetX < -threshold) {
                        onDelete()
                    }
                    scope.launch { offsetX = 0f }
                }
            )
            .background(
                color = when {
                    offsetX > 50 -> if (subItem.completed) Color(0xFFFFA500) else Color(0xFF4CAF50)
                    offsetX < -50 -> Color(0xFFF44336)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (offsetX > 50) {
            Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                Icon(if (subItem.completed) Icons.Default.Refresh else Icons.Default.Check, null, tint = Color.White)
            }
        } else if (offsetX < -50) {
            Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, null, tint = Color.White)
            }
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .clickable { onClick() },
            color = if (subItem.completed) Color(0xFF1C1C1E) else Color(0xFF2C2C2E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subItem.title,
                        fontSize = 16.sp,
                        color = if (subItem.completed) Color.Gray else Color.White,
                        textDecoration = if (subItem.completed) TextDecoration.LineThrough else null,
                        fontWeight = FontWeight.Medium
                    )
                    if (subItem.description.isNotBlank()) {
                        Text(text = subItem.description, fontSize = 12.sp, color = Color.Gray)
                    }
                    if (subItem.location.isNotBlank()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp), tint = Color.Red.copy(alpha = 0.6f))
                            Spacer(Modifier.width(4.dp))
                            Text(text = subItem.location, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                if (subItem.location.isNotBlank()) {
                    IconButton(onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(subItem.location)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = strings.viewOnMap,
                            tint = com.lexnicholls.lovecounter.ui.theme.LovePink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

class BucketCategory(
    val id: String,
    val title: String,
    val description: String = "",
    var items: List<BucketSubItem> = emptyList(),
    val addedBy: String
)

data class BucketSubItem(
    val id: String,
    val title: String,
    val description: String = "",
    val completed: Boolean,
    val location: String = "",
    val addedBy: String
)
