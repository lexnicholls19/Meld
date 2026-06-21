package com.lexnicholls.lovecounter.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
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
    
    val sharedPrefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
    val localCurrency = remember { sharedPrefs.getString("local_currency", "COP") ?: "COP" }
    
    var items by remember { mutableStateOf<List<ShoppingItem>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Aseo", "Comida", "Lista de deseos")

    // Multi-selection state
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
    
    // Validation: Get the status of the current selection (null if empty, true if all bought, false if all pending)
    val selectionStatus by remember {
        derivedStateOf {
            if (selectedIds.isEmpty()) null
            else {
                items.find { it.id == selectedIds.first() }?.bought
            }
        }
    }

    // Currency rates for conversion
    var exchangeRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    LaunchedEffect(localCurrency) {
        try {
            val response = CurrencyClient.api.getRates(localCurrency)
            exchangeRates = response.rates
        } catch (e: Exception) {
            Log.e("Currency", "Error fetching rates", e)
        }
    }

    // SnapshotListener optimized for real-time reactivity
    DisposableEffect(userId) {
        val collection = db.collection("users").document(userId).collection("shopping_list")
        val registration = collection
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
                            category = doc.getString("category") ?: "Comida",
                            bought = doc.getBoolean("bought") ?: false,
                            addedBy = doc.getString("addedBy") ?: "Alguien",
                            details = doc.getString("details") ?: "",
                            price = doc.getString("price") ?: "",
                            currency = doc.getString("currency") ?: localCurrency
                        )
                    }
                }
            }
        onDispose { registration.remove() }
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
        val isWishlist = category == "Lista de deseos"

        LoveAlertDialog(
            onDismissRequest = { editingItem = null },
            title = "Editar en $category",
            onConfirm = {
                if (text.isNotBlank()) {
                    db.collection("users").document(userId).collection("shopping_list")
                        .document(editingItem!!.id)
                        .update(mapOf(
                            "name" to text,
                            "details" to details,
                            "price" to price,
                            "currency" to (if (isWishlist) selectedItemCurrency else localCurrency)
                        ))
                    editingItem = null
                }
            }
        ) {
            Column {
                LoveTextField(value = text, onValueChange = { text = it }, label = "Nombre del producto")
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = details, onValueChange = { details = it }, label = "Datos adicionales", isOptional = true)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Top) {
                    if (isWishlist) {
                        CurrencyDropdown(
                            selectedCurrency = selectedItemCurrency,
                            onCurrencyChange = { selectedItemCurrency = it },
                            modifier = Modifier.width(110.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    LoveTextField(
                        value = price,
                        onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                        label = "Valor",
                        isOptional = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isWishlist && price.isNotEmpty() && selectedItemCurrency != localCurrency && exchangeRates.containsKey(selectedItemCurrency)) {
                    val rate = exchangeRates[selectedItemCurrency]!!
                    val converted = price.toDoubleOrNull()?.let { (it / rate) } ?: 0.0
                    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO"))
                    Text(
                        text = "aprox. ${formatter.format(converted.toLong())} $localCurrency",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, start = if (isWishlist) 118.dp else 0.dp)
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
        val category = tabs[selectedTab]
        val isWishlist = category == "Lista de deseos"

        LoveAlertDialog(
            onDismissRequest = onDismissDialog,
            title = "Añadir a $category",
            onConfirm = {
                if (text.isNotBlank()) {
                    val item = hashMapOf(
                        "name" to text,
                        "category" to category,
                        "details" to details,
                        "price" to price,
                        "currency" to (if (isWishlist) selectedItemCurrency else localCurrency),
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
                LoveTextField(value = text, onValueChange = { text = it }, label = "Nombre del producto")
                Spacer(Modifier.height(8.dp))
                LoveTextField(value = details, onValueChange = { details = it }, label = "Datos adicionales", isOptional = true)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Top) {
                    if (isWishlist) {
                        CurrencyDropdown(
                            selectedCurrency = selectedItemCurrency,
                            onCurrencyChange = { selectedItemCurrency = it },
                            modifier = Modifier.width(110.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    LoveTextField(
                        value = price,
                        onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                        label = "Valor",
                        isOptional = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isWishlist && price.isNotEmpty() && selectedItemCurrency != localCurrency && exchangeRates.containsKey(selectedItemCurrency)) {
                    val rate = exchangeRates[selectedItemCurrency]!!
                    val converted = price.toDoubleOrNull()?.let { (it / rate) } ?: 0.0
                    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO"))
                    Text(
                        text = "aprox. ${formatter.format(converted.toLong())} $localCurrency",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, start = if (isWishlist) 118.dp else 0.dp)
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
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                    Text(text = "${selectedIds.size} seleccionados", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    // NEW: Bulk Toggle Action (Complete/Restore)
                    IconButton(onClick = {
                        val batch = db.batch()
                        val newStatus = selectionStatus != true // Toggle current status
                        selectedIds.forEach { id ->
                            val docRef = db.collection("users").document(userId).collection("shopping_list").document(id)
                            batch.update(docRef, "bought", newStatus)
                        }
                        batch.commit()
                        selectedIds.clear()
                    }) {
                        Icon(
                            imageVector = if (selectionStatus == true) Icons.Default.Refresh else Icons.Default.Check,
                            contentDescription = if (selectionStatus == true) "Restaurar" else "Completar",
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
                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Lista de Mercado", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    val batch = db.batch()
                    items.filter { it.category == tabs[selectedTab] && it.bought }.forEach { item ->
                        val docRef = db.collection("users").document(userId).collection("shopping_list").document(item.id)
                        batch.delete(docRef)
                    }
                    batch.commit()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar Comprados", tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
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

        // Sort items: bought items at the end
        val currentList = remember(items, selectedTab) {
            items
                .filter { it.category == tabs[selectedTab] }
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
                            // Validation: Only allow selecting items with the SAME status
                            if (isSelected) {
                                selectedIds.remove(item.id)
                            } else {
                                if (selectionStatus == item.bought) {
                                    selectedIds.add(item.id)
                                } else {
                                    Toast.makeText(context, "No puedes mezclar items pendientes y comprados", Toast.LENGTH_SHORT).show()
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
            label = { Text("Moneda") },
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
    val isWishlist = item.category == "Lista de deseos"
    val scope = rememberCoroutineScope()
    
    // CUSTOM GESTURE REPLACEMENT FOR SWIPETODISMISSBOX
    // We use Draggable + manual state to ENSURE it never locks up
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "offset")
    val threshold = 250f

    // Reset visually when database changes
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
                    // Resistance and constraints
                    offsetX = newOffset.coerceIn(-500f, 500f)
                },
                onDragStopped = {
                    if (offsetX > threshold) {
                        onToggle()
                    } else if (offsetX < -threshold) {
                        if (isWishlist) onDelete() else onToggle()
                    }
                    scope.launch {
                        offsetX = 0f
                    }
                }
            )
            .background(
                color = when {
                    offsetX > 50 -> if (item.bought) Color(0xFFFFA500) else Color(0xFF4CAF50)
                    offsetX < -50 -> if (isWishlist) Color(0xFFF44336) else (if (item.bought) Color(0xFFFFA500) else Color(0xFF4CAF50))
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Background Icons
        if (offsetX > 50) {
            Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                Icon(if (item.bought) Icons.Default.Refresh else Icons.Default.Check, null, tint = Color.White)
            }
        } else if (offsetX < -50) {
            Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(if (isWishlist) Icons.Default.Delete else (if (item.bought) Icons.Default.Refresh else Icons.Default.Check), null, tint = Color.White)
            }
        }

        // Main Item Card
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
                        Text(text = "Añadido por: ${item.addedBy}", fontSize = 11.sp, color = Color.Gray)
                        
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
                            val priceLong = item.price.toLongOrNull() ?: 0L
                            
                            Text(
                                text = "${item.currency} ${formatter.format(priceLong)}",
                                fontSize = 16.sp,
                                color = MarketColor.copy(alpha = if(item.bought) 0.5f else 1f),
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (item.currency != localCurrency && exchangeRates.containsKey(item.currency)) {
                                val rate = exchangeRates[item.currency]!!
                                val converted = (priceLong / rate).toLong()
                                Text(
                                    text = "aprox. ${formatter.format(converted)} $localCurrency",
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
