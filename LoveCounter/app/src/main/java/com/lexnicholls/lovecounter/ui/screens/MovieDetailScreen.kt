package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val dateFormatter = remember { java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault()) }

    LaunchedEffect(movieId) {
        viewModel.getMovieDetails(userId, movieId, mediaType)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop (Blurred Poster)
        movie?.posterUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .blur(50.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LovePink)
            }
        } else if (movie != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row: Title aligned Left + Watched Toggle Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = movie!!.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
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
                            if (isWatched) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isWatched) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Movie Poster with high quality styling
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .aspectRatio(0.68f)
                        .shadow(24.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
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
                            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Movie, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Metadata Info Row (Year + Specs)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = movie!!.releaseYear,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (movie!!.mediaType == "tv") {
                            DetailInfoItem(Icons.AutoMirrored.Filled.FormatListBulleted, "${movie!!.episodeCount ?: "?"} ep")
                            DetailDivider()
                            DetailInfoItem(Icons.Default.Repeat, "${movie!!.seasonCount ?: "?"} se")
                        } else {
                            val durationText = movie!!.duration?.let { 
                                val h = it / 60
                                val m = it % 60
                                if (h > 0) "${h}h ${m}m" else "${m}m"
                            } ?: "?"
                            DetailInfoItem(Icons.Default.AccessTime, durationText)
                        }
                        
                        if (movie!!.rating > 0) {
                            DetailDivider()
                            DetailInfoItem(Icons.Default.Star, movie!!.rating.toString(), Color(0xFFFFD700))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Watch Status Section
                val isWatched = movie?.watchState == com.lexnicholls.lovecounter.domain.model.WatchState.WATCHED
                if (isWatched && movie!!.watchedDate != null) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = strings.markedAsWatchedOn.format(dateFormatter.format(java.util.Date(movie!!.watchedDate!!))),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Added By
                if (movie!!.addedBy.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = LovePink.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(6.dp), tint = LovePink)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "${strings.addedBy}: ",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = movie!!.addedBy,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LovePink
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Platforms
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
                            if (seenBases.contains(baseName)) false else { seenBases.add(baseName); true }
                        }
                    } ?: emptyList()
                }

                if (distinctPlatforms.isNotEmpty()) {
                    Text(
                        text = "Available on",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(distinctPlatforms) { platform ->
                            Card(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clickable { openPlatform(context, platform.url, platform.deepLink, platform.name) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                AsyncImage(
                                    model = platform.logoUrl,
                                    contentDescription = platform.name,
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Overview
                Text(
                    text = strings.description,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (movie!!.overview.isNotBlank()) movie!!.overview else "No description available.",
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontStyle = if (movie!!.overview.isBlank()) FontStyle.Italic else FontStyle.Normal,
                    textAlign = TextAlign.Justify
                )
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error loading details", color = Color.Gray)
            }
        }
    }
}

@Composable
fun DetailDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .size(4.dp)
            .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
    )
}

@Composable
fun DetailInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = Color.Gray) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun openPlatform(context: android.content.Context, url: String?, deepLink: String?, platformName: String) {
    val name = platformName.lowercase()
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
