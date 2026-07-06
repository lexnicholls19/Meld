package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.domain.model.MeldMovie
import com.lexnicholls.lovecounter.domain.model.WatchState
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.ui.theme.MoviesColor
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.CinemaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DisplayMode {
    COMPACT, COMFORTABLE, COVER_ONLY, LIST
}

data class MovieCategoryConfig(
    val name: String = "",
    val type: String = "both" // "movie", "tv", "both"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoviesListScreen(
    userName: String,
    userId: String,
    showAddDialog: Boolean,
    showDeleteDialog: Boolean,
    onDismissDialog: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onMovieClick: (id: String, type: String) -> Unit,
    onSelectionChange: (Boolean) -> Unit,
    viewModel: CinemaViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val db = FirebaseFirestore.getInstance()
    val strings = t()
    val scope = rememberCoroutineScope()
    var watchlist by remember { mutableStateOf<List<MeldMovie>>(emptyList()) }
    var tabs by remember { mutableStateOf(listOf(
        MovieCategoryConfig(strings.films, "movie"),
        MovieCategoryConfig(strings.series, "tv")
    )) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Display Preferences
    var displayMode by rememberSaveable { mutableStateOf(DisplayMode.COMPACT) }
    var itemsPerRow by rememberSaveable { mutableFloatStateOf(3f) }
    
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    val randomTrigger by viewModel.randomTrigger
    LaunchedEffect(randomTrigger) {
        if (randomTrigger > 0) {
            delay(500)
            val currentCategoryPage = tabs[pagerState.currentPage].name
            val filteredList = watchlist.filter { it.category == currentCategoryPage }
            if (filteredList.isNotEmpty()) {
                val randomMovie = filteredList.random()
                viewModel.clearRandomTrigger()
                onMovieClick(randomMovie.id, randomMovie.mediaType)
            }
        }
    }

    LaunchedEffect(isSelectionMode) {
        onSelectionChange(isSelectionMode)
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.clearResults()
    }

    DisposableEffect(userId) {
        val registration = db.collection("users").document(userId).collection("movies")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    watchlist = snapshot.documents.mapNotNull { doc ->
                        val movie = doc.toObject(MeldMovie::class.java)
                        movie?.copy(category = movie.category.ifBlank { movie.mediaType })
                    }
                }
            }

        val categoriesDoc = db.collection("users").document(userId).collection("settings").document("movie_categories")
        val categoriesRegistration = categoriesDoc.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val rawList = snapshot["list"] as? List<*>
                if (rawList != null) {
                    val newTabs = mutableListOf<MovieCategoryConfig>()
                    rawList.forEach { item ->
                        when (item) {
                            is Map<*, *> -> {
                                newTabs.add(
                                    MovieCategoryConfig(
                                        name = item["name"] as? String ?: "",
                                        type = item["type"] as? String ?: "both"
                                    )
                                )
                            }
                            is String -> {
                                newTabs.add(
                                    MovieCategoryConfig(
                                        name = item,
                                        type = if (item == strings.films) "movie" else if (item == strings.series) "tv" else "both"
                                    )
                                )
                            }
                        }
                    }
                    if (newTabs.isNotEmpty()) {
                        tabs = newTabs
                    } else {
                        tabs = listOf(
                            MovieCategoryConfig(strings.films, "movie"),
                            MovieCategoryConfig(strings.series, "tv")
                        )
                    }
                }
            }
        }

        onDispose { 
            registration.remove() 
            categoriesRegistration.remove()
        }
    }

    val currentCategory = if (pagerState.currentPage < tabs.size) tabs[pagerState.currentPage] else MovieCategoryConfig()
    var searchMediaType by remember(pagerState.currentPage) { 
        mutableStateOf(if (currentCategory.type == "both") "movie" else currentCategory.type)
    }

    if (showAddDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val searchResults by viewModel.searchResults
        val isLoading by viewModel.isLoading

        LoveAlertDialog(
            onDismissRequest = {
                viewModel.clearResults()
                onDismissDialog()
            },
            title = strings.addNew,
            showDismissButton = true,
            onConfirm = {
                viewModel.clearResults()
                onDismissDialog()
            }
        ) {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                LoveTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchMovies(it, searchMediaType)
                    },
                    label = strings.movieTitle,
                    placeholder = "Search..."
                )
                
                if (currentCategory.type == "both") {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.type + ": ", fontSize = 12.sp, color = Color.Gray)
                        FilterChip(
                            selected = searchMediaType == "movie",
                            onClick = { 
                                searchMediaType = "movie"
                                if (searchQuery.isNotBlank()) viewModel.searchMovies(searchQuery, "movie")
                            },
                            label = { Text(strings.films) }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = searchMediaType == "tv",
                            onClick = { 
                                searchMediaType = "tv"
                                if (searchQuery.isNotBlank()) viewModel.searchMovies(searchQuery, "tv")
                            },
                            label = { Text(strings.series) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LovePink)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchResults.size) { index ->
                            val movie = searchResults[index]
                            MovieSearchRow(
                                movie = movie,
                                onAdd = {
                                    viewModel.addMovieToWatchlist(userId, movie.copy(category = currentCategory.name), userName)
                                    viewModel.clearResults()
                                    onDismissDialog()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        LoveAlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = strings.delete,
            onConfirm = {
                selectedIds.forEach { id ->
                    viewModel.removeMovieFromWatchlist(userId, id)
                }
                selectedIds.clear()
                onDismissDeleteDialog()
            }
        ) {
            Text(strings.deleteConfirm)
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            CinemaSettingsContent(
                tabs = tabs,
                onTabsChange = { newTabs ->
                    tabs = newTabs
                    db.collection("users").document(userId).collection("settings")
                        .document("movie_categories")
                        .set(mapOf("list" to newTabs.map { mapOf("name" to it.name, "type" to it.type) }))
                },
                onCategoryRename = { oldName, newName ->
                    val batch = db.batch()
                    watchlist.filter { it.category == oldName }.forEach { movie ->
                        val docRef = db.collection("users").document(userId).collection("movies").document(movie.id)
                        batch.update(docRef, "category", newName)
                    }
                    batch.commit()
                },
                displayMode = displayMode,
                onDisplayModeChange = { displayMode = it },
                itemsPerRow = itemsPerRow,
                onItemsPerRowChange = { itemsPerRow = it },
                onClose = { showSettingsSheet = false }
            )
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.movies, 
                fontSize = 32.sp, 
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { showSettingsSheet = true }) {
                Icon(
                    Icons.Default.Tune, 
                    contentDescription = strings.settings, 
                    tint = MoviesColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Glassy TabRow
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = LovePink,
                divider = {},
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = LovePink
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { 
                            scope.launch { pagerState.animateScrollToPage(index) } 
                        },
                        text = { 
                            Text(
                                text = tabTitle.name,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            val currentCategoryPage = tabs[pageIndex].name
            val filteredList = watchlist.filter { it.category == currentCategoryPage }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MovieFilter, 
                            contentDescription = null, 
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.noItemsYet,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.moviesEmpty,
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (displayMode == DisplayMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList, key = { it.id }) { movie ->
                            val isSelected = selectedIds.contains(movie.id)
                            MovieRow(
                                movie = movie,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedIds.remove(movie.id) else selectedIds.add(movie.id)
                                    } else {
                                        onMovieClick(movie.id, movie.mediaType)
                                    }
                                },
                                onLongClick = { if (!isSelectionMode) selectedIds.add(movie.id) },
                                onDelete = { viewModel.removeMovieFromWatchlist(userId, movie.id) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = if (itemsPerRow == 1f) GridCells.Adaptive(120.dp) else GridCells.Fixed(itemsPerRow.toInt()),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredList, key = { it.id }) { movie ->
                            val isSelected = selectedIds.contains(movie.id)
                            MovieGridItem(
                                movie = movie,
                                isSelected = isSelected,
                                displayMode = displayMode,
                                onClick = { 
                                    if (isSelectionMode) {
                                        if (isSelected) selectedIds.remove(movie.id) else selectedIds.add(movie.id)
                                    } else {
                                        onMovieClick(movie.id, movie.mediaType) 
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) selectedIds.add(movie.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CinemaSettingsContent(
    tabs: List<MovieCategoryConfig>,
    onTabsChange: (List<MovieCategoryConfig>) -> Unit,
    onCategoryRename: (String, String) -> Unit,
    displayMode: DisplayMode,
    onDisplayModeChange: (DisplayMode) -> Unit,
    itemsPerRow: Float,
    onItemsPerRowChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val strings = t()
    var selectedSection by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = Color.Transparent,
            contentColor = LovePink,
            divider = {}
        ) {
            Tab(selected = selectedSection == 0, onClick = { selectedSection = 0 }, text = { Text(strings.display) })
            Tab(selected = selectedSection == 1, onClick = { selectedSection = 1 }, text = { Text(strings.manageCategories) })
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedSection == 0) {
            Text(strings.displayMode.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LovePink)
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DisplayModeChip(DisplayMode.COMPACT, strings.compactGrid, displayMode == DisplayMode.COMPACT) { onDisplayModeChange(it) }
                DisplayModeChip(DisplayMode.COMFORTABLE, strings.comfortableGrid, displayMode == DisplayMode.COMFORTABLE) { onDisplayModeChange(it) }
                DisplayModeChip(DisplayMode.COVER_ONLY, strings.coverOnlyGrid, displayMode == DisplayMode.COVER_ONLY) { onDisplayModeChange(it) }
                DisplayModeChip(DisplayMode.LIST, strings.listMode, displayMode == DisplayMode.LIST) { onDisplayModeChange(it) }
            }

            if (displayMode != DisplayMode.LIST) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.itemsPerRow.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LovePink, modifier = Modifier.weight(1f))
                    Surface(color = LovePink.copy(alpha = 0.1f), shape = CircleShape) {
                        Text(
                            text = if (itemsPerRow == 1f) "Auto" else itemsPerRow.toInt().toString(), 
                            fontWeight = FontWeight.ExtraBold, 
                            color = LovePink,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }
                }
                Slider(
                    value = itemsPerRow,
                    onValueChange = onItemsPerRowChange,
                    valueRange = 1f..6f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = LovePink, activeTrackColor = LovePink)
                )
            }
        } else {
            CategoryManagementList(tabs, onTabsChange, onCategoryRename)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun DisplayModeChip(mode: DisplayMode, label: String, isSelected: Boolean, onClick: (DisplayMode) -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = { onClick(mode) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LovePink.copy(alpha = 0.2f),
            selectedLabelColor = LovePink,
            selectedLeadingIconColor = LovePink
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = Color.Gray.copy(alpha = 0.2f),
            selectedBorderColor = LovePink.copy(alpha = 0.5f)
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryManagementList(
    tabs: List<MovieCategoryConfig>,
    onTabsChange: (List<MovieCategoryConfig>) -> Unit,
    onCategoryRename: (String, String) -> Unit
) {
    val strings = t()
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf("both") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }
    var editingType by remember { mutableStateOf("both") }

    var showDeleteCategoryConfirm by remember { mutableStateOf<Int?>(null) }

    if (showDeleteCategoryConfirm != null) {
        val index = showDeleteCategoryConfirm!!
        LoveAlertDialog(
            onDismissRequest = { showDeleteCategoryConfirm = null },
            title = strings.delete,
            onConfirm = {
                val newList = tabs.toMutableList()
                newList.removeAt(index)
                onTabsChange(newList)
                showDeleteCategoryConfirm = null
            }
        ) {
            Text(strings.deleteMovieCategoryWarning)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).verticalScroll(rememberScrollState())) {
            tabs.forEachIndexed { index, tab ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (index > 0) {
                                        val newList = tabs.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        onTabsChange(newList)
                                    }
                                }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = {
                                    if (index < tabs.size - 1) {
                                        val newList = tabs.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        onTabsChange(newList)
                                    }
                                }, enabled = index < tabs.size - 1, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            if (editingIndex == index) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                )
                                IconButton(onClick = {
                                    if (editingText.isNotBlank()) {
                                        val exists = tabs.any { it.name == editingText && tabs.indexOf(it) != index }
                                        if (!exists) {
                                            onCategoryRename(tabs[index].name, editingText)
                                            val newList = tabs.toMutableList()
                                            newList[index] = MovieCategoryConfig(editingText, editingType)
                                            onTabsChange(newList)
                                            editingIndex = null
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                                }
                            } else {
                                Text(tab.name, modifier = Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.Bold)
                                IconButton(onClick = { 
                                    editingIndex = index
                                    editingText = tab.name
                                    editingType = tab.type
                                }) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                                }
                                if (tabs.size > 1) {
                                    IconButton(onClick = {
                                        showDeleteCategoryConfirm = index
                                    }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        
                        if (editingIndex == index) {
                            FlowRow(
                                modifier = Modifier.padding(start = 64.dp, top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CategoryTypeChip("movie", strings.films, editingType == "movie") { editingType = it }
                                CategoryTypeChip("tv", strings.series, editingType == "tv") { editingType = it }
                                CategoryTypeChip("both", strings.both, editingType == "both") { editingType = it }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LovePink.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(strings.newCategory, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CategoryTypeChip("movie", strings.films, newCategoryType == "movie") { newCategoryType = it }
                        CategoryTypeChip("tv", strings.series, newCategoryType == "tv") { newCategoryType = it }
                        CategoryTypeChip("both", strings.both, newCategoryType == "both") { newCategoryType = it }
                    }
                    
                    FloatingActionButton(
                        onClick = {
                            if (newCategoryName.isNotBlank() && tabs.none { it.name == newCategoryName }) {
                                onTabsChange(tabs + MovieCategoryConfig(newCategoryName, newCategoryType))
                                newCategoryName = ""
                                newCategoryType = "both"
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = LovePink,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTypeChip(type: String, label: String, isSelected: Boolean, onSelect: (String) -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = { onSelect(type) },
        label = { Text(label, fontSize = 10.sp) },
        modifier = Modifier.height(28.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LovePink.copy(alpha = 0.1f),
            selectedLabelColor = LovePink
        )
    )
}

@Composable
fun MovieGridItem(
    movie: MeldMovie, 
    isSelected: Boolean, 
    displayMode: DisplayMode,
    onClick: () -> Unit, 
    onLongClick: () -> Unit
) {
    val isWatched = movie.watchState == WatchState.WATCHED

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(
                    elevation = if (isSelected) 8.dp else 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = if (isSelected) LovePink else Color.Black,
                    spotColor = if (isSelected) LovePink else Color.Black
                )
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (isSelected) BorderStroke(3.dp, LovePink) else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (movie.posterUrl != null) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    }
                }

                // Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (isWatched) {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (isSelected) {
                        Surface(
                            color = LovePink,
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                        }
                    }
                }

                if (displayMode == DisplayMode.COMPACT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)), 
                                startY = 250f
                            )),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp),
                            lineHeight = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        if (displayMode == DisplayMode.COMFORTABLE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MovieSearchRow(movie: MeldMovie, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(shape = RoundedCornerShape(8.dp)) {
                if (movie.posterUrl != null) {
                    AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.size(60.dp, 90.dp), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.size(60.dp, 90.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = movie.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = movie.releaseYear, fontSize = 13.sp, color = Color.Gray)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.AddCircle, null, tint = LovePink, modifier = Modifier.size(28.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieRow(movie: MeldMovie, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, onDelete: () -> Unit) {
    val isWatched = movie.watchState == WatchState.WATCHED
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) LovePink.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = if (isSelected) BorderStroke(2.dp, LovePink) else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(shape = RoundedCornerShape(8.dp)) {
                if (movie.posterUrl != null) {
                    AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.size(50.dp, 75.dp), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.size(50.dp, 75.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = movie.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (isWatched) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (movie.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD700))
                        Spacer(Modifier.width(4.dp))
                        Text(text = movie.rating.toString(), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = LovePink)
            } else {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
