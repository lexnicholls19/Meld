package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.domain.model.MeldMovie
import com.lexnicholls.lovecounter.ui.components.LoveAlertDialog
import com.lexnicholls.lovecounter.ui.components.LoveTextField
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.CinemaViewModel

enum class DisplayMode {
    COMPACT, COMFORTABLE, COVER_ONLY, LIST
}

@OptIn(ExperimentalMaterial3Api::class)
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
    viewModel: CinemaViewModel = hiltViewModel()
) {
    val db = FirebaseFirestore.getInstance()
    val strings = t()
    var watchlist by remember { mutableStateOf<List<MeldMovie>>(emptyList()) }
    var tabs by remember { mutableStateOf(listOf(strings.films, strings.series)) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Display Preferences
    var displayMode by rememberSaveable { mutableStateOf(DisplayMode.COMPACT) }
    var itemsPerRow by rememberSaveable { mutableFloatStateOf(3f) }
    
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
    
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { tabs.size })

    LaunchedEffect(isSelectionMode) {
        onSelectionChange(isSelectionMode)
    }

    LaunchedEffect(selectedTab) {
        viewModel.clearResults()
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
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
                val list = snapshot["list"] as? List<String>
                if (!list.isNullOrEmpty()) {
                    tabs = list
                }
            }
        }

        onDispose { 
            registration.remove() 
            categoriesRegistration.remove()
        }
    }

    val currentCategory = if (selectedTab < tabs.size) tabs[selectedTab] else ""
    var searchMediaType by remember(selectedTab) { 
        mutableStateOf(if (currentCategory == strings.series) "tv" else "movie")
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
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                LoveTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchMovies(it, searchMediaType)
                    },
                    label = strings.movieTitle,
                    placeholder = "Search..."
                )
                
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

                Spacer(Modifier.height(8.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchResults.size) { index ->
                            val movie = searchResults[index]
                            MovieSearchRow(
                                movie = movie,
                                onAdd = {
                                    viewModel.addMovieToWatchlist(userId, movie.copy(category = currentCategory), userName)
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
                        .set(mapOf("list" to newTabs))
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
        .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = strings.movies, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showSettingsSheet = true }) {
                Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            tabs.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tabTitle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            val currentCategoryPage = tabs[pageIndex]
            val filteredList = watchlist.filter { it.category == currentCategoryPage }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.noItemsYet, 
                        color = Color.Gray, 
                        modifier = Modifier.fillMaxWidth(), 
                        textAlign = TextAlign.Center
                    )
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
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedIds.remove(movie.id) else selectedIds.add(movie.id)
                                    } else {
                                        onMovieClick(movie.id, movie.mediaType)
                                    }
                                },
                                onDelete = { viewModel.removeMovieFromWatchlist(userId, movie.id) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = if (itemsPerRow == 1f) GridCells.Adaptive(120.dp) else GridCells.Fixed(itemsPerRow.toInt()),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
    tabs: List<String>,
    onTabsChange: (List<String>) -> Unit,
    onCategoryRename: (String, String) -> Unit,
    displayMode: DisplayMode,
    onDisplayModeChange: (DisplayMode) -> Unit,
    itemsPerRow: Float,
    onItemsPerRowChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val strings = t()
    var selectedSection by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(selected = selectedSection == 0, onClick = { selectedSection = 0 }, text = { Text(strings.display) })
            Tab(selected = selectedSection == 1, onClick = { selectedSection = 1 }, text = { Text(strings.manageCategories) })
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedSection == 0) {
            Text(strings.displayMode, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
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
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.itemsPerRow, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        text = if (itemsPerRow == 1f) "Auto" else itemsPerRow.toInt().toString(), 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = itemsPerRow,
                    onValueChange = onItemsPerRowChange,
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        } else {
            CategoryManagementList(tabs, onTabsChange, onCategoryRename)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DisplayModeChip(mode: DisplayMode, label: String, isSelected: Boolean, onClick: (DisplayMode) -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = { onClick(mode) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun CategoryManagementList(
    tabs: List<String>,
    onTabsChange: (List<String>) -> Unit,
    onCategoryRename: (String, String) -> Unit
) {
    val strings = t()
    var newCategoryName by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).verticalScroll(rememberScrollState())) {
            tabs.forEachIndexed { index, tab ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (index > 0) {
                                val newList = tabs.toMutableList()
                                val item = newList.removeAt(index)
                                newList.add(index - 1, item)
                                onTabsChange(newList)
                            }
                        }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            if (index < tabs.size - 1) {
                                val newList = tabs.toMutableList()
                                val item = newList.removeAt(index)
                                newList.add(index + 1, item)
                                onTabsChange(newList)
                            }
                        }, enabled = index < tabs.size - 1, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    
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
                                        onCategoryRename(tabs[index], editingText)
                                        val newList = tabs.toMutableList()
                                        newList[index] = editingText
                                        onTabsChange(newList)
                                        editingIndex = null
                                    }
                                }) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    } else {
                        Text(tab, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        IconButton(onClick = { editingIndex = index; editingText = tab }) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                        }
                        if (tabs.size > 1) {
                            IconButton(onClick = {
                                val newList = tabs.toMutableList()
                                newList.removeAt(index)
                                onTabsChange(newList)
                            }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text(strings.newCategory) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            IconButton(onClick = {
                if (newCategoryName.isNotBlank() && !tabs.contains(newCategoryName)) {
                    onTabsChange(tabs + newCategoryName)
                    newCategoryName = ""
                }
            }) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MovieGridItem(
    movie: MeldMovie, 
    isSelected: Boolean, 
    displayMode: DisplayMode,
    onClick: () -> Unit, 
    onLongClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
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

                if (isSelected) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), contentAlignment = Alignment.TopEnd) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(4.dp))
                    }
                }

                if (displayMode == DisplayMode.COMPACT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 300f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp),
                            lineHeight = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        if (displayMode == DisplayMode.COMFORTABLE) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = movie.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun MovieSearchRow(movie: MeldMovie, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (movie.posterUrl != null) {
                AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.size(60.dp, 90.dp), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(60.dp, 90.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = movie.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = movie.releaseYear, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun MovieRow(movie: MeldMovie, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (movie.posterUrl != null) {
                AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.size(50.dp, 75.dp), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(50.dp, 75.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Color.Gray) }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = movie.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (movie.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD700))
                        Spacer(Modifier.width(4.dp))
                        Text(text = movie.rating.toString(), fontSize = 12.sp)
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}
