package com.lexnicholls.lovecounter.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.CurrencyClient
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.ui.components.LoveUndoSnackbar
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

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
    
    val selectionStatus by remember { derivedStateOf { if (selectedIds.isEmpty()) null else items.find { it.id == selectedIds.first() }?.bought } }
    var exchangeRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    LaunchedEffect(localCurrency) {
        try { exchangeRates = CurrencyClient.api.getRates(localCurrency).rates } 
        catch (e: Exception) { Log.e("Currency", "Error fetching rates", e) }
    }

    DisposableEffect(userId) {
        val shoppingCollection = db.collection("users").document(userId).collection("shopping_list")
        val shoppingRegistration = shoppingCollection.orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
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
            if (e == null && snapshot != null && snapshot.exists()) {
                val list = snapshot["list"] as? List<String>
                if (!list.isNullOrEmpty()) tabs = list
            }
        }
        onDispose { shoppingRegistration.remove(); categoriesRegistration.remove() }
    }

    LaunchedEffect(selectedTab) { selectedIds.clear() }

    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    if (editingItem != null && !isSelectionMode) {
        var text by remember { mutableStateOf(editingItem!!.name) }
        var details by remember { mutableStateOf(editingItem!!.details) }
        var price by remember { mutableStateOf(editingItem!!.price) }
        var selectedItemCurrency by remember { mutableStateOf(editingItem!!.currency) }
        LoveAlertDialog(
            onDismissRequest = { editingItem = null },
            title = strings.edit,
            onConfirm = {
                if (text.isNotBlank()) {
                    db.collection("users").document(userId).collection("shopping_list").document(editingItem!!.id)
                        .update(mapOf("name" to text, "details" to details, "price" to price, "currency" to selectedItemCurrency))
                    editingItem = null
                }
            }
        ) {
            Column {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.product)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = details, onValueChange = { details = it }, label = strings.additionalData, isOptional = true)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top) {
                    CurrencyDropdown(selectedCurrency = selectedItemCurrency, onCurrencyChange = { selectedItemCurrency = it }, modifier = Modifier.width(110.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        LoveTextField(value = price, onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' }) price = input.replace(',', '.') }, label = strings.value, isOptional = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        getConvertedPrice(price, selectedItemCurrency, localCurrency, exchangeRates)?.let { converted ->
                            Text(text = converted, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                        }
                    }
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
                    val item = hashMapOf("name" to text, "category" to category, "details" to details, "price" to price, "currency" to selectedItemCurrency, "bought" to false, "timestamp" to Timestamp.now(), "addedBy" to userName)
                    db.collection("users").document(userId).collection("shopping_list").add(item).addOnSuccessListener { onDismissDialog() }
                }
            }
        ) {
            Column {
                LoveTextField(value = text, onValueChange = { text = it }, label = strings.product)
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = details, onValueChange = { details = it }, label = strings.additionalData, isOptional = true)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top) {
                    CurrencyDropdown(selectedCurrency = selectedItemCurrency, onCurrencyChange = { selectedItemCurrency = it }, modifier = Modifier.width(110.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        LoveTextField(value = price, onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' }) price = input.replace(',', '.') }, label = strings.value, isOptional = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        getConvertedPrice(price, selectedItemCurrency, localCurrency, exchangeRates)?.let { converted ->
                            Text(text = converted, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            if (isSelectionMode) {
                SelectionHeader(selectedCount = selectedIds.size, onCancel = { selectedIds.clear() }, onDelete = {
                    val batch = db.batch()
                    selectedIds.forEach { id -> batch.delete(db.collection("users").document(userId).collection("shopping_list").document(id)) }
                    batch.commit().addOnSuccessListener { selectedIds.clear() }
                })
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = strings.marketList, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Row {
                        IconButton(onClick = {
                            val batch = db.batch()
                            val currentCategory = if (selectedTab < tabs.size) tabs[selectedTab] else ""
                            items.filter { it.category == currentCategory && it.bought }.forEach { batch.delete(db.collection("users").document(userId).collection("shopping_list").document(it.id)) }
                            batch.commit()
                        }) { Icon(Icons.Default.DeleteSweep, null, tint = Color.Gray.copy(alpha = 0.6f)) }
                        IconButton(onClick = { showTabSettings = true }) { Icon(Icons.Default.Settings, null, tint = Color.Gray.copy(alpha = 0.6f)) }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary, divider = {}, indicator = { tabPositions -> if (selectedTab < tabPositions.size) { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = MaterialTheme.colorScheme.primary) } }) {
                tabs.forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 14.sp) }) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val rawList = remember(items, selectedTab, tabs) {
                val currentCategory = if (selectedTab < tabs.size) tabs[selectedTab] else ""
                items.filter { it.category == currentCategory }.sortedBy { it.bought }
            }
            val currentList = remember(rawList, pendingDeleteId) {
                rawList.filter { it.id != pendingDeleteId }
            }

            if (currentList.isEmpty()) {
                EmptyMarketState()
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val totalPending = currentList.count { !it.bought }
                        val totalPrice = currentList.filter { !it.bought && it.price.isNotBlank() && it.currency == localCurrency }.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
                        if (totalPending > 0) {
                            MarketHeroCard(count = totalPending, total = totalPrice, currencyCode = localCurrency)
                        }
                    }

                    items(currentList, key = { it.id }) { item ->
                        ShoppingRowModern(
                            item = item,
                            isSelected = selectedIds.contains(item.id),
                            localCurrency = localCurrency,
                            rates = exchangeRates,
                            onToggle = { db.collection("users").document(userId).collection("shopping_list").document(item.id).update("bought", !item.bought) },
                            onDelete = { 
                                pendingDeleteId?.let { oldId ->
                                    db.collection("users").document(userId).collection("shopping_list").document(oldId).delete()
                                }
                                pendingDeleteId = item.id 
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedIds.contains(item.id)) selectedIds.remove(item.id) else selectedIds.add(item.id)
                                } else editingItem = item
                            },
                            onLongClick = { if (!isSelectionMode) selectedIds.add(item.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        LoveUndoSnackbar(
            show = pendingDeleteId != null,
            onUndo = { pendingDeleteId = null },
            onDismiss = {
                pendingDeleteId?.let { id ->
                    db.collection("users").document(userId).collection("shopping_list").document(id).delete()
                }
                pendingDeleteId = null
            }
        )
    }

    if (showTabSettings) {
        var newCategoryName by remember { mutableStateOf("") }
        var editingIndex by remember { mutableStateOf<Int?>(null) }
        var editingText by remember { mutableStateOf("") }
        
        LoveAlertDialog(
            onDismissRequest = { showTabSettings = false; editingIndex = null },
            title = strings.manageCategories,
            onConfirm = {
                db.collection("users").document(userId).collection("settings").document("shopping_categories").set(mapOf("list" to tabs))
                showTabSettings = false; editingIndex = null
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                    tabs.forEachIndexed { index, tab ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(tab, modifier = Modifier.weight(1f))
                            IconButton(onClick = { 
                                val newList = tabs.toMutableList()
                                newList.removeAt(index)
                                tabs = newList
                            }, enabled = tabs.size > 1) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f)) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoveTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = strings.newCategory, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (newCategoryName.isNotBlank()) { tabs = tabs + newCategoryName; newCategoryName = "" } }) { Icon(Icons.Default.Add, null) }
                }
            }
        }
    }
}

@Composable
fun MarketHeroCard(count: Int, total: Double, currencyCode: String) {
    val strings = t()
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(110.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp),
                tint = MarketColor.copy(alpha = 0.05f)
            )
            
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MarketColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MarketColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "$count", 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MarketColor, 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(text = strings.pending.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MarketColor)
                    if (total > 0) {
                        val format = NumberFormat.getCurrencyInstance().apply { 
                            currency = java.util.Currency.getInstance(currencyCode)
                            maximumFractionDigits = 0
                        }
                        Text(text = "Total: ${format.format(total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        Text(text = strings.marketList, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.ShoppingRowModern(
    item: ShoppingItem,
    isSelected: Boolean,
    localCurrency: String,
    rates: Map<String, Double>,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true }
            else if (it == SwipeToDismissBoxValue.StartToEnd) { onToggle(); false }
            else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled || dismissState.currentValue != SwipeToDismissBoxValue.Settled
            if (isSwiping) {
                val color = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> if (item.bought) Color(0xFFFFA500) else Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }
                Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(color), contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) {
                    Icon(if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) (if(item.bought) Icons.Default.Refresh else Icons.Default.Check) else Icons.Default.Delete, null, tint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.White, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        },
        modifier = Modifier.animateItem()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else if (item.bought) MaterialTheme.colorScheme.surface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = if(item.bought) Color.Gray.copy(alpha = 0.1f) else MarketColor.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (item.bought) Icons.Default.CheckCircle else Icons.Default.ShoppingCart, null, tint = if (item.bought) Color.Gray else MarketColor, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name, 
                        fontSize = 17.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (item.bought) Color.Gray else MaterialTheme.colorScheme.onSurface, 
                        textDecoration = if (item.bought) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.details.isNotBlank()) {
                        Text(
                            text = item.details, 
                            fontSize = 12.sp, 
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (item.price.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "${item.currency} ${item.price}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (item.bought) Color.Gray else MarketColor)
                        getConvertedPrice(item.price, item.currency, localCurrency, rates)?.let { converted ->
                            Text(text = converted, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(selectedCurrency: String, onCurrencyChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(value = selectedCurrency, onValueChange = {}, readOnly = true, label = { Text(t().currency) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable), shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("COP", "USD", "EUR").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onCurrencyChange(it); expanded = false }) }
        }
    }
}

@Composable
fun EmptyMarketState() {
    val strings = t()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.RemoveShoppingCart, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text(text = strings.noItemsYet, color = Color.Gray, fontWeight = FontWeight.Bold)
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

private fun getConvertedPrice(price: String, itemCurrency: String, localCurrency: String, rates: Map<String, Double>): String? {
    if (price.isBlank() || itemCurrency == localCurrency || rates.isEmpty()) return null
    val priceValue = price.toDoubleOrNull() ?: return null
    val rate = rates[itemCurrency] ?: return null
    if (rate == 0.0) return null
    val convertedValue = priceValue / rate
    val format = NumberFormat.getCurrencyInstance().apply { 
        currency = java.util.Currency.getInstance(localCurrency)
        maximumFractionDigits = 0
    }
    return "Aprox. ${format.format(convertedValue)}"
}
