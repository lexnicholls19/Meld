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
    val addedBy: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = ""
)

data class MeldPlatform(
    val name: String = "",
    val logoUrl: String = ""
)

enum class WatchState {
    UNWATCHED,
    IN_WATCHLIST,
    WATCHED
}
