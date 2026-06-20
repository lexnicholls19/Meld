package com.lexnicholls.lovecounter

import androidx.activity.compose.BackHandler
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit
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
    val categories = listOf(strings.hygiene, strings.food, strings.wishlist)
    val isWishlist = categories[selectedTabIndex] == strings.wishlist

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
    var itemToEdit by remember { mutableStateOf<ImportantDate?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Deletion Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(strings.delete) },
            text = { Text("${strings.deleteConfirm} '${item.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("market_list").document(item.id)
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

    // Delete All Completed Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(strings.clearList) },
            text = { Text(strings.clearListConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        val completedItems = itemsList.filter { it.isCompleted }
                        if (completedItems.isNotEmpty()) {
                            val batch = db.batch()
                            completedItems.forEach { item ->
                                val docRef = db.collection("market_list").document(item.id)
                                batch.delete(docRef)
                            }
                            batch.commit().addOnSuccessListener {
                                Toast.makeText(context, "${strings.clearList} ✨", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                        }
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Fetch items from Firestore (Real-time Sync)
    DisposableEffect(refreshTrigger) {
        isLoading = true
        val registration = db.collection("market_list")
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
    BackHandler(enabled = showDeleteAllDialog) {
        showDeleteAllDialog = false
    }

    if (showAddDialog || itemToEdit != null) {
        val currentCategory = categories[selectedTabIndex]
        AddMarketItemDialog(
            initialItem = itemToEdit,
            currentCategory = currentCategory,
            onDismiss = {
                onDismissDialog()
                itemToEdit = null
            },
            onConfirm = { name, category, quantity, price, currency ->
                val data = hashMapOf(
                    "name" to name,
                    "type" to category,
                    "value" to quantity,
                    "price" to price,
                    "currency" to currency,
                    "createdBy" to (itemToEdit?.createdBy ?: userName),
                    "userName" to userName,
                    "isCompleted" to (itemToEdit?.isCompleted ?: false),
                    "senderId" to deviceId,
                    "deviceId" to deviceId
                )
                
                if (itemToEdit == null) {
                    db.collection("market_list").add(data)
                        .addOnSuccessListener {
                            Toast.makeText(context, "¡Añadido a $currentCategory! 🛒", Toast.LENGTH_SHORT).show()
                            refreshTrigger++
                            onDismissDialog()
                        }
                } else {
                    db.collection("market_list").document(itemToEdit!!.id).update(data as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Actualizado ✅", Toast.LENGTH_SHORT).show()
                            refreshTrigger++
                            itemToEdit = null
                        }
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
                        text = strings.market, 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (!isWishlist) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = strings.clearList,
                                tint = MaterialTheme.colorScheme.error
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

                Box(modifier = Modifier.weight(1f)) { // Contenedor flexible
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val currentCategory = categories[selectedTabIndex]
                        val filteredItems = itemsList.filter { it.type == currentCategory }

                        if (filteredItems.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay elementos en $currentCategory", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    if (isWishlist) {
                                        SwipeToCompleteWrapper(
                                            onSwipeRight = {}, // No complete in wishlist
                                            onSwipeLeft = { itemToDelete = item },
                                            enableRightSwipe = false,
                                            content = {
                                                MarketItemCard(
                                                    item = item,
                                                    showCheck = false,
                                                    onToggleComplete = {},
                                                    onClick = { itemToEdit = item },
                                                    onLongClick = { itemToDelete = item }
                                                )
                                            }
                                        )
                                    } else {
                                        MarketItemCard(
                                            item = item,
                                            onToggleComplete = {
                                                db.collection("market_list").document(item.id)
                                                    .update(
                                                        "isCompleted", !item.isCompleted,
                                                        "senderId", deviceId,
                                                        "deviceId", deviceId,
                                                        "userName", userName,
                                                        "senderName", userName
                                                    )
                                                    .addOnSuccessListener { refreshTrigger++ }
                                        },
                                            onClick = { itemToEdit = item },
                                            onLongClick = { itemToDelete = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Barra invisible para reservar espacio sobre los botones flotantes
                Spacer(modifier = Modifier.height(80.dp))
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
fun MarketItemCard(
    item: ImportantDate,
    showCheck: Boolean = true,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val indicatorColor = Color(0xFF4CAF50) // Mercado siempre verde
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCompleted) 0.dp else 2.dp)
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
                    .background(indicatorColor)
            )
            
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleLarge,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isCompleted) Color.Gray else Color.Unspecified
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.value.isNotBlank()) {
                            Text(
                                text = item.value,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = "Añadido por: ${if (item.createdBy.isNotBlank()) item.createdBy else "Alguien"}",
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                
                if (item.price.isNotBlank()) {
                    val sharedPrefs = LocalContext.current.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    val globalCurrency = sharedPrefs.getString("local_currency", "COP") ?: "COP"
                    val currency = if (item.currency.isNotBlank()) item.currency else globalCurrency

                    var rate by remember(currency, globalCurrency) { mutableStateOf<Double?>(null) }
                    LaunchedEffect(currency, globalCurrency) {
                        if (currency != globalCurrency && currency.isNotBlank()) {
                            try {
                                val response = CurrencyClient.api.getRates(currency)
                                rate = response.rates[globalCurrency]
                            } catch (e: Exception) { }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$currency ${formatPrice(item.price, currency)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        if (rate != null) {
                            val converted = item.price.toDoubleOrNull()?.let { it * rate!! }
                            if (converted != null) {
                                Text(
                                    text = "Aprox. $globalCurrency ${formatPrice(converted.toLong().toString(), globalCurrency)}",
                                    fontSize = 11.sp,
                                    color = Color.Gray.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (showCheck) {
                    RadioButton(
                        selected = item.isCompleted,
                        onClick = onToggleComplete
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMarketItemDialog(
    initialItem: ImportantDate? = null,
    currentCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    val strings = t()
    val sharedPrefs = LocalContext.current.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val globalCurrency = sharedPrefs.getString("local_currency", "COP") ?: "COP"

    var isEditMode by remember { mutableStateOf(initialItem == null) }
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var quantity by remember { mutableStateOf(initialItem?.value ?: "") }
    var price by remember { mutableStateOf(initialItem?.price ?: "") }
    var selectedCurrency by remember { 
        mutableStateOf(if (initialItem?.currency?.isNotBlank() == true) initialItem.currency else globalCurrency) 
    }
    
    var exchangeRate by remember { mutableStateOf<Double?>(null) }
    
    LaunchedEffect(selectedCurrency, globalCurrency) {
        if (selectedCurrency != globalCurrency) {
            try {
                val response = CurrencyClient.api.getRates(selectedCurrency)
                exchangeRate = response.rates[globalCurrency]
            } catch (e: Exception) {
                exchangeRate = null
            }
        } else {
            exchangeRate = null
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
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
                                 else if (!isEditMode) strings.details 
                                 else "${strings.edit} item"
                Text(dialogTitle)
                if (initialItem != null && !isEditMode) {
                    IconButton(onClick = { isEditMode = true }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.product) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = !isEditMode,
                    colors = fieldColors
                )
                if (isEditMode || quantity.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("${strings.details} ${strings.optional}") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = !isEditMode,
                        colors = fieldColors
                    )
                }
                if (isEditMode || price.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentCategory == strings.wishlist && isEditMode) {
                            var currencyExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.width(100.dp).fillMaxHeight()) {
                                ExposedDropdownMenuBox(
                                    expanded = currencyExpanded,
                                    onExpandedChange = { currencyExpanded = !currencyExpanded }
                                ) {
                                    TextField(
                                        value = selectedCurrency,
                                        onValueChange = {},
                                        readOnly = true,
                                        colors = fieldColors,
                                        label = { Text(strings.localCurrency, fontSize = 10.sp) },
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                        trailingIcon = { 
                                            Icon(
                                                Icons.Default.ArrowDropDown, 
                                                null, 
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        modifier = Modifier.menuAnchor().fillMaxSize()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = currencyExpanded,
                                        onDismissRequest = { currencyExpanded = false }
                                    ) {
                                        listOf("COP", "USD", "EUR").forEach { curr ->
                                            DropdownMenuItem(
                                                text = { Text(curr) },
                                                onClick = {
                                                    selectedCurrency = curr
                                                    currencyExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        TextField(
                            value = price,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() }) price = it 
                            },
                            label = { Text("${strings.value} (${strings.optional})") },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            prefix = { 
                                if (! (currentCategory == strings.wishlist && isEditMode)) {
                                    Text("$selectedCurrency ") 
                                }
                            },
                            readOnly = !isEditMode,
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PriceVisualTransformation(selectedCurrency)
                        )
                    }

                    if (exchangeRate != null && price.isNotBlank()) {
                        val converted = price.toDoubleOrNull()?.let { it * exchangeRate!! }
                        if (converted != null) {
                            Text(
                                text = "${strings.approx} $globalCurrency ${formatPrice(converted.toLong().toString(), globalCurrency)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, start = if (currentCategory == strings.wishlist && isEditMode) 108.dp else 0.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (!isEditMode && price.isNotBlank()) {
                    // Modo lectura (solo texto)
                    val currency = if (initialItem?.currency?.isNotBlank() == true) initialItem.currency else globalCurrency
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = formatPrice(price, currency),
                        onValueChange = {},
                        label = { Text(strings.value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("$currency ") },
                        readOnly = true,
                        colors = fieldColors
                    )
                    
                    if (exchangeRate != null && price.isNotBlank()) {
                        val converted = price.toDoubleOrNull()?.let { it * exchangeRate!! }
                        if (converted != null) {
                            Text(
                                text = "${strings.approx} $globalCurrency ${formatPrice(converted.toLong().toString(), globalCurrency)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditMode) {
                Button(onClick = { if (name.isNotBlank()) onConfirm(name, currentCategory, quantity, price, selectedCurrency) }) {
                    Text(if (initialItem == null) strings.add else strings.save)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text(if (isEditMode && initialItem != null) strings.cancel else if (initialItem == null) strings.cancel else strings.close)
            }
        }
    )
}

fun formatPrice(price: String, currency: String): String {
    if (price.isEmpty()) return ""
    val reversed = price.filter { it.isDigit() }.reversed()
    val sb = StringBuilder()
    for (i in reversed.indices) {
        if (i > 0 && i % 3 == 0) {
            if (currency == "COP" && i == 6) sb.append('\'') 
            else sb.append('.')
        }
        sb.append(reversed[i])
    }
    return sb.reverse().toString()
}

class PriceVisualTransformation(val currency: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        
        val formattedText = formatPrice(originalText, currency)
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val textBeforeOffset = originalText.substring(0, offset.coerceAtMost(originalText.length))
                return formatPrice(textBeforeOffset, currency).length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val transformedBeforeOffset = formattedText.substring(0, offset.coerceAtMost(formattedText.length))
                return transformedBeforeOffset.count { it.isDigit() }
            }
        }
        
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
