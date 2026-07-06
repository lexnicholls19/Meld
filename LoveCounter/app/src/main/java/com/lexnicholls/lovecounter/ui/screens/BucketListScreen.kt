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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.ui.components.LoveUndoSnackbar
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
    val db = FirebaseFirestore.getInstance()
    val strings = t()
    val context = LocalContext.current
    
    var categories by remember { mutableStateOf<List<BucketCategory>>(emptyList()) }
    var expandedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val categoryStats = remember { mutableStateMapOf<String, Pair<Int, Int>>() }

    var pendingDeleteInfo by remember { mutableStateOf<Pair<String, String>?>(null) } // (categoryId, itemId)
    val scope = rememberCoroutineScope()
    
    val totalItems = categoryStats.values.sumOf { it.second }
    val completedItems = categoryStats.values.sumOf { it.first }
    val progress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f

    DisposableEffect(userId) {
        val registration = db.collection("users").document(userId).collection("bucket_list")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    val newCategories = snapshot.documents.map { doc ->
                        BucketCategory(id = doc.id, title = doc.getString("title") ?: "", addedBy = doc.getString("addedBy") ?: strings.someone)
                    }
                    categoryStats.keys.retainAll(newCategories.map { it.id }.toSet())
                    categories = newCategories
                }
            }
        onDispose { registration.remove() }
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
            title = strings.addNew,
            onConfirm = {
                if (!isSubItemMode && categoryTitle.isNotBlank()) {
                    db.collection("users").document(userId).collection("bucket_list").add(hashMapOf("title" to categoryTitle, "timestamp" to Timestamp.now(), "addedBy" to userName))
                        .addOnSuccessListener { onDismissDialog() }
                } else if (isSubItemMode && selectedParentId.isNotBlank() && itemTitle.isNotBlank()) {
                    db.collection("users").document(userId).collection("bucket_list").document(selectedParentId).collection("items").add(hashMapOf("title" to itemTitle, "description" to itemDesc, "location" to itemLocation, "completed" to false, "timestamp" to Timestamp.now(), "addedBy" to userName))
                        .addOnSuccessListener { onDismissDialog() }
                } else if (isSubItemMode && selectedParentId.isBlank()) {
                    Toast.makeText(context, strings.selectParentError, Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cat", fontSize = 12.sp, color = if(!isSubItemMode) MaterialTheme.colorScheme.primary else Color.Gray)
                    Switch(checked = isSubItemMode, onCheckedChange = { isSubItemMode = it }, modifier = Modifier.scale(0.7f).padding(horizontal = 4.dp))
                    Text("Item", fontSize = 12.sp, color = if(isSubItemMode) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                if (!isSubItemMode) { LoveTextField(value = categoryTitle, onValueChange = { categoryTitle = it }, label = strings.categoryName) }
                else {
                    Box {
                        OutlinedCard(onClick = { dropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedParentTitle, modifier = Modifier.padding(16.dp)) }
                        DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            categories.forEach { DropdownMenuItem(text = { Text(it.title) }, onClick = { selectedParentId = it.id; selectedParentTitle = it.title; dropdownExpanded = false }) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LoveTextField(value = itemTitle, onValueChange = { itemTitle = it }, label = strings.name)
                    Spacer(Modifier.height(8.dp))
                    LoveTextField(value = itemDesc, onValueChange = { itemDesc = it }, label = strings.description)
                    Spacer(Modifier.height(8.dp))
                    LoveTextField(value = itemLocation, onValueChange = { itemLocation = it }, label = strings.location, isOptional = true)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(text = strings.bucket, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            
            Spacer(Modifier.height(16.dp))
            BucketHeroCard(progress = progress, completed = completedItems, total = totalItems)
            
            Spacer(Modifier.height(24.dp))
            
            if (categories.isEmpty()) { EmptyBucketState() }
            else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(categories, key = { it.id }) { category ->
                        CategoryItemModern(
                            category = category,
                            userId = userId,
                            db = db,
                            userName = userName,
                            isExpanded = expandedCategoryId == category.id,
                            onExpandedChange = { expanded -> expandedCategoryId = if (expanded) category.id else null },
                            onStatsChange = { c, t -> categoryStats[category.id] = Pair(c, t) },
                            pendingDeleteItemId = if (pendingDeleteInfo?.first == category.id) pendingDeleteInfo?.second else null,
                            onSwipeToDelete = { itemId -> 
                                pendingDeleteInfo?.let { (oldCatId, oldItemId) ->
                                    db.collection("users").document(userId).collection("bucket_list").document(oldCatId).collection("items").document(oldItemId).delete()
                                }
                                pendingDeleteInfo = category.id to itemId 
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        LoveUndoSnackbar(
            show = pendingDeleteInfo != null,
            onUndo = { pendingDeleteInfo = null },
            onDismiss = {
                pendingDeleteInfo?.let { (catId, itemId) ->
                    db.collection("users").document(userId).collection("bucket_list").document(catId).collection("items").document(itemId).delete()
                }
                pendingDeleteInfo = null
            }
        )
    }
}

@Composable
fun BucketHeroCard(progress: Float, completed: Int, total: Int) {
    val strings = t()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Fondo decorativo sutil
            Icon(
                Icons.Default.Explore,
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp),
                tint = BucketColor.copy(alpha = 0.05f)
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Surface(
                    color = BucketColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, BucketColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BucketColor
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.adventureProgress,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = BucketColor,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$completed / $total ${strings.bucket.lowercase()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryItemModern(category: BucketCategory, userId: String, db: FirebaseFirestore, userName: String, isExpanded: Boolean, onExpandedChange: (Boolean) -> Unit, onStatsChange: (Int, Int) -> Unit, pendingDeleteItemId: String?, onSwipeToDelete: (String) -> Unit) {
    val strings = t()
    var rawSubItems by remember { mutableStateOf<List<BucketSubItem>>(emptyList()) }
    val subItems = remember(rawSubItems, pendingDeleteItemId) {
        rawSubItems.filter { it.id != pendingDeleteItemId }
    }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingSubItem by remember { mutableStateOf<BucketSubItem?>(null) }

    LaunchedEffect(category.id) {
        db.collection("users").document(userId).collection("bucket_list").document(category.id).collection("items")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        BucketSubItem(id = doc.id, title = doc.getString("title") ?: "", description = doc.getString("description") ?: "", completed = doc.getBoolean("completed") ?: false, location = doc.getString("location") ?: "", addedBy = doc.getString("addedBy") ?: strings.someone)
                    }
                    rawSubItems = list
                    onStatsChange(list.count { it.completed }, list.size)
                }
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onExpandedChange(!isExpanded) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = BucketColor.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(if(isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BucketColor) }
                }
                Spacer(Modifier.width(16.dp))
                Text(text = category.title, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddTaskDialog = true }) { Icon(Icons.Default.Add, null, tint = Color.Gray) }
            }
        }

        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 8.dp)) {
                subItems.forEach { item ->
                    BucketSubItemRowModern(
                        item = item,
                        onToggle = { db.collection("users").document(userId).collection("bucket_list").document(category.id).collection("items").document(item.id).update("completed", !item.completed) },
                        onDelete = { onSwipeToDelete(item.id) },
                        onClick = { editingSubItem = item }
                    )
                }
            }
        }
    }
    
    if (showAddTaskDialog) {
        var text by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        LoveAlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = strings.addNew,
            onConfirm = {
                if (text.isNotBlank()) {
                    db.collection("users").document(userId).collection("bucket_list").document(category.id).collection("items").add(hashMapOf("title" to text, "description" to description, "location" to location, "completed" to false, "timestamp" to Timestamp.now(), "addedBy" to userName))
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

    if (editingSubItem != null) {
        var text by remember { mutableStateOf(editingSubItem!!.title) }
        var description by remember { mutableStateOf(editingSubItem!!.description) }
        var location by remember { mutableStateOf(editingSubItem!!.location) }
        
        LoveAlertDialog(
            onDismissRequest = { editingSubItem = null },
            title = strings.edit,
            onConfirm = {
                if (text.isNotBlank()) {
                    db.collection("users").document(userId).collection("bucket_list").document(category.id).collection("items").document(editingSubItem!!.id).update(mapOf("title" to text, "description" to description, "location" to location))
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BucketSubItemRowModern(item: BucketSubItem, onToggle: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val strings = t()
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true }
        else if (it == SwipeToDismissBoxValue.StartToEnd) { onToggle(); false }
        else false
    })
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
                Box(
                    modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(color),
                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    if (icon != null) Icon(
                        icon, 
                        contentDescription = null, 
                        tint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.White, 
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = BucketColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = BucketColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title, 
                        fontSize = 17.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if(item.completed) Color.Gray else MaterialTheme.colorScheme.onSurface, 
                        textDecoration = if(item.completed) TextDecoration.LineThrough else null
                    )
                    if(item.description.isNotBlank()) {
                        Text(
                            text = item.description, 
                            fontSize = 13.sp, 
                            color = Color.Gray,
                            textDecoration = if(item.completed) TextDecoration.LineThrough else null
                        )
                    }
                    if (item.location.isNotBlank()) {
                        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp), tint = Color.Red.copy(alpha = 0.6f))
                            Spacer(Modifier.width(4.dp))
                            Text(text = item.location, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
                if (item.location.isNotBlank()) {
                    IconButton(onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(item.location)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Map, contentDescription = strings.viewOnMap, tint = BucketColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyBucketState() {
    val strings = t()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Explore, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text(text = strings.noItemsYet, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

class BucketCategory(val id: String, val title: String, val addedBy: String)
data class BucketSubItem(val id: String, val title: String, val description: String, val completed: Boolean, val location: String, val addedBy: String)
