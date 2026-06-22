package com.lexnicholls.lovecounter.domain.repository

import com.lexnicholls.lovecounter.domain.model.MeldMovie

interface CinemaRepository {
    suspend fun getTrendingMovies(region: String, lang: String): Result<List<MeldMovie>>
    suspend fun searchMovies(query: String, type: String? = null, region: String, lang: String): Result<List<MeldMovie>>
    suspend fun getMovieDetail(movieId: String, type: String, region: String, lang: String): Result<MeldMovie>
}
