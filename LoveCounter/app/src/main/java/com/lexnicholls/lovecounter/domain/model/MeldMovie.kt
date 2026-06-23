package com.lexnicholls.lovecounter.domain.model

data class MeldMovie(
    val id: String = "",
    val title: String = "",
    val overview: String = "",
    val posterUrl: String? = null,
    val releaseYear: String = "",
    val rating: Double = 0.0,
    val mediaType: String = "movie", // "movie" or "tv"
    val category: String = "",
    val platforms: List<MeldPlatform> = emptyList(),
    val watchState: WatchState = WatchState.UNWATCHED,
    val watchedDate: Long? = null,
    val addedBy: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "",
    val duration: Int? = null, // In minutes for movies
    val episodeCount: Int? = null, // For TV
    val seasonCount: Int? = null // For TV
)

data class MeldPlatform(
    val name: String = "",
    val logoUrl: String = "",
    val url: String? = null,
    val deepLink: String? = null
)

enum class WatchState {
    UNWATCHED,
    IN_WATCHLIST,
    WATCHED
}
