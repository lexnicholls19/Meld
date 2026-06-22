package com.lexnicholls.lovecounter.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.CurrencyClient
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.ui.theme.MarketColor
import com.lexnicholls.lovecounter.util.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    deviceId: String,
    userName: String,
    userId: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val strings = t()
    
    val sharedPrefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
    val localCurrency = remember { sharedPrefs.getString("local_currency", "COP") ?: "COP" }
    
    var items by remember { mutableStateOf<List<ShoppingItem>>(emptyList()) }
    var tabs by remember { mutableStateOf(listOf(strings.hygiene, strings.food, strings.wishlist)) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showTabSettings by remember { mutableStateOf(false) }

    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
    
    val selectionStatus by remember {
        derivedStateOf {
            if (selectedIds.isEmpty()) null
            else {
                items.find { it.id == selectedIds.first() }?.bought
            }
        }
    }

    var exchangeRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    LaunchedEffect(localCurrency) {
        try {
            val response = CurrencyClient.api.getRates(localCurrency)
            exchangeRates = response.rates
        } catch (e: Exception) {
            Log.e("Currency", "Error fetching rates", e)
        }
    }

    DisposableEffect(userId) {
        val shoppingCollection = db.collection("users").document(userId).collection("shopping_list")
        val shoppingRegistration = shoppingCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error listening to shopping list", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    items = snapshot.documents.mapNotNull { doc ->
                        ShoppingItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            category = doc.getString("category") ?: strings.food,
                            bought = doc.getBoolean("bought") ?: false,
                            addedBy = doc.getString("addedBy") ?: strings.someone,
                            details = doc.getString("details") ?: "",
                            price = doc.getString("price") ?: "",
                            currency = doc.getString("currency") ?: localCurrency
                        )
                    }
                }
            }
            
        val categoriesDoc = db.collection("users").document(userId).collection("settings").document("shopping_categories")
        val categoriesRegistration = categoriesDoc.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val list = snapshot.get("list") as? List<String>
                if (list != null && list.isNotEmpty()) {
                    tabs = list
                }
            }
        }
        
        onDispose { 
            shoppingRegistration.remove()
            categoriesRegistration.remove()
        }
    }

    LaunchedEffect(selectedTab) {
        selectedIds.clear()
    }

    // Edit Dialog
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    if (editingItem != null && !isSelectionMode) {
        var text by remember { mutableStateOf(editingItem!!.name) }
        var details by remember { mutableStateOf(editingItem!!.details) }
        var price by remember { mutableStateOf(editingItem!!.price) }
        var selectedItemCurrency by remember { mutableStateOf(editingItem!!.currency) }
        val category = editingItem!!.category

        LoveAlertDialog(
            onDismissRequest = { editingItem = null },
            title = strings.edit + " " + category,
            onConfirm = {
                if (text.isNotBlank()) {
                    db.collection("users").document(userId).collection("shopping_list")
                        .document(editingItem!!.id)
                        .update(mapOf(
                            "name" to text,
                            "details" to details,
                            "price" to price,
                            "currency" to selectedItemCurrency
                        ))
                    editingItem = null
                }
            }
        ) {
            Column {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.product)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = details, onValueChange = { details = it }, label = strings.additionalData, isOptional = true)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Top) {
                    CurrencyDropdown(
                        selectedCurrency = selectedItemCurrency,
                        onCurrencyChange = { selectedItemCurrency = it },
                        modifier = Modifier.width(110.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    
                    LoveTextField(
                        value = price,
                        onValueChange = { input ->
                            if (input.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                                price = input.replace(',', '.')
                            }
                        },
                        label = strings.value,
                        isOptional = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (price.isNotEmpty() && selectedItemCurrency != localCurrency && exchangeRates.containsKey(selectedItemCurrency)) {
                    val rate = exchangeRates[selectedItemCurrency]!!
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    val converted = priceDouble / rate
                    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO"))
                    Text(
                        text = "${strings.approx} ${formatter.format(converted)} $localCurrency",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, start = 118.dp)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var text by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var selectedItemCurrency by remember { mutableStateOf(localCurrency) }
        val category = if (selectedTab < tabs.size) tabs[selectedTab] else ""

        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            title = strings.add + " " + category,
            onConfirm = {
                if (text.isNotBlank()) {
                    val item = hashMapOf(
                        "name" to text,
                        "category" to category,
                        "details" to details,
                        "price" to price,
                        "currency" to selectedItemCurrency,
                        "bought" to false,
                        "timestamp" to Timestamp.now(),
                        "addedBy" to userName
                    )
                    db.collection("users").document(userId).collection("shopping_list").add(item)
                        .addOnSuccessListener { onDismissDialog() }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        ) {
            Column {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.product)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = details, onValueChange = { details = it }, label = strings.additionalData, isOptional = true)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Top) {
                    CurrencyDropdown(
                        selectedCurrency = selectedItemCurrency,
                        onCurrencyChange = { selectedItemCurrency = it },
                        modifier = Modifier.width(110.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    LoveTextField(
                        value = price,
                        onValueChange = { input ->
                            if (input.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                                price = input.replace(',', '.')
                            }
                        },
                        label = strings.value,
                        isOptional = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (price.isNotEmpty() && selectedItemCurrency != localCurrency && exchangeRates.containsKey(selectedItemCurrency)) {
                    val rate = exchangeRates[selectedItemCurrency]!!
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    val converted = priceDouble / rate
                    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO"))
                    Text(
                        text = "${strings.approx} ${formatter.format(converted)} $localCurrency",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, start = 118.dp)
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isSelectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedIds.clear() }) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancel)
                    }
                    Text(text = "${selectedIds.size} ${strings.completed.lowercase()}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = {
                        val batch = db.batch()
                        val newStatus = selectionStatus != true
                        selectedIds.forEach { id ->
                            val docRef = db.collection("users").document(userId).collection("shopping_list").document(id)
                            batch.update(docRef, "bought", newStatus)
                        }
                        batch.commit()
                        selectedIds.clear()
                    }) {
                        Icon(
                            imageVector = if (selectionStatus == true) Icons.Default.Refresh else Icons.Default.Check,
                            contentDescription = if (selectionStatus == true) strings.restore else strings.confirm,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        val batch = db.batch()
                        selectedIds.forEach { id ->
                            val docRef = db.collection("users").document(userId).collection("shopping_list").document(id)
                            batch.delete(docRef)
                        }
                        batch.commit()
                        selectedIds.clear()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = Color.Red)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = strings.marketList, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val batch = db.batch()
                        val currentCategory = if (selectedTab < tabs.size) tabs[selectedTab] else ""
                        items.filter { it.category == currentCategory && it.bought }.forEach { item ->
                            val docRef = db.collection("users").document(userId).collection("shopping_list").document(item.id)
                            batch.delete(docRef)
                        }
                        batch.commit()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = strings.deleteBought, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showTabSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = strings.manageCategories, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF9575CD)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 14.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentList = remember(items, selectedTab, tabs) {
            val currentCategory = if (selectedTab < tabs.size) tabs[selectedTab] else ""
            items
                .filter { it.category == currentCategory }
                .sortedBy { it.bought }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(currentList, key = { it.id }) { item ->
                val isSelected = selectedIds.contains(item.id)
                ShoppingRow(
                    item = item,
                    isSelected = isSelected,
                    localCurrency = localCurrency,
                    exchangeRates = exchangeRates,
                    onToggle = {
                        db.collection("users").document(userId).collection("shopping_list")
                            .document(item.id).update("bought", !item.bought)
                    },
                    onDelete = {
                        db.collection("users").document(userId).collection("shopping_list")
                            .document(item.id).delete()
                    },
                    onClick = {
                        if (isSelectionMode) {
                            if (isSelected) {
                                selectedIds.remove(item.id)
                            } else {
                                if (selectionStatus == item.bought) {
                                    selectedIds.add(item.id)
                                } else {
                                    Toast.makeText(context, strings.cannotMixItems, Toast.LENGTH_SHORT).show()
                                }
                            }
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

    if (showTabSettings) {
        var newCategoryName by remember { mutableStateOf("") }
        var editingIndex by remember { mutableStateOf<Int?>(null) }
        var editingText by remember { mutableStateOf("") }
        
        LoveAlertDialog(
            onDismissRequest = { 
                showTabSettings = false
                editingIndex = null 
            },
            title = strings.manageCategories,
            confirmButtonText = strings.save,
            onConfirm = {
                db.collection("users").document(userId).collection("settings")
                    .document("shopping_categories")
                    .set(mapOf("list" to tabs))
                showTabSettings = false
                editingIndex = null
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(strings.manageCategories, fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, tab ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newList = tabs.toMutableList()
                                            val itm = newList.removeAt(index)
                                            newList.add(index - 1, itm)
                                            tabs = newList
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = if(index > 0) Color.Gray.copy(alpha = 0.6f) else Color.Transparent, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (index < tabs.size - 1) {
                                            val newList = tabs.toMutableList()
                                            val itm = newList.removeAt(index)
                                            newList.add(index + 1, itm)
                                            tabs = newList
                                        }
                                    },
                                    enabled = index < tabs.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = if(index < tabs.size - 1) Color.Gray.copy(alpha = 0.6f) else Color.Transparent, modifier = Modifier.size(18.dp))
                                }
                            }
                            
                            Spacer(Modifier.width(8.dp))

                            if (editingIndex == index) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            if (editingText.isNotBlank() && !tabs.contains(editingText)) {
                                                val oldName = tabs[index]
                                                val newList = tabs.toMutableList()
                                                newList[index] = editingText
                                                tabs = newList
                                                
                                                val batch = db.batch()
                                                items.filter { it.category == oldName }.forEach { item ->
                                                    val docRef = db.collection("users").document(userId).collection("shopping_list").document(item.id)
                                                    batch.update(docRef, "category", editingText)
                                                }
                                                batch.commit()
                                                
                                                editingIndex = null
                                            }
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = strings.save, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            } else {
                                Text(tab, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                
                                IconButton(onClick = {
                                    editingIndex = index
                                    editingText = tab
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                                if (tabs.size > 1) {
                                    IconButton(onClick = {
                                        val newList = tabs.toMutableList()
                                        newList.removeAt(index)
                                        tabs = newList
                                        if (selectedTab >= newList.size) selectedTab = 0
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                com.lexnicholls.lovecounter.ui.components.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text(strings.newCategory) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (newCategoryName.isNotBlank() && !tabs.contains(newCategoryName)) {
                            tabs = tabs + newCategoryName
                            newCategoryName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = strings.add, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencies = listOf("COP", "USD", "EUR")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            label = { Text(t().currency) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Start),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MarketColor,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = MarketColor,
                unfocusedLabelColor = Color.Gray
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencyChange(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShoppingRow(
    item: ShoppingItem,
    isSelected: Boolean,
    localCurrency: String,
    exchangeRates: Map<String, Double>,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val strings = t()
    val scope = rememberCoroutineScope()
    
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "offset")
    val threshold = 250f

    LaunchedEffect(item.bought) {
        offsetX = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    val newOffset = offsetX + delta
                    offsetX = newOffset.coerceIn(-500f, 500f)
                },
                onDragStopped = {
                    if (offsetX > threshold) {
                        onToggle()
                    } else if (offsetX < -threshold) {
                        onDelete()
                    }
                    scope.launch {
                        offsetX = 0f
                    }
                }
            )
            .background(
                color = when {
                    offsetX > 50 -> if (item.bought) Color(0xFFFFA500) else Color(0xFF4CAF50)
                    offsetX < -50 -> Color(0xFFF44336)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (offsetX > 50) {
            Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                Icon(if (item.bought) Icons.Default.Refresh else Icons.Default.Check, null, tint = Color.White)
            }
        } else if (offsetX < -50) {
            Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, null, tint = Color.White)
            }
        }

        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer 
                } else if (item.bought) {
                    Color(0xFF1C1C1E) 
                } else {
                    Color(0xFF2C2C2E)
                }
            ),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(if (item.bought) MarketColor.copy(alpha = 0.4f) else MarketColor)
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 18.sp,
                            color = if (item.bought) Color.Gray else Color.White,
                            fontWeight = FontWeight.Normal,
                            textDecoration = if (item.bought) TextDecoration.LineThrough else null
                        )
                        Text(text = "${strings.addedBy}: ${item.addedBy}", fontSize = 11.sp, color = Color.Gray)
                        
                        if (item.details.isNotBlank()) {
                            Text(
                                text = item.details,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (item.price.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            val formatter = NumberFormat.getNumberInstance(Locale("es", "CO"))
                            val priceDouble = item.price.toDoubleOrNull() ?: 0.0
                            
                            Text(
                                text = "${item.currency} ${formatter.format(priceDouble)}",
                                fontSize = 16.sp,
                                color = MarketColor.copy(alpha = if(item.bought) 0.5f else 1f),
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (item.currency != localCurrency && exchangeRates.containsKey(item.currency)) {
                                val rate = exchangeRates[item.currency]!!
                                val converted = priceDouble / rate
                                Text(
                                    text = "${strings.approx} ${formatter.format(converted)} $localCurrency",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    textDecoration = if (item.bought) TextDecoration.LineThrough else null
                                )
                            }
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

data class ShoppingItem(
    val id: String,
    val name: String,
    val category: String,
    val bought: Boolean,
    val addedBy: String,
    val details: String = "",
    val price: String = "",
    val currency: String = "COP"
)
