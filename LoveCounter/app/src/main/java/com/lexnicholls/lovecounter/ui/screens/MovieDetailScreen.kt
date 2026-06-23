package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.CinemaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    userId: String,
    movieId: String,
    mediaType: String,
    onBack: () -> Unit,
    viewModel: CinemaViewModel = hiltViewModel()
) {
    val strings = t()
    val movie by viewModel.selectedMovie
    val isLoading by viewModel.isLoading
    val context = LocalContext.current
    val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()) }

    LaunchedEffect(movieId) {
        viewModel.getMovieDetails(userId, movieId, mediaType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movie?.title ?: strings.details) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    val isWatched = movie?.watchState == com.lexnicholls.lovecounter.domain.model.WatchState.WATCHED
                    IconButton(onClick = {
                        movie?.let {
                            val newState = if (isWatched) 
                                com.lexnicholls.lovecounter.domain.model.WatchState.IN_WATCHLIST 
                            else 
                                com.lexnicholls.lovecounter.domain.model.WatchState.WATCHED
                            viewModel.updateWatchState(userId, it.id, newState)
                        }
                    }) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isWatched) Color.Green else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LovePink)
            }
        } else if (movie != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Movie Poster
                val isWatched = movie?.watchState == com.lexnicholls.lovecounter.domain.model.WatchState.WATCHED
                Card(
                    modifier = Modifier
                        .size(200.dp, 300.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (movie!!.posterUrl != null) {
                            AsyncImage(
                                model = movie!!.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Movie, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            }
                        }

                        if (isWatched && movie!!.watchedDate != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.markedAsWatchedOn.format(dateFormatter.format(java.util.Date(movie!!.watchedDate!!))),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (movie!!.mediaType == "tv") {
                        // Series: episodes and seasons
                        val episodesText = "${movie!!.episodeCount ?: "?"} ${strings.episodes}"
                        val seasonsText = "${movie!!.seasonCount ?: "?"} ${strings.seasons}"
                        
                        InfoChip(icon = Icons.AutoMirrored.Filled.List, text = episodesText)
                        if (movie!!.rating > 0) {
                            InfoChip(icon = Icons.Default.Star, text = movie!!.rating.toString(), color = Color(0xFFFFD700))
                        }
                        InfoChip(icon = Icons.Default.Repeat, text = seasonsText)
                    } else {
                        // Movies: duration
                        val durationText = movie!!.duration?.let { 
                            val h = it / 60
                            val m = it % 60
                            if (h > 0) "${h}h ${m}m" else "${m}m"
                        } ?: strings.notSelected
                        
                        InfoChip(icon = Icons.Default.AccessTime, text = durationText)
                        if (movie!!.rating > 0) {
                            InfoChip(icon = Icons.Default.Star, text = movie!!.rating.toString(), color = Color(0xFFFFD700))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Streaming Platforms
                val distinctPlatforms: List<com.lexnicholls.lovecounter.domain.model.MeldPlatform> = remember(movie?.platforms) {
                    movie?.platforms?.let { list ->
                        val seenBases = mutableSetOf<String>()
                        list.filter { platform ->
                            val name = platform.name.lowercase()
                            val baseName = when {
                                name.contains("netflix") -> "netflix"
                                name.contains("crunchyroll") -> "crunchyroll"
                                name.contains("disney") -> "disney"
                                name.contains("hbo") || name.contains("max") -> "hbo"
                                name.contains("prime video") || name.contains("amazon") -> "prime"
                                name.contains("apple tv") -> "apple"
                                name.contains("hulu") -> "hulu"
                                name.contains("youtube") -> "youtube"
                                else -> name.trim()
                            }
                            if (seenBases.contains(baseName)) {
                                false
                            } else {
                                seenBases.add(baseName)
                                true
                            }
                        }
                    } ?: emptyList()
                }

                if (distinctPlatforms.isNotEmpty()) {
                    Text(
                        text = "Available on",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        distinctPlatforms.forEach { platform ->
                            AsyncImage(
                                model = platform.logoUrl,
                                contentDescription = platform.name,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { 
                                        openPlatform(context, platform.url, platform.deepLink, platform.name)
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Added By (If from watchlist)
                if (movie!!.addedBy.isNotBlank()) {
                    Surface(
                        color = LovePink.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = LovePink)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${strings.addedBy}: ${movie!!.addedBy}",
                                fontSize = 12.sp,
                                color = LovePink,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Overview
                Text(
                    text = strings.description,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (movie!!.overview.isNotBlank()) movie!!.overview else "No description available.",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontStyle = if (movie!!.overview.isBlank()) FontStyle.Italic else FontStyle.Normal
                )
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error loading details", color = Color.Gray)
            }
        }
    }
}

private fun openPlatform(context: android.content.Context, url: String?, deepLink: String?, platformName: String) {
    val name = platformName.lowercase()
    android.util.Log.d("CinemaDeepLink", "Opening platform: $platformName, Target: $name")
    
    val targetPackage = when {
        name.contains("netflix") -> "com.netflix.mediaclient"
        name.contains("hulu") -> "com.hulu.plus"
        name.contains("crunchyroll") -> "com.crunchyroll.crunchyroid"
        name.contains("disney") -> "com.disney.disneyplus"
        name.contains("hbo") || name.contains("max") -> {
            when {
                isPackageInstalled(context, "com.wbd.max.android") -> "com.wbd.max.android"
                isPackageInstalled(context, "com.wbd.hbomax") -> "com.wbd.hbomax"
                else -> "com.hbo.hbonow"
            }
        }
        name.contains("prime video") || name.contains("amazon") -> "com.amazon.avod.thirdpartyclient"
        name.contains("apple tv") -> "com.apple.atve.android.app.internal"
        name.contains("youtube") -> "com.google.android.youtube"
        else -> null
    }

    if (targetPackage != null) {
        if (isPackageInstalled(context, targetPackage)) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                openPlayStore(context, targetPackage)
            }
        } else {
            openPlayStore(context, targetPackage)
        }
    } else if (!url.isNullOrBlank()) {
        // Fallback for unknown platforms that still have a URL
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("CinemaDeepLink", "Failed to open URL: $url", e)
        }
    }
}

private fun isPackageInstalled(context: android.content.Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

private fun openPlayStore(context: android.content.Context, packageName: String) {
    try {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName")))
    } catch (e: Exception) {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(Modifier.width(8.dp))
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
