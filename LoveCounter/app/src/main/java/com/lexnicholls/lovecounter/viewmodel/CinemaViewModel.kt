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
                // Si el idioma de la sinopsis guardada no coincide con el actual de la app, forzamos actualización desde red
                if (movie != null && movie.language == userLang && movie.overview.isNotBlank()) {
                    _selectedMovie.value = movie
                } else {
                    repository.getMovieDetail(movieId, type, userRegion, userLang)
                        .onSuccess { 
                            val updatedMovie = it.copy(addedBy = movie?.addedBy ?: "", language = userLang)
                            _selectedMovie.value = updatedMovie
                            // Opcional: Actualizar Firestore con la nueva traducción
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
}
