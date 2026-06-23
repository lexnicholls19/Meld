package com.lexnicholls.lovecounter.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.domain.model.MeldMovie
import com.lexnicholls.lovecounter.domain.repository.CinemaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CinemaViewModel @Inject constructor(
    private val repository: CinemaRepository,
    private val db: FirebaseFirestore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _searchResults = mutableStateOf<List<MeldMovie>>(emptyList())
    val searchResults: State<List<MeldMovie>> = _searchResults

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _selectedMovie = mutableStateOf<MeldMovie?>(null)
    val selectedMovie: State<MeldMovie?> = _selectedMovie

    private val userRegion = java.util.Locale.getDefault().country
    
    private val userLang: String
        get() {
            val prefs = context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
            val lang = prefs.getString("app_language", "system") ?: "system"
            return if (lang == "system") java.util.Locale.getDefault().language else lang
        }

    fun clearResults() {
        _searchResults.value = emptyList()
        _error.value = null
    }

    fun getMovieDetails(userId: String, movieId: String, type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Primero intentamos buscar en Firestore (nuestra watchlist)
            val doc = db.collection("users").document(userId).collection("movies").document(movieId).get().await()
            if (doc.exists()) {
                val movie = doc.toObject(MeldMovie::class.java)
                val hasPlatformUrls = movie?.platforms?.any { !it.url.isNullOrBlank() } ?: false
                val hasDeepLinks = movie?.platforms?.any { !it.deepLink.isNullOrBlank() } ?: false
                
                // Nueva verificación: ¿Tenemos la información detallada (duración/episodios)?
                val hasDetailedInfo = if (movie?.mediaType == "tv") {
                    movie.episodeCount != null && movie.seasonCount != null
                } else {
                    movie?.duration != null
                }

                // Si el idioma no coincide o faltan datos clave, actualizamos desde red
                if (movie != null && movie.language == userLang && movie.overview.isNotBlank() && 
                    hasPlatformUrls && hasDeepLinks && hasDetailedInfo) {
                    _selectedMovie.value = movie
                } else {
                    repository.getMovieDetail(movieId, type, userRegion, userLang)
                        .onSuccess { 
                            val updatedMovie = it.copy(
                                addedBy = movie?.addedBy ?: "", 
                                category = movie?.category ?: "", // Preservar la categoría (ej: k drama, crunchyroll)
                                language = userLang,
                                watchState = movie?.watchState ?: com.lexnicholls.lovecounter.domain.model.WatchState.IN_WATCHLIST,
                                watchedDate = movie?.watchedDate
                            )
                            _selectedMovie.value = updatedMovie
                            // Actualizar Firestore con la nueva traducción y plataformas, manteniendo el estado de visto
                            db.collection("users").document(userId).collection("movies").document(movieId).set(updatedMovie)
                        }
                        .onFailure { _error.value = it.message }
                }
            } else {
                repository.getMovieDetail(movieId, type, userRegion, userLang)
                    .onSuccess { _selectedMovie.value = it.copy(language = userLang) }
                    .onFailure { _error.value = it.message }
            }
            _isLoading.value = false
        }
    }

    fun searchMovies(query: String, type: String? = null) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.searchMovies(query, type, userRegion, userLang)
                .onSuccess { results -> 
                    _searchResults.value = results.map { it.copy(language = userLang) }
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addMovieToWatchlist(userId: String, movie: MeldMovie, userName: String) {
        val movieData = movie.copy(
            addedBy = userName,
            timestamp = System.currentTimeMillis(),
            language = userLang
        )
        db.collection("users").document(userId).collection("movies")
            .document(movie.id)
            .set(movieData)
    }

    fun removeMovieFromWatchlist(userId: String, movieId: String) {
        db.collection("users").document(userId).collection("movies")
            .document(movieId)
            .delete()
    }

    fun updateWatchState(userId: String, movieId: String, newState: com.lexnicholls.lovecounter.domain.model.WatchState) {
        val watchedDate = if (newState == com.lexnicholls.lovecounter.domain.model.WatchState.WATCHED) System.currentTimeMillis() else null
        val updates = mapOf(
            "watchState" to newState,
            "watchedDate" to watchedDate
        )
        db.collection("users").document(userId).collection("movies")
            .document(movieId)
            .update(updates)
            .addOnSuccessListener {
                if (_selectedMovie.value?.id == movieId) {
                    _selectedMovie.value = _selectedMovie.value?.copy(
                        watchState = newState,
                        watchedDate = watchedDate
                    )
                }
            }
    }

    fun removeMoviesByCategory(userId: String, categoryName: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId).collection("movies")
                    .whereEqualTo("category", categoryName)
                    .get().await()
                
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
